/*
 * Copyright 2011 Google Inc.
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

package com.google.speedtracer.hintletengine.client;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.coreext.client.JSON;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.speedtracer.client.model.EventRecord;
import com.google.speedtracer.client.model.HintRecord;
import com.google.speedtracer.hintletengine.client.rules.HintletFrequentLayout;
import com.google.speedtracer.hintletengine.client.rules.HintletLongDuration;
import com.google.speedtracer.hintletengine.client.rules.HintletRule;

import java.util.ArrayList;
import java.util.List;

/**
 * Tests {@link HintletEventRecordProcessor}.
 * 
 */
public class HintletEventRecordProcessorTests extends GWTTestCase {

  private static class OnHintCallback implements HintletOnHintListener {

    private List<JavaScriptObject> expectedHints = new ArrayList<JavaScriptObject>();

    // one event may trigger different type of rule.
    // use this to focus on current rule in test
    private String ruleToTest;

    private int expectedMatch = 0;
    private boolean mismatch = false;

    public OnHintCallback(String ruleToTest, List<JavaScriptObject> expectedHints) {
      this.ruleToTest = ruleToTest;
      this.expectedHints = expectedHints;
    }

    /**
     * Compare the incoming hint with the next expected hint
     */
    public void onHint(String hintletRule, double timestamp, String description, int refRecord,
        int severity) {
      // we do not check hint from different rule.
      if (!hintletRule.equals(ruleToTest)) {
        GWTTestCase.fail("Got hint from different rule");
      }

      if (expectedMatch >= expectedHints.size()) {
        mismatch = true; // more hints than expected
        GWTTestCase.fail("Got more hints than expected");
      }

      HintRecord expectedHint = expectedHints.get(expectedMatch).cast();
      if (expectedHint.getDescription().equals(description)
          && expectedHint.getTimestamp() == timestamp && expectedHint.getRefRecord() == refRecord
          && expectedHint.getSeverity() == severity) {
        expectedMatch++; // match
      } else {
        mismatch = true;
        GWTTestCase.fail("Expecting "
            + JSON.stringify(expectedHint)
            + ". But got "
            + JSON.stringify(HintRecord.create(hintletRule, timestamp, severity, description,
                refRecord)));
      }
    }

    /**
     * @return true if all expectedHints have been matched. no more, no less.
     *         return false otherwise
     */
    public boolean hintsMatched() {
      if (expectedMatch != expectedHints.size()) {
        return false;
      }

      return !mismatch;
    }

  }

  @Override
  public String getModuleName() {
    return "com.google.speedtracer.hintletengine.HintletEngineTest";
  }


  public void testFrequentLayout() {
    runTest(new HintletFrequentLayout(), HintletTestData.getFrequentLayoutInput(),
        HintletTestData.getFrequentLayoutOutput());
  }

  public void testLongDuration() {
    runTest(new HintletLongDuration(), HintletTestData.getLongDurationInput(),
        HintletTestData.getLongDurationOutput());
  }

  private void runTest(HintletRule rule, List<JavaScriptObject> input,
      List<JavaScriptObject> expectedOutput) {

    OnHintCallback hintCallback = new OnHintCallback(rule.getHintletName(), expectedOutput);
    rule.setOnHintCallback(hintCallback);
    HintletEventRecordProcessor evenRecordProcessor = new HintletEventRecordProcessor(rule);

    for (int i = 0; i < input.size(); i++) {
      EventRecord eventRecord = input.get(i).cast();
      eventRecord.setSequence(i + 1); // output expects sequence
      evenRecordProcessor.onEventRecord(eventRecord);
    }

    assertTrue("Hintlet rule \"" + rule.getHintletName() + "\" test failed.",
        hintCallback.hintsMatched());
  }
}
