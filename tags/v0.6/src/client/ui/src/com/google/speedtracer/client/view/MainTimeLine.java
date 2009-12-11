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
package com.google.speedtracer.client.view;

import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.CssResource.Strict;
import com.google.gwt.topspin.ui.client.Container;
import com.google.speedtracer.client.model.Visualization;
import com.google.speedtracer.client.timeline.Constants;
import com.google.speedtracer.client.timeline.TimeLine;
import com.google.speedtracer.client.timeline.TimeLineGraph;
import com.google.speedtracer.client.timeline.TimeLineModel;
import com.google.speedtracer.client.timeline.TransientGraphSelection;
import com.google.speedtracer.client.timeline.fx.Zoom;
import com.google.speedtracer.client.visualizations.model.NetworkVisualization;
import com.google.speedtracer.client.visualizations.model.SluggishnessVisualization;

import java.util.ArrayList;
import java.util.List;

/**
 * The main UI which contains the top graphs and central Details views.
 */
public class MainTimeLine extends TimeLine {

  /**
   * Css for MainTimeLine.
   */
  public interface Css extends CssResource {
    String mainTimeLine();
  }

  /**
   * Externalized resources interface.
   */
  public interface Resources extends DetailViews.Resources,
      MainGraph.Resources, OverViewGraph.Resources,
      TransientGraphSelection.Resources, NetworkVisualization.Resources,
      SluggishnessVisualization.Resources {
    @Source("resources/MainTimeLine.css")
    @Strict
    MainTimeLine.Css mainTimeLineCss();
  }

  private Zoom transition;

  public MainTimeLine(Container container,
      List<Visualization<?, ?>> visualizations, TimeLineModel timeLineModel, Zoom.CallBack cb,
      MainTimeLine.Resources resources) {
    super(container, resources.mainTimeLineCss().mainTimeLine(), timeLineModel);
    this.transition = new Zoom(this);
    transition.setCallBack(cb);
    addGraph(new MainGraph(this, visualizations, resources));
    // Create the BlockingTransientSelection. It installs itself.
    new TransientGraphSelection(this, resources);
  }

  public boolean isAnimating() {
    return transition.isInProgress();
  }

  @Override
  public void transitionTo(double newLeft, double newRight) {
    if (!transition.isInProgress()) {
      transition.zoom(Constants.ZOOM_DURATION, newLeft, newRight);
    }
  }

  public void updateYAxisLabel(double value, String yAxisUnit) {
    ArrayList<TimeLineGraph> graphs = getModel().getGraphs();
    for (TimeLineGraph graph : graphs) {
      ((MainGraph) graph).updateScaleLabel(value, yAxisUnit);
    }
  }
}
