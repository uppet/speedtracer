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

import com.google.gwt.coreext.client.JsIntegerDoubleMap;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.TableCellElement;
import com.google.gwt.dom.client.TableRowElement;
import com.google.gwt.graphics.client.charts.ColorCodedValue;
import com.google.gwt.graphics.client.charts.PieChart;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.topspin.ui.client.ClickEvent;
import com.google.gwt.topspin.ui.client.ClickListener;
import com.google.gwt.topspin.ui.client.Container;
import com.google.gwt.topspin.ui.client.DefaultContainerImpl;
import com.google.gwt.topspin.ui.client.Div;
import com.google.gwt.topspin.ui.client.Root;
import com.google.gwt.topspin.ui.client.Table;
import com.google.speedtracer.client.model.DataDispatcher;
import com.google.speedtracer.client.model.EventRecord;
import com.google.speedtracer.client.timeline.TimeLineModel;
import com.google.speedtracer.client.util.TimeStampFormatter;
import com.google.speedtracer.client.view.HoveringPopup;
import com.google.speedtracer.client.view.SortableTableHeader;
import com.google.speedtracer.client.visualizations.model.HintletReportModel;
import com.google.speedtracer.client.visualizations.model.ReportDataCollector;
import com.google.speedtracer.client.visualizations.model.ReportDataCollector.ReportData;

import java.util.ArrayList;
import java.util.Collections;
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
      HoveringPopup.Resources, PieChart.Resources {

    @Source("resources/ReportDialog.css")
    ReportDialog.Css hintletReportDialogCss();

    @Source("resources/close-x-15px.png")
    ImageResource reportClose();
  }

  private final Div glassPane;

  private PieChart pieChart;

  private HintletReport report;

  private final Div reportPane;

  private DivElement summaryTitle;

  private final TimeLineModel timelineModel;

  private final ReportDataCollector dataCollector;

  public ReportDialog(TimeLineModel timelineModel,
      DataDispatcher dataDispatcher, ReportDialog.Resources resources) {
    // Initialize locals.
    this.glassPane = new Div(Root.getContainer());
    this.reportPane = new Div(Root.getContainer());
    this.timelineModel = timelineModel;
    this.dataCollector = new ReportDataCollector(dataDispatcher);

    // Set style names for the base elements.
    glassPane.setStyleName(resources.hintletReportDialogCss().glassPane());
    reportPane.setStyleName(resources.hintletReportDialogCss().reportPane());

    // Make sure the dialog is created hidden.
    glassPane.setVisible(false);
    reportPane.setVisible(false);

    // Build the UI for the report dialog.
    constructReportUi(resources);
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

  /**
   * TODO (jaimeyap): This should be moved into the HintletReport class.
   */
  private void buildHintReportScopeBar(ScopeBar.Resources resources,
      Container container) {
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

  private void constructReportUi(Resources resources) {
    Css css = resources.hintletReportDialogCss();
    Container reportPaneContainer = new DefaultContainerImpl(
        reportPane.getElement());

    // Create a container for the summary information.
    Div summaryInfoDiv = new Div(reportPaneContainer);
    summaryInfoDiv.setStyleName(css.reportPaneInner());
    Container summaryInfoContainer = new DefaultContainerImpl(
        summaryInfoDiv.getElement());

    // Create the title for the summary information.
    summaryTitle = summaryInfoDiv.getElement().getOwnerDocument().createDivElement();
    summaryTitle.setClassName(css.reportTitle());
    updateSummaryTitle();
    summaryInfoDiv.getElement().appendChild(summaryTitle);

    // Summary info is a 2 column section. PieChart on the left, and the startup
    // statistics on the right.
    Table summaryLayout = new Table(summaryInfoContainer);
    summaryLayout.setFixedLayout(true);
    TableRowElement row = summaryLayout.insertRow(-1);
    row.setVAlign("top");
    TableCellElement leftCell = row.insertCell(-1);
    Container pieChartContainer = new DefaultContainerImpl(leftCell);

    // Create a piechart with no data initially.
    this.pieChart = new PieChart(pieChartContainer,
        new ArrayList<ColorCodedValue>(), resources);

    // TODO (jaimeyap): Add startup statistics to the right of the pie chart.
    // Things like "time to first paint" or "page load time".

    // Create the inner container to hold to hint report.
    Div hintReportDiv = new Div(reportPaneContainer);
    hintReportDiv.setStyleName(css.reportPaneInner());
    Container hintReportContainer = new DefaultContainerImpl(
        hintReportDiv.getElement());

    // Create the title for the hint report.
    DivElement hintTitle = hintReportDiv.getElement().getOwnerDocument().createDivElement();
    hintTitle.setInnerText("Hints");
    hintTitle.setClassName(css.reportTitle());
    hintReportDiv.getElement().appendChild(hintTitle);

    // Construct the scope bar for selecting different type of hint reports.
    buildHintReportScopeBar(resources, hintReportContainer);

    // Create the hint report.
    this.report = new HintletReport(hintReportContainer,
        new HintletReportModel(), resources, HintletReport.REPORT_TYPE_SEVERITY);

    // Close button for hiding the report glass panel.
    Div closeButton = new Div(reportPaneContainer);
    closeButton.setStyleName(css.reportClose());
    closeButton.addClickListener(new ClickListener() {
      public void onClick(ClickEvent event) {
        setVisible(false);
      }
    });
  }

  private void gatherDataWithinWindow() {
    ReportData data = dataCollector.gatherDataWithinWindow(
        timelineModel.getLeftBound(), timelineModel.getRightBound());

    // Update the report views.
    updateSummaryTitle();
    populatePieChart(data.getAggregatedTypeDurations());
    report.refresh(data.getHints());
  }

  private void populatePieChart(JsIntegerDoubleMap aggregateTypeDurations) {
    final List<ColorCodedValue> data = new ArrayList<ColorCodedValue>();

    assert (aggregateTypeDurations != null);

    // Flatten the aggregate map into a sorted list.
    aggregateTypeDurations.iterate(new JsIntegerDoubleMap.IterationCallBack() {
      public void onIteration(int key, double val) {
        if (val > 0) {
          String typeName = (key == -1) ? "UI Thread Available"
              : EventRecord.typeToString(key);
          data.add(new ColorCodedValue(typeName, val,
              EventRecordColors.getColorForType(key)));
        }
      }
    });

    Collections.sort(data);
    // Update the piechart with this data.
    this.pieChart.setData(data);
    this.pieChart.render();
    this.pieChart.showLegend();
  }

  private void updateSummaryTitle() {
    double left = timelineModel.getLeftBound();
    double right = timelineModel.getRightBound();
    summaryTitle.setInnerText("Summary Report for Selection: "
        + TimeStampFormatter.format(left) + " - "
        + TimeStampFormatter.format(right) + " ("
        + TimeStampFormatter.format(right - left) + ")");
  }
}
