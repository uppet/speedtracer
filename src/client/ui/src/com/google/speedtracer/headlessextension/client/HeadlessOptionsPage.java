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
package com.google.speedtracer.headlessextension.client;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.speedtracer.client.model.WhitelistModel;

/**
 * A page that allows users to set options for the headless api.
 * 
 * TODO(zundel): This page is not wired in to the headless extension. It requires
 * better verification, layout and styling fixes.
 */
public class HeadlessOptionsPage implements EntryPoint {
  public WhitelistTable whitelistTable;
  public WhitelistModel whitelistModel;
  public TableControls tableControls;

  public void onModuleLoad() {
    buildUi();
    populateUi();
  }

  private void buildUi() {
    HeadlessOptionsResources.init();
    HeadlessOptionsResources.Resources resources = HeadlessOptionsResources.getResources();
    HeadlessOptionsResources.Css css = resources.optionsPageCss();

    whitelistTable = new WhitelistTable(HeadlessOptionsResources.getResources());
    VerticalPanel outer = new VerticalPanel();
    outer.setStyleName(css.outer());
    outer.add(new HTML("<h1>Speed Tracer Headless Extension- Options</h1>"));
    outer.add(new HTML("<h3>Whitelist for XHR connections</h3>"));
    tableControls = new TableControls(resources, whitelistTable,
        WhitelistModel.get());
    HorizontalPanel tablePanel = new HorizontalPanel();
    tablePanel.add(whitelistTable);
    tablePanel.add(tableControls);
    outer.add(tablePanel);
    outer.add(new HTML(
        "<p>This list enables SpeedTracer to post results via"
            + " XHR.  The host name must be a valid DNS name or ipv4 dotted decimal "
            + " address.</p>"
            + " <p>Use a single '*' in the port field to match any port. Use of "
            + " a single leading or trailing '*' in the host field is allowed to match more "
            + " than one host in a domain or subnet."));
    RootPanel.get().add(outer);
  }

  private void populateUi() {
    tableControls.reloadTable();
  }

}
