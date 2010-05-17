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

import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.speedtracer.latencydashboard.shared.CustomDashboardRecord;
import com.google.speedtracer.latencydashboard.shared.DashboardRecord;

import java.util.Date;

/**
 * A base class for charts to be included in the Dashboard.
 */
public abstract class LatencyDashboardChart extends Composite {

  /**
   * Css definitions for this UI component.
   */
  public interface Css extends CssResource {
    String chartTitle();

    String indicator();

    String indicatorPanel();

    String latencyChart();
  }

  /**
   * Resources for Css and Images.
   */
  public interface Resources extends RightPieChart.Resources, ClientBundle {

    @Source("resources/check-48.gif")
    ImageResource getCheck();

    @Source("resources/arrow-68.gif")
    ImageResource getGreenArrow();

    @Source("resources/circle_slash-64.gif")
    ImageResource getRedSlash();

    @Source("resources/LatencyDashboardChart.css")
    Css latencyDashboardChartCss();
  }

  public static final int CHART_HEIGHT = 275;
  protected static final int indicatorWidth = 100;
  protected static final String REVISION_TITLE = "Revision";
  @SuppressWarnings("deprecation")
  public static String formatTimestamp(double timestampMsec) {
    Date timestamp = new Date((long) timestampMsec);
    String timestampString = (timestamp.getYear() + 1900) + "0"
        + timestamp.getMonth() + 1 + "0" + timestamp.getDate() + "";
    timestampString += format2Digit(timestamp.getHours());
    timestampString += format2Digit(timestamp.getMinutes());
    timestampString += format2Digit(timestamp.getSeconds());
    return timestampString;
  }

  private static String format2Digit(int val) {
    if (val > 9) {
      return "" + val % 100;
    }
    return "0" + val;
  }

  protected final DockLayoutPanel chartPanel = new DockLayoutPanel(Unit.PX);
  protected final Image indicator = new Image();
  protected final SimplePanel indicatorPanel = new SimplePanel();

  protected final DockLayoutPanel outerPanel = new DockLayoutPanel(Unit.PX);

  private final Resources resources;

  public LatencyDashboardChart(Resources resources, String titleText) {
    this.resources = resources;
    Css css = resources.latencyDashboardChartCss();
    Label title = new Label(titleText);
    title.addStyleName(css.chartTitle());
    chartPanel.addNorth(title, 0);
    indicatorPanel.addStyleName(css.indicatorPanel());
    indicator.addStyleName(css.indicator());
    indicatorPanel.add(indicator);
    chartPanel.addWest(indicatorPanel, indicatorWidth);
    outerPanel.setWidth("100%");
    outerPanel.addNorth(chartPanel, CHART_HEIGHT + 25);

    outerPanel.insertNorth(title, 40, chartPanel);
    chartPanel.setHeight((CHART_HEIGHT + 25) + "px");
    initWidget(outerPanel);
    this.addStyleName(css.latencyChart());
  }

  public abstract void populateChart(DashboardRecord record[]);

  public abstract void populateChart(CustomDashboardRecord record[]);

  public void setIndicatorBetter() {
    indicator.setResource(resources.getGreenArrow());
  }

  public void setIndicatorSame() {
    indicator.setResource(resources.getCheck());
  }

  public void setIndicatorWorse() {
    indicator.setResource(resources.getRedSlash());
  }
}
