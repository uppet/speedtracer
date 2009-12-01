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
public class InspectorResourceConverter {
  /**
   * These are the fields we care about on addResource messages. These should be
   * consistent across addResource invocations.
   */
  static class AddResource extends JavaScriptObject {
    protected AddResource() {
    }

    final native String getLastPathComponent() /*-{
      return this.lastPathComponent;
    }-*/;

    final native HeaderMap getRequestHeaders() /*-{
      return this.requestHeaders;
    }-*/;

    final native String getRequestMethod() /*-{
      return this.requestMethod;
    }-*/;

    // For a proper addResource the requestURL field is set. For an update
    // resource that is actually a redirect, they have a hybrid payload that is
    // both an Update and an Add. The 'url' field is set for those payloads.
    final native String getRequestUrl() /*-{
      return this.requestURL || this.url;
    }-*/;

    final native boolean isMainResource() /*-{
      return !!this.isMainResource;
    }-*/;

    final native boolean wasCached() /*-{
      return !!this.cached;
    }-*/;
  }

  /**
   * Calls to updateResource also pass along boolean fields that indicate
   * whether or not the relevant fields are present. Once we have accumulated
   * all the fields we care about for a specific checkpoint event, we shoot it
   * off.
   */
  static class UpdateResource extends JavaScriptObject {
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

    final native boolean didResponseChange() /*-{
      return !!this.didResponseChange;
    }-*/;

    final native boolean didTimingChange() /*-{
      return !!this.didTimingChange;
    }-*/;

    final native double getEndTime() /*-{
      return this.endTime;
    }-*/;

    final native String getMimeType() /*-{
      return this.mimeType;
    }-*/;

    // This may be null for responses that are not redirects.
    final native String getRedirectUrl() /*-{
      return this.url;
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
  }

  /**
   * Simple state transition tracker.
   */
  private class ResourceStatus {
    // The possible states in the state machine.
    static final int ADDED_UNSENT = 0;
    static final int SENT_ERROR = 4;
    static final int SENT_FINISH = 3;
    static final int SENT_RESPONSE_RECEIVED = 2;
    static final int SENT_START = 1;

    // The initial resource information.
    final AddResource addResource;

    int currentState = ADDED_UNSENT;
    final int inspectorId;
    final String resourceId;

    public ResourceStatus(int inspectorId, String resourceId,
        AddResource addResource) {
      this.inspectorId = inspectorId;
      this.resourceId = resourceId;
      this.addResource = addResource;
    }
  }

  private static final int NO_REDIRECT = 0;

  private final DevToolsDataProxy proxy;

  private int redirectCount = 0;

  private final JsIntegerMap<ResourceStatus> resourceCheckpointMap = JsIntegerMap.<ResourceStatus> create();

  public InspectorResourceConverter(DevToolsDataProxy proxy) {
    this.proxy = proxy;
  }

  /**
   * Simply provisions a resource. This has no timing information so we cant yet
   * send a resource start checkpoint.
   * 
   * @param inspectorIdentifier the int id of the resource in inspector speak
   * @param resource the payload of an addResource event
   */
  public ResourceStatus onAddResource(int inspectorIdentifier,
      JavaScriptObject resource) {
    // Check to see if we already have an add for this resource ID. If we do,
    // then we assume it is a redirect and make an appropriate internal ID.
    ResourceStatus resourceStatus = resourceCheckpointMap.get(inspectorIdentifier);
    String internalResourceId = NetworkResourceRecord.generateResourceId(
        (resourceStatus == null) ? NO_REDIRECT : redirectCount,
        inspectorIdentifier);

    resourceStatus = new ResourceStatus(inspectorIdentifier,
        internalResourceId, resource.<AddResource> cast());
    resourceCheckpointMap.put(inspectorIdentifier, resourceStatus);

    return resourceStatus;
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
      return;
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

    // TODO(jaimeyap): Verify that these two fields always get set as expected.
    if (update.didCompletionChange() && update.didTimingChange()) {
      if (update.didFail()) {
        NetworkResourceError error = NetworkResourceError.create(
            resourceStatus.resourceId, normalizeTime(update.getEndTime()));
        proxy.onEventRecord(error);
        resourceStatus.currentState = ResourceStatus.SENT_ERROR;
        return;
      }

      if (update.didFinish()) {
        NetworkResourceFinished finished = NetworkResourceFinished.create(
            resourceStatus.resourceId, normalizeTime(update.getEndTime()));
        proxy.onEventRecord(finished);
        resourceStatus.currentState = ResourceStatus.SENT_FINISH;
        return;
      }
    }
  }

  private void maybeSendResponseReceived(ResourceStatus resourceStatus,
      UpdateResource update) {
    assert (resourceStatus.currentState == ResourceStatus.SENT_START);

    // TODO(jaimeyap): Verify that these two fields always get set as expected.
    if (update.didResponseChange() && update.didTimingChange()) {
      AddResource add = resourceStatus.addResource;

      // Special case if we get a 302 redirect. Need to close this record out
      // and start over. We send a response, a finished, and a new start for the
      // new resource.
      if (update.getStatusCode() == 302 || update.getStatusCode() == 301) {
        double time = update.getStartTime();
        // Send a response received.
        NetworkResourceResponse response = NetworkResourceResponse.create(
            resourceStatus.resourceId, normalizeTime(time),
            update.getMimeType(), update.getStatusCode(), add.wasCached(),
            update.getResponseHeaders(), update.getRedirectUrl());
        proxy.onEventRecord(response);

        // Close the resource.
        NetworkResourceFinished finished = NetworkResourceFinished.create(
            resourceStatus.resourceId, normalizeTime(time));
        proxy.onEventRecord(finished);

        redirectCount++;

        // Pretend we are back in start state by shooting off a start and
        // starting over. This type of update smells like an Add because it
        // actually has fields of both.
        resourceStatus = onAddResource(resourceStatus.inspectorId, update);        
        maybeSendStart(resourceStatus, update);

        // State machine should now be back in a sane state.
        // The next update resource should be unaware that it was birthed from a
        // redirect.
        return;
      }

      NetworkResourceResponse response = NetworkResourceResponse.create(
          resourceStatus.resourceId,
          normalizeTime(update.getResponseReceivedTime()),
          update.getMimeType(), update.getStatusCode(), add.wasCached(),
          update.getResponseHeaders(), add.getRequestUrl());

      // Send to UI.
      proxy.onEventRecord(response);
      resourceStatus.currentState = ResourceStatus.SENT_RESPONSE_RECEIVED;

      // At this point the UI should have a start and a response event. We can
      // now send a page transition if this happens to be a main resource.
      if (add.isMainResource()) {
        // Send a page transition.
        TabChange tabChange = TabChange.create(
            normalizeTime(update.getResponseReceivedTime()),
            add.getRequestUrl());
        proxy.onEventRecord(tabChange);
      }
    }
  }

  /**
   * Sends the NetworkResourceStart. As soon as we get a startTime, we should be
   * good to make the state transition.
   */
  private void maybeSendStart(ResourceStatus resourceStatus,
      UpdateResource update) {
    assert (resourceStatus.currentState == ResourceStatus.ADDED_UNSENT);

    if (update.didTimingChange()) {
      AddResource add = resourceStatus.addResource;
      // Updates will always re-send all timings reported so far. Guaranteed to
      // have the startTime set.
      NetworkResourceStart start = NetworkResourceStart.create(
          resourceStatus.resourceId, normalizeTime(update.getStartTime()),
          add.getRequestUrl(), add.getLastPathComponent(),
          add.getRequestMethod(), add.getRequestHeaders());

      // Forward to the UI.
      proxy.onEventRecord(start);

      resourceStatus.currentState = ResourceStatus.SENT_START;
    }
  }

  private double normalizeTime(double seconds) {
    double millis = seconds * 1000;
    if (proxy.getBaseTime() < 0) {
      proxy.setBaseTime(millis);
    }
    return millis - proxy.getBaseTime();
  }
}