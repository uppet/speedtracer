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

import com.google.gwt.graphics.client.Canvas;
import com.google.gwt.graphics.client.Color;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.CssResource.Strict;
import com.google.gwt.topspin.ui.client.Container;
import com.google.gwt.topspin.ui.client.DefaultContainerImpl;
import com.google.gwt.topspin.ui.client.Widget;

import java.util.List;

/**
 * A pie chart!
 */
public class PieChart extends SimpleChart {
  /**
   * Legend styles.
   */
  public interface Css extends CssResource {
    String legend();

    String pieChart();
  }

  /**
   * Resources interface.
   */
  public interface Resources extends Legend.Resources {
    @Source("resources/PieChart.css")
    @Strict()
    PieChart.Css pieChartCss();
  }

  private static final int COORD_RADIUS = 50;
  private static final int PADDING = 10;
  private static final int CANVAS_COORD_SIZE = COORD_RADIUS * 2 + PADDING;

  private final Canvas canvas;
  private Legend legend;
  private final PieChart.Resources resources;

  public PieChart(Widget parent, List<ColorCodedValue> data,
      PieChart.Resources resources) {
    this(new DefaultContainerImpl(parent.getElement()), data, resources);
  }

  public PieChart(Container container, List<ColorCodedValue> data,
      PieChart.Resources resources) {
    super(container, data);
    // We want our wrapping container to size to the contents.
    getElement().getStyle().setProperty("display", "inline-block");
    this.resources = resources;
    canvas = new Canvas(CANVAS_COORD_SIZE, CANVAS_COORD_SIZE);
    getElement().appendChild(canvas.getElement());
    canvas.getElement().setClassName(resources.pieChartCss().pieChart());
    canvas.setStrokeStyle(Color.MIDNIGHT_BLUE);
    canvas.setLineWidth(1.0);

    render();
  }

  public void resize(int width, int height) {
    canvas.resize(width, height);
  }

  @Override
  public void setData(List<ColorCodedValue> data) {
    super.setData(data);
    if (legend != null) {
      // It has been queried before and is already attached to us.
      boolean wasVisible = legend.isVisible();

      legend.destroy();
      legend = null;

      // Redisplay if needed. We invoke the hide/show methods and not simply
      // setVisible() because they call ensureLegend() first.
      if (wasVisible) {
        showLegend();
      } else {
        hideLegend();
      }
    }
  }

  @Override
  protected Legend getLegend() {
    ensureLegend();
    return legend;
  }

  @Override
  protected void render() {
    canvas.clear();
    double lastAngle = 0;
    double center = CANVAS_COORD_SIZE / 2;
    List<ColorCodedValue> data = getData();
    // Draw pie chart
    for (int i = 0, n = data.size(); i < n; i++) {
      ColorCodedValue entry = data.get(i);
      canvas.setFillStyle(entry.labelColor);
      double arcFraction = entry.value / getDataTotal();
      double arcAngle = Math.PI * 2 * arcFraction;
      double newAngle = lastAngle + arcAngle;
      canvas.beginPath();
      canvas.moveTo(center, center);
      canvas.arc(center, center, COORD_RADIUS, lastAngle, newAngle, false);
      canvas.fill();
      canvas.stroke();
      lastAngle = newAngle;
    }
  }

  private void ensureLegend() {
    if (legend == null) {
      legend = new Legend(this, getData(), getDataTotal(), false, true,
          resources);
      legend.getElement().setClassName(resources.pieChartCss().legend());
    }
  }
}
