/*
 * Copyright 2010 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.speedtracer.client;

import com.google.gwt.xhr.client.XMLHttpRequest;
import com.google.speedtracer.client.SymbolServerManifest.ResourceSymbolInfo;
import com.google.speedtracer.client.model.JsSymbolMap;
import com.google.speedtracer.client.util.IterableFastStringMap;
import com.google.speedtracer.client.util.JSON;
import com.google.speedtracer.client.util.Url;
import com.google.speedtracer.client.util.Xhr;
import com.google.speedtracer.client.util.Xhr.XhrCallback;

import java.util.ArrayList;
import java.util.List;

/**
 * Class resolving the symbol map file and original source files for
 * re-symbolization.
 */
public class SymbolServerController {
  /**
   * Listener interface for getting notified when a symbols or source are
   * available.
   */
  public interface Callback {
    /**
     * Called if the request for the symbols failed.
     */
    void onSymbolsFetchFailed(int errorReason);

    /**
     * Called when the symbols for a resource have been loaded.
     */
    void onSymbolsReady(JsSymbolMap symbols);
  }

  /**
   * A request that has been queued for servicing.
   */
  private class PendingRequest {
    final Callback callback;
    final String resourceUrl;

    PendingRequest(String resourceUrl, Callback callback) {
      this.resourceUrl = resourceUrl;
      this.callback = callback;
    }
  }

  public static final int ERROR_MANIFEST_NOT_LOADED = 0;

  public static final int ERROR_SYMBOL_FETCH_FAIL = 1;

  private static IterableFastStringMap<JsSymbolMap> resourceSymbols = new IterableFastStringMap<JsSymbolMap>();

  /**
   * Retrieve a Symbol Map for a particular resource URL.
   * 
   * @param resource The resource we want the symbol mapping for.
   * @return The symbol map.
   */
  public static JsSymbolMap get(String resource) {
    return resourceSymbols.get(resource);
  }

  /**
   * Inserts a symbol map into the set of available symbol maps, keyed by
   * resource URL.
   * 
   * @param resource The resource we want the symbol mapping for.
   * @return The symbol map.
   */
  static void put(String resource, JsSymbolMap symbols) {
    resourceSymbols.put(resource, symbols);
  }

  private final Url mainResourceUrl;

  private boolean manifestLoaded = false;

  private final List<PendingRequest> pendingRequests;

  private final Url symbolManifestUrl;

  private SymbolServerManifest symbolServerManifest;

  SymbolServerController(Url mainResourceUrl, String symbolManifestUrl) {
    this.mainResourceUrl = mainResourceUrl;
    this.symbolManifestUrl = new Url(symbolManifestUrl);
    this.pendingRequests = new ArrayList<PendingRequest>();
    // Start xhr for fetching our associated symbol manifest.
    init();
  }

  /**
   * Cancels all pending requests.
   */
  public void cancelPendingRequests() {
    pendingRequests.clear();
  }

  /**
   * Looks up the {@link JsSymbolMap} associated with a particular resource url
   * and calls the specified callback when it has been fetched.
   * 
   * If the symbol manifest is not loaded, this method will queue the request to
   * be serviced as soon as the manifest loads.
   * 
   * This call may or may not call back synchronously. Do not bank on
   * asynchronous behavior.
   * 
   * @param resourceUrl the resource that we intend to get the symbols for.
   * @param callback the {@link Callback} that gets invoked when the symbols are
   *          loaded.
   */
  public void requestSymbolsFor(final String resourceUrl,
      final Callback callback) {
    PendingRequest request = new PendingRequest(resourceUrl, callback);
    if (!manifestLoaded) {
      // Queue a pending request.
      pendingRequests.add(request);
      return;
    }

    serviceRequest(request);
  }

  private void init() {
    Xhr.get(symbolManifestUrl.getUrl(), new XhrCallback() {

      public void onFail(XMLHttpRequest xhr) {
        // Let pending requests know that the manifest failed to load.
        for (PendingRequest request : pendingRequests) {
          request.callback.onSymbolsFetchFailed(ERROR_MANIFEST_NOT_LOADED);
        }
        cancelPendingRequests();
      }

      public void onSuccess(XMLHttpRequest xhr) {
        SymbolServerController.this.manifestLoaded = true;
        // TODO (jaimeyap): This needs to be validated... and we should handle
        // any parsing errors as well.
        SymbolServerController.this.symbolServerManifest = JSON.parse(
            xhr.getResponseText()).cast();
        // Now service all the pending requests.
        while (!pendingRequests.isEmpty()) {
          serviceRequest(pendingRequests.remove(0));
        }
      }
    });
  }

  private ResourceSymbolInfo lookupEntryInManifest(String resourceUrl) {
    ResourceSymbolInfo resourceSymbolInfo = null;

    // If the resourceUrl begins with a '/' then we assume it is relative to the
    // origin.
    if (resourceUrl.charAt(0) == '/') {
      resourceSymbolInfo = symbolServerManifest.getResourceSymbolInfo(Url.convertToRelativeUrl(
          mainResourceUrl.getOrigin(), resourceUrl));
    } else {
      // First try looking for the resource using the full URL.
      resourceSymbolInfo = symbolServerManifest.getResourceSymbolInfo(resourceUrl);
      // If the lookup was null, then attempt a relative url lookup.
      if (resourceSymbolInfo == null) {
        String relativeUrl = Url.convertToRelativeUrl(
            mainResourceUrl.getResourceBase(), resourceUrl);
        return symbolServerManifest.getResourceSymbolInfo(relativeUrl);
      }
    }
    return resourceSymbolInfo;
  }

  private void serviceRequest(PendingRequest request) {
    assert manifestLoaded;

    final ResourceSymbolInfo resourceSymbolInfo = lookupEntryInManifest(request.resourceUrl);
    final Callback callback = request.callback;
    if (resourceSymbolInfo == null) {
      callback.onSymbolsFetchFailed(ERROR_SYMBOL_FETCH_FAIL);
      return;
    }

    JsSymbolMap symbolMap = get(resourceSymbolInfo.getSymbolMapUrl());
    if (symbolMap == null) {
      // We have not yet parsed it.
      Xhr.get(symbolManifestUrl.getResourceBase()
          + resourceSymbolInfo.getSymbolMapUrl(), new XhrCallback() {

        public void onFail(XMLHttpRequest xhr) {
          callback.onSymbolsFetchFailed(ERROR_SYMBOL_FETCH_FAIL);
        }

        public void onSuccess(XMLHttpRequest xhr) {
          // Double check that another XHR didnt pull it down and parse it.
          JsSymbolMap fetchedSymbolMap = get(resourceSymbolInfo.getSymbolMapUrl());
          if (fetchedSymbolMap == null) {
            fetchedSymbolMap = JsSymbolMap.parse(
                resourceSymbolInfo.getSourceServer(),
                resourceSymbolInfo.getType(), xhr.getResponseText());
            put(resourceSymbolInfo.getSymbolMapUrl(), fetchedSymbolMap);
          }
          callback.onSymbolsReady(fetchedSymbolMap);
        }
      });
    } else {
      // We have already fetched this and parsed it before. Send it to the
      // callback.
      callback.onSymbolsReady(symbolMap);
    }
  }
}
