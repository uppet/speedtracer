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

import com.google.gwt.core.client.JavaScriptObject;
import com.google.speedtracer.client.model.ResourceUpdateEvent.UpdateResource;
import com.google.speedtracer.client.util.JSOArray;

/**
 * Data Payload for Network Resource Events.
 */
public class NetworkResource {
  /**
   * Header Map of Strings.
   */
  public static class HeaderMap extends JavaScriptObject {
    /**
     * interface that allows for iteration through Object Map.
     */
    public interface IterationCallBack {
      void onIteration(String key, String value);
    }

    protected HeaderMap() {
    }

    public final native String get(String key) /*-{
      return this[key];
    }-*/;

    public final native void iterate(IterationCallBack cb) /*-{
      for (var key in this) {
        cb.@com.google.speedtracer.client.model.NetworkResource.HeaderMap.IterationCallBack::onIteration(Ljava/lang/String;Ljava/lang/String;)(key,this[key]);
      }
    }-*/;

    public final native void put(String key, String value) /*-{
      this[key] = value;
    }-*/;
  }

  public static boolean isRedirect(int statusCode) {
    return statusCode == 302 || statusCode == 301;
  }

  private boolean cached;

  private int contentLength = -1;

  private boolean didFail;

  private String domain;

  private double endTime = Double.NaN;

  private int expectedContentLength = -1;

  private ResourceFinishEvent finishEvent;

  private final String httpMethod;

  private final int identifier;

  private final boolean isMainResource;

  private String lastPathComponent = "/";

  private String mimeType;

  // This is the start time reported by Inspector updateResource messages. It is
  // differenct from startTime solely because the timestamp is grabbed a second
  // time in the resource tracking code and not the TimelineAgent code. We
  // keep track of this because if this resource happens to get redirected, we
  // need to later match things up by URL and startime.
  private double otherStartTime;

  private String path;

  private HeaderMap requestHeaders;

  // Kept around only because hintlets can be accumulated on it.
  private ResourceResponseEvent responseEvent;

  private HeaderMap responseHeaders;

  private double responseReceivedTime = Double.NaN;

  // Kept around only because hintlets can be accumulated on it.
  private final ResourceWillSendEvent startEvent;

  private final double startTime;

  private int statusCode = -1;

  private final String url;

  public NetworkResource(ResourceWillSendEvent startEvent) {
    this.startTime = startEvent.getTime();
    this.identifier = startEvent.getIdentifier();
    this.url = startEvent.getUrl();
    this.isMainResource = startEvent.isMainResource();
    this.httpMethod = startEvent.getHttpMethod();
    // Cache the ResourceEvent to later pull hintlets.
    this.startEvent = startEvent;
  }

  public String asString() {
    return getIdentifier() + " , " + getUrl() + " , " + getStartTime() + " , "
        + getResponseReceivedTime() + " , " + getEndTime();
  }

  public boolean didFail() {
    return didFail;
  }

  public int getContentLength() {
    return contentLength;
  }

  public String getDomain() {
    return domain;
  }

  public double getEndTime() {
    return endTime;
  }

  public int getExpectedContentLength() {
    return expectedContentLength;
  }

  public JSOArray<HintRecord> getHintRecords() {
    JSOArray<HintRecord> hintlets = JSOArray.createArray().cast();

    if (startEvent.hasHintRecords()) {
      hintlets.concat(startEvent.getHintRecords());
    }

    if (responseEvent != null && responseEvent.hasHintRecords()) {
      hintlets.concat(responseEvent.getHintRecords());
    }

    if (finishEvent != null && finishEvent.hasHintRecords()) {
      hintlets.concat(finishEvent.getHintRecords());
    }

    if (hintlets.size() <= 0) {
      return null;
    }
    return hintlets;
  }

  public String getHttpMethod() {
    return httpMethod;
  }

  public int getIdentifier() {
    return identifier;
  }

  public String getLastPathComponent() {
    return lastPathComponent;
  }

  public String getMimeType() {
    return mimeType;
  }

  public double getOtherStartTime() {
    return otherStartTime;
  }

  public String getPath() {
    return path;
  }

  public HeaderMap getRequestHeaders() {
    return requestHeaders;
  }

  public HeaderMap getResponseHeaders() {
    return responseHeaders;
  }

  public double getResponseReceivedTime() {
    return responseReceivedTime;
  }

  public double getStartTime() {
    return startTime;
  }

  public int getStatusCode() {
    return statusCode;
  }

  public String getUrl() {
    return url;
  }

  public boolean isCached() {
    return cached;
  }

  public boolean isDidFail() {
    return didFail;
  }

  public boolean isMainResource() {
    return isMainResource;
  }

  public boolean isRedirect() {
    return isRedirect(statusCode);
  }

  public void update(ResourceFinishEvent finishEvent) {
    this.endTime = finishEvent.getTime();
    this.didFail = finishEvent.didFail();
    // Cache the ResourceEvent to later pull hintlets.
    this.finishEvent = finishEvent;
  }

  public void update(ResourceResponseEvent responseEvent) {
    this.responseReceivedTime = responseEvent.getTime();
    this.expectedContentLength = responseEvent.getExpectedContentLength();
    this.mimeType = responseEvent.getMimeType();
    this.statusCode = responseEvent.getStatusCode();
    // Cache the ResourceEvent to later pull hintlets.
    this.responseEvent = responseEvent;
  }

  /**
   * Updates information about this record. Note that we use the timeline
   * checkpoint records to establish all timing information. We therefore ignore
   * the timing information present in these updates.
   * 
   * @param update
   */
  public void update(ResourceUpdateEvent updateEvent) {
    UpdateResource update = updateEvent.getUpdate();
    if (update.didRequestChange()) {
      this.domain = update.getHost();
      this.path = update.getPath();
      this.lastPathComponent = update.getLastPathComponent();
      this.requestHeaders = update.getRequestHeaders();
      this.cached = update.wasCached();
    }

    if (update.didResponseChange()) {
      this.responseHeaders = update.getResponseHeaders();

      if (this.statusCode < 0) {
        this.statusCode = update.getStatusCode();
      }
    }

    if (update.didLengthChange()) {
      this.contentLength = update.getContentLength();
    }

    if (update.didTimingChange()) {
      if ((Double.isNaN(this.endTime)) && (update.getEndTime() > 0)) {
        this.endTime = update.getEndTime();
      }

      if ((Double.isNaN(this.responseReceivedTime))
          && (update.getResponseReceivedTime() > 0)) {
        this.responseReceivedTime = update.getResponseReceivedTime();
      }

      // We record this for redirect matching purposes.
      if (update.getStartTime() > 0) {
        this.otherStartTime = update.getStartTime();
      }
    }
  }
}