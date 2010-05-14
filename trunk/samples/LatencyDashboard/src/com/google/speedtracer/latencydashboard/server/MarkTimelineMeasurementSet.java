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

package com.google.speedtracer.latencydashboard.server;

import com.google.speedtracer.latencydashboard.shared.CustomDashboardRecord;

import java.util.Map;
import java.util.TreeMap;

/**
 * Data model markTimeline LatencyDashboard messages.
 */
public class MarkTimelineMeasurementSet {

  private double baseTime = 0d;

  private TreeMap<String, Double> elapsedTimes = new TreeMap<String, Double>();
  private double endTime = 0d;

  private final String name;

  public MarkTimelineMeasurementSet(String name) {
    this.name = name;
  }

  public double getBaseTime() {
    return baseTime;
  }

  public String getName() {
    return name;
  }

  public double getTotal() {
    double total = this.endTime - this.baseTime;
    return (total > 0) ? total : 0d;
  }

  /**
   * Parse the event string from the JSON latency record and store the
   * necessary. information.
   *
   * Special events: 'start' and 'total' mark the beginning and end of a
   * measurement set.
   *
   * TODO(conroy): handle multiple reports of the same measurement set?
   *
   * @param event
   * @param eventTime
   */
  public void handleEvent(String event, double eventTime) {
    if (endTime != 0) {
      System.out.println("Ignoring multiple event reports for " + name);
      return;
    }

    if (event.equals("start")) {
      baseTime = eventTime;
    } else if (event.equals("total") && baseTime != 0d) {
      endTime = eventTime;
    } else if (baseTime != 0d) {
      elapsedTimes.put(event, eventTime - baseTime);
    } else {
      System.err.println("Got premature [" + event + " @" + eventTime + " for "
          + name);
    }
  }

  /**
   * Store this measurement set in a {@link CustomDashboardRecord} .
   * @param customRecord
   */
  public void store(CustomDashboardRecord customRecord) {
    if (getTotal() == 0) {
      System.err.println("Not storing 0 total for " + name);
      return;
    }
    customRecord.addCustomMeasure(name + ":total", getTotal());
    for (Map.Entry<String, Double> elapsed : elapsedTimes.entrySet()) {
      customRecord.addCustomMeasure(name + ":" + elapsed.getKey(),
          elapsed.getValue());
    }
  }
}
