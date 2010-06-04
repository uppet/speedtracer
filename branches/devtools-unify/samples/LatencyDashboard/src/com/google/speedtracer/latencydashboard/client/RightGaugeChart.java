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
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.visualization.client.DataTable;
import com.google.gwt.visualization.client.visualizations.Gauge;

import java.util.ArrayList;
import java.util.List;

/**
 * Gauge chart that sits to the right of the timeline graph.
 */
public class RightGaugeChart extends Composite {
  private Gauge gaugeChart = new Gauge();
  private final Gauge.Options gaugeOptions;
  private Legend legend = new Legend();
  private List<String> legendColors = new ArrayList<String>();
  private DockLayoutPanel outerPanel = new DockLayoutPanel(Unit.PX);

  public RightGaugeChart(RightPieChart.Resources resources, Gauge.Options gaugeOptions) {
    RightPieChart.Css css = resources.rightPieChartCss();
    SimplePanel gaugeWrapper = new SimplePanel();
    // Center the gauge.
    gaugeWrapper.addStyleName(css.gaugeWrapper());
    gaugeWrapper.add(gaugeChart);
    gaugeOptions.setHeight(LatencyDashboardChart.RIGHT_CHART_HEIGHT);
    outerPanel.addNorth(gaugeWrapper, LatencyDashboardChart.RIGHT_CHART_HEIGHT);
    
    SimplePanel wrapper = new SimplePanel();
    wrapper.addStyleName(css.legendWrapper());
    wrapper.add(legend);
    outerPanel.add(wrapper);
    initWidget(outerPanel);
    this.gaugeOptions = gaugeOptions;
  }

  public void addItem(String color, String title) {
    legend.addItem(color, title);
    legendColors.add(color);
  }

  public void clear() {
    legend.clear();
    legendColors.clear();
  }

  public void draw(DataTable data) {
    gaugeChart.draw(data, gaugeOptions);
  }
}
