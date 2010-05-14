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

import com.google.json.serialization.JsonException;
import com.google.json.serialization.JsonObject;
import com.google.json.serialization.JsonString;
import com.google.speedtracer.latencydashboard.shared.CustomDashboardRecord;
import com.google.speedtracer.shared.EventRecordType;

import java.io.IOException;
import java.io.StringReader;
import java.util.Map;
import java.util.TreeMap;
import java.util.List;

/**
 * Constructs {@link CustomDashboardRecord}s from console.markTimeline messages
 * in the following format
 * 
 * String: <MT_PREFIX><JSON Measurement Object> JSON Measurement Object Schema:
 * {
 *  "description" : "A format for making custom LatencyDashboard timings",
 *  "type" : "object",
 *  "properties" : {
 *    "measurementSet" : {
 *      "type" : "string",
 *      "description" : "The name of the measurement set for this event",
 *    },
 *    "event" : { 
 *      "type" : "string",
 *      "description" : "The sub-event within the set. start/total mark the beginning/end"
 *    }
 *  },
 *  "additionalProperties" : false,
 * }
 * 
 * e.g. __myTimeline{"measurementSet" : "foo", event : "baz" } records the 'baz'
 * event within the 'foo' measurement set. This should occur after the 'start'
 * event and before the 'total' event.
 */

public class MarkTimelineAnalyzer {
  private Map<String, MarkTimelineMeasurementSet> measurements = new TreeMap<String, MarkTimelineMeasurementSet>();

  private final String prefix;

  private final SpeedTraceAnalyzer speedTraceAnalyzer;

  public MarkTimelineAnalyzer(SpeedTraceAnalyzer speedTraceAnalyzer,
      String prefix) {
    this.prefix = prefix;
    this.speedTraceAnalyzer = speedTraceAnalyzer;
  }

  // TODO(conroy): refactor this and the other analyzers to use a visitor (for
  // a single pass on the data)
  public void analyze() throws JsonException {
    List<JsonObject> logRecords = speedTraceAnalyzer.findRecordsByType(EventRecordType.LOG_MESSAGE_EVENT);
    for (JsonObject logRecord : logRecords) {
      JsonObject dataObject = logRecord.get("data").asObject();
      String message = dataObject.get("message").asString().getString();
      if (message.startsWith(prefix)) {
        try {
          String embeddedJson = message.substring(prefix.length());
          JsonObject timelineObject = JsonObject.parse(new StringReader(
              embeddedJson));
          analyzeTimelineMessage(timelineObject,
              logRecord.get("time").asNumber().getDecimal());
        } catch (IOException ex) {
          System.err.println("Huh? IO Exception? " + ex);
          ex.printStackTrace();
        }
      }
    }
  }

  /**
   * Tell the analyzer to listen for the given measurement set.
   * @param measurementName
   */
  public void registerMeasurementSet(String measurementName) {
    measurements.put(measurementName, new MarkTimelineMeasurementSet(
        measurementName));
  }

  /**
   * Store the measurements in a @{link {@link CustomDashboardRecord}.
   * @param customRecord
   */
  public void store(CustomDashboardRecord customRecord) {
    for (MarkTimelineMeasurementSet measurement : measurements.values()) {
      measurement.store(customRecord);
    }
  }

  /**
   * Analyze a JSON timelineObject. 
   * @param timelineObject
   * @param eventTime
   */
  private void analyzeTimelineMessage(JsonObject timelineObject,
      double eventTime) {
    JsonString measurementNameJS = timelineObject.get("measurementSet").asString();
    JsonString eventJS = timelineObject.get("event").asString();
    if (measurementNameJS == null || eventJS == null) {
      System.err.println("Encountered malformed timeline object: "
          + timelineObject.toString());
      return;
    }

    String measurementName = measurementNameJS.getString();
    String event = eventJS.getString();

    if (measurements.containsKey(measurementName)) {
      MarkTimelineMeasurementSet measurement = measurements.get(measurementName);
      measurement.handleEvent(event, eventTime);
    } else {
      System.out.println("Skipping measurementSet " + measurementName);
    }
  }
}
