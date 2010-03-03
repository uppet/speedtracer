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
package com.google.speedtracer.client.visualizations.model;

import com.google.speedtracer.client.model.DataModel;
import com.google.speedtracer.client.model.EventRecord;
import com.google.speedtracer.client.model.HintRecord;
import com.google.speedtracer.client.model.HintletEngineHost;
import com.google.speedtracer.client.model.NetworkResource;
import com.google.speedtracer.client.model.NetworkResourceModel;
import com.google.speedtracer.client.model.ResourceFinishEvent;
import com.google.speedtracer.client.model.ResourceRecord;
import com.google.speedtracer.client.model.ResourceResponseEvent;
import com.google.speedtracer.client.model.ResourceUpdateEvent;
import com.google.speedtracer.client.model.ResourceWillSendEvent;
import com.google.speedtracer.client.model.ResourceUpdateEvent.UpdateResource;
import com.google.speedtracer.client.timeline.GraphModel;
import com.google.speedtracer.client.timeline.HighlightModel;
import com.google.speedtracer.client.timeline.ModelData;
import com.google.speedtracer.client.util.JsIntegerMap;

import java.util.ArrayList;
import java.util.List;

/**
 * Underlying model implementation that maintains NetworkTimeLineDetailView's
 * state.
 */
public class NetworkTimeLineModel implements VisualizationModel,
    NetworkResourceModel.Listener, HintletEngineHost.HintListener {

  /**
   * Invoked when a resource has a change and may need to be refreshed in the
   * user interface.
   */
  interface ResourceRefreshListener {
    void onResourceRefresh(NetworkResource resource);
  }

  private final DataModel dataModel;

  private final GraphModel graphModel;

  private final HighlightModel highlightModel = HighlightModel.create();

  private int openRequests = 0;

  private List<NetworkResource> redirectQueue = new ArrayList<NetworkResource>();

  private List<ResourceRefreshListener> resourceRefreshListeners = new ArrayList<ResourceRefreshListener>();

  /**
   * Map of NetworkResource POJOs. Information about a network resource is
   * filled in progressively as we get chunks of information about it.
   */
  private final JsIntegerMap<NetworkResource> resourceStore = JsIntegerMap.create();

  /**
   * We keep an index sorted by start time.
   */
  private final List<NetworkResource> sortedResources = new ArrayList<NetworkResource>();

  private final NetworkResourceModel sourceModel;

  public NetworkTimeLineModel(DataModel dataModel) {
    this.dataModel = dataModel;
    graphModel = GraphModel.createGraphModel(new ModelData(), "", "ms", "",
        " requests", true);

    // Register for source events
    this.sourceModel = dataModel.getNetworkResourceModel();
    sourceModel.addListener(this);
    dataModel.getHintletEngineHost().addHintListener(this);
  }

  /**
   * Adds a NetworkResource to our resource store as well as to the list of
   * resources sorted by start time. We assume that we do not get network
   * resource starts out of order.
   * 
   * @param resource the resource we are adding to our book keeping
   */
  public void addResource(NetworkResource resource) {
    resourceStore.put(resource.getIdentifier(), resource);
    sortedResources.add(resource);
  }

  public void addResourceRefreshListener(ResourceRefreshListener listener) {
    resourceRefreshListeners.add(listener);
  }

  public void clearData() {
    sortedResources.clear();
  }

  public void detachFromSourceModel() {
    sourceModel.removeListener(this);
  }

  public GraphModel getGraphModel() {
    return graphModel;
  }

  public HighlightModel getHighlightModel() {
    return highlightModel;
  }

  /**
   * Gets a stored resource from our book keeping, or null if it hasnt been
   * stored before.
   * 
   * @param id the request id of our {@link NetworkResource}
   * @return returns the {@link NetworkResource}
   */
  public NetworkResource getResource(int id) {
    return resourceStore.get(id);
  }

  public List<NetworkResource> getSortedResources() {
    return sortedResources;
  }

  public void onHint(HintRecord hintlet) {
    // Only process hintlet references to a Ui Event
    int refRecord = hintlet.getRefRecord();
    EventRecord rec = dataModel.findEventRecord(refRecord);
    if (!ResourceRecord.isResourceRecord(rec)) {
      return;
    }
    int value;
    switch (hintlet.getSeverity()) {
      case HintRecord.SEVERITY_CRITICAL:
        value = HighlightModel.HIGHLIGHT_CRITICAL;
        break;
      case HintRecord.SEVERITY_WARNING:
        value = HighlightModel.HIGHLIGHT_WARNING;
        break;
      case HintRecord.SEVERITY_INFO:
        value = HighlightModel.HIGHLIGHT_INFO;
        break;
      default:
        value = HighlightModel.HIGHLIGHT_NONE;
    }
    highlightModel.addData(rec.getTime(), value);

    // Notify any listeners wanting to hear about such changes.
    NetworkResource res = findResourceForRecord(rec.<ResourceRecord> cast());
    fireResourceRefreshListeners(res);
  }

  public void onNetworkResourceRequestStarted(
      ResourceWillSendEvent resourceStart) {
    // Check for dupe IDs. If we find one, assume it is a redirect.
    NetworkResource previousResource = getResource(resourceStart.getIdentifier());
    if (previousResource != null) {
      // We have a redirect.
      redirectQueue.add(previousResource);
    }
    openRequests++;
    getGraphModel().addData(resourceStart.getTime(), openRequests);
    NetworkResource resource = new NetworkResource(resourceStart);
    addResource(resource);
  }

  public void onNetworkResourceResponseFinished(
      ResourceFinishEvent resourceFinish) {
    openRequests--;
    getGraphModel().addData(resourceFinish.getTime(), openRequests);
    NetworkResource resource = getResource(resourceFinish.getIdentifier());
    if (resource != null) {
      resource.update(resourceFinish);
    }
  }

  public void onNetworkResourceResponseStarted(
      ResourceResponseEvent resourceResponse) {
    NetworkResource resource = getResource(resourceResponse.getIdentifier());
    if (resource != null) {
      resource.update(resourceResponse);
    }
  }

  public void onNetworkResourceUpdated(ResourceUpdateEvent resourceUpdate) {
    // TODO(jaimeyap): We should check for the load event and the domcontent
    // event here and do something with it.
    NetworkResource resource = getResource(resourceUpdate.getIdentifier());
    if (resource != null) {
      resource.update(resourceUpdate);
    } else {
      // We are dealing potentially with an update for a redirect.
      UpdateResource update = resourceUpdate.getUpdate();
      // Look for it.
      int i = 0;
      while (i < redirectQueue.size()) {
        NetworkResource redirectCandidate = redirectQueue.get(i);
        if (redirectCandidate.getUrl().equals(update.getUrl())) {
          // If we have a main resource, then we need to provision a spot for it
          // since it would be lost on the previous page transition.
          if (redirectCandidate.isMainResource() && update.isMainResource()) {
            matchRedirect(i, redirectCandidate, resourceUpdate);
            break;
          }
          // If the URLs are the same and they have the same start time, then we
          // have a match.
          if (redirectCandidate.getOtherStartTime() == update.getStartTime()) {
            matchRedirect(i, redirectCandidate, resourceUpdate);
            break;
          }
        }
        i++;
      }
    }
  }

  /**
   * Adds a NetworkResource to our resource store and sorted resource list that
   * might be out of order.
   * 
   * @param resource the resource we are adding to our book keeping
   */
  private void addResourceMaybeOutOfOrder(NetworkResource resource) {
    resourceStore.put(resource.getIdentifier(), resource);

    if (sortedResources.size() == 0) {
      return;
    }

    for (int i = sortedResources.size() - 1; i >= 0; i--) {
      NetworkResource curr = sortedResources.get(i);
      if (resource.getStartTime() > curr.getStartTime()) {
        sortedResources.add(i + 1, resource);
        break;
      }
    }
  }

  private NetworkResource findResourceForRecord(ResourceRecord rec) {
    // A simple linear scan will suffice - we don't expect this list to get very
    // big.
    for (int i = 0, l = sortedResources.size(); i < l; ++i) {
      NetworkResource res = sortedResources.get(i);
      if (res.getStartTime() > rec.getTime()) {
        // The rest of the resources in this list started too late.
        return null;
      }
      if (res.getIdentifier() == rec.getIdentifier()) {
        return res;
      }
    }
    return null;
  }

  private void fireResourceRefreshListeners(NetworkResource res) {
    for (int i = 0, l = resourceRefreshListeners.size(); i < l; ++i) {
      resourceRefreshListeners.get(i).onResourceRefresh(res);
    }
  }

  private void matchRedirect(int index, NetworkResource redirectCandidate,
      ResourceUpdateEvent resourceUpdate) {
    // We have found the redirect.
    redirectCandidate.update(resourceUpdate);
    // Should not be a concurrent modification, since we now bail out of
    // the loop right after mutating the queue.
    redirectQueue.remove(index);

    // We know that redirects get a single update that populates all the
    // fields. We assume the graph is closed out.
    openRequests--;
    double endTime = redirectCandidate.getEndTime();

    assert (!Double.isNaN(endTime) && endTime > 0) : "endTime unset for redirect update!";

    // Add the data point.
    getGraphModel().addData(endTime, openRequests);
  }
}
