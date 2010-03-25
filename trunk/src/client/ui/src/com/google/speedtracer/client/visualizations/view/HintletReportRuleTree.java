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
import java.util.Map;
import java.util.Set;

/**
 * Presents a tree sorted by rule name.
 */
public class HintletReportRuleTree extends HintletReportTree {
  private class RuleRow extends ReportRow {
    private final RuleRowDetails details;

    /**
     * Summary line for all hintlet records organized by rule.
     * 
     * @param ruleName name of the rule this table holds.
     * @param hintletRecords list of records to display.
     */
    public RuleRow(String ruleName, List<HintRecord> hintletRecords) {
      super(tree);
      Container rowContainer = new DefaultContainerImpl(getItemLabelElement());
      populateRowSummary(rowContainer, ruleName, hintletRecords);
      details = new RuleRowDetails(this, hintletRecords, reportModel);
    }

    public void detachEventListeners() {
      details.detachEventListeners();
    }

    /**
     * Fills the summary row showing a hintlet indicator and the name of the
     * rule.
     * 
     * @param rowContainer the container that will hold this row
     * @param ruleName the name of the rule this table holds
     * @param hintletRecords list of the records to display.
     */
    private void populateRowSummary(Container rowContainer, String ruleName,
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
      Div ruleNameDiv = new Div(rowContainer);
      ruleNameDiv.setText(ruleName);
      ruleNameDiv.setStyleName(css.reportRowNameDiv());
    }
  }

  /**
   * Displays each hintlet record in a row using an HTML table.
   */
  private class RuleRowDetails extends ReportDetails {

    public RuleRowDetails(Tree.Item parent, List<HintRecord> hintletRecords,
        HintletReportModel reportModel) {
      super(parent, hintletRecords, reportModel);

      addColumn(COL_SEVERITY);
      addColumn(COL_TIME);
      addColumn(COL_DESCRIPTION);

      // The data rows are populated by the sort handler firing.
      setSortColumn(COL_TIME, false);
    }
  }

  public HintletReportRuleTree(Container container,
      HintletReport.Resources resources, HintletReportModel reportModel) {
    super(container, resources, reportModel);
  }

  /**
   * Fill the table with data from the current reportModel.
   */
  protected void populateTable() {
    clearReportTree();
    Map<String, List<HintRecord>> hintletMap = reportModel.getHintsByRule();
    Set<String> keys = hintletMap.keySet();
    for (String key : keys) {
      rows.add(new RuleRow(key, hintletMap.get(key)));
    }
  }
}
