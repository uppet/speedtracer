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

import com.google.gwt.core.client.JsArray;
import com.google.gwt.coreext.client.DataBag;
import com.google.gwt.xhr.client.XMLHttpRequest;
import com.google.speedtracer.client.util.Xhr;
import com.google.speedtracer.client.util.Xhr.XhrCallback;

/**
 * Handles Jump To IDE functionality.
 */
public class SourceViewerServer extends DataBag {
  /**
   * Callback interface for determining if a SourceViewerServer is available.
   */
  public interface ActionCompletedCallback {
    void onFail();

    void onSuccess();
  }

  /**
   * The generator of the manifest file may want to pass some optional URL
   * params to the SourceViewerServer. These params are opaque to us, but we are
   * responsible for forwarding them on.
   */
  public static class OpaqueParam extends DataBag {
    protected OpaqueParam() {
    }

    public final String getKey() {
      return getStringProperty("key");
    }

    public final String getValue() {
      return getStringProperty("value");
    }
  }

  private static final int API_VERSION = 1;

  private static final String OPAQUE_PARAM_PREFIX = "_param";

  public static void jumpToIde(SourceViewerServer sourceViewerServer,
      String absoluteFilePath, int lineNumber,
      final ActionCompletedCallback actionCallback) {
    if (null == sourceViewerServer || null == absoluteFilePath) {
      actionCallback.onFail();
      return;
    }

    String url = sourceViewerServer.getUrl() + "?apiVersion=" + API_VERSION
        + "&filePath=" + absoluteFilePath + "&lineNumber=" + lineNumber;
    // Add opaque params.
    JsArray<OpaqueParam> opaqueParams = sourceViewerServer.getOpaqueUrlParams();
    for (int i = 0, n = opaqueParams.length(); i < n; i++) {
      OpaqueParam tuple = opaqueParams.get(i);
      url += "&" + tuple.getKey() + "=" + tuple.getValue();
    }
    // Send out the request.
    Xhr.get(url, new XhrCallback() {
      public void onFail(XMLHttpRequest xhr) {
        actionCallback.onSuccess();
      }

      public void onSuccess(XMLHttpRequest xhr) {
        actionCallback.onFail();
      }
    });
  }

  protected SourceViewerServer() {
  }

  /**
   * Returns the OpaqueParam key-value tuples for this SourceViewerServer.
   */
  public final native JsArray<OpaqueParam> getOpaqueUrlParams() /*-{
    var tuples = [];
    var prefix = @com.google.speedtracer.client.SourceViewerServer::OPAQUE_PARAM_PREFIX;
    for (key in this) {           
      if (key.indexOf(prefix) == 0) {        
        tuples.push({
          key: key.substr(6),
          value: this[key]
        });
      }
    }
    return tuples;
  }-*/;

  /**
   * Returns the URL for this SourceViewerServer.
   */
  public final String getUrl() {
    return getStringProperty("url");
  }
}
