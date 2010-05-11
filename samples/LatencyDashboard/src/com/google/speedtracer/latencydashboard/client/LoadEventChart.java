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
import com.google.gwt.visualization.client.visualizations.ColumnChart;
import com.google.speedtracer.latencydashboard.shared.DashboardRecord;

/**
 * Shows "load" and "DOMContentLoaded" events on the chart.
 */
public class LoadEventChart extends LatencyDashboardChart {
  private static final String domContentLoadedColor = "orange";
  private static final String domContentLoadedTitle = "Load Event";
  private static final String pageLoadEventColor = "blue";
  private static final String pageLoadEventTitle = "DOMContentLoaded";
  private ColumnChart leftChart;
  private RightPieChart rightChart;

  public LoadEventChart(Resources resources, String titleText) {
    super(resources, titleText);
    rightChart = new RightPieChart(resources);
    chartPanel.addEast(rightChart, CHART_HEIGHT);
    leftChart = new ColumnChart();
    chartPanel.add(leftChart);
    addLegend();
  }

  public void addLegend() {
    rightChart.clear();
    rightChart.addItem(domContentLoadedColor, domContentLoadedTitle);
    rightChart.addItem(pageLoadEventColor, pageLoadEventTitle);
  }

  public void addRow(DataTable dataTable, int row, DashboardRecord record) {
    int column = 0;
    dataTable.setCell(row, column++, record.getRevision(), null, null);
    dataTable.setCell(row, column++, record.domContentLoadedTime, null, null);
    dataTable.setCell(row, column++, record.loadEventTime, null, null);
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
    dataTable.addRows(2);
    int row = 0;
    dataTable.setCell(row, 0, domContentLoadedTitle, null, null);
    dataTable.setCell(row++, 1, serverData.domContentLoadedTime, null, null);
    dataTable.setCell(row, 0, pageLoadEventTitle, null, null);
    dataTable.setCell(row++, 1, serverData.loadEventTime, null, null);
    rightChart.draw(dataTable);
  }

  public void populateTimeline(DashboardRecord[] serverData) {
    DataTable dataTable = DataTable.create();
    dataTable.addColumn(ColumnType.STRING, REVISION_TITLE);
    dataTable.addColumn(ColumnType.NUMBER, domContentLoadedTitle);
    dataTable.addColumn(ColumnType.NUMBER, pageLoadEventTitle);
    int length = serverData.length;
    dataTable.addRows(length);
    for (int i = 0; i < length; ++i) {
      addRow(dataTable, length - (i + 1), serverData[i]);
    }

    ColumnChart.Options options = ColumnChart.Options.create();
    options.setLegend(LegendPosition.NONE);
    options.setHeight(CHART_HEIGHT);
    // options.setStacked(true);
    options.setColors(domContentLoadedColor, pageLoadEventColor);
    leftChart.draw(dataTable, options);
  }

  public double sumTimes(DashboardRecord serverData) {
    return Math.max(serverData.domContentLoadedTime, serverData.loadEventTime);
  }
}
