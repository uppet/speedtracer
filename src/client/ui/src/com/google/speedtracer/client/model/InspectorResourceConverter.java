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
import com.google.speedtracer.client.model.DevToolsDataInstance.DevToolsDataProxy;
import com.google.speedtracer.client.model.NetworkResource.HeaderMap;
import com.google.speedtracer.client.util.JsIntegerMap;

/**
 * Utility class responsible for transforming inspector style network resources
 * into checkpoint style resources that our UI expects.
 */
public abstract class InspectorResourceConverter {

  /**
   * TODO (jaimeyap): We have the InspectorResourceConverter be an abstract base
   * class for now in order to provide a legacy implementation that we swap out
   * at runtime to support WebKit revisions before r52154. We should remove that
   * and just have a single impl once r52154 gets pushed to the Dev Channel.
   * Default implementation of InspectorResourceConverter.
   */
  public static class InspectorResourceConverterImpl extends
      InspectorResourceConverter {
    private final JsIntegerMap<ResourceStatus> resourceCheckpointMap = JsIntegerMap.<ResourceStatus> create();

    public InspectorResourceConverterImpl(DevToolsDataProxy proxy) {
      super(proxy);
    }

    /**
     * Each update will either trigger a state transition (sending of a
     * checkpoint) or is irrelevant. We get luck in that the updates that do not
     * trigger a state transition generally are updates to the expected
     * content-length, and we simply can just ignore them.
     * 
     * @param resourceId the id of the resource
     * @param resource the payload of an updateResource event
     */
    public void onUpdateResource(int resourceId, JavaScriptObject resource) {
      ResourceStatus resourceStatus = resourceCheckpointMap.get(resourceId);

      if (resourceStatus == null) {
        resourceStatus = new ResourceStatus(resourceId + "");
        resourceCheckpointMap.put(resourceId, resourceStatus);
      }

      if (resourceStatus.currentState == ResourceStatus.ADDED_UNSENT) {
        maybeSendStart(resourceStatus, resource.<UpdateResource> cast());
      }

      if (resourceStatus.currentState == ResourceStatus.SENT_START) {
        maybeSendResponseReceived(resourceStatus,
            resource.<UpdateResource> cast());
      }

      if (resourceStatus.currentState == ResourceStatus.SENT_RESPONSE_RECEIVED) {
        maybeSendFinish(resourceStatus, resource.<UpdateResource> cast());
      }

      if (resourceStatus.currentState == ResourceStatus.SENT_FINISH
          || resourceStatus.currentState == ResourceStatus.SENT_ERROR) {
        resourceCheckpointMap.put(resourceId, null);
      }
    }

    private void maybeSendFinish(ResourceStatus resourceStatus,
        UpdateResource update) {
      assert (resourceStatus.currentState == ResourceStatus.SENT_RESPONSE_RECEIVED);
      maybeUpdateContentLength(resourceStatus, update);
      if (update.didCompletionChange() && update.didTimingChange()) {
        if (update.didFail()) {
          NetworkResourceError error = NetworkResourceError.create(
              resourceStatus.resourceId, normalizeTime(update.getEndTime()),
              resourceStatus.contentLength);
          getProxy().onEventRecord(error);
          resourceStatus.currentState = ResourceStatus.SENT_ERROR;
          return;
        }

        if (update.didFinish()) {
          NetworkResourceFinished finished = NetworkResourceFinished.create(
              resourceStatus.resourceId, normalizeTime(update.getEndTime()),
              resourceStatus.contentLength);
          getProxy().onEventRecord(finished);
          resourceStatus.currentState = ResourceStatus.SENT_FINISH;
          return;
        }
      }
    }

    private void maybeSendResponseReceived(ResourceStatus resourceStatus,
        UpdateResource update) {
      assert (resourceStatus.currentState == ResourceStatus.SENT_START);
      maybeUpdateContentLength(resourceStatus, update);
      // TODO(jaimeyap): Verify that these two fields always get set as
      // expected.
      if (update.didResponseChange() && update.didTimingChange()) {
        String url = (update.getUrl() == null) ? resourceStatus.getRequestUrl()
            : update.getUrl();
        NetworkResourceResponse response = NetworkResourceResponse.create(
            resourceStatus.resourceId,
            normalizeTime(update.getResponseReceivedTime()),
            update.getMimeType(), update.getStatusCode(), update.wasCached(),
            update.getResponseHeaders(), url);

        // Send to UI.
        getProxy().onEventRecord(response);
        resourceStatus.currentState = ResourceStatus.SENT_RESPONSE_RECEIVED;
      }
    }

    /**
     * Sends the NetworkResourceStart. As soon as we get a startTime, we should
     * be good to make the state transition.
     */
    private void maybeSendStart(ResourceStatus resourceStatus,
        UpdateResource update) {
      assert (resourceStatus.currentState == ResourceStatus.ADDED_UNSENT);
      maybeUpdateContentLength(resourceStatus, update);
      if (update.didTimingChange()) {
        // With redirects, the url is in the response header.
        if (update.getStatusCode() == 302 || update.getStatusCode() == 301) {
          update.setUrl(update.getResponseHeaders().get("Location"));
        }

        String url = update.getUrl();
        if (url == null) {
          return;
        }
        resourceStatus.setRequestUrl(url);

        // Updates will always re-send all timings reported so far. Guaranteed
        // to have the startTime set.
        NetworkResourceStart start = NetworkResourceStart.create(
            resourceStatus.resourceId, normalizeTime(update.getStartTime()),
            url, update.getLastPathComponent(), update.getRequestMethod(),
            update.getRequestHeaders());

        // Forward to the UI.
        getProxy().onEventRecord(start);

        resourceStatus.currentState = ResourceStatus.SENT_START;

        // At this point the UI should have a start and a response event. We can
        // now send a page transition if this happens to be a main resource.
        if (update.isMainResource()) {
          // Send a page transition.
          TabChange tabChange = TabChange.create(
              normalizeTime(update.getResponseReceivedTime()), url);
          getProxy().onEventRecord(tabChange);
        }
      }
    }

    /**
     * Updates the tracked content length.
     * 
     * @param status
     * @param resource
     */
    private void maybeUpdateContentLength(ResourceStatus currStatus,
        UpdateResource resource) {
      if (resource.didLengthChange()) {
        currStatus.setContentLength(resource.getContentLength());
      }
    }

    private double normalizeTime(double seconds) {
      double millis = seconds * 1000;
      if (getProxy().getBaseTime() < 0) {
        getProxy().setBaseTime(millis);
      }
      return millis - getProxy().getBaseTime();
    }
  }

  /**
   * Simple state transition tracker to accumulate the state for a specific
   * network resource..
   */
  protected class ResourceStatus {
    // The possible states in the state machine.
    static final int ADDED_UNSENT = 0;
    static final int SENT_ERROR = 4;
    static final int SENT_FINISH = 3;
    static final int SENT_RESPONSE_RECEIVED = 2;
    static final int SENT_START = 1;

    int contentLength = -1;
    int currentState = ADDED_UNSENT;

    final String resourceId;
    private String requestUrl;

    public ResourceStatus(String resourceId) {
      this.resourceId = resourceId;
    }

    public String getRequestUrl() {
      return this.requestUrl;
    }

    public void setContentLength(int contentLength) {
      this.contentLength = contentLength;
    }

    public void setRequestUrl(String url) {
      this.requestUrl = url;
    }
  }

  /**
   * Calls to updateResource also pass along boolean fields that indicate
   * whether or not the relevant fields are present. Once we have accumulated
   * all the fields we care about for a specific checkpoint event, we shoot it
   * off.
   */
  private static class UpdateResource extends JavaScriptObject {
    @SuppressWarnings("unused")
    protected UpdateResource() {
    }

    final native boolean didCompletionChange() /*-{
      return !!this.didCompletionChange;
    }-*/;

    final native boolean didFail() /*-{
      return !!this.failed;
    }-*/;

    final native boolean didFinish() /*-{
      return !!this.finished;
    }-*/;

    final native boolean didLengthChange() /*-{
      return !!this.didLengthChange;
    }-*/;

    final native boolean didResponseChange() /*-{
      return !!this.didResponseChange;
    }-*/;

    final native boolean didTimingChange() /*-{
      return !!this.didTimingChange;
    }-*/;

    final native int getContentLength() /*-{
      return this.contentLength;
    }-*/;

    final native double getEndTime() /*-{
      return this.endTime;
    }-*/;

    final native String getLastPathComponent() /*-{
      return this.lastPathComponent;
    }-*/;

    final native String getMimeType() /*-{
      return this.mimeType;
    }-*/;

    final native HeaderMap getRequestHeaders() /*-{
      return this.requestHeaders;
    }-*/;

    final native String getRequestMethod() /*-{
      return this.requestMethod;
    }-*/;

    final native HeaderMap getResponseHeaders() /*-{
      return this.responseHeaders;
    }-*/;

    final native double getResponseReceivedTime() /*-{
      return this.responseReceivedTime;
    }-*/;

    final native double getStartTime() /*-{
      return this.startTime;
    }-*/;

    final native int getStatusCode() /*-{
      return this.statusCode;
    }-*/;

    final native String getUrl() /*-{
      return this.url;
    }-*/;

    final native boolean isMainResource() /*-{
      return !!this.mainResource;
    }-*/;

    final native void setUrl(String url) /*-{
      this.url = url;
    }-*/;

    final native boolean wasCached() /*-{
      return !!this.cached;
    }-*/;
  }

  private final DevToolsDataProxy proxy;

  public InspectorResourceConverter(DevToolsDataProxy proxy) {
    this.proxy = proxy;
  }

  /**
   * Takes in an Inspector style updateResource message and updates our network
   * resource tracking state for the associated resource.
   * 
   * @param resourceId The Id of the resource
   * @param updateResource The update payload
   */
  public abstract void onUpdateResource(int resourceId,
      JavaScriptObject updateResource);

  protected DevToolsDataProxy getProxy() {
    return proxy;
  }
}