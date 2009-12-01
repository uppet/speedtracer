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

import com.google.gwt.dom.client.Element;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.resources.client.CssResource.Strict;
import com.google.gwt.topspin.ui.client.ClickEvent;
import com.google.gwt.topspin.ui.client.ClickListener;
import com.google.gwt.topspin.ui.client.Container;
import com.google.gwt.topspin.ui.client.DefaultContainerImpl;
import com.google.gwt.topspin.ui.client.Div;
import com.google.gwt.topspin.ui.client.Root;
import com.google.speedtracer.client.view.HoveringPopup;
import com.google.speedtracer.client.view.SortableTableHeader;
import com.google.speedtracer.client.visualizations.model.HintletReportModel;

/**
 * A dialog to hold the HintletReport.
 */
public class HintletReportDialog {

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

    @Source("resources/HintletReportDialog.css")
    @Strict
    HintletReportDialog.Css hintletReportDialogCss();

    @Source("resources/close-x-15px.png")
    ImageResource reportClose();
  }

  private final HintletReport report;
  private final Div glassPane;
  private final Div reportPane;

  public HintletReportDialog(HintletReportModel reportModel,
      HintletReportDialog.Resources resources) {
    HintletReportDialog.Css css = resources.hintletReportDialogCss();
    glassPane = new Div(Root.getContainer());
    glassPane.setStyleName(css.glassPane());
    reportPane = new Div(Root.getContainer());
    reportPane.setStyleName(css.reportPane());
    Container reportPaneContainer = new DefaultContainerImpl(
        reportPane.getElement());
    glassPane.setVisible(false);
    reportPane.setVisible(false);

    Div reportPaneTitle = new Div(reportPaneContainer);
    reportPaneTitle.setText("Hints");
    reportPaneTitle.setStyleName(css.reportTitle());
    Div reportPaneInner = new Div(reportPaneContainer);
    reportPaneInner.setStyleName(css.reportPaneInner());
    Container thisContainer = new DefaultContainerImpl(
        reportPaneInner.getElement());
    buildScopeBar(resources, thisContainer);
    report = new HintletReport(thisContainer, reportModel, resources,
        HintletReport.REPORT_TYPE_SEVERITY);

    Div closeButton = new Div(reportPaneContainer);
    closeButton.setStyleName(css.reportClose());
    closeButton.addClickListener(new ClickListener() {

      public void onClick(ClickEvent event) {
        setVisible(false);
      }

    });
  }

  private void buildScopeBar(ScopeBar.Resources resources,
      Container thisContainer) {
    ScopeBar scopeBar = new ScopeBar(thisContainer, resources);
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

  public void setHintletReportModel(HintletReportModel reportModel) {
    report.setReportModel(reportModel);
  }

  /**
   * Display the report dialog, first refreshing with the latest info.
   */
  public void setVisible(boolean visible) {
    if (visible) {
      glassPane.setVisible(true);
      reportPane.setVisible(true);
      report.refresh();
    } else {
      glassPane.setVisible(false);
      reportPane.setVisible(false);
    }
  }
}
