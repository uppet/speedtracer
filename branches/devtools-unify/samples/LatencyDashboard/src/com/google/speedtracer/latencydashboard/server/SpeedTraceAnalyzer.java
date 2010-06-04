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

import com.google.json.serialization.JsonArray;
import com.google.json.serialization.JsonException;
import com.google.json.serialization.JsonObject;
import com.google.json.serialization.JsonValue;
import com.google.speedtracer.server.JsonTraverser;
import com.google.speedtracer.server.SelfTimeVisitor;
import com.google.speedtracer.shared.EventRecordType;

import java.util.ArrayList;
import java.util.List;

/**
 * A class that analyzes a SpeedTrace sent from the headless extension via XHR.
 */
public class SpeedTraceAnalyzer {
  private double domContentLoadedTime = 0;
  private double evalScriptDuration = 0;
  private double garbageCollectionDuration = 0;
  private double javaScriptExecutionDuration = 0;
  private double layoutDuration = 0;
  private double loadEventTime = 0;
  private long mainResourceIdentfier;
  private double mainResourceResponseTime = 0;
  private int mainResourceStartIndex = -1;
  private double mainResourceStartTime = 0;
  private double paintDuration = 0;
  private double parseHtmlDuration = 0;
  private final JsonArray records;
  private double styleRecalculationDuration;

  public SpeedTraceAnalyzer(JsonArray recordsJsonArray) {
    this.records = recordsJsonArray;
  }

  /**
   * Run the analysis on the specified records.
   * 
   * @throws JsonException
   */
  public void analyze() throws JsonException {
    calcSelfTime();

    this.mainResourceStartIndex = findMainResourceStartTime();
    this.mainResourceResponseTime = findMainResourceResponseTime();
    
    JsonTraverser traverser = new JsonTraverser();

    JsonTraverser.JsonVisitor visitor = new JsonTraverser.JsonVisitor() {
      public void postProcess() {
      }

      public void visit(JsonObject node) throws JsonException {
        int type = (int) (node.get("type").asNumber().getInteger());
        switch (type) {
          // Look for DOMContentLoaded & Load events
          case EventRecordType.DOM_CONTENT_LOADED:
            domContentLoadedTime = node.get("time").asNumber().getDecimal();
            break;
            
          case EventRecordType.LOAD_EVENT:
            loadEventTime = node.get("time").asNumber().getDecimal();
            break;

          // Aggregate event type times.
          case EventRecordType.JAVASCRIPT_EXECUTION:
            javaScriptExecutionDuration += node.get("selfTime").asNumber().getDecimal();
            break;

          case EventRecordType.LAYOUT_EVENT:
            layoutDuration += node.get("selfTime").asNumber().getDecimal();
            break;

          case EventRecordType.RECALC_STYLE_EVENT:
            styleRecalculationDuration += node.get("selfTime").asNumber().getDecimal();
            break;

          case EventRecordType.EVAL_SCRIPT_EVENT:
            evalScriptDuration += node.get("selfTime").asNumber().getDecimal();
            break;

          case EventRecordType.GC_EVENT:
            garbageCollectionDuration += node.get("selfTime").asNumber().getDecimal();
            break;

          case EventRecordType.PAINT_EVENT:
            paintDuration += node.get("selfTime").asNumber().getDecimal();
            break;

          case EventRecordType.PARSE_HTML_EVENT:
            parseHtmlDuration += node.get("selfTime").asNumber().getDecimal();
            break;
        }
      }
    };
    for (int i = this.mainResourceStartIndex, length = records.getLength(); i < length; ++i) {
      traverser.traversePreOrder(records.get(i).asObject(), visitor);
    }
  }

  public List<JsonObject> findRecordsByType(int queryType) throws JsonException {
    List<JsonObject> results = new ArrayList<JsonObject>();
    for (int i = this.mainResourceStartIndex, length = this.records.getLength(); i < length; ++i) {
      // Perform a recursive search
      findRecordsByTypeRecursive(records.get(i).asObject(), queryType, results);
    }
    return results;
  }

  public double getDomContentLoadedTime() {
    return domContentLoadedTime;
  }

  public double getEvalScriptDuration() {
    return evalScriptDuration;
  }

  public double getGarbageCollectionDuration() {
    return garbageCollectionDuration;
  }

  public double getJavaScriptExecutionDuration() {
    return javaScriptExecutionDuration;
  }

  public double getLayoutDuration() {
    return layoutDuration;
  }

  public double getLoadEventTime() {
    return loadEventTime;
  }

  public long getMainResourceIdentfier() {
    return mainResourceIdentfier;
  }

  public double getMainResourceRequestTime() {
    return this.mainResourceStartTime;
  }

  public double getMainResourceResponseTime() {
    return this.mainResourceResponseTime;
  }

  public int getMainResourceStartIndex() {
    return mainResourceStartIndex;
  }

  public double getMainResourceStartTime() {
    return mainResourceStartTime;
  }

  public double getPaintDuration() {
    return paintDuration;
  }

  public double getParseHtmlDuration() {
    return parseHtmlDuration;
  }

  public JsonArray getRecords() {
    return records;
  }

  public double getStyleRecalculationDuration() {
    return styleRecalculationDuration;
  }

  private void calcSelfTime() throws JsonException {
    SelfTimeVisitor selfTimeVisitor = new SelfTimeVisitor();
    for (int i = 0, length = records.getLength(); i < length; ++i) {
      JsonObject record = records.get(i).asObject();
      JsonTraverser.get().traverse(record, selfTimeVisitor);
    }
  }

  private double findMainResourceResponseTime() {
    for (int i = this.mainResourceStartIndex, length = records.getLength(); i < length; ++i) {
      JsonObject topLevelRec = records.get(i).asObject();
      long type = topLevelRec.get("type").asNumber().getInteger();
      if (type == EventRecordType.RESOURCE_RECEIVE_RESPONSE
          || type == EventRecordType.RESOURCE_DATA_RECEIVED
          || type == EventRecordType.RESOURCE_FINISH) {
        JsonObject data = topLevelRec.get("data").asObject();
        if (data == JsonValue.NULL) {
          throw new AnalyzeException(
              "Expected data object in RESOURCE_RECEIVE_RESPONSE");
        }
        long identifier = data.get("identifier").asNumber().getInteger();
        if (this.mainResourceIdentfier == identifier) {
          return topLevelRec.get("time").asNumber().getDecimal();
        }
      }
    }
    throw new AnalyzeException(
        "Could not find time of main resource receive response between "
            + this.mainResourceStartIndex + " and " + records.getLength() + ".");
  }

  private int findMainResourceStartTime() {
    for (int i = 0, length = records.getLength(); i < length; ++i) {
      JsonObject topLevelRec = records.get(i).asObject();
      long type = topLevelRec.get("type").asNumber().getInteger();
      if (type == EventRecordType.RESOURCE_SEND_REQUEST) {
        JsonObject data = topLevelRec.get("data").asObject();
        if (data == JsonValue.NULL) {
          throw new AnalyzeException(
              "Expected data object in RESOURCE_SEND_REQUEST");
        }
        JsonValue isMainResource = data.get("isMainResource");
        JsonValue identifier = data.get("identifier");
        if (isMainResource != null && isMainResource != JsonValue.NULL
            && identifier != null && identifier != JsonValue.NULL) {
          this.mainResourceIdentfier = identifier.asNumber().getInteger();
          this.mainResourceStartTime = topLevelRec.get("time").asNumber().getDecimal();
          // System.out.println("Found: " + data);
          return i;
        }
      }
    }
    throw new AnalyzeException(
        "Could not find time of main resource send request.");
  }

  private void findRecordsByTypeRecursive(JsonObject record, int queryType,
      List<JsonObject> results) throws JsonException {
    long type = record.get("type").asNumber().getInteger();
    if (type == queryType) {
      results.add(record);
    }
    JsonValue childNode = record.get("children");
    if (childNode != JsonValue.NULL) {
      JsonArray children = childNode.asArray();
      for (int i = 0, length = children.getLength(); i < length; ++i) {
        findRecordsByTypeRecursive(children.get(i).asObject(), queryType,
            results);
      }
    }
  }
}
