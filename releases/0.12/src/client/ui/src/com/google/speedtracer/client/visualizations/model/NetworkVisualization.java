/*
 * Copyright 2008 Google Inc.
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

import com.google.gwt.topspin.ui.client.Container;
import com.google.speedtracer.client.model.NetworkResource;
import com.google.speedtracer.client.model.Visualization;
import com.google.speedtracer.client.timeline.Constants;
import com.google.speedtracer.client.timeline.GraphUiProps;
import com.google.speedtracer.client.view.FastTooltip;
import com.google.speedtracer.client.view.MainTimeLine;
import com.google.speedtracer.client.visualizations.view.NetworkTimeLineDetailView;
import com.google.speedtracer.client.visualizations.view.PageTransitionMarker;

/**
 * Monitors network events.
 */
public class NetworkVisualization extends
    Visualization<NetworkTimeLineDetailView, NetworkVisualizationModel> implements
    NetworkVisualizationModel.ResourceRefreshListener {

  /**
   * Resources used by children of NetworkVisualization.
   */
  public interface Resources extends PageTransitionMarker.Resources,
      FastTooltip.Resources, NetworkTimeLineDetailView.Resources {
  }

  public static final String TITLE = "Network";

  /**
   * Default Scale Max for Y Axis.
   */
  private static final double defaultPendingRequestCount = 4;

  private static final String SUBTITLE = "resources";

  private static GraphUiProps createGraphUiProps() {
    return new GraphUiProps(Constants.NETWORK_GRAPH_COLOR,
        Constants.GRAPH_STROKE_COLOR, defaultPendingRequestCount);
  }

  private final NetworkVisualization.Resources resources;

  /**
   * Constructor.
   * 
   * @param parent The containing {@link MainTimeLine}
   * @param model
   * @param detailsContainer
   * @param resources
   */
  public NetworkVisualization(MainTimeLine parent, NetworkVisualizationModel model,
      Container detailsContainer, NetworkVisualization.Resources resources) {
    super(TITLE, SUBTITLE, model, createGraphUiProps());
    this.resources = resources;
    setDetailsView(createDetailsView(detailsContainer, parent));
  }

  @Override
  public NetworkTimeLineDetailView createDetailsView(Container container,
      MainTimeLine timeLine) {
    return new NetworkTimeLineDetailView(container, this, resources);
  }

  public void onResourceRefresh(NetworkResource resource) {
    this.getDetailsView().refreshResource(resource);
  }

  @Override
  public void setModel(VisualizationModel model) {
    super.setModel(model);
    NetworkVisualizationModel nModel = (NetworkVisualizationModel) model;
    nModel.addResourceRefreshListener(this);
  }
}
