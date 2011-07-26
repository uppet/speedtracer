/*
 * Copyright 2011 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.speedtracer.hintletengine.client.rules;

import com.google.gwt.coreext.client.JSOArray;
import com.google.gwt.coreext.client.JSON;
import com.google.gwt.coreext.client.JSON.JSONParseException;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.speedtracer.breaky.client.DumpValidator;
import com.google.speedtracer.breaky.client.JsonSchema.JsonSchemaResults;
import com.google.speedtracer.client.model.EventRecord;
import com.google.speedtracer.client.model.HintRecord;
import com.google.speedtracer.hintletengine.client.HintletEventRecordProcessor;
import com.google.speedtracer.hintletengine.client.HintletOnHintListener;
import com.google.speedtracer.hintletengine.client.rules.HintletRule;

import java.util.ArrayList;
import java.util.List;

/**
 * Helper for running hintlet test case. Validate input event using speedtracer JSON schema first.
 * Match hints fired by hintlet rule against expected hints.
 */
public class HintletTestHelper {

  private static DumpValidator validator;

  private HintletTestHelper() {

  }

  private static class OnHintCallback implements HintletOnHintListener {

    private List<HintRecord> savedHints = new ArrayList<HintRecord>();

    public OnHintCallback() {
    }

    /**
     * Save the incoming hint
     */
    public void onHint(String hintletRule, double timestamp, String description, int refRecord,
        int severity) {
      savedHints.add(HintRecord.create(hintletRule, timestamp, severity, description, refRecord));
    }

    public List<HintRecord> getHints() {
      return savedHints;
    }

  }

  public static void runTest(HintletRule rule, HintletTestCase testCase) {

    OnHintCallback hintCallback = new OnHintCallback();
    rule.setOnHintCallback(hintCallback);
    HintletEventRecordProcessor eventRecordProcessor = new HintletEventRecordProcessor(rule);

    validator = new DumpValidator();

    JSOArray<EventRecord> inputs = testCase.getInputs();
    for (int i = 0; i < inputs.size(); i++) {
      EventRecord event = inputs.get(i);
      // first validate using SpeedTracer JSON Schema
      validateEventRecordFormat(event);
      // then feed to even record processor
      eventRecordProcessor.onEventRecord(event);
    }

    matchHints(testCase.getExpectedHints(), hintCallback.getHints());
  }

  private static void matchHints(JSOArray<HintRecord> expectedHints, List<HintRecord> hints) {

    if ( expectedHints == null && hints.size() != 0) {
      GWTTestCase.fail("Expecting zero hints. But got " + hints.size() + " hints." + "\nHints:\n"
          + stringify(hints));
    }

    if (expectedHints.size() != hints.size()) {
      GWTTestCase.fail("Expecting " + expectedHints.size() + " hints. But got " + hints.size()
          + " hints." + "\nHints:\n" + stringify(hints) + "Expected Hints:\n"
          + stringify(expectedHints));
    }

    for (int i = 0; i < expectedHints.size(); i++) {
      if (!hintsMatch(expectedHints.get(i), hints.get(i))) {
        GWTTestCase.fail("Expecting \n   " + JSON.stringify(expectedHints.get(i)) + "\nBut got \n   "
            + JSON.stringify(hints.get(i)));
      }
    }
  }

  private static String stringify(JSOArray<HintRecord> expectedHints) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < expectedHints.size(); i++) {
      sb.append("   " + ( i + 1) + ": ");
      sb.append(JSON.stringify(expectedHints.get(i)));
      sb.append("\n");
    }
    return sb.toString();
  }

  private static String stringify(List<HintRecord> hints) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < hints.size(); i++) {
      sb.append("   " + ( i + 1) + ": ");
      sb.append(JSON.stringify(hints.get(i)));
      sb.append("\n");
    }
    return sb.toString();
  }

  private static void validateEventRecordFormat(EventRecord event) {
    try {
      JsonSchemaResults results = validator.validate(event);
      if(!results.isValid()) {
        GWTTestCase.fail("Could not validate: \n" + JSON.stringify(event) + "\n"
              + results.getErrors().get(0).getProperty() + " "
              + results.getErrors().get(0).getMessage());
      }
    } catch (JSONParseException e) {
      GWTTestCase.fail("Got an exception trying to JSON parse the record: " + e.getMessage());
    }
  }

  private static boolean hintsMatch(HintRecord hint1, HintRecord hint2) {
    return (hint1.getDescription().equals(hint2.getDescription())
        && hint1.getTimestamp() == hint2.getTimestamp() && hint1.getSeverity() == hint2
        .getSeverity());
  }
}
