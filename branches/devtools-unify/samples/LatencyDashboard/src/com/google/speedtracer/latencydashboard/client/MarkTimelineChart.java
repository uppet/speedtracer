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
import com.google.gwt.visualization.client.visualizations.Gauge;
import com.google.gwt.visualization.client.visualizations.LineChart;
import com.google.speedtracer.latencydashboard.shared.CustomDashboardRecord;
import com.google.speedtracer.latencydashboard.shared.DashboardRecord;

/**
 * Shows a Line Graph and Gauge based on the data from
 * {@link MarkTimelineAnalyzer}. Like the analyzer, this class is data driven
 * but needs to be told what measurements to look for.
 */
public class MarkTimelineChart extends LatencyDashboardChart {

  // Default gviz colors
  private static final String[] COLORS = {
      "#4684ee", "#dc3912", "#ff9900", "#008000", "#666666", "#4942cc",
      "#cb4ac5", "#d6ae00", "#336699", "#dd4477", "#aaaa11", "#66aa00",
      "#888888", "#994499", "#dd5511", "#22aa99", "#999999", "#705770",
      "#109618", "#a32929"};
  private static final String[] emptyEvents = {};

  private static final String revisionTitle = "Revision";

  private static String displayEventName(String event) {
    return event.replaceAll("_", " ").replaceAll("wave", "");
  }
  private final String[] events;
  private RightGaugeChart gaugeChart;
  private LineChart leftChart;

  private final String measurementName;

  public MarkTimelineChart(LatencyDashboardChart.Resources resources,
      String measurementName, String[] events, Gauge.Options gaugeOptions) {
    super(resources, displayEventName(measurementName));
    this.measurementName = measurementName;
    this.gaugeChart = new RightGaugeChart(resources, gaugeOptions);
    chartPanel.addEast(gaugeChart, CHART_HEIGHT);
    this.leftChart = new LineChart();
    chartPanel.add(leftChart);

    if (events != null) {
      this.events = events;
    } else {
      this.events = emptyEvents;
    }
  }

  public void addLegend() {
    int color = 0;
    gaugeChart.clear();
    gaugeChart.addItem(COLORS[color++], "Total");
    for (int i = 0, n = events.length; i < n; i++) {
      gaugeChart.addItem(COLORS[color++ % COLORS.length],
          displayEventName(events[i]));
    }
  }

  public void populateChart(CustomDashboardRecord[] serverData) {
    addLegend();
    if (serverData.length > 0) {
      populateLastData(serverData[0]);
    }
    populateTimeline(serverData);
    populateIndicator(serverData);
  }

  // TODO(conroy): Fix this inheritance model to be a bit more sane
  @Override
  public void populateChart(DashboardRecord[] record) {
    // Empty Impl
  }

  public void populateIndicator(CustomDashboardRecord[] serverData) {
    if (serverData.length <= 1) {
      setIndicatorSame();
      return;
    }

    double prev = serverData[1].getMetric(measurementName + ":total");
    double curr = serverData[0].getMetric(measurementName + ":total");
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

  /**
   * Populate the gauge indicator with the most recent record.
   * 
   * @param serverData
   */
  public void populateLastData(CustomDashboardRecord serverData) {
    DataTable dataTable = DataTable.create();
    dataTable.addColumn(ColumnType.STRING, "Type");
    dataTable.addColumn(ColumnType.NUMBER, "Milliseconds");

    dataTable.addRows(1);
    dataTable.setCell(0, 0, "Latest", null, null);
    dataTable.setCell(0, 1,
        serverData.getMetric(measurementName + ":total").intValue(), null, null);

    gaugeChart.draw(dataTable);
  }

  /**
   * Populates the timeline with the total time of the measurement set and any
   * of the relevant events.
   * 
   * @param serverData array of custom records for the timeline
   */
  public void populateTimeline(CustomDashboardRecord[] serverData) {
    DataTable dataTable = DataTable.create();
    dataTable.addColumn(ColumnType.STRING, revisionTitle);
    dataTable.addColumn(ColumnType.NUMBER, "Total");

    for (String event : events) {
      dataTable.addColumn(ColumnType.NUMBER, displayEventName(event));
    }

    int length = serverData.length;
    dataTable.addRows(length);
    for (int i = 0; i < length; i++) {
      addRow(dataTable, length - (i + 1), serverData[i]);
    }

    LineChart.Options options = LineChart.Options.create();
    leftChart.getLayoutData();
    options.setLegend(LegendPosition.NONE);
    options.setHeight(CHART_HEIGHT);
    options.setTitleY("milliseconds");
    options.setColors(COLORS);
    leftChart.draw(dataTable, options);
  }

  /**
   * Populate a single timeline row.
   * 
   * @param dataTable
   * @param row
   * @param customDashboardRecord
   */
  private void addRow(DataTable dataTable, int row,
      CustomDashboardRecord customDashboardRecord) {
    int column = 0;
    dataTable.setCell(row, column++, customDashboardRecord.getRevision() + "-"
        + formatTimestamp(customDashboardRecord.getTimestamp()), null, null);
    dataTable.setCell(row, column++,
        customDashboardRecord.getMetric(measurementName + ":total"), null, null);

    for (String event : events) {
      dataTable.setCell(row, column++,
          customDashboardRecord.getMetric(measurementName + ":" + event), null,
          null);
    }
  }

}
