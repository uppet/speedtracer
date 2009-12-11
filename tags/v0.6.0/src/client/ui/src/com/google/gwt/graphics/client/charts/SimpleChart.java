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
package com.google.gwt.graphics.client.charts;

import com.google.gwt.topspin.ui.client.Container;
import com.google.gwt.topspin.ui.client.DefaultContainerImpl;
import com.google.gwt.topspin.ui.client.Div;

import java.util.ArrayList;
import java.util.List;

/**
 * Simple base class for Charts that are backed by a single DataTuple list.
 */
public abstract class SimpleChart extends Div {
  private final List<ColorCodedValue> data = new ArrayList<ColorCodedValue>();
  private Container myContainer;
  private double dataTotal = 0;

  /**
   * Creates a new SimpleChart, attaches to the supplied container, and sets the
   * supplied data as the backing data for the Chart.
   * 
   * @param container the Container we attach to.
   * @param data the data for the chart.
   */
  public SimpleChart(Container container, List<ColorCodedValue> data) {
    super(container);
    myContainer = new DefaultContainerImpl(getElement());
    setData(data);
  }

  private void addData(ColorCodedValue entry) {
    dataTotal += entry.value;
    data.add(new ColorCodedValue(entry.key, entry.value, entry.labelColor));
  }

  public Container getContainer() {
    return myContainer;
  }

  public List<ColorCodedValue> getData() {
    return data;
  }

  public double getDataTotal() {
    return dataTotal;
  }

  public void hideLegend() {
    getLegend().setVisible(false);
  }

  /**
   * Makes a local copy of the data, and sets this new data as the backing data
   * for the chart.
   * 
   * @param data
   */
  public void setData(List<ColorCodedValue> data) {
    this.data.clear();
    for (int i = 0, n = data.size(); i < n; i++) {
      addData(data.get(i));
    }
  }

  public void showLegend() {
    getLegend().setVisible(true);
  }

  /**
   * Allow the details of how the subclasses Legend is constructed, and rendered
   * to be left up to the derived classes. Also affords for lazy creation of the
   * Legend.
   * 
   * @return the {@link Legend} for this chart.
   */
  protected abstract Legend getLegend();

  protected abstract void render();
}
