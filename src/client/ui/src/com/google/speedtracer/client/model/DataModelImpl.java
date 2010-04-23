/*
 * Copyright 2008 Google Inc.
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
package com.google.speedtracer.client.model;

import com.google.gwt.chrome.crx.client.Tabs;
import com.google.gwt.core.client.GWT;
import com.google.gwt.coreext.client.JSOArray;
import com.google.gwt.user.client.Window;

/**
 * Getters for getting all Models which provide events to drive UI.
 */
public class DataModelImpl extends DataModel {
  DataModelImpl() {
  }

  @Override
  protected void bind(TabDescription tabDescription,
      final DataInstance dataInstance) {
    dataInstance.<DataInstance> cast().load(this);
  }

  @Override
  public void resumeMonitoring(int tabId) {
    getDataInstance().<DataInstance> cast().resumeMonitoring();
  }

  @Override
  public void saveRecords(JSOArray<String> visitedUrls, String version) {
    // Create expando on our View so that the tab we create can callback and
    // receive the record data and file information.
    setupViewCallback(visitedUrls, version);

    // Create a new tab at the save data template page. Give it the same query
    // string as our own.
    Tabs.create(GWT.getModuleBaseURL() + "SpeedTracerData.html"
        + Window.Location.getQueryString());
  }

  @Override
  public void stopMonitoring() {
    getDataInstance().<DataInstance> cast().stopMonitoring();
  }

  /**
   * Hangs an expando on our view that the save data page will use to request
   * the record data and file information.
   * 
   * @param visitedUrls An array of URLs visited in this data set.
   * @param version The Speed Tracer version.
   */
  private native void setupViewCallback(JSOArray<String> visitedUrls,
      String version) /*-{
    var me = this;
    top._onSaveReady = function(doSave) {
      doSave(version,
             visitedUrls,
             me.@com.google.speedtracer.client.model.DataModel::getTraceCopy()());
    };
  }-*/;
}
