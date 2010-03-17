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
package com.google.speedtracer.client.model;

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.TextResource;
import com.google.speedtracer.client.util.JSON;

import java.util.ArrayList;
import java.util.List;

/**
 * Generated File that Generates MockEvents for MockModels.
 */
public class MockModelGenerator {

  /**
   * Pull in the simulated data from external text files.
   */
  public interface MockResources extends ClientBundle {
    @Source("resources/digg.com")
    TextResource diggDotCom();

    @Source("resources/maps-stacktraces")
    TextResource mapsDotGoogleDotComWithStackTraces();

    @Source("resources/marktimeline")
    TextResource markTimeline();

    @Source("resources/profiler-test1-compressed")
    TextResource profilerTest1Compressed();

    @Source("resources/profiler-test1-uncompressed")
    TextResource profilerTest1Uncompressed();

    @Source("resources/profiler-test2-logs")
    TextResource profilerTest2Logs();

    @Source("resources/reddit.com-profiling")
    TextResource redditDotComWithProfiling();
  }

  private static class DataSet {
    String name;
    TextResource dataSetResource;

    public DataSet(String name, TextResource resource) {
      this.name = name;
      this.dataSetResource = resource;
    }
  }

  /**
   * Base class for generated mock data.
   */
  private static class Generator {
    private final MockDataModel model;

    public Generator(MockDataModel model) {
      this.model = model;
    }

    public void run(String[] eventRecords) {
      for (int i = 0; i < eventRecords.length; i++) {
        EventRecord record = JSON.parse(eventRecords[i]).cast();
        record.setSequence(i);
        model.onEventRecord(record);
      }
    }
  }

  private static List<DataSet> dataSets = new ArrayList<DataSet>();
  private static MockResources mockResources;

  public static List<String> getDataSetNames() {
    initializeDataSets();
    List<String> result = new ArrayList<String>();
    for (int i = 0; i < dataSets.size(); ++i) {
      result.add(dataSets.get(i).name);
    }
    return result;
  }

  public static void simulateDataSet(MockDataModel mockModel, int dataSetIndex) {
    initializeDataSets();
    TextResource resource = dataSets.get(dataSetIndex).dataSetResource;
    final Generator generator = new Generator(mockModel);
    String[] events = resource.getText().split("\n");
    generator.run(events);
  }

  private static void initializeDataSets() {
    if (mockResources == null) {
      mockResources = GWT.create(MockResources.class);
      dataSets.add(new DataSet("digg.com", mockResources.diggDotCom()));
      dataSets.add(new DataSet("markTimeline", mockResources.markTimeline()));
    }
  }
}
