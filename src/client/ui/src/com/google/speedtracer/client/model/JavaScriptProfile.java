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

import com.google.gwt.core.client.JsArrayNumber;
import com.google.speedtracer.client.util.TimeStampFormatter;

/**
 * This class stores a profile for a single top level event. It references into
 * data stored in the {@link JavaScriptProfileModel}.
 */
public class JavaScriptProfile {
  public static final int PROFILE_TYPE_FLAT = 0;
  public static final int PROFILE_TYPE_BOTTOM_UP = 1;
  public static final int PROFILE_TYPE_TOP_DOWN = 2;

  public static final int STATE_JS = 0;
  public static final int STATE_GC = 1;
  public static final int STATE_COMPILER = 2;
  public static final int STATE_OTHER = 3;
  public static final int STATE_EXTERNAL = 4;
  public static final int STATE_UNKNOWN = 5;
  public static final int NUM_STATES = 6;

  public static String stateToString(int state) {
    switch (state) {
      case JavaScriptProfile.STATE_COMPILER:
        return "Compiler";
      case JavaScriptProfile.STATE_JS:
        return "JavaScript";
      case JavaScriptProfile.STATE_GC:
        return "Garbage Collection";
      case JavaScriptProfile.STATE_EXTERNAL:
        return "External";
      case JavaScriptProfile.STATE_OTHER:
        return "Other";
      case JavaScriptProfile.STATE_UNKNOWN:
      default:
        return "Unknown";
    }
  }

  private JavaScriptProfileNode profiles[] = new JavaScriptProfileNode[3];

  private final JsArrayNumber stateTimes = JsArrayNumber.createArray().cast();

  public JavaScriptProfile() {
    for (int i = 0; i < NUM_STATES; ++i) {
      stateTimes.push(0.0);
    }
  }

  public void addStateTime(int stateIndex, double msecs) {
    Double found = stateTimes.get(stateIndex);
    stateTimes.set(stateIndex, found + msecs);
  }

  public JavaScriptProfileNode getProfile(int profileType) {
    return profiles[profileType];
  }

  public double getStateTime(int stateIndex) {
    return stateTimes.get(stateIndex);
  }

  public double getTotalTime() {
    if (profiles[PROFILE_TYPE_FLAT] == null) {
      return 0.0;
    }

    // The total time is stored in the root node of the profile.
    return profiles[PROFILE_TYPE_FLAT].getTime();
  }

  /**
   * Return the profile for the specified event in a simple HTML representation.
   * 
   */
  public void getVmStateHtml(StringBuilder result) {
    // Give a table of the time spent in each VM state
    result.append("<table>");
    double total = getTotalTime();
    for (int index = 0; index < JavaScriptProfile.NUM_STATES; ++index) {
      double value = getStateTime(index);
      if (value > 0.0) {
        String percentage = TimeStampFormatter.formatToFixedDecimalPoint(
            (value / total) * 100.0, 1);
        result.append("<tr><td>" + JavaScriptProfile.stateToString(index)
            + "</td><td>" + (int) value + "</td><td>ticks</td><td>"
            + percentage + "%</td></tr>");
      }
    }
    result.append("</table>");
  }

  JavaScriptProfileNode getOrCreateProfile(int profileType) {
    if (profiles[profileType] == null) {
      profiles[profileType] = new JavaScriptProfileNode("(root)");
    }
    return profiles[profileType];
  }
}
