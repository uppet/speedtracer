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

package com.google.speedtracer.client.model;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.speedtracer.client.model.NetworkResource.HeaderMap;

/**
 * Overlay type for Inspector resource updates.
 */
public class ResourceUpdateEvent extends ResourceRecord {
  /**
   * Calls to updateResource also pass along boolean fields that indicate
   * whether or not the relevant fields are present. Once we have accumulated
   * all the fields we care about for a specific checkpoint event, we shoot it
   * off.
   */
  public static class UpdateResource extends JavaScriptObject {
    protected UpdateResource() {
    }

    public final native boolean didCompletionChange() /*-{
      return !!this.didCompletionChange;
    }-*/;

    public final native boolean didFail() /*-{
      return !!this.failed;
    }-*/;

    public final native boolean didFinish() /*-{
      return !!this.finished;
    }-*/;

    public final native boolean didLengthChange() /*-{
      return !!this.didLengthChange;
    }-*/;

    public final native boolean didRequestChange() /*-{
      return !!this.didRequestChange;
    }-*/;

    public final native boolean didResponseChange() /*-{
      return !!this.didResponseChange;
    }-*/;

    public final native boolean didTimingChange() /*-{
      return !!this.didTimingChange;
    }-*/;

    // TODO (jaimeyap): Follow up to ensure that we display both the transfer
    // size and the uncompressed resource size. The following is just a
    // temporary fix.
    public final native int getContentLength() /*-{
      return this.contentLength || this.resourceSize || 0;
    }-*/;

    public final native double getDomContentEventTime() /*-{
      return this.domContentEventTime || -1;
    }-*/;

    public final native double getEndTime() /*-{
      return this.endTime || -1;
    }-*/;

    public final native String getHost() /*-{
      return this.host;
    }-*/;

    public final native String getLastPathComponent() /*-{
      return this.lastPathComponent;
    }-*/;

    public final native double getLoadEventTime() /*-{
      return this.loadEventTime || -1;
    }-*/;

    public final native String getMimeType() /*-{
      return this.mimeType;
    }-*/;

    public final native String getPath() /*-{
      return this.path;
    }-*/;

    public final native HeaderMap getRequestHeaders() /*-{
      return this.requestHeaders;
    }-*/;

    public final native String getRequestMethod() /*-{
      return this.requestMethod;
    }-*/;

    public final native HeaderMap getResponseHeaders() /*-{
      return this.responseHeaders;
    }-*/;

    public final native double getResponseReceivedTime() /*-{
      return this.responseReceivedTime || -1;
    }-*/;

    public final native double getStartTime() /*-{
      return this.startTime || -1;
    }-*/;

    public final native int getStatusCode() /*-{
      return this.statusCode || -1;
    }-*/;

    public final native String getUrl() /*-{
      return this.url;
    }-*/;

    public final native boolean isMainResource() /*-{
      return !!this.mainResource;
    }-*/;

    public final native void setDomContentEventTime(double normalizeTime) /*-{
      return this.domContentEventTime;
    }-*/;

    public final native void setEndTime(double normalizedTime) /*-{
      this.endTime = normalizedTime;
    }-*/;

    public final native void setLoadEventTime(double normalizedTime) /*-{
      return this.loadEventTime = normalizedTime;
    }-*/;

    public final native void setResponseReceivedTime(double normalizedTime) /*-{
      this.responseReceivedTime = normalizedTime;
    }-*/;

    public final native void setStartTime(double normalizedTime) /*-{
      this.startTime = normalizedTime;
    }-*/;

    public final native boolean wasCached() /*-{
      return !!this.cached;
    }-*/;
  }

  public static final int TYPE = EventRecordType.RESOURCE_UPDATED;

  public static native ResourceUpdateEvent create(int resourceId,
      UpdateResource update) /*-{
    update["identifier"] = resourceId;    
    return {
      type: @com.google.speedtracer.client.model.ResourceUpdateEvent::TYPE,
      data: update,
      time: 0
    }
  }-*/;

  protected ResourceUpdateEvent() {
  }

  public final UpdateResource getUpdate() {
    return getData().cast();
  }

  /**
   * These events are always synthesized to wrap inspector updateResource
   * messages. As such we need to give it a "time" to make it consistent with
   * the Timeline record format.
   */
  public final native void setTime(double time) /*-{
    this.time = time;
  }-*/;
}
