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

import com.google.json.serialization.JsonArray;
import com.google.json.serialization.JsonException;
import com.google.json.serialization.JsonObject;
import com.google.json.serialization.JsonValue;
import com.google.speedtracer.server.JsonTraverser.JsonVisitor;
import com.google.speedtracer.server.JsonTraverser.JsonVisitorDouble;

import junit.framework.TestCase;

import java.util.List;

/**
 * Test for {@link JsonTraverser} class.
 */
public class JsonTraverserTest extends TestCase {

  private JsonObject makeChild(String property, long value) {
    JsonObject result = new JsonObject();
    result.put(property, value);
    return result;
  }

  public void testTraversePostorder() throws JsonException {
    JsonObject root = new JsonObject();
    root.put("value", 3);
    JsonArray children = new JsonArray();
    children.add(makeChild("value", 1));
    children.add(makeChild("value", 2));
    root.put("children", children);

    JsonTraverser.get().traversePostOrder(root, new JsonVisitor() {
      private long curr = 0;

      public void postProcess() {
      }

      public void visit(JsonObject node) throws JsonException {
        JsonValue valueNode = node.get("value");
        assertNotNull(valueNode);
        long value = valueNode.asNumber().getInteger();
        assertEquals(curr + 1, value);
        curr++;
      }
    });
  }

  public void testTraversePreorder() throws JsonException {
    JsonObject root = new JsonObject();
    root.put("value", 1);
    JsonArray children = new JsonArray();
    children.add(makeChild("value", 2));
    children.add(makeChild("value", 3));
    root.put("children", children);

    JsonTraverser.get().traversePreOrder(root, new JsonVisitor() {
      private long curr = 0;

      public void postProcess() {
      }

      public void visit(JsonObject node) throws JsonException {
        JsonValue valueNode = node.get("value");
        assertNotNull(valueNode);
        long value = valueNode.asNumber().getInteger();
        assertEquals(curr + 1, value);
        curr++;
      }
    });
  }

  public void testTraversePostorderNumber() throws JsonException {
    JsonObject root = new JsonObject();
    root.put("value", 3);
    JsonArray children = new JsonArray();
    children.add(makeChild("value", 1));
    children.add(makeChild("value", 2));
    root.put("children", children);

    JsonTraverser.get().traverse(root, new JsonVisitorDouble() {
      private long curr = 0;

      public void postProcess() {
      }

      public double visit(JsonObject node, List<Double> values)
          throws JsonException {
        JsonValue valueNode = node.get("value");
        assertNotNull(valueNode);
        long value = valueNode.asNumber().getInteger();
        assertEquals(curr + 1, value);
        curr++;
        if (values.size() > 0) {
          double sum = 0;
          for (double childValue : values) {
            sum += childValue;
          }
          assertEquals(sum, value, .001);
        }
        return value;
      }
    });
  }
}
