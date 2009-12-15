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

    /**
     * TODO (jaimeyap): IterableFastStringMap shares this implementation.
     * Consider sharing the IterationCallBack class.
     * 
     * @param cb
     */
    public final native void iterate(IterationCallBack cb) /*-{
      for (var key in this) {
        cb.@com.google.speedtracer.client.model.NetworkResource.HeaderMap.IterationCallBack::onIteration(Ljava/lang/String;Ljava/lang/String;)(key,this[key]);
      }
    }-*/;

    public final native void put(String key, String value) /*-{
      this[key] = value;
    }-*/;
  }

  private double endTime = Double.NaN;
  private NetworkResourceError errorEvent;
  private NetworkResourceFinished finishedEvent;
  private NetworkResourceResponse responseEvent;
  private double responseReceivedTime = Double.NaN;
  private final NetworkResourceStart startEvent;

  public NetworkResource(NetworkResourceStart start) {
    this.startEvent = start;
  }

  public String asString() {
    return getResourceId() + " , " + getUrl() + " , " + getStartTime() + " , "
        + getResponseReceivedTime() + " , " + getEndTime();
  }

  public boolean didError() {
    return errorEvent != null;
  }

  /**
   * Returns the WebKit resource load computed content length (can be -1 if not
   * set). This does not always agree with the response header Content-Length
   * entry. Both can be set, or only one can be set. Callers should decide how
   * to display this based on both numbers.
   * 
   * @return the content length in bytes that WebKit reported for this resource.
   */
  public int getContentLength() {
    return finishedEvent.getContentLength();
  }

  public double getEndTime() {
    return this.endTime;
  }

  public NetworkResourceError getErrorEvent() {
    return errorEvent;
  }

  public NetworkResourceFinished getFinishedEvent() {
    return finishedEvent;
  }

  public JSOArray<HintRecord> getHintRecords() {
    JSOArray<HintRecord> result = JSOArray.createArray().cast();
    if (startEvent != null && startEvent.hasHintRecords()) {
      result = result.concat(startEvent.getHintRecords());
    }
    if (responseEvent != null && responseEvent.hasHintRecords()) {
      result = result.concat(responseEvent.getHintRecords());
    }
    if (finishedEvent != null && finishedEvent.hasHintRecords()) {
      result = result.concat(finishedEvent.getHintRecords());
    }
    if (errorEvent != null && errorEvent.hasHintRecords()) {
      result = result.concat(errorEvent.getHintRecords());
    }
    if (result.size() <= 0) {
      return null;
    }
    return result;
  }

  public String getLastPathComponent() {
    return startEvent.getLastPathComponent();
  }

  public String getMethod() {
    return startEvent.getHttpMethod();
  }

  public HeaderMap getRequestHeaders() {
    return startEvent.getHeaders();
  }

  public String getResourceId() {
    return startEvent.getResourceId();
  }

  /**
   * Returns the HTTP response code for the network event.
   * 
   * @return the HTTP response code for the network event. Returns -1 if the
   *         response has not been received.
   */
  public int getResponseCode() {
    if (responseEvent != null) {
      return responseEvent.getResponseCode();
    }
    return -1;
  }

  public NetworkResourceResponse getResponseEvent() {
    return responseEvent;
  }

  /**
   * Returns the headers from the HTTP response.
   * 
   * @return the headers from the HTTP response. Returns <code>null</code> if
   *         the response has not been received.
   */
  public HeaderMap getResponseHeaders() {
    if (responseEvent != null) {
      return responseEvent.getHeaders();
    }
    return null;
  }

  /**
   * Returns the MIME type from the HTTP Response.
   * 
   * @return the MIME type from the HTTP Response. Returns <code>null</code> if
   *         the response has not been received.
   */
  public String getResponseMimeType() {
    if (responseEvent != null) {
      return responseEvent.getMimeType();
    }
    return null;
  }

  public double getResponseReceivedTime() {
    return this.responseReceivedTime;
  }

  public NetworkResourceStart getStartEvent() {
    return startEvent;
  }

  public double getStartTime() {
    return startEvent.getTime();
  }

  public String getUrl() {
    return startEvent.getUrl();
  }

  public boolean hasError() {
    return errorEvent != null;
  }

  public boolean hasFinished() {
    return finishedEvent != null || errorEvent != null;
  }

  /**
   * Returns <code>true</code> if this resource has associated hintlet records.
   * 
   * @return <code>true</code> if this resource has associated hintlet records.
   */
  public boolean hasHintletRecords() {
    if (startEvent != null && startEvent.hasHintRecords()) {
      return true;
    } else if (responseEvent != null && responseEvent.hasHintRecords()) {
      return true;
    } else if (finishedEvent != null && finishedEvent.hasHintRecords()) {
      return true;
    } else if (errorEvent != null && errorEvent.hasHintRecords()) {
      return true;
    }
    return false;
  }

  public boolean hasRecord(NetworkResourceRecord rec) {
    if (rec.getType() == EventRecordType.NETWORK_RESOURCE_START
        && startEvent != null) {
      return rec.getSequence() == startEvent.getSequence();
    }
    if (rec.getType() == EventRecordType.NETWORK_RESOURCE_RESPONSE
        && responseEvent != null) {
      return rec.getSequence() == responseEvent.getSequence();
    }
    if (rec.getType() == EventRecordType.NETWORK_RESOURCE_FINISH
        && finishedEvent != null) {
      return rec.getSequence() == finishedEvent.getSequence();
    }
    if (rec.getType() == EventRecordType.NETWORK_RESOURCE_ERROR
        && errorEvent != null) {
      return rec.getSequence() == errorEvent.getSequence();
    }
    return false;
  }

  public boolean hasResponse() {
    return responseEvent != null;
  }

  public boolean isCached() {
    if (responseEvent != null) {
      return responseEvent.isCached();
    }
    return false;
  }

  public boolean isRedirect() {
    int responseCode = getResponseCode();
    return responseCode == 302 || responseCode == 301;
  }

  public void update(NetworkResourceError error) {
    this.errorEvent = error;
    assert (error.getResourceId().equals(getResourceId()));
    responseReceivedTime = error.getTime();
    endTime = error.getTime();
  }

  public void update(NetworkResourceFinished finish) {
    this.finishedEvent = finish;
    assert (finish.getResourceId().equals(getResourceId()));
    endTime = finish.getTime();
  }

  public void update(NetworkResourceResponse response) {
    this.responseEvent = response;
    assert (response.getResourceId().equals(getResourceId()));
    responseReceivedTime = response.getTime();
  }
}
