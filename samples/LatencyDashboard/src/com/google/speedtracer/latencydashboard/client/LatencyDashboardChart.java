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
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.HTML;

/**
 * A base class for charts to be included in the Dashboard.
 */
public class LatencyDashboardChart extends Composite {
  protected static final int chartHeight = 275;
  protected static final int graphHeight = 400;
  protected final DockLayoutPanel outerPanel = new DockLayoutPanel(Unit.PX);
  protected final DockLayoutPanel chartPanel = new DockLayoutPanel(Unit.PX);
  protected final DockLayoutPanel readoutPanel = new DockLayoutPanel(Unit.PX);

  public LatencyDashboardChart(String titleText) {
    // TODO(zundel): offload more styling stuff to CSS
    HTML title = new HTML("<h3>" + titleText + "</h3>");
    title.getElement().getStyle().setMarginLeft(2, Unit.EM);
    chartPanel.addNorth(title, 10);
    outerPanel.setWidth("100%");
    outerPanel.addNorth(chartPanel, chartHeight + 25);
    outerPanel.insertNorth(title, 40, chartPanel);
    chartPanel.setHeight((chartHeight + 25) + "px");
    outerPanel.add(readoutPanel);
    initWidget(outerPanel);
    this.setSize("100%", graphHeight + "px");
  }
}
