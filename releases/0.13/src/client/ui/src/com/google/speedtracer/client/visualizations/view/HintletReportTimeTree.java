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
package com.google.speedtracer.client.visualizations.view;

import com.google.gwt.topspin.ui.client.Container;
import com.google.gwt.topspin.ui.client.DefaultContainerImpl;
import com.google.gwt.topspin.ui.client.Div;
import com.google.speedtracer.client.model.HintRecord;
import com.google.speedtracer.client.visualizations.model.HintletReportModel;

import java.util.List;

/**
 * Presents a tree sorted by time.
 */
public class HintletReportTimeTree extends HintletReportTree {
  /**
   * Represents one rule in the report. Represented as a collapsable tree
   * branch.
   * 
   */
  private class RuleRow extends ReportRow {
    private final HintletReportRowDetails details;

    public RuleRow(List<HintRecord> hintletRecords) {
      super(tree);
      Container rowContainer = new DefaultContainerImpl(getItemLabelElement());
      populateRowSummary(rowContainer, hintletRecords);
      details = new HintletReportRowDetails(this, hintletRecords, reportModel);
    }

    public void detachEventListeners() {
      details.detachEventListeners();
    }

    private void populateRowSummary(Container rowContainer,
        List<HintRecord> hintletRecords) {

      final HintletReport.Css css = resources.hintletReportCss();
      // Hintlet Indicator(s)
      int severityCount[] = new int[4];
      for (int i = 0, j = hintletRecords.size(); i < j; i++) {
        HintRecord rec = hintletRecords.get(i);
        severityCount[rec.getSeverity()]++;
      }

      for (int i = 0, j = severityCount.length; i < j; i++) {
        if (severityCount[i] > 0) {
          HintletIndicator indicator = new HintletIndicator(rowContainer, i,
              severityCount[i], null, resources);

          String className = indicator.getElement().getClassName();
          indicator.getElement().setClassName(
              css.reportHintletIndicator() + " " + className);
        }
      }

      // Rule name
      Div rowNameDiv = new Div(rowContainer);
      rowNameDiv.setText("All Hintlets");
      rowNameDiv.setStyleName(css.reportRowNameDiv());
    }
  }

  public HintletReportTimeTree(Container container,
      HintletReport.Resources resources, HintletReportModel reportModel) {
    super(container, resources, reportModel);
  }

  /**
   * Fill the table with data from the current reportModel.
   */
  protected void populateTable() {
    clearReportTree();
    rows.add(new RuleRow(reportModel.getHintsByTime()));
  }
}
