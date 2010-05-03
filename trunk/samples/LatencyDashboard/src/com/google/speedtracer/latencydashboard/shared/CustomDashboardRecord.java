/**
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
package com.google.speedtracer.latencydashboard.shared;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Object stored in the datastore to represent a collection of related timings
 * for a custom dashboard. Any number of timings can be stored.
 */
public class CustomDashboardRecord implements Serializable {

  private static final long serialVersionUID = 1016899757513911899L;

  /**
   * A number of custom timings to show on the same dashboard.
   */
  private Map<String, Double> customMetrics = new HashMap<String, Double>();

  /**
   * A descriptive name provided as a header to the data dump.
   */
  private String name;

  /**
   * A revision string provided as a header to the data dump.
   */
  private String revision;

  /**
   * The time in milliseconds since 1970 representing the time the capture of
   * data started.
   */
  private long timeStamp;

  public CustomDashboardRecord() {
  }

  public CustomDashboardRecord(long timeStamp, String name, String revision) {
    this.timeStamp = timeStamp;
    this.name = name;
    this.revision = revision;
  }

  /**
   * Add one metric to this record to be displayed on the custom dashboard.
   * 
   * @param label the name of this metric
   * @param value the datapoint for this metric
   */
  public void addCustomMeasure(String label, Double value) {
    customMetrics.put(label, value);
  }

  public Map<String, Double> getCustomMetrics() {
    return customMetrics;
  }

  /**
   * Human readable version of the record - for debugging.
   */
  public String getFormattedRecord() {
    StringBuilder builder = new StringBuilder();
    builder.append("name: " + name + "\n");
    builder.append("timeStamp: " + timeStamp + "\n");
    builder.append("revision: " + revision + "\n");
    for (String key : customMetrics.keySet()) {
      Double data = customMetrics.get(key);
      builder.append("  " + key + ": " + data);
    }
    return builder.toString();
  }

  public String getName() {
    return name;
  }

  public String getRevision() {
    return this.revision;
  }

  public long getTimestamp() {
    return this.timeStamp;
  }
}
