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
import com.google.speedtracer.latencydashboard.shared.CustomDashboardRecord;
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
  private AreaChart leftChart;
  private RightPieChart rightChart;

  public GwtLightweightMetricsChart(LatencyDashboardChart.Resources resources,
      String title) {
    super(resources, title);

    rightChart = new RightPieChart(resources);
    chartPanel.addEast(rightChart, CHART_HEIGHT);
    leftChart = new AreaChart();
    chartPanel.add(leftChart);
    addLegend();
  }

  public void addLegend() {
    rightChart.clear();
    rightChart.addItem(bootstrapDurationColor, bootstrapDurationTitle);
    rightChart.addItem(loadExternalRefsColor, loadExternalRefsTitle);
    rightChart.addItem(moduleStartupColor, moduleStartupTitle);
  }

  public void addRow(DataTable dataTable, int row, DashboardRecord record) {

    int column = 0;
    dataTable.setCell(row, column++, record.getRevision() + "-"
        + formatTimestamp(record.getTimestamp()), null, null);
    dataTable.setCell(row, column++, record.bootstrapDuration, null, null);
    dataTable.setCell(row, column++, record.loadExternalRefsDuration, null,
        null);
    dataTable.setCell(row, column++, record.moduleStartupDuration, null, null);
  }

  public void populateChart(DashboardRecord[] serverData) {
    populateLastData(serverData[0]);
    populateTimeline(serverData);
    populateIndicator(serverData);
  }

  public void populateIndicator(DashboardRecord[] serverData) {
    if (serverData.length <= 1) {
      setIndicatorSame();
      return;
    }
    double prev = sumTimes(serverData[1]);
    double curr = sumTimes(serverData[0]);
    double diff = prev - curr;
    if (Math.abs(diff) > (.2 * prev)) {
      if (diff < 0) {
        setIndicatorWorse();
      } else {
        setIndicatorBetter();
      }
      return;
    }
    setIndicatorSame();
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
    rightChart.draw(dataTable);
  }

  public void populateTimeline(DashboardRecord[] serverData) {
    DataTable dataTable = DataTable.create();
    dataTable.addColumn(ColumnType.STRING, REVISION_TITLE);
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
    options.setHeight(CHART_HEIGHT);
    options.setStacked(true);
    leftChart.draw(dataTable, options);
  }

  public double sumTimes(DashboardRecord serverData) {
    return serverData.bootstrapDuration + serverData.loadExternalRefsDuration
        + serverData.moduleStartupDuration;
  }
  
  @Override
  public void populateChart(CustomDashboardRecord[] record) {
    //Empty impl
  }
}
