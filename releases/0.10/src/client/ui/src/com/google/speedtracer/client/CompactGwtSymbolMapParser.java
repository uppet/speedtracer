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

import com.google.speedtracer.client.model.JsSymbol;
import com.google.speedtracer.client.model.JsSymbolMap;
import com.google.speedtracer.client.model.JsSymbolMap.JsSymbolMapParser;
import com.google.speedtracer.client.util.IterableFastStringMap;
import com.google.speedtracer.client.util.JSOArray;

/**
 * Parses a compressed form of the Gwt Symbol map.
 */
public class CompactGwtSymbolMapParser implements JsSymbolMapParser {

  private final IterableFastStringMap<String> packageMap;

  private final JsSymbolMap symbolMap;

  public CompactGwtSymbolMapParser(JsSymbolMap symbolMap) {
    this.symbolMap = symbolMap;
    this.packageMap = new IterableFastStringMap<String>();
  }

  /**
   * File begins with a header denoted by lines starting with # marks.
   * 
   * Lines starting with %% define a package string literal with a short key.
   * 
   * A line in the symbolMap is a comma delimited list of: jsName,
   * jsniSymbolType (blank for class, 'm' for method), package key, className,
   * memberName, fileName (without .java), sourceLine
   * 
   * If the FileName and the ClassName are the same, the fileName can be
   * replaced by %.
   */
  public void parse(String symbolMapStr) {    
    int start = 0;
    int end = symbolMapStr.indexOf('\n', start);
    while (end != -1) {
      processLine(symbolMapStr.substring(start, end));
      start = end + 1;
      end = symbolMapStr.indexOf('\n', start);
    }
  }

  private void definePackage(JSOArray<String> symbolInfo) {
    packageMap.put(symbolInfo.get(2), symbolInfo.get(1));
  }

  private void processLine(String line) {
    if (line.charAt(0) == '#') {
      return;
    }
    
    JSOArray<String> symbolInfo = JSOArray.splitString(line, ",");
    if ("%%".equals(symbolInfo.get(0))) {
      definePackage(symbolInfo);
    } else {
      processSymbol(symbolInfo);
    }
  }

  private void processSymbol(JSOArray<String> symbolInfo) {
    String jsName = symbolInfo.get(0);
    String packageKey = symbolInfo.get(2);
    String className = symbolInfo.get(3);
    String memberName = symbolInfo.get(4);
    String fileName = symbolInfo.get(5);
    String sourceLine = symbolInfo.get(6);

    if ("%".equals(fileName)) {
      fileName = className;
    }

    String packageName = packageMap.get(packageKey);
    
    assert (packageName != null);
    
    packageName = "".equals(packageName) ? "" : packageName + ".";
    fileName = "".equals(fileName) ? "Unknown" : fileName + ".java";

    // The path relative to the source server. We assume it is just the class
    // path base.
    String sourcePathBase = packageName.replace(".", "/");
    String sourceSymbolName = packageName + className + "::" + memberName;

    JsSymbol sourceSymbol = new JsSymbol(sourcePathBase, fileName,
        Integer.parseInt(sourceLine), sourceSymbolName);
    symbolMap.put(jsName, sourceSymbol);
  }
}
