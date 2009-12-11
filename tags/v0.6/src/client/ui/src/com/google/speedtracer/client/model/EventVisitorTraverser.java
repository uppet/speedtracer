/*
 * Copyright 2008 Google Inc.
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

import com.google.speedtracer.client.model.EventVisitor.PostOrderVisitor;
import com.google.speedtracer.client.model.EventVisitor.PreOrderVisitor;
import com.google.speedtracer.client.util.JSOArray;

/**
 * Traverses UiEvent trees and applies visitors.
 */
public class EventVisitorTraverser {

  /**
   * Traverses a UiEvent Context tree and applies both PreOrder and PostOrder
   * Visitors along the same traversal.
   * 
   * @param preOrderVisitors the {@link EventVisitor}s to apply in pre order
   * @param eventNode the root node for the traversal
   * @param postOrderVisitors the {@link EventVisitor}s to apply in post
   */
  public static void traverse(PreOrderVisitor[] preOrderVisitors,
      UiEvent eventNode, PostOrderVisitor[] postOrderVisitors) {
    traverseImpl(preOrderVisitors, eventNode, postOrderVisitors);

    // Do post processing steps on all the visitors.
    for (int i = 0, n = preOrderVisitors.length; i < n; i++) {
      preOrderVisitors[i].postProcess();
    }
    for (int i = 0, n = postOrderVisitors.length; i < n; i++) {
      postOrderVisitors[i].postProcess();
    }
  }

  /**
   * Pre-order traversal of a UiEvent Context tree applying the visitor.
   * 
   * @param postOrderVisitor visitors to apply.
   * @param eventNode the root node for the traversal.
   */
  public static void traversePostOrder(UiEvent eventNode,
      PostOrderVisitor[] postOrderVisitors) {
    traverseImpl(null, eventNode, postOrderVisitors);
    // Do post processing steps on all the visitors.
    for (int i = 0, n = postOrderVisitors.length; i < n; i++) {
      postOrderVisitors[i].postProcess();
    }
  }

  /**
   * Pre-order traversal of a UiEvent Context tree applying the visitor.
   * 
   * @param preOrderVisitor visitors to apply.
   * @param eventNode the root node for the traversal.
   */
  public static void traversePreOrder(PreOrderVisitor[] preOrderVisitors,
      UiEvent eventNode) {
    traverseImpl(preOrderVisitors, eventNode, null);
    // Do post processing steps on all the visitors.
    for (int i = 0, n = preOrderVisitors.length; i < n; i++) {
      preOrderVisitors[i].postProcess();
    }
  }

  private static void traverseImpl(PreOrderVisitor[] preOrderVisitors,
      UiEvent eventNode, PostOrderVisitor[] postOrderVisitors) {
    if (preOrderVisitors != null) {
      // Visit da visitors
      for (int i = 0, n = preOrderVisitors.length; i < n; i++) {
        eventNode.acceptVisitor(preOrderVisitors[i]);
      }
    }

    // Continue da traversal
    JSOArray<UiEvent> children = eventNode.getChildren();
    for (int i = 0, n = children.size(); i < n; i++) {
      traverseImpl(preOrderVisitors, children.get(i), postOrderVisitors);
    }

    if (postOrderVisitors != null) {
      // Visit da visitors
      for (int i = 0, n = postOrderVisitors.length; i < n; i++) {
        eventNode.acceptVisitor(postOrderVisitors[i]);
      }
    }
  }

  private EventVisitorTraverser() {
  }
}
