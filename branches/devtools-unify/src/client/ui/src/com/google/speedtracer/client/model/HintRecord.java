/*
 * Copyright 2008 Google Inc.
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

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.coreext.client.JSOArray;

/**
 * Overlay for Hintlet payload. See the hintlet.addHint() function in
 * hintlet_main.js for a list of the fields populated in this object.
 */
public class HintRecord extends JavaScriptObject {
  public static final int SEVERITY_CRITICAL = 1;
  public static final int SEVERITY_INFO = 3;
  public static final int SEVERITY_VALIDATION = 0;
  public static final int SEVERITY_WARNING = 2;
  
  public static native HintRecord create(String hintletRule, double timestamp,
      int severity, String description, int refRecord) /*-{
    return {description: description, hintletRule: hintletRule, 
      refRecord: refRecord, severity: severity, timestamp: timestamp};
  }-*/;

  /**
   * Returns the most severe severity value found the list.
   * 
   * @param hintRecords a list of Hintlet records to search.
   * @return the most severe severity value found in the list.
   */
  public static int mostSevere(JSOArray<HintRecord> hintRecords) {
    int numRecs = hintRecords.size();
    int maxSeverity = HintRecord.SEVERITY_INFO;
    for (int i = 0; i < numRecs; i++) {
      int severity = hintRecords.get(i).getSeverity();
      if (severity < maxSeverity) {
        maxSeverity = severity;
      }
    }
    return maxSeverity;
  }

  /**
   * @param severity
   * @return A string representation of the severity level
   */
  public static String severityToString(int severity) {
    switch (severity) {
      case HintRecord.SEVERITY_VALIDATION:
        return "Validation";
      case HintRecord.SEVERITY_CRITICAL:
        return "Critical";
      case HintRecord.SEVERITY_WARNING:
        return "Warning";
      case HintRecord.SEVERITY_INFO:
        return "Info";
      default:
        // Unknown severity!
        assert false : "encountered unknown severity " + severity;
        return "Unknown Severity!";
    }
  }

  protected HintRecord() {
  }

  public final String asString() {
    return "Rule: " + getHintletRule() + " Description: " + getDescription();
  }

  /**
   * Returns a human readable description of why this rule was triggered.
   * 
   * @return Human readable description of why this rule was triggered.
   */
  public final native String getDescription() /*-{
    return this.description;
  }-*/;

  /**
   * Returns the name of the hintlet rule.
   * 
   * @return The name of the hintlet rule.
   */
  public final native String getHintletRule() /*-{
    return this.hintletRule;
  }-*/;

  /**
   * Returns the sequence number of the record that triggered this hintlet
   * record.
   * 
   * @return the sequence number of the record that triggered this hintlet
   *         record. Returns -1 if the reference record field is not specified.
   */
  public final native int getRefRecord() /*-{
    return this.refRecord >= 0  ? this.refRecord : -1;
  }-*/;

  /**
   * Indicates the severity of the problem.
   * 
   * @return one of the HintletRecord.SEVERITY_ constant values
   */
  public final native int getSeverity() /*-{
    return this.severity;
  }-*/;

  /**
   * Returns a time value associated with the record (in milliseconds, relative
   * to the start of the recording).
   * 
   * @return a time value associated with the record (in milliseconds, relative
   *         to the start of the recording)
   */
  public final native double getTimestamp() /*-{
    return this.timestamp;
  }-*/;

}
