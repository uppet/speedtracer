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

import com.google.speedtracer.client.util.JSOArray;

import java.util.List;

/**
 * A data model that can be driven by pre-recorded data. See #
 * {@link MockModelGenerator} to get a MockModel based on a number of different
 * event logs.
 */
public class MockDataModel extends DataModel {
  public static final int MOCK_TABID = 0;

  MockDataModel() {
    super();
  }

  public List<String> getDataSetNames() {
    return MockModelGenerator.getDataSetNames();
  }

  @Override
  public void resumeMonitoring(int tabId) {
  }

  @Override
  public void saveRecords(JSOArray<String> visitedUrls, String version) {
  }

  public void simulateDataSet(int dataSetIndex) {
    MockModelGenerator.simulateDataSet(this, dataSetIndex);
  }

  @Override
  public void stopMonitoring() {
  }

  @Override
  protected void bind(TabDescription tabDescription, DataInstance dataInstance) {
    setTabDescription(tabDescription);
  }
}
