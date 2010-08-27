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
import com.google.speedtracer.shared.EventRecordType;

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
  public static final class UpdateResource extends JavaScriptObject {
    protected UpdateResource() {
    }

    public native boolean didCompletionChange() /*-{
      return !!this.didCompletionChange;
    }-*/;

    public native boolean didFail() /*-{
      return !!this.failed;
    }-*/;

    public native boolean didFinish() /*-{
      return !!this.finished;
    }-*/;

    public native boolean didLengthChange() /*-{
      return !!this.didLengthChange;
    }-*/;

    public native boolean didRequestChange() /*-{
      return !!this.didRequestChange;
    }-*/;

    public native boolean didResponseChange() /*-{
      return !!this.didResponseChange;
    }-*/;

    public native boolean didTimingChange() /*-{
      return !!this.didTimingChange;
    }-*/;

    public native int getConnectionID() /*-{
      return this.connectionID || -1;
    }-*/;

    public native boolean getConnectionReused() /*-{
      return !!this.connectionReused;
    }-*/;

    // TODO (jaimeyap): Follow up to ensure that we display both the transfer
    // size and the uncompressed resource size. The following is just a
    // temporary fix.
    public native int getContentLength() /*-{
      return this.contentLength || this.resourceSize || 0;
    }-*/;

    /**
     * Returns the detailed timing info for this request. This method will
     * return <code>null</code> if there is no timing data or if the timing data
     * is not valid (i.e. is based on an older schema).
     */
    public DetailedResponseTiming getDetailedResponseTiming() {
      final DetailedResponseTiming timing = getDetailedResponseTimingImpl();
      return (timing != null && timing.isValid()) ? timing : null;
    }

    public native double getDomContentEventTime() /*-{
      return this.domContentEventTime || -1;
    }-*/;

    public native double getEndTime() /*-{
      return this.endTime || -1;
    }-*/;

    public native String getHost() /*-{
      return this.host;
    }-*/;

    public native String getLastPathComponent() /*-{
      return this.lastPathComponent;
    }-*/;

    public native double getLoadEventTime() /*-{
      return this.loadEventTime || -1;
    }-*/;

    public native String getMimeType() /*-{
      return this.mimeType;
    }-*/;

    public native String getPath() /*-{
      return this.path;
    }-*/;

    public native HeaderMap getRequestHeaders() /*-{
      return this.requestHeaders;
    }-*/;

    public native String getRequestMethod() /*-{
      return this.requestMethod;
    }-*/;

    public native HeaderMap getResponseHeaders() /*-{
      return this.responseHeaders;
    }-*/;

    public native double getResponseReceivedTime() /*-{
      return this.responseReceivedTime || -1;
    }-*/;

    public native double getStartTime() /*-{
      return this.startTime || -1;
    }-*/;

    public native int getStatusCode() /*-{
      return this.statusCode || -1;
    }-*/;

    public native String getStatusText() /*-{
      return this.statusText || "";
    }-*/;

    public native String getUrl() /*-{
      return this.url;
    }-*/;

    public native boolean isMainResource() /*-{
      return !!this.mainResource;
    }-*/;

    public native void setDomContentEventTime(double normalizeTime) /*-{
      return this.domContentEventTime;
    }-*/;

    public native void setEndTime(double normalizedTime) /*-{
      this.endTime = normalizedTime;
    }-*/;

    public native void setLoadEventTime(double normalizedTime) /*-{
      this.loadEventTime = normalizedTime;
    }-*/;

    public native void setResponseReceivedTime(double normalizedTime) /*-{
      this.responseReceivedTime = normalizedTime;
    }-*/;

    public native void setStartTime(double normalizedTime) /*-{
      this.startTime = normalizedTime;
    }-*/;

    public native boolean wasCached() /*-{
      return !!this.cached;
    }-*/;

    private native DetailedResponseTiming getDetailedResponseTimingImpl() /*-{
      return this.timing;
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
