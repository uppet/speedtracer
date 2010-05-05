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
import com.google.speedtracer.shared.EventRecordType;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;

/**
 * A class that analyzes a SpeedTrace sent from the headless extension via XHR.
 */
public class GwtAnalyzer {
  private static final String GWT_LIGHTWEIGHT_STATS_PREFIX = "__gwtStatsEvent: ";

  private double bootstrapEndTime = 0;
  private double bootstrapStartTime = 0;
  private double domContentLoadedTime = 0;
  private double loadExternalRefsEndTime = 0;
  private double loadExternalRefsStartTime = 0;
  private double moduleEvalEndTime = 0;
  private double moduleEvalStartTime = 0;
  private double moduleStartupEndTime = 0;
  private double moduleStartupTime = 0;

  private final SpeedTraceAnalyzer speedTraceAnalyzer;

  public GwtAnalyzer(SpeedTraceAnalyzer speedTraceAnalyzer) {

    this.speedTraceAnalyzer = speedTraceAnalyzer;
  }

  /**
   * Scans the data used to create this analyzer and extracts key metrics from
   * the data.
   * 
   * @throws JsonException
   */
  public void analyze() throws JsonException {
    analyzeLogRecords();
  }

  public double getBootstrapEndTime() {
    return bootstrapEndTime;
  }

  public double getBootstrapStartTime() {
    return bootstrapStartTime;
  }

  public double getDomContentLoadedTime() {
    return domContentLoadedTime;
  }

  public double getLoadExternalRefsEndTime() {
    return loadExternalRefsEndTime;
  }

  public double getLoadExternalRefsStartTime() {
    return loadExternalRefsStartTime;
  }

  public double getModuleEvalEndTime() {
    return moduleEvalEndTime;
  }

  public double getModuleEvalStartTime() {
    return moduleEvalStartTime;
  }

  public double getModuleStartupEndTime() {
    return moduleStartupEndTime;
  }

  public double getModuleStartupTime() {
    return moduleStartupTime;
  }

  private void analyzeLightweightMetric(JsonObject messageObject,
      double eventTime) {
    if (isMatchingMetric(messageObject, "startup", "bootstrap", "begin")) {
      this.bootstrapStartTime = eventTime;
    } else if (isMatchingMetric(messageObject, "startup", "bootstrap", "end")) {
      this.bootstrapEndTime = eventTime;
    } else if (isMatchingMetric(messageObject, "startup", "loadExternalRefs",
        "begin")) {
      this.loadExternalRefsStartTime = eventTime;
    } else if (isMatchingMetric(messageObject, "startup", "loadExternalRefs",
        "end")) {
      this.loadExternalRefsEndTime = eventTime;
    } else if (isMatchingMetric(messageObject, "startup", "moduleStartup",
        "moduleRequested")) {
      this.moduleStartupTime = eventTime;
    } else if (isMatchingMetric(messageObject, "startup", "moduleStartup",
        "moduleEvalStart")) {
      this.moduleEvalStartTime = eventTime;
    } else if (isMatchingMetric(messageObject, "startup", "moduleStartup",
        "moduleEvalEnd")) {
      this.moduleEvalEndTime = eventTime;
    } else if (isMatchingMetric(messageObject, "startup", "moduleStartup",
        "end")) {
      this.moduleStartupEndTime = eventTime;
    }
  }

  private void analyzeLogRecords() throws JsonException {
    List<JsonObject> logRecords = speedTraceAnalyzer.findRecordsByType(EventRecordType.LOG_MESSAGE_EVENT);

    for (JsonObject logRecord : logRecords) {
      JsonObject dataObject = logRecord.get("data").asObject();
      String message = dataObject.get("message").asString().getString();
      if (message.startsWith(GWT_LIGHTWEIGHT_STATS_PREFIX)) {
        try {
          String embeddedJson = message.substring(GWT_LIGHTWEIGHT_STATS_PREFIX.length());
          JsonObject messageObject = JsonObject.parse(new StringReader(
              embeddedJson));
          analyzeLightweightMetric(messageObject,
              logRecord.get("time").asNumber().getDecimal());
        } catch (IOException ex) {
          System.err.println("Huh? IO Exception? " + ex);
          ex.printStackTrace();
        }
      }
    }
  }

  private boolean isMatchingMetric(JsonObject messageObject, String subSystem,
      String evtGroup, String type) {
    String subSystemCmp = messageObject.get("subSystem").asString().getString();
    if (!subSystem.equals(subSystemCmp)) {
      return false;
    }
    String evtGroupCmp = messageObject.get("evtGroup").asString().getString();
    if (!evtGroup.equals(evtGroupCmp)) {
      return false;
    }
    String typeCmp = messageObject.get("type").asString().getString();
    return (type.equals(typeCmp));
  }
}
