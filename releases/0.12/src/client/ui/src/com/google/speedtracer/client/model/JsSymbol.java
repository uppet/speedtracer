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

import com.google.speedtracer.client.util.Url;

/**
 * A Source and line number for a JavaScript symbol as specified in the symbol
 * mapping.
 */
public class JsSymbol {
  private final boolean isNativeSymbol;

  private int lineNumber;

  private Url resourceUrl;

  private final String symbolName;

  public JsSymbol(Url resourceUrl, int lineNumber, String symbolName) {
    this(resourceUrl, lineNumber, symbolName, false);
  }

  public JsSymbol(Url resourceUrl, int lineNumber, String symbolName,
      boolean isNativeSymbol) {
    this.resourceUrl = resourceUrl;
    this.lineNumber = lineNumber;
    this.symbolName = symbolName;
    this.isNativeSymbol = isNativeSymbol;
  }

  public int getLineNumber() {
    return lineNumber;
  }

  public Url getResourceUrl() {
    return resourceUrl;
  }

  public String getSymbolName() {
    return symbolName;
  }

  public boolean isNativeSymbol() {
    return isNativeSymbol;
  }

  public void merge(JsSymbol toMerge) {
    if ("".equals(resourceUrl.getLastPathComponent())) {
      resourceUrl = toMerge.getResourceUrl();
      lineNumber = toMerge.getLineNumber();
    }
  }

  public boolean sameAs(JsSymbol symbol) {
    return resourceUrl.getLastPathComponent().equals(
        symbol.getResourceUrl().getLastPathComponent())
        && symbolName.equals(symbol.getSymbolName())
        && (lineNumber == symbol.getLineNumber());
  }
}