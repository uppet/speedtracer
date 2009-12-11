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

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.topspin.ui.client.Container;
import com.google.gwt.topspin.ui.client.DefaultContainerImpl;
import com.google.gwt.topspin.ui.client.Div;
import com.google.gwt.topspin.ui.client.Window;

import java.util.ArrayList;

/**
 * The main parent container. Contains rows of TimeLineGraphs.
 */
public abstract class TimeLine extends Div {
  /**
   * This is a helper for handling ScrollWheel input. Performs bounds
   * computation and applies the transition.
   * 
   * @param delta the raw delta from the scroll event.
   * @param left the left bound of the time window.
   * @param right the right bound of the time window.
   * @param max the total max value we cannot exceed.
   */
  public static void doMouseScroll(int delta, double left, double right,
      double min, double max, TimeLine toAdjust) {
    if (delta == 0) {
      return;
    }

    // cap the velocity of scrolls
    if (delta > Constants.SCROLL_VELOCITY_CAP) {
      delta = Constants.SCROLL_VELOCITY_CAP;
    } else if (delta < -Constants.SCROLL_VELOCITY_CAP) {
      delta = -Constants.SCROLL_VELOCITY_CAP;
    }

    double domainWidth = right - left;
    // delta is auto coerced to double
    double deltaFraction = 1.0 + (delta / 10.0);
    double newDomainWidth = domainWidth * deltaFraction;
    double domainDelta = newDomainWidth - domainWidth;

    // move the bounds
    double newLeft = left + domainDelta;
    double newRight = right - domainDelta;
    // catch cross over
    newLeft = Math.min(newLeft, newRight);
    newRight = Math.max(newLeft, newRight);
    // Cap at bounds
    newLeft = Math.max(min, newLeft);
    newRight = Math.min(max, newRight);

    toAdjust.transitionTo(newLeft, newRight);
  }

  // The current width of the graph in pixels;
  protected int currentGraphWidth;

  // The container element which we attach to.
  private final Container container;

  private final Element graphContainerElement;

  private final TimeLineModel model;

  public TimeLine(Container container, String cssClassName) {
    this(container, cssClassName, new TimeLineModel(false, false));
  }

  protected TimeLine(Container container, String cssClassName,
      TimeLineModel model) {
    super(container);
    Element elem = getElement();
    elem.setClassName(cssClassName);
    this.model = model;
    this.container = new DefaultContainerImpl(elem);
    graphContainerElement = Document.get().createElement("div");
    getElement().appendChild(graphContainerElement);
    graphContainerElement.getStyle().setProperty("position", "relative");

    recomputeGraphDimensions();
  }

  /**
   * Adds a graph to the TimeLine. Adds the graph as a WindowObserver.
   * 
   * @param graph
   */
  public void addGraph(TimeLineGraph graph) {
    model.addGraph(graph);
    getGraphContainerElement().appendChild(graph.getElement());
  }

  public Container getContainer() {
    return container;
  }

  public int getCurrentGraphWidth() {
    return currentGraphWidth;
  }

  public Element getGraphContainerElement() {
    return graphContainerElement;
  }

  public TimeLineModel getModel() {
    return model;
  }

  public void recomputeGraphDimensions() {
    currentGraphWidth = Window.getInnerWidth() - Constants.GRAPH_PIXEL_OFFSET;
  }

  public void refresh() {
    model.refresh();
  }
  
  public void toggleGraphPrecision() {
    ArrayList<TimeLineGraph> graphs = model.getGraphs();
    for (int i = 0, n = graphs.size(); i < n; i++) {
      graphs.get(i).toggleGraphPrecision();
    }
  }

  public abstract void transitionTo(double newLeft, double newRight);
}
