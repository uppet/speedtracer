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

import com.google.gwt.visualization.client.DataTable;
import com.google.gwt.visualization.client.LegendPosition;
import com.google.gwt.visualization.client.AbstractDataTable.ColumnType;
import com.google.gwt.visualization.client.visualizations.AreaChart;
import com.google.gwt.visualization.client.visualizations.PieChart;
import com.google.speedtracer.latencydashboard.shared.DashboardRecord;

/**
 * Shows some of the lightweight metrics in a stacked chart.
 */
public class GwtLightweightMetricsChart extends LatencyDashboardChart {
  private static final String bootstrapDurationColor = "#4684ee";
  private static final String bootstrapDurationTitle = "Bootstrap Duration";
  private static final String loadExternalRefsColor = "#dc3912";
  private static final String loadExternalRefsTitle = "Load External Refs";
  private static final String moduleStartupColor = "#ff9900";
  private static final String moduleStartupTitle = "Module Startup";
  private static final String revisionTitle = "Revision";
  private AreaChart leftChart;
  private Legend legend;
  private PieChart rightChart;

  public GwtLightweightMetricsChart(String title) {
    super(title);
    rightChart = new PieChart();
    chartPanel.addEast(rightChart, chartHeight);
    leftChart = new AreaChart();
    chartPanel.add(leftChart);
  }

  public void addLegend() {
    if (legend == null) {
      legend = new Legend();
      readoutPanel.addEast(legend, 200);
    }
    legend.clear();
    legend.addRow(bootstrapDurationColor, bootstrapDurationTitle);
    legend.addRow(loadExternalRefsColor, loadExternalRefsTitle);
    legend.addRow(moduleStartupColor, moduleStartupTitle);
  }

  public void addRow(DataTable dataTable, int row, DashboardRecord record) {
    dataTable.setCell(row, 0, record.getRevision(), null, null);
    dataTable.setCell(row, 1, record.bootstrapDuration, null, null);
    dataTable.setCell(row, 2, record.loadExternalRefsDuration, null, null);
    dataTable.setCell(row, 3, record.moduleStartupDuration, null, null);
  }

  public void populateChart(DashboardRecord[] serverData) {
    addLegend();
    populateLastData(serverData[0]);
    populateTimeline(serverData);
  }

  public void populateLastData(DashboardRecord serverData) {
    DataTable dataTable = DataTable.create();
    dataTable.addColumn(ColumnType.STRING, "Type");
    dataTable.addColumn(ColumnType.NUMBER, "Milliseconds");
    dataTable.addRows(3);
    int row = 0;
    dataTable.setCell(row, 0, bootstrapDurationTitle, null, null);
    dataTable.setCell(row++, 1, serverData.bootstrapDuration, null, null);
    dataTable.setCell(row, 0, loadExternalRefsTitle, null, null);
    dataTable.setCell(row++, 1, serverData.loadExternalRefsDuration, null, null);
    dataTable.setCell(row, 0, moduleStartupTitle, null, null);
    dataTable.setCell(row++, 1, serverData.moduleStartupDuration, null, null);
    PieChart.Options options = PieChart.Options.create();
    options.setLegend(LegendPosition.NONE);
    options.setHeight(chartHeight);
    rightChart.draw(dataTable, options);
  }

  public void populateTimeline(DashboardRecord[] serverData) {
    DataTable dataTable = DataTable.create();
    dataTable.addColumn(ColumnType.STRING, revisionTitle);
    dataTable.addColumn(ColumnType.NUMBER, bootstrapDurationTitle);
    dataTable.addColumn(ColumnType.NUMBER, loadExternalRefsTitle);
    dataTable.addColumn(ColumnType.NUMBER, moduleStartupTitle);
    int length = serverData.length;
    dataTable.addRows(length);
    for (int i = 0; i < length; ++i) {
      addRow(dataTable, length - (i + 1), serverData[i]);
    }

    AreaChart.Options options = AreaChart.Options.create();
    options.setLegend(LegendPosition.NONE);
    options.setHeight(275);
    options.setStacked(true);
    leftChart.draw(dataTable, options);
  }
}
