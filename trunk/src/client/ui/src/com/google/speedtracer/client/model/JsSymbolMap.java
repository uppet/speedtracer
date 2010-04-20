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

import com.google.speedtracer.client.ClientConfig;
import com.google.speedtracer.client.CompactGwtSymbolMapParser;
import com.google.speedtracer.client.GwtSymbolMapParser;
import com.google.speedtracer.client.Logging;
import com.google.speedtracer.client.util.IterableFastStringMap;

/**
 * Class used for re-symbolizing obfuscated JavaScript. Provides a simple
 * mapping back to original source and line number.
 * 
 * TODO (jaimeyap): Move to a symbol map format that supports character by
 * character mapping.
 */
public class JsSymbolMap {
  /**
   * Symbol map parsers implement this interface.
   */
  public interface JsSymbolMapParser {
    void parse(String symbolMapStr);
  }

  public static final String COMPACT_GWT_SYMBOL_MAP = "compactGwt";

  public static final String GWT_SYMBOL_MAP = "gwt";

  public static final String UNKNOWN_RESOURCE_PATH = "Unknown";

  /**
   * Parses the JavaScript symbol map and initializes a {@link JsSymbolMap} with
   * the corresponding symbols and their source mappings.
   * 
   * @param sourceServer The server that will serve original source files
   *          referenced in the symbol map.
   * @param sourceViewerServer Server that implements the source viewer
   *          protocol. Currently only used for Jump-To-IDE functionality.
   * @param type The type of symbol map we want to parse.
   * @param symbolMapStr The unprocessed text corresponding to the Symbol Map.
   * @return
   */
  public static JsSymbolMap parse(String sourceServer,
      String sourceViewerServer, String type, String symbolMapStr) {
    JsSymbolMap symbolMap = new JsSymbolMap(sourceServer, sourceViewerServer);
    JsSymbolMapParser parser = null;
    if (GWT_SYMBOL_MAP.equals(type)) {
      parser = new GwtSymbolMapParser(symbolMap);
    } else if (COMPACT_GWT_SYMBOL_MAP.equals(type)) {
      parser = new CompactGwtSymbolMapParser(symbolMap);
    } else {
      if (ClientConfig.isDebugMode()) {
        Logging.getLogger().logText(
            "Ignoring unknown symbol map type: " + type + " for symbol map "
                + symbolMapStr + ".");
      }
    }

    if (parser != null) {
      parser.parse(symbolMapStr);
    }

    return symbolMap;
  }

  private final String sourceServer;

  // get API for supporting jump to IDE.
  private final String sourceViewerServer;

  private IterableFastStringMap<JsSymbol> symbols;

  protected JsSymbolMap(String sourceServer, String sourceViewerServer) {
    sourceServer = (null == sourceServer) ? "" : sourceServer;
    this.sourceServer = (sourceServer.charAt(sourceServer.length() - 1) == '/')
        ? sourceServer : sourceServer + "/";
    this.symbols = new IterableFastStringMap<JsSymbol>();
    this.sourceViewerServer = sourceViewerServer;
  }

  public String getSourceServer() {
    return sourceServer;
  }

  public String getSourceViewerServer() {
    return sourceViewerServer;
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
