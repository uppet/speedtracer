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

import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.CssResource.Strict;
import com.google.gwt.topspin.ui.client.Container;
import com.google.gwt.topspin.ui.client.DefaultContainerImpl;
import com.google.gwt.topspin.ui.client.Div;
import com.google.speedtracer.client.view.SortableTableHeader;
import com.google.speedtracer.client.visualizations.model.HintletReportModel;

/**
 * Presents the hintlets in their own view.
 * 
 */
public class HintletReport extends Div {
  /**
   * CSS.
   */
  public interface Css extends CssResource {
    String reportHintletIndicator();

    String reportOuterDiv();

    String reportRowDetailCell();

    String reportRowDetailContentElem();

    String reportRowDetailSeverityCell();

    String reportRowDetailTable();

    String reportRowDetailTimeCell();

    String reportRowNameDiv();

    String reportRowSummaryDiv();

    String reportSeverityDot();

    String reportTableEvenRow();

    String reportTableHeader();

    String reportTree();

    int severityColumnWidth();

    int timeColumnWidth();
  }

  /**
   * Externalized interface.
   */
  public interface Resources extends ClientBundle, Tree.Resources,
      SortableTableHeader.Resources, HintletIndicator.Resources {

    @Source("resources/HintletReport.css")
    @Strict
    HintletReport.Css hintletReportCss();
  }

  public static final int REPORT_TYPE_RULE = 3;
  public static final int REPORT_TYPE_SEVERITY = 2;
  public static final int REPORT_TYPE_TIME = 1;
  public static final String TITLE = "Hint Report";

  private final HintletReport.Css css;
  private final Container reportContainer;
  private HintletReportModel reportModel;
  private HintletReportTree reportTree;
  private final HintletReport.Resources resources;

  /**
   * Create a new report.
   * 
   * @param parentContainer A Topspin container to put this report in.
   * @param reportModel Contains the data for the report
   * @param resources Css resources to use for the report
   * @param reportType pass one of the REPORT_TYPE constants
   */
  public HintletReport(Container parentContainer,
      HintletReportModel reportModel, HintletReport.Resources resources,
      int reportType) {
    super(parentContainer);
    this.resources = resources;
    this.reportModel = reportModel;
    css = resources.hintletReportCss();
    setStyleName(css.reportOuterDiv());
    reportContainer = new DefaultContainerImpl(getElement());
    setType(reportType);
  }

  public void refresh() {
    reportTree.refresh();
  }

  public void setReportModel(HintletReportModel reportModel) {
    this.reportModel = reportModel;
    reportTree.setReportModel(reportModel);
  }

  public void setType(int reportType) {
    // Clean up the old tree if it is there.
    if (reportTree != null) {
      reportTree.detachEventListeners();
      getElement().removeChild(reportTree.getElement());
    }

    // Create a new tree
    switch (reportType) {
      case REPORT_TYPE_TIME:
        reportTree = new HintletReportTimeTree(reportContainer, resources,
            reportModel);
        break;
      case REPORT_TYPE_RULE:
        reportTree = new HintletReportRuleTree(reportContainer, resources,
            reportModel);
        break;
      case REPORT_TYPE_SEVERITY:
      default:
        // defaults to case REPORT_TYPE_SEVERITY:
        reportTree = new HintletReportSeverityTree(reportContainer, resources,
            reportModel);
        break;
    }
  }
}
