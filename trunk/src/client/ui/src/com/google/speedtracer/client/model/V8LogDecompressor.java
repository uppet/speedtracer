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

import com.google.gwt.core.client.JsArrayString;
import com.google.speedtracer.client.util.Csv;

/**
 * Uses a window of the previous 'n' log entries to use in decompressing the v8
 * log when compression is enabled.
 */
public class V8LogDecompressor {
  private String window[];
  private int windowSize;
  private int lastWindow;

  public V8LogDecompressor(int windowSize) {
    this.windowSize = windowSize;
    window = new String[windowSize];
  }

  public String decompressLogEntry(String logLine) {
    String decompressedLogEntry = logLine;

    if (windowSize > 0) {

      /**
       * Compression will cause some lines to have # references in them.
       * 
       * Formatting string for back references to the whole line. E.g. "#2"
       * means "the second line above".
       * 
       * Formatting string for back references. E.g. "#2:10" means
       * "the second line above, start from char 10 (0-based)".
       */

      int compressionStart = logLine.indexOf('#');

      /**
       * logLine.indexOf('#') is too simple - it is tricked by # chars inside of
       * quoted strings. This follows the example of devtools, where they use
       * the knowledge that only RegExp entries have an embedded #, and those
       * lines always end with a quote character.
       */
      if (compressionStart != -1 && !logLine.endsWith("\"")) {
        String compressionString = logLine.substring(compressionStart + 1);
        int colonStart = compressionString.indexOf(':');
        int lineOffset = 0;
        int charOffset = 0;
        if (colonStart < 0) {
          lineOffset = Integer.parseInt(compressionString);
        } else {
          lineOffset = Integer.parseInt(compressionString.substring(0,
              colonStart));
          charOffset = Integer.parseInt(compressionString.substring(colonStart + 1));
        }
        assert charOffset >= 0;
        decompressedLogEntry = logLine.substring(0, compressionStart)
            + fetchLogBackref(lineOffset, charOffset);
      }
    }
    JsArrayString logEntry = Csv.split(decompressedLogEntry);
    if (logEntry.length() == 0) {

    } else {
      String command = logEntry.get(0);
      if (command.equals("profiler")) {
        // ignore
      } else if (command.equals("repeat") || command.equals("r")) {
        // skip the first 2 fields.
        int firstCommaOffset = decompressedLogEntry.indexOf(",");
        int secondCommaOffset = decompressedLogEntry.indexOf(",",
            firstCommaOffset + 1);
        appendLogEntry(decompressedLogEntry.substring(secondCommaOffset + 1));
      } else {
        appendLogEntry(decompressedLogEntry);
      }
    }
    return decompressedLogEntry;
  }

  private void appendLogEntry(String logEntry) {
    lastWindow = ++lastWindow % windowSize;
    window[lastWindow] = logEntry;
  }

  private String fetchLogBackref(int lineOffset, int charOffset) {
    assert charOffset >= 0;
    assert lineOffset >= 0;
    return this.getWindowBackref(lineOffset).substring(charOffset);
  }

  private String getWindowBackref(int index) {
    int arrayIndex = (lastWindow - (index - 1)) % windowSize;
    if (arrayIndex < 0) {
      arrayIndex = windowSize + arrayIndex;
    }
    assert (arrayIndex >= 0 && arrayIndex < window.length);
    return window[arrayIndex];
  }
}
