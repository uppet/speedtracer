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

package com.google.speedtracer.client.visualizations.model;

import com.google.speedtracer.client.GwtSymbolMapParser;
import com.google.speedtracer.client.util.IterableFastStringMap;

/**
 * Class used for re-symbolizing obfuscated JavaScript. Provides a simple
 * mapping back to original source and line number.
 * 
 * TODO (jaimeyap): Move to a symbol map format that supports character by
 * character mapping.
 */
public class JsSymbolMap {
  public static final String UNKNOWN_RESOURCE_PATH = "Unknown";

  /**
   * A Source and line number for a JavaScript symbol as specified in the symbol
   * mapping.
   */
  public static class JsSymbol {
    private final int lineNumber;

    private final String resourceName;

    private final String resourcePathBase;

    private final String symbolName;

    public JsSymbol(String resourcePathBase, String resourceName,
        int lineNumber, String symbolName) {
      this.resourcePathBase = resourcePathBase;
      this.resourceName = resourceName;
      this.lineNumber = lineNumber;
      this.symbolName = symbolName;
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
  }

  /**
   * Parses the JavaScript symbol map and initializes a {@link JsSymbolMap} with
   * the corresponding symbols and their source mappings.
   * 
   * @param sourceServer The base URL that should be used for looking up
   *          original source files from the symbolMap.
   * @param symbolMapStr The unprocessed text corresponding to the Symbol Map.
   * @return
   */
  public static JsSymbolMap parse(String sourceServer, String type,
      String symbolMapStr) {
    // For now we only support GWT symbol maps.
    if (!"gwt".equals(type.toLowerCase())) {
      return null;
    }

    JsSymbolMap symbolMap = new JsSymbolMap(sourceServer);
    GwtSymbolMapParser parser = new GwtSymbolMapParser(symbolMap);
    parser.parse(symbolMapStr);

    return symbolMap;
  }

  private final String sourceServer;

  private IterableFastStringMap<JsSymbol> symbols;

  protected JsSymbolMap(String sourceServer) {
    this.sourceServer = (sourceServer.charAt(sourceServer.length() - 1) == '/')
        ? sourceServer : sourceServer + "/";
    this.symbols = new IterableFastStringMap<JsSymbol>();
  }

  public String getSourceServer() {
    return sourceServer;
  }

  public int getSymbolCount() {
    return symbols.size();
  }

  public JsSymbol lookup(String symbolName) {
    return symbols.get(symbolName);
  }

  public void put(String obfuscatedSymbolName, JsSymbol sourceSymbol) {
    symbols.put(obfuscatedSymbolName, sourceSymbol);
  }
}
