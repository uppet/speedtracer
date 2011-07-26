/*
 * Copyright 2011 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.speedtracer.hintletengine.client;

import com.google.speedtracer.client.model.NetworkResponseReceivedEvent;

/**
 * Builds a NetworkResponseReceivedEvent for testing
 */
public class NetworkResponseReceivedEventBuilder {

  private NetworkResponseReceivedEvent event;

  /**
   * Builds a default skeleton 
   * identifier: 1 
   * timestamp: 1 
   * sequence: 1
   */
  public NetworkResponseReceivedEventBuilder() {
    event = createSkeletonEvent();
  }

  /**
   * Creates builder using the given event
   */
  public NetworkResponseReceivedEventBuilder(NetworkResponseReceivedEvent event) {
    this.event = event;
  }

  /**
   * Creates builder with the given values
   */
  public NetworkResponseReceivedEventBuilder(int identifier, int time, int sequence) {
    event = createSkeletonEvent();
    this.setIdentifier(identifier);
    this.setTime(time);
    this.setSequence(sequence);
  }

  /**
   * Get the current NetworkResponseReceivedEvent
   */
  public NetworkResponseReceivedEvent getEvent() {
    return event;
  }

  /**
   * Build the basic JSON object with required fields set to default values
   */
  private static native NetworkResponseReceivedEvent createSkeletonEvent() /*-{
    return {
        "type" : @com.google.speedtracer.shared.EventRecordType::NETWORK_RESPONSE_RECEIVED,
        "time" : 1,
        "data" : {
            "identifier" : 1,
            "response" : {
                "connectionId" : 0,
                "headers" : {}
            }
        }
    };
  }-*/;

  public native NetworkResponseReceivedEventBuilder setIdentifier(int identifier) /*-{
    var event = this.@com.google.speedtracer.hintletengine.client.NetworkResponseReceivedEventBuilder::event;
    event.data.identifier = identifier;
    return this;
  }-*/;

  public native NetworkResponseReceivedEventBuilder setTime(int time) /*-{
    var event = this.@com.google.speedtracer.hintletengine.client.NetworkResponseReceivedEventBuilder::event;
    event.time = time;
    return this;
  }-*/;

  public native NetworkResponseReceivedEventBuilder setSequence(int sequence) /*-{
    var event = this.@com.google.speedtracer.hintletengine.client.NetworkResponseReceivedEventBuilder::event;
    event.sequence = sequence;
    return this;
  }-*/;

  public native NetworkResponseReceivedEventBuilder setResponseConnectionId(int connectionId) /*-{
    var event = this.@com.google.speedtracer.hintletengine.client.NetworkResponseReceivedEventBuilder::event;
    event.data.response["connectionId"] = connectionId;
    return this;
  }-*/;

  public native NetworkResponseReceivedEventBuilder setResponseFromDiskCache(
      boolean fromDiskCache) /*-{
    var event = this.@com.google.speedtracer.hintletengine.client.NetworkResponseReceivedEventBuilder::event;
    event.data.response["fromDiskCache"] = fromDiskCache;
    return this;
  }-*/;

  public native NetworkResponseReceivedEventBuilder setResponseStatus(int status) /*-{
    var event = this.@com.google.speedtracer.hintletengine.client.NetworkResponseReceivedEventBuilder::event;
    event.data.response["status"] = status;
    return this;
  }-*/;

  public NetworkResponseReceivedEventBuilder setResponseHeaderDate(String date) {
    setResponseHeader("Date", date);
    return this;
  }

  public NetworkResponseReceivedEventBuilder setResponseHeaderCacheControl(String cacheControl) {
    setResponseHeader("Cache-Control", cacheControl);
    return this;
  }

  public NetworkResponseReceivedEventBuilder setResponseHeaderContentEncoding(
      String contentEncoding) {
    setResponseHeader("Content-Encoding", contentEncoding);
    return this;
  }

  public NetworkResponseReceivedEventBuilder setResponseHeaderContentLength(String contentLength) {
    setResponseHeader("Content-Length", contentLength);
    return this;
  }

  public NetworkResponseReceivedEventBuilder setResponseHeaderContentType(String contentType) {
    setResponseHeader("Content-Type", contentType);
    return this;
  }

  public NetworkResponseReceivedEventBuilder setResponseHeaderCookie(String cookie) {
    setResponseHeader("Cookie", cookie);
    return this;
  }
  
  public NetworkResponseReceivedEventBuilder setResponseHeaderLastModified(String lastModified) {
    setResponseHeader("Last-Modified", lastModified);
    return this;
  }

  public NetworkResponseReceivedEventBuilder setResponseHeaderSetCookie(String setCookie) {
    setResponseHeader("Set-Cookie", setCookie);
    return this;
  }

  public NetworkResponseReceivedEventBuilder setResponseHeaderVary(String vary) {
    setResponseHeader("Vary", vary);
    return this;
  }

  public NetworkResponseReceivedEventBuilder setResponseHeaderExpires(String expires) {
    setResponseHeader("Expires", expires);
    return this;
  }

  private native void setResponseHeader(String header, String value) /*-{
    this.@com.google.speedtracer.hintletengine.client.NetworkResponseReceivedEventBuilder::event.data.response.headers[header] = value;
  }-*/;

}
