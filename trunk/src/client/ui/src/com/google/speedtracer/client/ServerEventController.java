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

import com.google.gwt.coreext.client.JSON;
import com.google.gwt.coreext.client.JsStringBooleanMap;
import com.google.gwt.xhr.client.XMLHttpRequest;
import com.google.speedtracer.client.model.NetworkResource;
import com.google.speedtracer.client.model.ServerEvent;
import com.google.speedtracer.client.util.Xhr;

/**
 * Controls fetching of {@link ServerEvent}s from the origin server and probes
 * for the existence of valid traces.
 */
public class ServerEventController {

  /**
   * A callback interface for querying the validity of a trace url.
   * 
   * @see ServerEventController#serverHasValidTrace(NetworkResource,
   *      HasTraceCallback)
   */
  public interface HasTraceCallback {
    void onResponse(boolean hasTrace);
  }

  /**
   * A callback interface for requesting a server trace.
   * 
   * @see ServerEventController#requestTraceFor(NetworkResource,
   *      RequestTraceCallback)
   */
  public interface RequestTraceCallback {
    void onFailure();

    void onSuccess(ServerEvent event);
  }

  private static class HasTraceHandler implements Xhr.XhrCallback {
    private final HasTraceCallback callback;
    private final String traceUrl;
    private final JsStringBooleanMap traceValidityByUrl;

    private HasTraceHandler(HasTraceCallback callback, String traceUrl,
        JsStringBooleanMap traceValidityByUrl) {
      this.callback = callback;
      this.traceUrl = traceUrl;
      this.traceValidityByUrl = traceValidityByUrl;
    }

    public void onFail(XMLHttpRequest xhr) {
      respond(false);
    }

    public void onSuccess(XMLHttpRequest xhr) {
      respond(true);
    }

    private void respond(boolean hasTrace) {
      traceValidityByUrl.put(traceUrl, hasTrace);
      callback.onResponse(hasTrace);
    }
  }

  private static class TraceRequestHandler implements Xhr.XhrCallback {
    private final RequestTraceCallback callback;
    private final NetworkResource resource;

    private TraceRequestHandler(RequestTraceCallback callback,
        NetworkResource resource) {
      this.callback = callback;
      this.resource = resource;
    }

    public void onFail(XMLHttpRequest xhr) {
      callback.onFailure();
    }

    public void onSuccess(XMLHttpRequest xhr) {
      final ServerEvent event = ServerEvent.fromSpringInsightTrace(resource,
          JSON.parse(xhr.getResponseText()));
      callback.onSuccess(event);
    }
  }

  private final JsStringBooleanMap traceValidityByUrl = JsStringBooleanMap.create();

  /**
   * Requests the {@link ServerEvent} associated with the <code>resource</code>.
   * 
   * @param resource
   * @param callback
   */
  public void requestTraceFor(NetworkResource resource,
      RequestTraceCallback callback) {
    Xhr.get(resource.getServerTraceUrl(), new TraceRequestHandler(callback,
        resource));
  }

  /**
   * Checks the validity of a trace URL by issuing a lightweight request to the
   * origin server.
   * 
   * NOTE: Responses may be dispatched immediately if the validity is already
   * known. Callers should not depend on the response being deferred.
   * 
   * @param resource
   * @param callback
   */
  public void serverHasValidTrace(NetworkResource resource,
      HasTraceCallback callback) {
    final String traceUrl = resource.getServerTraceUrl();
    if (traceValidityByUrl.hasKey(traceUrl)) {
      callback.onResponse(traceValidityByUrl.get(traceUrl));
    } else {
      Xhr.head(traceUrl, new HasTraceHandler(callback, traceUrl,
          traceValidityByUrl));
    }
  }
}
