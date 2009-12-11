/*
 * Copyright 2008 Google Inc.
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
package com.google.speedtracer.client.model;

import com.google.speedtracer.client.model.NetworkResource.HeaderMap;

/**
 * Single event record with no duration corresponding to the beginning of the
 * response from the server for a network resource request.
 */
public class NetworkResourceResponse extends NetworkResourceRecord {
  public static final int TYPE = EventRecordType.NETWORK_RESOURCE_RESPONSE;

  static NetworkResourceResponse create(String resourceId, double responseTime,
      String mimeType, int responseCode, boolean isCached,
      HeaderMap responseHeaders, String url) {
    return createImpl(resourceId, TYPE, responseTime, mimeType, responseCode,
        isCached, responseHeaders, url);
  }

  private static native NetworkResourceResponse createImpl(String id, int type,
      double responseTime, String mimeType, int responseCode, boolean isCached,
      HeaderMap responseHeaders, String url) /*-{
    return {
      type: type,
      time: responseTime,
      data: {
        resourceId: id,
        mimeType: mimeType,
        headers: responseHeaders,
        responseCode: responseCode,
        isCached: isCached,
        url: url
      }
    };
  }-*/;

  protected NetworkResourceResponse() {
  }

  public final HeaderMap getHeaders() {
    return getData().getJSObjectProperty("headers").cast();
  }

  public final String getMimeType() {
    return getData().getStringProperty("mimeType");
  }

  public final int getResponseCode() {
    return getData().getIntProperty("responseCode");
  }

  /**
   * This can return null. In the case of a redirect this will be set to the
   * redirected URL.
   */
  public final String getResponseUrl() {
    return getData().getStringProperty("url");
  }

  public final boolean isCached() {
    return getData().getBooleanProperty("isCached");
  }
}
