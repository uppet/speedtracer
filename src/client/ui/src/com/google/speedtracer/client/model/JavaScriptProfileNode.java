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
package com.google.speedtracer.client.model;

import java.util.ArrayList;
import java.util.List;

/**
 * This stores a hierarchy of the profile data for bottom-up and top-down profiles.
 */
public class JavaScriptProfileNode {
  private final String symbolName;
  private List<JavaScriptProfileNode> children = new ArrayList<JavaScriptProfileNode>();
  private double selfTimeMsecs = 0;
  private double timeMsecs = 0;

  public JavaScriptProfileNode(String symbolName) {
    this.symbolName = symbolName;
  }

  public void addSelfTime(double msecs) {
    this.timeMsecs += msecs;
    this.selfTimeMsecs += msecs;
  }

  public void addTime(double msecs) {
    this.timeMsecs += msecs;
  }

  public List<JavaScriptProfileNode> getChildren() {
    return children;
  }

  public JavaScriptProfileNode getOrInsertChild(String symbolName) {
    // It might be better to use a hash table, but we don't
    // expect large numbers of children at each level.
    for (JavaScriptProfileNode node : children) {
      if (node.getSymbolName().equals(symbolName)) {
        return node;
      }
    }
    JavaScriptProfileNode result = new JavaScriptProfileNode(symbolName);
    children.add(result);
    return result;
  }

  /**
   * Returns the self time in Milliseconds.
   * 
   * @return
   */
  public double getSelfTime() {
    return this.selfTimeMsecs;
  }

  public String getSymbolName() {
    return symbolName;
  }

  /**
   * Returns time spent in this node in milliseconds.
   * 
   * @return
   */
  public double getTime() {
    return this.timeMsecs;
  }
}