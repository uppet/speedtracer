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
package com.google.speedtracer.client;

import com.google.gwt.coreext.client.JSOArray;
import com.google.speedtracer.client.model.JsSymbol;
import com.google.speedtracer.client.model.JsSymbolMap;
import com.google.speedtracer.client.model.JsSymbolMap.JsSymbolMapParser;
import com.google.speedtracer.client.util.Url;

/**
 * Parses a GWT symbol map and initializes a {@link JsSymbolMap}.
 */
public class GwtSymbolMapParser implements JsSymbolMapParser {
  private final JsSymbolMap symbolMap;

  public GwtSymbolMapParser(JsSymbolMap symbolMap) {
    this.symbolMap = symbolMap;
  }

  public void parse(String symbolMapStr) {
    int start = 0;
    int end = symbolMapStr.indexOf('\n', start);
    while (end != -1) {
      processLine(symbolMapStr.substring(start, end));
      start = end + 1;
      end = symbolMapStr.indexOf('\n', start);
    }
  }

  private void processLine(String line) {
    if (line.charAt(0) == '#') {
      return;
    }

    JSOArray<String> symbolInfo = JSOArray.splitString(line, ",");
    String jsName = symbolInfo.get(0);
    String className = symbolInfo.get(2);
    String memberName = symbolInfo.get(3);
    String fileName = symbolInfo.get(4);
    String sourceLine = symbolInfo.get(5);

    // The path relative to the source server. We assume it is just the class
    // path base.
    String sourcePath = className.replace('.', '/');
    int lastSlashIndex = sourcePath.lastIndexOf("/") + 1;
    String sourcePathBase = sourcePath.substring(0, lastSlashIndex);

    // The sourceUri contains the actual file name.
    String sourceFileName = fileName.substring(fileName.lastIndexOf('/') + 1,
        fileName.length());

    String sourceSymbolName = className + "::" + memberName;

    JsSymbol sourceSymbol = new JsSymbol(new Url(sourcePathBase
        + sourceFileName), Integer.parseInt(sourceLine), sourceSymbolName,
        false, fileName);
    symbolMap.put(jsName, sourceSymbol);
  }
}
