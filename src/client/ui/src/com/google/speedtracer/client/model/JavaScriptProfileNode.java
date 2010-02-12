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
 * This stores a hierarchy of the profile data for bottom-up and top-down
 * profiles.
 */
public class JavaScriptProfileNode {
  JsSymbol symbol;
  private List<JavaScriptProfileNode> children = new ArrayList<JavaScriptProfileNode>();
  private final String symbolType;
  private double selfTime = 0;
  private double time = 0;

  public JavaScriptProfileNode(JsSymbol symbol) {
    this(symbol, "");
  }

  public JavaScriptProfileNode(JsSymbol symbol, String symbolType) {
    this.symbol = symbol;
    this.symbolType = symbolType;
  }

  public void addChild(JavaScriptProfileNode child) {
    children.add(child);
  }

  public void addSelfTime(double msecs) {
    this.time += msecs;
    this.selfTime += msecs;
  }

  public void addTime(double msecs) {
    this.time += msecs;
  }

  public List<JavaScriptProfileNode> getChildren() {
    return children;
  }

  /**
   * Returns the self time.
   */
  public double getSelfTime() {
    return this.selfTime;
  }

  public JsSymbol getSymbol() {
    return this.symbol;
  }

  public String getSymbolType() {
    return this.symbolType;
  }

  /**
   * Returns time spent in this node.
   */
  public double getTime() {
    return this.time;
  }

  public JavaScriptProfileNode lookup(JsSymbol symbol, String typeName) {
    // It might be better to use a hash table, but we don't
    // expect large numbers of children at each level.
    for (JavaScriptProfileNode node : children) {
      if (node.getSymbol().sameAs(symbol)
          && node.getSymbolType().equals(typeName)) {
        return node;
      }
    }
    return null;
  }

  public void merge(JavaScriptProfileNode profileChild) {
    selfTime += profileChild.getSelfTime();
    time += profileChild.getTime();
  }
}