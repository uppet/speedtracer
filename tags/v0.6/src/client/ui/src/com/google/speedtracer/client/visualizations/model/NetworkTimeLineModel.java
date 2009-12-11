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

import com.google.speedtracer.client.model.ApplicationState;
import com.google.speedtracer.client.model.DataModel;
import com.google.speedtracer.client.model.EventRecord;
import com.google.speedtracer.client.model.HintRecord;
import com.google.speedtracer.client.model.HintletEngineHost;
import com.google.speedtracer.client.model.NetworkResource;
import com.google.speedtracer.client.model.NetworkResourceError;
import com.google.speedtracer.client.model.NetworkResourceFinished;
import com.google.speedtracer.client.model.NetworkResourceModel;
import com.google.speedtracer.client.model.NetworkResourceRecord;
import com.google.speedtracer.client.model.NetworkResourceResponse;
import com.google.speedtracer.client.model.NetworkResourceStart;
import com.google.speedtracer.client.model.TabChange;
import com.google.speedtracer.client.timeline.GraphModel;
import com.google.speedtracer.client.timeline.HighlightModel;
import com.google.speedtracer.client.timeline.ModelData;
import com.google.speedtracer.client.util.IterableFastStringMap;
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

  private List<ResourceRefreshListener> resourceRefreshListeners = new ArrayList<ResourceRefreshListener>();

  /**
   * Map of NetworkResource POJOs. Information about a network resource is
   * filled in progressively as we get chunks of information about it.
   */
  private final IterableFastStringMap<NetworkResource> resourceStore = new IterableFastStringMap<NetworkResource>();

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
    resourceStore.put(resource.getResourceId(), resource);
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
  public NetworkResource getResource(String id) {
    return resourceStore.get(id);
  }

  public List<NetworkResource> getSortedResources() {
    return sortedResources;
  }

  public void onHint(HintRecord hintlet) {
    // Only process hintlet references to a Ui Event
    int refRecord = hintlet.getRefRecord();
    EventRecord rec = dataModel.findEventRecord(refRecord);
    if (!NetworkResourceRecord.isNetworkResourceRecord(rec)) {
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
    NetworkResource res = findResourceForRecord((NetworkResourceRecord) rec);
    fireResourceRefreshListeners(res);
  }

  public void onNetworkResourceRequestStarted(NetworkResourceStart resourceStart) {
    openRequests++;
    getGraphModel().addData(resourceStart.getTime(), openRequests);
    NetworkResource resource = new NetworkResource(resourceStart);
    addResource(resource);
  }

  public void onNetworkResourceResponseFailed(NetworkResourceError resourceError) {
    openRequests--;
    getGraphModel().addData(resourceError.getTime(), openRequests);
    NetworkResource resource = getResource(resourceError.getResourceId());
    if (resource != null) {
      assert (Double.isNaN(resource.getEndTime())) : "Error end time should not be NaN.";
      resource.update(resourceError);
    }
  }

  public void onNetworkResourceResponseFinished(
      NetworkResourceFinished resourceFinish) {
    openRequests--;
    getGraphModel().addData(resourceFinish.getTime(), openRequests);
    NetworkResource resource = getResource(resourceFinish.getResourceId());
    if (resource != null) {
      assert (!resource.didError()) : "Wrong callback on resource error";
      resource.update(resourceFinish);
    }
  }

  public void onNetworkResourceResponseStarted(
      NetworkResourceResponse resourceResponse) {
    NetworkResource resource = getResource(resourceResponse.getResourceId());
    if (resource != null) {
      resource.update(resourceResponse);
    }
  }

  /**
   * We want to reach back to find the Network Resource request that corresponds
   * to this page transition. We simply match by URL and pull the sequence
   * number for the NetworkResourceStart Event. We then replay the events that
   * occurred after that start, since it is pertinent to the new page
   * transition.
   */
  public void transferEndingState(ApplicationState oldState,
      ApplicationState newState, String newUrl) {

    // We need to do something a little bit tricky. A page transition gets
    // triggered sometime after receiving the response for the main resource
    // for the new page. As such, we need to find the start time of the network
    // request associated with this main resource.
    int index = sortedResources.size() - 1;
    NetworkResource relatedResource = null;
    // Walk until we hit the resource with the same url as the page
    // transition.
    while (index >= 0
        && !(relatedResource = sortedResources.get(index)).getUrl().equals(
            newUrl)) {
      index--;
    }

    // This means we got a page transition before a network resource. Should
    // never happen in practice, but we should not crash.
    if (relatedResource == null) {
      newState.setFirstDomainValue(oldState.getLastDomainValue());
      return;
    }

    // The start time for the resource that corresponds to this page transition.
    newState.setFirstDomainValue(relatedResource.getStartTime());

    // We want to replay data that happen after the main resource has started.
    JsIntegerMap<EventRecord> eventRecords = oldState.getDataModel().getEventRecordMap();
    int seqNumber = relatedResource.getStartEvent().getSequence();
    EventRecord record;
    while ((record = eventRecords.get(seqNumber)) != null) {
      if (record.getType() != TabChange.TYPE) {
        newState.getDataModel().fireOnEventRecord(record);
      }
      seqNumber++;
    }
  }

  private NetworkResource findResourceForRecord(NetworkResourceRecord rec) {
    // A simple linear scan will suffice - we don't expect this list to get very
    // big.
    for (int i = 0, l = sortedResources.size(); i < l; ++i) {
      NetworkResource res = sortedResources.get(i);
      if (res.getStartTime() > rec.getTime()) {
        // The rest of the resources in this list started too late.
        return null;
      }
      if (res.hasRecord(rec)) {
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
}
