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
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.topspin.ui.client.Container;
import com.google.gwt.topspin.ui.client.ResizeEvent;
import com.google.gwt.topspin.ui.client.ResizeListener;
import com.google.gwt.topspin.ui.client.ScrollWheelEvent;
import com.google.gwt.topspin.ui.client.ScrollWheelListener;
import com.google.gwt.topspin.ui.client.Window;
import com.google.speedtracer.client.MonitorConstants;
import com.google.speedtracer.client.model.Visualization;
import com.google.speedtracer.client.timeline.Constants;
import com.google.speedtracer.client.timeline.TimeLine;
import com.google.speedtracer.client.timeline.TimeLineModel;
import com.google.speedtracer.client.timeline.TimeLineModel.WindowBoundsObserver;

import java.util.List;

/**
 * TimeLine of entire monitoring domain. Overview of recorded data.
 */
public class OverViewTimeLine extends TimeLine {
  /**
   * CSS used in {@link OverViewTimeLine}.
   */
  public interface Css extends CssResource {
    String overViewTimeLine();
  }

  /**
   * The {@link TimeLineModel} to be used for the {@link OverViewTimeLine}.
   * Overrides some update methods to modify some state here in the View.
   */
  public static class OverViewTimeLineModel extends TimeLineModel implements
      WindowBoundsObserver {
    private OverViewTimeLine view;

    public OverViewTimeLineModel() {
      super(true, true);
    }

    @Override
    public void onDomainChange(double newValue) {
      // We want the overview graph to always show 5% more of total data that
      // exists in the overview range. We also never want to "shrink" our
      // overview.
      newValue = newValue
          + ((getRightBound() - getLeftBound()) * MonitorConstants.EXTRA_DOMAIN_PADDING);
      double newMaxDomainVal = Math.max(newValue, getMostRecentDomainValue());
      super.onDomainChange(newMaxDomainVal);
    }

    /**
     * When the maintimeline ticks, we also want to update our grippies.
     */
    public void onWindowBoundsChange(double left, double right) {
      view.domainSelection.setLeftBound(view.mainTimeLine.getModel().getLeftBound());
      view.domainSelection.setRightBound(view.mainTimeLine.getModel().getRightBound());
    }

    /**
     * When our graph update tick timer fires, we also want to readjust the
     * selection grippies positions as well since the graph may have new data
     * streamed in, and the pixel->domain ratio may ahve changed.
     */
    @Override
    public void updateBounds(double left, double right) {
      super.updateBounds(left, right);
      view.domainSelection.setLeftBound(view.mainTimeLine.getModel().getLeftBound());
      view.domainSelection.setRightBound(view.mainTimeLine.getModel().getRightBound());
    }

    /**
     * This gets called from within the constructor of OverViewTimeLine.
     * 
     * @param view
     */
    private void setOverViewTimeLine(OverViewTimeLine view) {
      this.view = view;
    }
  }

  /**
   * ClientBundle resource interface.
   */
  public interface Resources extends DomainRegionSelection.Resources,
      OverViewGraph.Resources {
    @Source("resources/OverViewTimeLine.css")
    Css overViewTimeLineCss();

    @Source("resources/new-data.png")
    ImageResource overviewTimeLineNewData();
  }

  private final DomainRegionSelection domainSelection;

  /**
   * A reference to the MainTimeLine we will be bound to.
   */
  private final MainTimeLine mainTimeLine;

  public OverViewTimeLine(Container container, MainTimeLine mainTimeLine,
      OverViewTimeLineModel timeLineModel,
      List<Visualization<?, ?>> visualizations, Resources resources) {
    super(container, resources.overViewTimeLineCss().overViewTimeLine(),
        timeLineModel);
    timeLineModel.setOverViewTimeLine(this);
    domainSelection = new DomainRegionSelection(getGraphContainerElement(),
        this, resources);

    this.mainTimeLine = mainTimeLine;

    // Register our model as a consumer of events from this MainTimeLine
    mainTimeLine.getModel().addDomainObserver(timeLineModel);
    mainTimeLine.getModel().addWindowBoundsObserver(timeLineModel);

    addGraph(new OverViewGraph(this, visualizations, resources));

    sinkEvents();
  }

  public MainTimeLine getMainTimeLine() {
    return mainTimeLine;
  }

  /**
   * Sees when the MainTimeLine's current bounds changes. Adjusts the selection
   * region accordingly.
   */
  public void onWindowBoundsChange(double left, double right) {
    domainSelection.setLeftBound(left);
    domainSelection.setRightBound(right);
  }

  /**
   * Resets our overview graph's displayable bounds.
   * 
   * It is up to the caller to ensure that the state and underlying data for our
   * visualizations are cleaned up since it will no longer be reachable after
   * this is called.
   */
  public void resetDisplayableBounds() {
    double newLeft = 0;
    double newRight = Constants.DEFAULT_GRAPH_WINDOW_SIZE;
    getModel().updateBounds(newLeft, newRight);
    domainSelection.setLeftBound(newLeft);
    domainSelection.setRightBound(newRight);
    mainTimeLine.getModel().updateBounds(newLeft, newRight);
  }

  @Override
  public void transitionTo(double newLeft, double newRight) {
    // We technically do not drive the transitions. MainTimeLine does. We
    // just follow. So if someone tells us to transition... we forward it to
    // mainTimeLine.
    mainTimeLine.transitionTo(newLeft, newRight);
  }

  public void zoom(int delta) {
    doMouseScroll(delta, getMainTimeLine().getModel().getLeftBound(),
        getMainTimeLine().getModel().getRightBound(),
        getModel().getLeftBound(), getModel().getRightBound(),
        getMainTimeLine());
  }

  public void zoomAll() {
    getMainTimeLine().transitionTo(getModel().getLeftBound(),
        getModel().getRightBound());
  }

  /*
   * Interaction semantics are different for the overview.
   */
  private void sinkEvents() {
    ScrollWheelListener scrollListener = new ScrollWheelListener() {

      public void onMouseScroll(ScrollWheelEvent event) {
        if (!mainTimeLine.isAnimating()) {
          int delta = ((ScrollWheelEvent) event).getWheelDelta();
          doMouseScroll(delta, mainTimeLine.getModel().getLeftBound(),
              mainTimeLine.getModel().getRightBound(),
              getModel().getLeftBound(), getModel().getRightBound(),
              mainTimeLine);
        }
      }

    };

    ScrollWheelEvent.addScrollWheelListener(this, getElement(), scrollListener);
    ScrollWheelEvent.addScrollWheelListener(mainTimeLine,
        mainTimeLine.getGraphContainerElement(), scrollListener);

    // WindowLevelEvents
    ResizeEvent.addResizeListener(Window.get(), Window.get(),
        new ResizeListener() {
          public void onResize(ResizeEvent event) {
            recomputeGraphDimensions();
            onWindowBoundsChange(mainTimeLine.getModel().getLeftBound(),
                mainTimeLine.getModel().getRightBound());
          }
        });
  }
}
