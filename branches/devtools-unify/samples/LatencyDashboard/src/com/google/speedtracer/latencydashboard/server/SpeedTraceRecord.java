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

import java.io.IOException;
import java.io.StringReader;

/**
 * Represents a single trace returned from SpeedTracer in an XHR.
 */
public class SpeedTraceRecord {

  private final String name;
  private final String rawData;
  private JsonObject rawDataObject;
  private final String revision;
  private final long timeStamp;

  public SpeedTraceRecord(String rawData) throws JsonException {
    this.rawData = rawData;
    JsonObject headerObject = getHeaderObject();
    timeStamp = (long) headerObject.get("timeStamp").asNumber().getDecimal();
    name = headerObject.get("name").asString().getString();
    revision = headerObject.get("revision").asString().getString();
  }

  public JsonArray getDataObject() throws JsonException {
    return getRawDataObject().get("data").asArray();
  }

  public JsonObject getHeaderObject() throws JsonException {
    return getRawDataObject().get("header").asObject();
  }

  public String getName() {
    return name;
  }

  public JsonObject getRawDataObject() throws JsonException {
    if (rawDataObject == null) {
      try {
        rawDataObject = JsonObject.parse(new StringReader(rawData));
      } catch (IOException ex) {
        throw new RuntimeException("Couldn't parse data", ex);
      }
    }
    return rawDataObject;
  }

  public String getRevision() {
    return this.revision;
  }

  public long getTimestamp() {
    return this.timeStamp;
  }

}
