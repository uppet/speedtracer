/*
 * Copyright 2010 Google Inc.
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
package com.google.speedtracer.latencydashboard.client;

import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.visualization.client.DataTable;
import com.google.gwt.visualization.client.LegendPosition;
import com.google.gwt.visualization.client.visualizations.PieChart;
import com.google.gwt.visualization.client.visualizations.PieChart.Options;

import java.util.ArrayList;
import java.util.List;

/**
 * Pie chart that sits at the right side of the graphs.
 */
public class RightPieChart extends Composite {

  /**
   * Css definitions for this UI component.
   */
  public interface Css extends CssResource {
    String legendWrapper();
    String gaugeWrapper();
  }

  /**
   * Resources for CSS and images.
   */
  public interface Resources extends ClientBundle {
    @Source("resources/RightPieChart.css")
    Css rightPieChartCss();
  }

  private static final int PIE_CHART_HEIGHT = (int) (LatencyDashboardChart.CHART_HEIGHT * .75);

  /**
   * Options to pass to the pie chart.
   */
  public static PieChart.Options createOptions() {
    Options options = PieChart.Options.create().<Options> cast();
    options.setLegend(LegendPosition.NONE);
    options.setHeight(PIE_CHART_HEIGHT);
    return options;
  }

  private Legend legend = new Legend();
  private List<String> legendColors = new ArrayList<String>();
  private int numItems = 0;
  private DockLayoutPanel outerPanel = new DockLayoutPanel(Unit.PX);
  private PieChart pieChart = new PieChart();

  public RightPieChart(Resources resources) {
    Css css = resources.rightPieChartCss();
    outerPanel.addNorth(pieChart, PIE_CHART_HEIGHT);
    SimplePanel wrapper = new SimplePanel();
    wrapper.addStyleName(css.legendWrapper());
    wrapper.add(legend);
    outerPanel.add(wrapper);
    initWidget(outerPanel);
  }

  public void addItem(String color, String title) {
    numItems++;
    legend.addItem(color, title);
    legendColors.add(color);
  }

  public void clear() {
    numItems = 0;
    legend.clear();
    legendColors.clear();
  }

  public void draw(DataTable data) {
    draw(data, null);
  }

  public void draw(DataTable data, Options options) {
    if (options == null) {
      options = createOptions();
    }
    String[] colors = new String[numItems];
    options.setColors(legendColors.toArray(colors));
    pieChart.draw(data, options);
  }
}
