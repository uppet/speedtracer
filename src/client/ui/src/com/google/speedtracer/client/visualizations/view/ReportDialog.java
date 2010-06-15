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

import com.google.gwt.coreext.client.JSOArray;
import com.google.gwt.dom.client.Element;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.topspin.ui.client.ClickEvent;
import com.google.gwt.topspin.ui.client.ClickListener;
import com.google.gwt.topspin.ui.client.Container;
import com.google.gwt.topspin.ui.client.DefaultContainerImpl;
import com.google.gwt.topspin.ui.client.Div;
import com.google.gwt.topspin.ui.client.Root;
import com.google.speedtracer.client.model.DataDispatcher;
import com.google.speedtracer.client.model.EventRecord;
import com.google.speedtracer.client.model.HintRecord;
import com.google.speedtracer.client.timeline.TimeLineModel;
import com.google.speedtracer.client.view.HoveringPopup;
import com.google.speedtracer.client.view.SortableTableHeader;
import com.google.speedtracer.client.visualizations.model.HintletReportModel;

import java.util.ArrayList;
import java.util.List;

/**
 * A dialog to hold the Speed Tracer performance report for the current
 * selection window, as well as any hints fired within this window.
 */
public class ReportDialog {

  /**
   * CSS.
   */
  public interface Css extends CssResource {
    String glassPane();

    String reportClose();

    String reportPane();

    String reportPaneInner();

    String reportTitle();
  }

  /**
   * Externalized interface.
   */
  public interface Resources extends HintletReport.Resources,
      ScopeBar.Resources, SortableTableHeader.Resources,
      HoveringPopup.Resources {

    @Source("resources/ReportDialog.css")
    ReportDialog.Css hintletReportDialogCss();

    @Source("resources/close-x-15px.png")
    ImageResource reportClose();
  }

  private final DataDispatcher dataDispatcher;

  private final Div glassPane;

  private final HintletReport report;

  private final Div reportPane;

  private final TimeLineModel timelineModel;

  public ReportDialog(TimeLineModel timelineModel,
      DataDispatcher dataDispatcher, ReportDialog.Resources resources) {
    // Initialize locals.
    this.dataDispatcher = dataDispatcher;
    this.glassPane = new Div(Root.getContainer());
    this.reportPane = new Div(Root.getContainer());
    this.timelineModel = timelineModel;

    // Set style names.
    ReportDialog.Css css = resources.hintletReportDialogCss();
    glassPane.setStyleName(css.glassPane());
    reportPane.setStyleName(css.reportPane());

    // Create the main container for the report pane.
    Container reportPaneContainer = new DefaultContainerImpl(
        reportPane.getElement());

    // Create the inner container to hold to hint report.
    Div reportPaneInner = new Div(reportPaneContainer);
    reportPaneInner.setStyleName(css.reportPaneInner());
    Container innerContainer = new DefaultContainerImpl(
        reportPaneInner.getElement());

    // Create the title for the hint report.
    Div reportPaneTitle = new Div(innerContainer);
    reportPaneTitle.setText("Hints");
    reportPaneTitle.setStyleName(css.reportTitle());

    // Construct the scope bar for selecting different type of hint reports.
    buildScopeBar(resources, innerContainer);

    // Create the hint report.
    this.report = new HintletReport(innerContainer, new HintletReportModel(),
        resources, HintletReport.REPORT_TYPE_SEVERITY);

    // Close button for hiding the report glass panel.
    Div closeButton = new Div(reportPaneContainer);
    closeButton.setStyleName(css.reportClose());
    closeButton.addClickListener(new ClickListener() {
      public void onClick(ClickEvent event) {
        setVisible(false);
      }
    });

    // Make sure the dialog is created hidden.
    glassPane.setVisible(false);
    reportPane.setVisible(false);
  }

  /**
   * Display the report dialog, first refreshing with the latest info.
   */
  public void setVisible(boolean visible) {
    if (visible) {
      glassPane.setVisible(true);
      reportPane.setVisible(true);
      gatherDataWithinWindow();
    } else {
      glassPane.setVisible(false);
      reportPane.setVisible(false);
    }
  }

  private void buildScopeBar(ScopeBar.Resources resources, Container container) {
    ScopeBar scopeBar = new ScopeBar(container, resources);
    scopeBar.add("All", new ClickListener() {
      public void onClick(ClickEvent event) {
        report.setType(HintletReport.REPORT_TYPE_TIME);
      }
    });
    scopeBar.add("Rule", new ClickListener() {
      public void onClick(ClickEvent event) {
        report.setType(HintletReport.REPORT_TYPE_RULE);
      }
    });
    Element severity = scopeBar.add("Severity", new ClickListener() {
      public void onClick(ClickEvent event) {
        report.setType(HintletReport.REPORT_TYPE_SEVERITY);
      }
    });
    scopeBar.setSelected(severity, false);
    scopeBar.getElement().getStyle().setPropertyPx("marginBottom", 5);
  }

  private void gatherDataWithinWindow() {
    int numRecords = dataDispatcher.getEventRecords().size();
    int index = EventRecord.getIndexOfRecord(dataDispatcher.getEventRecords(),
        timelineModel.getLeftBound());
    // If we have a bogus insertion index, do nothing.
    if (index < 0 || index >= numRecords) {
      return;
    }

    // TODO(jaimeyap): Collect summary aggregate stats.
    List<HintRecord> hints = new ArrayList<HintRecord>();
    EventRecord record = dataDispatcher.findEventRecord(index);
    while (record.getTime() < timelineModel.getRightBound() && record != null) {
      JSOArray<HintRecord> hintArray = record.getHintRecords();
      if (hintArray != null) {
        // Collect hints.
        for (int i = 0, n = hintArray.size(); i < n; i++) {
          hints.add(hintArray.get(i));
        }
      }
      record = dataDispatcher.findEventRecord(++index);
    }
    report.refresh(hints);
  }
}
