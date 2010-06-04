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
package com.google.speedtracer.client.util;

import com.google.gwt.core.client.JsArrayString;

/**
 * Utility class to handle comma separated values.
 */
public class Csv {
  // States used by split()
  static final int SPLIT_LOOKING_FOR_COMMA = 0;
  static final int SPLIT_IN_STRING = 1;
  static final int SPLIT_IN_ESCAPE = 2;

  /**
   * Split a comma separated value log line while ignoring escaped strings.
   */
  public static JsArrayString split(String csvString) {
    // Implemented as a little state machine
    int state;
    JsArrayString results = JsArrayString.createArray().cast();
    int index = 0;
    StringBuilder field = new StringBuilder();

    state = SPLIT_LOOKING_FOR_COMMA;
    while (index < csvString.length()) {
      char nextCharacter = csvString.charAt(index);
      switch (state) {
        case SPLIT_LOOKING_FOR_COMMA:
          switch (nextCharacter) {
            case ',':
              results.push(field.toString());
              field = new StringBuilder();
              break;
            case '"':
              state = SPLIT_IN_STRING;
              break;
            default:
              field.append(nextCharacter);
          }
          break;
        case SPLIT_IN_STRING:
          switch (nextCharacter) {
            case '"':
              state = SPLIT_LOOKING_FOR_COMMA;
              break;
            case '\\':
              state = SPLIT_IN_ESCAPE;
              field.append(nextCharacter);
              break;
            default:
              field.append(nextCharacter);
          }
          break;
        case SPLIT_IN_ESCAPE:
          state = SPLIT_IN_STRING;
          field.append(nextCharacter);
          break;
        default:
          field.append(nextCharacter);
      }
      index++;
    }

    // save the last field.
    results.push(field.toString());

    return results;
  }

  private Csv() {
    // do not instantiate
  }
}
