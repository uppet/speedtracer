/*
 * Copyright 2009 Google Inc.
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
 * Overlay for data payload for NetworkResourceStart events.
 */
public class NetworkResourceStart extends NetworkResourceRecord {
  public static final int TYPE = EventRecordType.NETWORK_RESOURCE_START;

  static NetworkResourceStart create(String resourceId, double startTime,
      String url, String lastPathComponent, String httpMethod,
      HeaderMap requestHeaders) {
    return createImpl(resourceId, TYPE, startTime, url, lastPathComponent,
        httpMethod, requestHeaders);
  }

  private static native NetworkResourceStart createImpl(String id, int type,
      double startTime, String url, String lastPathComponent,
      String httpMethod, HeaderMap requestHeaders) /*-{
    return {
      type: type,
      time: startTime,
      data: {
        resourceId: id,
        httpMethod: httpMethod,
        headers: requestHeaders,
        url: url,
        lastPathComponent: lastPathComponent
      }
    };
  }-*/;

  protected NetworkResourceStart() {
  }

  public final HeaderMap getHeaders() {
    return getData().getJSObjectProperty("headers").cast();
  }

  public final String getHttpMethod() {
    return getData().getStringProperty("httpMethod");
  }

  public final String getLastPathComponent() {
    return getData().getStringProperty("lastPathComponent");
  }

  public final String getUrl() {
    return getData().getStringProperty("url");
  }
}
