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
package com.google.speedtracer.client.model;

import com.google.gwt.topspin.ui.client.Container;
import com.google.speedtracer.client.timeline.Constants;
import com.google.speedtracer.client.timeline.GraphUiProps;
import com.google.speedtracer.client.util.JSOArray;
import com.google.speedtracer.client.view.DetailView;
import com.google.speedtracer.client.view.MainTimeLine;
import com.google.speedtracer.client.visualizations.model.TransientMarkerModel;
import com.google.speedtracer.client.visualizations.model.VisualizationModel;

/**
 * Base class for Visualizations.
 * 
 * @param <V> the main view and concrete subclass of the DetailView for this
 *          Visualization.
 * @param <M> the underlying model and concrete subclass of the
 *          VisualizationModel for this Visualization.
 */
public abstract class Visualization<V extends DetailView, M extends VisualizationModel> {

  private final JSOArray<ButtonDescription> buttons = JSOArray.create();

  /**
   * Optional field for placing transient overlays on the graph.
   */
  private TransientMarkerModel currentEventMarkerModel;

  /**
   * The View into our visualization that appears in the details view panel.
   */
  private V detailView;

  /**
   * UI properties for our associated graph.
   */
  private final GraphUiProps graphUiProps;

  /**
   * The model underlying this visualization.
   */
  private VisualizationModel model;

  private final String subTitle;

  private final String title;

  public Visualization(String title, String subTitle, M model,
      GraphUiProps graphUiProps) {
    this.title = title;
    this.subTitle = subTitle;
    this.graphUiProps = graphUiProps;
    this.model = model;
  }

  public void addButton(ButtonDescription buttonDescription) {
    buttons.push(buttonDescription);
  }

  public void clearData() {
    model.clearData();
    detailView.updateView(0, Constants.DEFAULT_GRAPH_WINDOW_SIZE, true);
  }

  public JSOArray<ButtonDescription> getButtons() {
    return buttons;
  }

  public TransientMarkerModel getCurrentEventMarkerModel() {
    return currentEventMarkerModel;
  }

  public V getDetailsView() {
    return detailView;
  }

  public GraphUiProps getGraphUiProps() {
    return graphUiProps;
  }

  @SuppressWarnings("unchecked")
  public M getModel() {
    return (M) model;
  }

  public String getSubtitle() {
    return subTitle;
  }

  public String getTitle() {
    return title;
  }

  /**
   * Sets the underlying {@link VisualizationModel} and updates the
   * corresponding detailView.
   * 
   * @param model the VisualizationModel we are setting as our underlying model
   */
  public void setModel(VisualizationModel model) {
    this.model = model;
    detailView.updateView(0, Constants.DEFAULT_GRAPH_WINDOW_SIZE, true);
  }

  /**
   * Creates the {@link DetailView} panel for this Visualization.
   * 
   * @param container the parent Container that the DetailView will be attached
   *          to
   * @param timeLine the {@link MainTimeLine} that contains this Visualization
   * @return
   */
  protected abstract V createDetailsView(Container container,
      MainTimeLine timeLine);

  protected void setCurrentEventMarkerModel(TransientMarkerModel markerModel) {
    this.currentEventMarkerModel = markerModel;
  }

  protected void setDetailsView(V detailsView) {
    this.detailView = detailsView;
  }
}
