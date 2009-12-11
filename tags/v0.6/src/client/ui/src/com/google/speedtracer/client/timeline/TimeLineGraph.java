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
package com.google.speedtracer.client.timeline;

import com.google.gwt.dom.client.Element;
import com.google.gwt.graphics.client.Canvas;
import com.google.gwt.topspin.ui.client.DefaultContainerImpl;
import com.google.gwt.topspin.ui.client.Div;
import com.google.speedtracer.client.timeline.TimeLineModel.WindowBoundsObserver;

/**
 * Graph that gets added to a TimeLine.
 */
public abstract class TimeLineGraph extends Div implements
    WindowBoundsObserver {
  // Canvas coordinates
  protected static final int COORD_X_WIDTH = 1000;

  protected static final int COORD_Y_HEIGHT = 100;

  // The graph
  protected final Canvas canvas;

  /**
   * The number of canvas coordinates in each block. Where a block is the
   * logical division of the screen into {@link plotPrecision} number of
   * sections.
   */
  private double coordDelta;

  /**
   * The number of domain units in each block. Where a block is the logical
   * division of the screen into {@link plotPrecision} number of sections.
   */
  private double domainDelta;

  /**
   * Range in the traditional sense. NOT as in Y axis. Computed delta between
   * leftBound and rightBound. This value is cached so it doesn't need to be
   * recomputed in each paint() method.
   */
  private double domainRange;

  // The window bounds for the subset of the data we are viewing.
  // leave private because we cache computations based on this value.
  private double leftBound;

  // Level of Detail Plot Precision.
  private int lodPlotPrecision;

  // The number of Datapoints we render on the scale
  private int plotPrecision;

  private double rightBound;

  private final TimeLine timeLine;

  /**
   * This is a convenience constructor that uses the domain width for the
   * timeline.
   * 
   * @param parent the {@link TimeLine} in which this Graph lives
   */
  protected TimeLineGraph(TimeLine parent) {
    this(parent, 0, Constants.DEFAULT_GRAPH_WINDOW_SIZE,
        Constants.PLOT_PRECISION);
  }

  protected TimeLineGraph(TimeLine parent, double leftBound,
      double rightBound, int precision) {

    super(new DefaultContainerImpl(parent.getGraphContainerElement()));
    timeLine = parent;

    Element elem = getElement();

    // Canvas for drawing the graph.
    canvas = new Canvas(COORD_X_WIDTH, COORD_Y_HEIGHT);
    Element canvasElem = canvas.getElement();

    elem.appendChild(canvasElem);

    this.leftBound = leftBound;
    this.rightBound = rightBound;
    this.plotPrecision = precision;
    this.lodPlotPrecision = plotPrecision / 2;
    recomputeDomain();
  }

  public double getCoordDelta() {
    return coordDelta;
  }

  /**
   * Returns the number of canvas coords used to represent 1 ms of domain time
   * on the graph.
   * 
   * @return the number of canvas coords used to represent 1 ms.
   */
  public double getCoordsPerTime() {
    return COORD_X_WIDTH / domainRange;
  }

  public double getDomainDelta() {
    return domainDelta;
  }

  public double getDomainRange() {
    return domainRange;
  }

  public double getLeftBound() {
    return leftBound;
  }

  public int getPlotPrecision() {
    return plotPrecision;
  }

  public double getRightBound() {
    return rightBound;
  }

  public TimeLine getTimeLine() {
    return timeLine;
  }

  public void onWindowBoundsChange(double left, double right) {
    leftBound = left;
    rightBound = right;
    recomputeDomain();
    paint();
  }

  public void toggleGraphPrecision() {
    int a = plotPrecision;
    plotPrecision = lodPlotPrecision;
    lodPlotPrecision = a;
    recomputeDomain();
  }

  protected abstract void paint();

  /**
   * Perform computations that only change when the bounds change.
   */
  private void recomputeDomain() {
    domainRange = rightBound - leftBound;
    // we recompute Deltas to get LOD changes.
    coordDelta = COORD_X_WIDTH / (double) plotPrecision;
    domainDelta = domainRange / (double) plotPrecision;
  }
}
