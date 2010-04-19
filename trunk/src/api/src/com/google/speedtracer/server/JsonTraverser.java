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

import java.util.ArrayList;
import java.util.List;

/**
 * Traverse a tree of JavaScript nodes. The nodes are expected to have a
 * JavaScript property named 'children' that contains an array of child nodes of
 * the same type.
 */
public class JsonTraverser {
  /**
   * Interface used to pass logic to {@link JsonTraverser} to walk a JavaScript
   * tree.
   */
  public static interface JsonVisitor {
    public void postProcess();

    public void visit(JsonObject node) throws JsonException;
  }

  public static interface JsonVisitorDouble {
    public void postProcess();

    public double visit(JsonObject node, List<Double> values)
        throws JsonException;
  }

  private static JsonTraverser singleton;

  public static JsonTraverser get() {
    if (singleton == null) {
      singleton = new JsonTraverser();
    }
    return singleton;
  }

  public JsonTraverser() {
  }

  /**
   * Post-order traversal of a UiEvent Context tree applying the visitor.
   * 
   * @param eventNode the root node for the traversal
   * @param postOrderVisitors visitor to apply. The result from child visitors
   *          is passed as a list of values.
   * @throws JsonException
   */
  public void traverse(JsonObject node, JsonVisitorDouble postOrderVisitor)
      throws JsonException {
    traverseImpl(node, postOrderVisitor);
    postOrderVisitor.postProcess();
  }

  /**
   * Post-order traversal of a UiEvent Context tree applying the visitor.
   * 
   * @param eventNode the root node for the traversal
   * @param postOrderVisitors visitors to apply
   * @throws JsonException
   */
  public void traversePostOrder(JsonObject node, JsonVisitor postOrderVisitor)
      throws JsonException {
    traverseImpl(node, null, postOrderVisitor);
    if (postOrderVisitor != null) {
      postOrderVisitor.postProcess();
    }
  }

  /**
   * Pre-order traversal of a UiEvent Context tree applying the visitor.
   * 
   * @param preOrderVisitor visitor to apply.
   * @param eventNode the root node for the traversal.
   * @throws JsonException
   */
  public void traversePreOrder(JsonObject node, JsonVisitor preOrderVisitor)
      throws JsonException {
    traverseImpl(node, preOrderVisitor, null);
    if (preOrderVisitor != null) {
      preOrderVisitor.postProcess();
    }
  }

  private void traverseImpl(JsonObject node, JsonVisitor preOrderVisitor,
      JsonVisitor postOrderVisitor) throws JsonException {
    if (preOrderVisitor != null) {
      preOrderVisitor.visit(node);
    }

    JsonValue childNode = node.get("children");
    if (childNode != JsonValue.NULL) {
      JsonArray children = childNode.asArray();

      for (int i = 0, n = children.getLength(); i < n; i++) {
        traverseImpl(children.get(i).asObject(), preOrderVisitor,
            postOrderVisitor);
      }
    }

    if (postOrderVisitor != null) {
      postOrderVisitor.visit(node);
    }
  }

  private double traverseImpl(JsonObject node,
      JsonVisitorDouble postOrderVisitor) throws JsonException {
    List<Double> values = new ArrayList<Double>();
    JsonValue childNode = node.get("children");
    if (childNode != JsonValue.NULL) {
      JsonArray children = childNode.asArray();
      for (int i = 0, n = children.getLength(); i < n; i++) {
        values.add(traverseImpl(children.get(i).asObject(), postOrderVisitor));
      }
    }
    return postOrderVisitor.visit(node, values);
  }
}
