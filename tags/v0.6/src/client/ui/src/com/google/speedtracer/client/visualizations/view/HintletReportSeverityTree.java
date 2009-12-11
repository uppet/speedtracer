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
package com.google.speedtracer.client.visualizations.view;

import com.google.gwt.topspin.ui.client.Container;
import com.google.gwt.topspin.ui.client.DefaultContainerImpl;
import com.google.gwt.topspin.ui.client.Div;
import com.google.speedtracer.client.MonitorResources;
import com.google.speedtracer.client.model.HintRecord;
import com.google.speedtracer.client.visualizations.model.HintletReportModel;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Presents a tree sorted by rule name.
 */
public class HintletReportSeverityTree extends HintletReportTree {
  private class SeverityRow extends ReportRow {
    private final HintletReportRowDetails details;

    public SeverityRow(int severity, List<HintRecord> hintletRecords) {
      super(resources, tree);
      Container rowContainer = new DefaultContainerImpl(getItemLabelElement());
      populateRowSummary(rowContainer, severity, hintletRecords);
      details = new HintletReportRowDetails(this, hintletRecords, resources,
          reportModel);
    }

    public void detachEventListeners() {
      details.detachEventListeners();
    }

    private void populateRowSummary(Container rowContainer, int severity,
        List<HintRecord> hintletRecords) {

      // Hintlet Indicator(s)
      int severityCount = hintletRecords.size();

      HintletIndicator indicator = new HintletIndicator(rowContainer, severity,
          severityCount, null, MonitorResources.getResources());

      String className = indicator.getElement().getClassName();
      indicator.getElement().setClassName(
          css.reportHintletIndicator() + " " + className);

      // Rule name
      Div severityNameDiv = new Div(rowContainer);
      String severityString;
      switch (severity) {
        case HintRecord.SEVERITY_CRITICAL:
          severityString = "Critical";
          break;
        case HintRecord.SEVERITY_WARNING:
          severityString = "Warning";
          break;
        case HintRecord.SEVERITY_INFO:
          severityString = "Info";
          break;
        default:
          severityString = "uNkNoWn";
      }
      severityNameDiv.setText(severityString);
      severityNameDiv.setStyleName(css.reportRowNameDiv());
    }
  }

  public HintletReportSeverityTree(Container container,
      HintletReport.Resources resources, HintletReportModel reportModel) {
    super(container, resources, reportModel);
  }

  /**
   * Fill the table with data from the current reportModel.
   */
  protected void populateTable() {
    clearReportTree();
    Map<Integer, List<HintRecord>> hintletMap = reportModel.getHintsBySeverity();
    Set<Integer> keys = hintletMap.keySet();
    for (int key : keys) {
      rows.add(new SeverityRow(key, hintletMap.get(key)));
    }
  }
}
