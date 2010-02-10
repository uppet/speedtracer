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

/**
 * A Source and line number for a JavaScript symbol as specified in the symbol
 * mapping.
 */
public class JsSymbol {
  private final boolean isNativeSymbol;

  private int lineNumber;

  private String resourceName;

  private String resourcePathBase;

  private final String symbolName;

  public JsSymbol(String resourcePathBase, String resourceName,
      int lineNumber, String symbolName) {
    this(resourcePathBase, resourceName, lineNumber, symbolName, false);
  }

  public JsSymbol(String resourcePathBase, String resourceName,
      int lineNumber, String symbolName, boolean isNativeSymbol) {
    this.resourcePathBase = resourcePathBase;
    this.resourceName = resourceName;
    this.lineNumber = lineNumber;
    this.symbolName = symbolName;
    this.isNativeSymbol = isNativeSymbol;
  }

  public int getLineNumber() {
    return lineNumber;
  }

  /**
   * The base of the resource. The base can be interpreted as the base of the
   * URL to the resource.
   * 
   * @return the resource base path.
   */
  public String getResourceBase() {
    return resourcePathBase;
  }

  /**
   * The name of the source file or resource containing the symbol.
   * 
   * @return the name of the source file containing the symbol.
   */
  public String getResourceName() {
    return resourceName;
  }

  public String getSymbolName() {
    return symbolName;
  }

  public boolean isNativeSymbol() {
    return isNativeSymbol;
  }
  
  public void merge(JsSymbol toMerge) {
    if ("".equals(this.resourceName)) {
      this.resourceName = toMerge.getResourceName();
      this.resourcePathBase = toMerge.getResourceBase();
      this.lineNumber = toMerge.getLineNumber();
    }
  }

  public boolean sameAs(JsSymbol symbol) {
    return this.resourceName.equals(symbol.getResourceName())
        && this.symbolName.equals(symbol.getSymbolName())
        && (this.lineNumber == symbol.getLineNumber());
  }
}