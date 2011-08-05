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

import com.google.gwt.junit.client.GWTTestCase;
import com.google.speedtracer.client.model.HintRecord;
import com.google.speedtracer.hintletengine.client.HintletEventRecordBuilder;
import com.google.speedtracer.hintletengine.client.rules.HintletLongDuration;
import static com.google.speedtracer.hintletengine.client.HintletEventRecordBuilder.createPaintEvent;

/**
 * Tests {@link HintletLongDuration}.
 */
public class HintletLongDurationTests extends GWTTestCase {
  
  private HintletRule rule;
  private HintletTestCase test;

  @Override
  protected void gwtSetUp() {
    test = HintletTestCase.getHintletTestCase();
    rule = new HintletLongDuration();
  }
  
  @Override
  public String getModuleName() {
    return "com.google.speedtracer.hintletengine.HintletEngineTest";
  }

  /**
   * Shouldn't trigger a rule - too short.
   */
  public void testShortEvent() {
    test.addInput(createPaintEvent(.3));
    HintletTestHelper.runTest(rule, test);
  }

  /**
   * Should trigger a rule w/ warning severity.
   */
  public void testWarningHint() {
    test.addInput(createPaintEvent(500.9));
    
    test.addExpectedHint(
        HintRecord.create(rule.getHintletName(), HintletEventRecordBuilder.DEFAULT_TIME,
            HintRecord.SEVERITY_INFO, "Event lasted: 500ms.  Exceeded threshold: 100ms",
            HintletEventRecordBuilder.DEFAULT_SEQUENCE));

    HintletTestHelper.runTest(rule, test);
  }

  /**
   * Should trigger a warning alarm.
   */
  public void testCriticalHint() {
    test.addInput(createPaintEvent(3000.2));
    
    test.addExpectedHint(
        HintRecord.create(rule.getHintletName(), HintletEventRecordBuilder.DEFAULT_TIME,
            HintRecord.SEVERITY_WARNING, "Event lasted: 3000ms.  Exceeded threshold: 2000ms",
            HintletEventRecordBuilder.DEFAULT_SEQUENCE));
    
    HintletTestHelper.runTest(rule, test);
  }
}
