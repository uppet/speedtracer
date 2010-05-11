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
import com.google.speedtracer.latencydashboard.shared.DashboardRecord;

/**
 * Shows some of the lightweight metrics in a stacked chart.
 */
public class AggregatedEventTypeChart extends LatencyDashboardChart {
  // Color mappings in SpeedTracer defined in Color.java and
  // EventRecordColors.java
  private static final String evalScriptColor = "#ffd393";
  private static final String evalScriptTitle = "Evaluate Script";
  private static final String garbageCollectionColor = "#ab8f38";
  private static final String garbageCollectionTitle = "Garbage Collection";
  // See EventRecordColors.java and Color.java in SpeedTracer
  private static final String javaScriptExecutionColor = "yellow"; // YELLOW
  private static final String javaScriptExecutionTitle = "Javascript Execution";
  private static final String layoutColor = "#8a2be2"; // BLUEVIOLET
  private static final String layoutTitle = "Layout";
  private static final String paintColor = "#7483aa";
  private static final String paintTitle = "Paint";
  private static final String parseHtmlColor = "#cd5c5c";
  private static final String parseHtmlTitle = "Parse HTML";
  private static final String recalculateStylesColor = "#52b453"; // DARKGREEN
  private static final String recalculateStylesTitle = "Recalculate Styles";

  private AreaChart leftChart;
  private RightPieChart rightChart;

  public AggregatedEventTypeChart(LatencyDashboardChart.Resources resources,
      String title) {
    super(resources, title);
    rightChart = new RightPieChart(resources);
    chartPanel.addEast(rightChart, CHART_HEIGHT);
    leftChart = new AreaChart();
    chartPanel.add(leftChart);
  }

  public void addLegend() {
    rightChart.clear();
    rightChart.addItem(paintColor, paintTitle);
    rightChart.addItem(layoutColor, layoutTitle);
    rightChart.addItem(recalculateStylesColor, recalculateStylesTitle);
    rightChart.addItem(parseHtmlColor, parseHtmlTitle);
    rightChart.addItem(evalScriptColor, evalScriptTitle);
    rightChart.addItem(javaScriptExecutionColor, javaScriptExecutionTitle);
    rightChart.addItem(garbageCollectionColor, garbageCollectionTitle);
  }

  public void addRow(DataTable dataTable, int row, DashboardRecord record) {
    int column = 0;
    dataTable.setCell(row, column++, record.getRevision() + "-"
        + formatTimestamp(record.getTimestamp()), null, null);
    dataTable.setCell(row, column++, record.paintDuration, null, null);
    dataTable.setCell(row, column++, record.layoutDuration, null, null);
    dataTable.setCell(row, column++, record.recalculateStyleDuration, null,
        null);
    dataTable.setCell(row, column++, record.parseHtmlDuration, null, null);
    dataTable.setCell(row, column++, record.evalScriptDuration, null, null);
    dataTable.setCell(row, column++, record.javaScriptExecutionDuration, null,
        null);
    dataTable.setCell(row, column++, record.garbageCollectionDuration, null,
        null);
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
    dataTable.addRows(7);
    int row = 0;
    dataTable.setCell(row, 0, paintTitle, null, null);
    dataTable.setCell(row++, 1, clampDatapoint(serverData.paintDuration), null,
        null);
    dataTable.setCell(row, 0, layoutTitle, null, null);
    dataTable.setCell(row++, 1, clampDatapoint(serverData.layoutDuration),
        null, null);
    dataTable.setCell(row, 0, recalculateStylesTitle, null, null);
    dataTable.setCell(row++, 1,
        clampDatapoint(serverData.recalculateStyleDuration), null, null);
    dataTable.setCell(row, 0, parseHtmlTitle, null, null);
    dataTable.setCell(row++, 1, clampDatapoint(serverData.parseHtmlDuration),
        null, null);
    dataTable.setCell(row, 0, evalScriptTitle, null, null);
    dataTable.setCell(row++, 1, clampDatapoint(serverData.evalScriptDuration),
        null, null);
    dataTable.setCell(row, 0, javaScriptExecutionTitle, null, null);
    dataTable.setCell(row++, 1,
        clampDatapoint(serverData.javaScriptExecutionDuration), null, null);
    dataTable.setCell(row, 0, garbageCollectionTitle, null, null);
    dataTable.setCell(row++, 1,
        clampDatapoint(serverData.garbageCollectionDuration), null, null);
    rightChart.draw(dataTable);
  }

  public void populateTimeline(DashboardRecord[] serverData) {
    DataTable data = DataTable.create();
    data.addColumn(ColumnType.STRING, REVISION_TITLE);
    data.addColumn(ColumnType.NUMBER, paintTitle);
    data.addColumn(ColumnType.NUMBER, layoutTitle);
    data.addColumn(ColumnType.NUMBER, recalculateStylesTitle);
    data.addColumn(ColumnType.NUMBER, parseHtmlTitle);
    data.addColumn(ColumnType.NUMBER, evalScriptTitle);
    data.addColumn(ColumnType.NUMBER, javaScriptExecutionTitle);
    data.addColumn(ColumnType.NUMBER, garbageCollectionTitle);

    int length = serverData.length;
    data.addRows(length);
    for (int i = 0; i < length; ++i) {
      addRow(data, length - (i + 1), serverData[i]);
    }
    leftChart.setHeight(CHART_HEIGHT + "px");
    AreaChart.Options options = AreaChart.Options.create();
    options.setHeight(CHART_HEIGHT);
    options.setLegend(LegendPosition.NONE);
    options.setStacked(true);
    options.setColors(paintColor, layoutColor, recalculateStylesColor,
        parseHtmlColor, evalScriptColor, javaScriptExecutionColor,
        garbageCollectionColor);
    leftChart.draw(data, options);
  }

  public double sumTimes(DashboardRecord record) {
    return record.javaScriptExecutionDuration + record.layoutDuration
        + record.recalculateStyleDuration + record.evalScriptDuration
        + record.garbageCollectionDuration + record.paintDuration
        + record.parseHtmlDuration;
  }

  private double clampDatapoint(double value) {
    if (value == Double.NaN || value < 0) {
      return 0;
    }
    return value;
  }
}
