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
package com.google.speedtracer.server;

import com.google.json.serialization.JsonException;
import com.google.json.serialization.JsonObject;
import com.google.json.serialization.JsonValue;
import com.google.speedtracer.server.JsonTraverser.JsonVisitorDouble;

import java.util.List;

/**
 * Sets the selfTime property on all nodes in this event.
 */
public class SelfTimeVisitor implements JsonVisitorDouble {

  public void postProcess() {
  }

  public double visit(JsonObject node, List<Double> values)
      throws JsonException {
    double childTime = 0;
    for (double value : values) {
      childTime += value;
    }
    double duration = 0;
    JsonValue durationNode = node.get("duration");
    if (durationNode != JsonValue.NULL) {
      duration = durationNode.asNumber().getDecimal();
    }
    double selfTime = duration - childTime;
    node.put("selfTime", selfTime);
    return duration;
  }
}
