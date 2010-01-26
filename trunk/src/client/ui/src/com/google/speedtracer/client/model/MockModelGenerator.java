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

/**
 * Generated File that Generates MockEvents for MockModels.
 */
public class MockModelGenerator {

  /**
   * Pull in the simulated data from external text files.
   */
  public interface MockResources extends ClientBundle {
    @Source("../../tools/data/reddit.com")
    TextResource redditDotCom();
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

  public static MockResources mockResources = GWT.create(MockResources.class);

  public static void simulateRedditDotCom(MockDataModel mockModel) {
    final Generator generator = new Generator(mockModel);
    String[] events = mockResources.redditDotCom().getText().split("\n");
    generator.run(events);
  }

}
