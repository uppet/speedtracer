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

import com.google.gwt.core.client.JsArrayString;
import com.google.speedtracer.client.model.JsSymbol;
import com.google.speedtracer.client.util.Csv;
import com.google.speedtracer.client.util.JSOArray;

import java.util.ArrayList;
import java.util.List;

/**
 * Simple wrapper around the String backTrace's we pull out of chrome.
 */
public class JsStackTrace {
  /**
   * Simple typed wrapper for components of a StackFrame as spit out by they
   * JavaScript VM. It is essentially just a {@link JsSymbol} plus a column
   * offset.
   * 
   * TODO (jaimeyap): If we support a SymbolMap format that does character by
   * character mappings, then we can just use that and not need a separate
   * JsStackFrame.
   */
  public class JsStackFrame extends JsSymbol {
    private final int colNumber;

    private JsStackFrame(String resourceUrlBase, String resourceName,
        int lineNumber, int colNumber, String symbolName) {
      super(resourceUrlBase, resourceName, lineNumber, symbolName);
      this.colNumber = colNumber;
    }

    public int getColNumber() {
      return colNumber;
    }

    public String getResourceUrl() {
      // The base for a JsStackFrame is interpreted as the URL base.
      // This should already have a trailing slash.
      return getResourceBase() + getResourceName();
    }
  }

  public static JsStackTrace create(String backTrace) {
    JsStackTrace stackTrace = new JsStackTrace();
    stackTrace.init(backTrace);
    return stackTrace;
  }

  private final List<JsStackFrame> frames;

  private JsStackTrace() {
    frames = new ArrayList<JsStackFrame>();
  }

  public List<JsStackFrame> getFrames() {
    return frames;
  }

  protected void init(String backTrace) {
    JSOArray<String> frameStrings = JSOArray.splitString(backTrace, ";");
    for (int i = 0, n = frameStrings.size(); i < n; i++) {
      // The second field is quoted, so a simple split() won't work.
      JsArrayString stackFrame = Csv.split(frameStrings.get(i));

      String resourceUrl = stackFrame.get(1);
      int resourceBaseEnd = resourceUrl.lastIndexOf("/") + 1;
      String resourceUrlBase = resourceUrl.substring(0, resourceBaseEnd);
      // Note that resourceName can be the empty string for main resources (like
      // http://www.google.com/).
      String resourceName = resourceUrl.substring(resourceBaseEnd,
          resourceUrl.length());

      // We convert lineNumber and colNumber to a 1 based index.
      final int lineNumber = Integer.parseInt(stackFrame.get(2)) + 1;
      final int colNumber = Integer.parseInt(stackFrame.get(3)) + 1;

      // We get a funcName and an inferredName.
      // Prefer the last argument
      String symbolName = stackFrame.get(5);
      // but if it isnt there, try the 4th.
      symbolName = (symbolName.equals("")) ? stackFrame.get(4) : symbolName;

      this.frames.add(new JsStackFrame(resourceUrlBase, resourceName,
          lineNumber, colNumber, symbolName));
    }
  }
}
