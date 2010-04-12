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

import com.google.gwt.dom.client.Element;
import com.google.gwt.topspin.ui.client.Container;
import com.google.speedtracer.client.model.UiEvent;
import com.google.speedtracer.client.model.UiEventModel;
import com.google.speedtracer.client.model.Visualization;
import com.google.speedtracer.client.timeline.Constants;
import com.google.speedtracer.client.timeline.GraphUiProps;
import com.google.speedtracer.client.view.MainTimeLine;
import com.google.speedtracer.client.visualizations.view.CurrentSelectionMarker;
import com.google.speedtracer.client.visualizations.view.SluggishnessDetailView;
import com.google.speedtracer.client.visualizations.view.TransientMarker;

/**
 * Monitors Sluggishness.
 */
public class SluggishnessVisualization extends
    Visualization<SluggishnessDetailView, SluggishnessModel> implements
    UiEventModel.Listener, SluggishnessModel.EventRefreshListener {

  /**
   * Resources for children of {@link SluggishnessVisualization}.
   */
  public interface Resources extends SluggishnessDetailView.Resources {
  }

  public static final String TITLE = "Sluggishness";

  private static final String SUBTITLE = "events";

  /**
   * Constructor for the Graph UI properties that govern the graph's look and
   * feel.
   */
  private static GraphUiProps createGraphUiProps() {
    return new GraphUiProps(Constants.SLUGGISHNESS_GRAPH_COLOR,
        Constants.GRAPH_STROKE_COLOR,
        SluggishnessModel.defaultSluggishnessYScale);
  }

  private final SluggishnessVisualization.Resources resources;

  private final MainTimeLine timeline;

  /**
   * Constructor.
   * 
   * @param timeline The parent {@link MainTimeLine} for this Visualization.
   * @param sluggishnessModel the backing {@link VisualizationModel} for this
   *          Visualization.
   * @param sourceModel the {@link UiEventModel} that this Visualization is a
   *          subscribed to.
   */
  public SluggishnessVisualization(MainTimeLine timeline,
      SluggishnessModel sluggishnessModel, UiEventModel sourceModel,
      Container detailsContainer, SluggishnessVisualization.Resources resources) {
    super(TITLE, SUBTITLE, sluggishnessModel, createGraphUiProps());
    this.resources = resources;
    this.timeline = timeline;

    // Add the Transient Markers for page boundaries and current event
    // selection.
    setCurrentEventMarkerModel(new TransientMarkerModel() {

      @Override
      public TransientMarker createTransientMarkerInstance(Element element,
          MainTimeLine mainTimeLine, CurrentSelectionMarker.Resources resources) {
        return new CurrentSelectionMarker(element, this, mainTimeLine,
            resources);
      }

    });

    // The visualization may choose to update the view outside the model in
    // append only update cases where our right bound is in the future
    sourceModel.addListener(this);
    setDetailsView(createDetailsView(detailsContainer, timeline));
    setModel(sluggishnessModel);
  }

  /**
   * Required implementation of the constructor for the DetailsView.
   */
  @Override
  public SluggishnessDetailView createDetailsView(Container container,
      MainTimeLine timeLine) {
    return new SluggishnessDetailView(container, this,
        getModel().getSourceModel(), resources);
  }

  public MainTimeLine getTimeline() {
    return timeline;
  }

  public void onEventRefresh(UiEvent event) {
    this.getDetailsView().refreshRecord(event);
  }

  public void onUiEventFinished(UiEvent event) {
    // We can update the detail view outside the model itself. We normally
    // update the model, and then the view queries the model for state. But in
    // the case where we have data streaming into our current view, we can
    // short circuit this and just append rows to the detail view.
    SluggishnessDetailView details = getDetailsView();
    if (details != null) {
      SluggishnessModel model = getModel();

      double dispatchTime = event.getTime();
      double endTime = event.getEndTime();

      double currentRight = model.getCurrentRight();
      double currentLeft = model.getCurrentLeft();

      // For initial startup, we want to avoid doing binary searches for within
      // the window bounds. so we do a quick bounds check and append to the
      // window
      // if we need to.
      if (dispatchTime < currentRight && endTime > currentLeft) {
        details.shortCircuitAddEvent(event);
      }
    }
  }

  @Override
  public void setModel(VisualizationModel model) {
    super.setModel(model);
    SluggishnessModel sModel = (SluggishnessModel) model;
    sModel.addRecordRefreshListener(this);
  }
}
