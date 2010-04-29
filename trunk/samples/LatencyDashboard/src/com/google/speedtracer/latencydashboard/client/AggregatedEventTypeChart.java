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
import com.google.gwt.visualization.client.AbstractDataTable.ColumnType;
import com.google.gwt.visualization.client.visualizations.AreaChart;
import com.google.speedtracer.latencydashboard.shared.DashboardRecord;

/**
 * Shows some of the lightweight metrics in a stacked chart.
 */
public class AggregatedEventTypeChart extends AreaChart {

  public void populateChart(DashboardRecord[] serverData) {
    DataTable data = DataTable.create();
    data.addColumn(ColumnType.STRING, "Revision");
    data.addColumn(ColumnType.NUMBER, "Javascript Execution");
    data.addColumn(ColumnType.NUMBER, "Layout");
    data.addColumn(ColumnType.NUMBER, "Recalculate Styles");

    int length = serverData.length;
    data.addRows(length);
    for (int i = 0; i < length; ++i) {

      int col = 0;
      data.setCell(length - (i + 1), col++, serverData[i].getRevision(), null,
          null);
      data.setCell(length - (i + 1), col++,
          serverData[i].javaScriptExecutionDuration, null, null);
      data.setCell(length - (i + 1), col++, serverData[i].layoutDuration, null,
          null);
      data.setCell(length - (i + 1), col++,
          serverData[i].recalculateStyleDuration, null, null);
    }

    Options options = AreaChart.Options.create();
    options.setHeight(275);
    options.setStacked(true);
    draw(data, options);
  }
}
