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
import com.google.gwt.visualization.client.visualizations.AreaChart.Options;
import com.google.speedtracer.latencydashboard.shared.DashboardRecord;

/**
 * Shows some of the lightweight metrics in a stacked chart.
 */
public class AggregatedEventTypeChart extends LatencyDashboardChart {
  // See EventRecordColors.java and Color.java in SpeedTracer
  private static final String javaScriptExecutionColor = "yellow"; // YELLOW
  private static final String javaScriptExecutionTitle = "Javascript Execution";
  private static final String layoutColor = "#8a2be2"; // BLUEVIOLET
  private static final String layoutTitle = "Layout";
  private static final String recalculateStylesColor = "#52b453"; // DARKGREEN
  private static final String recalculateStylesTitle = "Recalculate Styles";
  private static final String revisionTitle = "Revision";

  // TODO(zundel): Add these 4 types of events to the graph.
  // *** colorMap.put(PaintEvent.TYPE, Color.MIDNIGHT_BLUE);
  // *** colorMap.put(ParseHtmlEvent.TYPE, Color.INDIAN_RED);
  // *** colorMap.put(EvalScript.TYPE, Color.PEACH);
  // *** colorMap.put(GarbageCollectionEvent.TYPE, Color.BROWN);

  private AreaChart leftChart;
  private Legend legend;
  private PieChart rightChart;

  public AggregatedEventTypeChart(LatencyDashboardChart.Resources resources,
      String title) {
    super(resources, title);
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
    legend.addRow(javaScriptExecutionColor, javaScriptExecutionTitle);
    legend.addRow(layoutColor, layoutTitle);
    legend.addRow(recalculateStylesColor, recalculateStylesTitle);
  }

  public void addRow(DataTable dataTable, int row, DashboardRecord record) {
    dataTable.setCell(row, 0, record.getRevision(), null, null);
    dataTable.setCell(row, 1, record.javaScriptExecutionDuration, null, null);
    dataTable.setCell(row, 2, record.layoutDuration, null, null);
    dataTable.setCell(row, 3, record.recalculateStyleDuration, null, null);
  }

  public void populateChart(DashboardRecord[] serverData) {
    addLegend();
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
      if (diff > 0) {
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
    dataTable.setCell(row, 0, javaScriptExecutionTitle, null, null);
    dataTable.setCell(row++, 1, serverData.javaScriptExecutionDuration, null,
        null);
    dataTable.setCell(row, 0, layoutTitle, null, null);
    dataTable.setCell(row++, 1, serverData.layoutDuration, null, null);
    dataTable.setCell(row, 0, recalculateStylesTitle, null, null);
    dataTable.setCell(row++, 1, serverData.recalculateStyleDuration, null, null);

    PieChart.Options options = PieChart.Options.create();
    options.setLegend(LegendPosition.NONE);
    options.setHeight(chartHeight);
    // TODO(zundel): set 3D colors
    options.setColors(javaScriptExecutionColor, layoutColor,
        recalculateStylesColor);

    rightChart.draw(dataTable, options);
  }

  public void populateTimeline(DashboardRecord[] serverData) {
    DataTable data = DataTable.create();
    data.addColumn(ColumnType.STRING, revisionTitle);
    data.addColumn(ColumnType.NUMBER, javaScriptExecutionTitle);
    data.addColumn(ColumnType.NUMBER, layoutTitle);
    data.addColumn(ColumnType.NUMBER, recalculateStylesTitle);

    int length = serverData.length;
    data.addRows(length);
    for (int i = 0; i < length; ++i) {
      addRow(data, length - (i + 1), serverData[i]);
    }

    leftChart.setHeight(chartHeight + "px");
    Options options = AreaChart.Options.create();
    options.setHeight(chartHeight);
    options.setLegend(LegendPosition.NONE);
    options.setStacked(true);
    options.setColors(javaScriptExecutionColor, layoutColor,
        recalculateStylesColor);
    leftChart.draw(data, options);
  }

  private double sumTimes(DashboardRecord record) {
    return record.javaScriptExecutionDuration + record.layoutDuration
        + record.recalculateStyleDuration;
  }
}
