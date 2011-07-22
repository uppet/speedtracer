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
import com.google.speedtracer.hintletengine.client.rules.HintletLongDuration;

/**
 * Tests {@link HintletLongDuration}.
 */
public class HinletLongDurationTests extends GWTTestCase {

  @Override
  public String getModuleName() {
    return "com.google.speedtracer.hintletengine.HintletEngineTest";
  }

  public void testNoHint() {
    HintletTestHelper.runTest(new HintletLongDuration(), getTestCaseNoHint());
  }

  public void testWarningHint() {
    HintletTestHelper.runTest(new HintletLongDuration(), getTestCaseWarningHint());
  }

  public void testCriticalHint() {
    HintletTestHelper.runTest(new HintletLongDuration(), getTestCaseCriticalHint());
  }

  private native static HintletTestCase getTestCaseNoHint()/*-{
    return {
        "inputs" : [
          {
              "duration" : 0.316162109375,
              "usedHeapSize" : 9427056,
              "sequence" : 1,
              "data" : {
                  "x" : 419,
                  "y" : 2,
                  "height" : 22,
                  "width" : 94
              },
              "time" : 1,
              "type" : @com.google.speedtracer.shared.EventRecordType::PAINT_EVENT,
              "children" : [

              ],
              "totalHeapSize" : 13131136
          }
        ],
        "expectedHints" : [],
        "description" : "Shouldn't trigger a rule - too short."
    };
  }-*/;

  private native static HintletTestCase getTestCaseWarningHint()/*-{
    return {
        "inputs" : [
          {
              "duration" : 500.9,
              "usedHeapSize" : 9427056,
              "sequence" : 2,
              "data" : {
                  "x" : 419,
                  "y" : 2,
                  "height" : 22,
                  "width" : 94
              },
              "time" : 2,
              "type" : @com.google.speedtracer.shared.EventRecordType::PAINT_EVENT,
              "children" : [

              ],
              "totalHeapSize" : 13131136
          }
        ],
        "expectedHints" : [
          {
              "timestamp" : 2,
              "hintletRule" : "Long Duration Events",
              "refRecord" : 2,
              "description" : "Event lasted: 500ms.  Exceeded threshold: 100ms",
              "severity" : 3
          }
        ],
        "description" : "Should trigger a rule w/ warning severity."
    };
  }-*/;

  private native static HintletTestCase getTestCaseCriticalHint()/*-{
    return {
        "inputs" : [
          {
              "data" : {
                  "x" : 419,
                  "y" : 2,
                  "width" : 94,
                  "height" : 22
              },
              "children" : [],
              "type" : @com.google.speedtracer.shared.EventRecordType::PAINT_EVENT,
              "usedHeapSize" : 9427056,
              "totalHeapSize" : 13131136,
              "duration" : 3000.2,
              "time" : 3,
              "sequence" : 3

          }
        ],

        "expectedHints" : [
          {
              "hintletRule" : "Long Duration Events",
              "timestamp" : 3,
              "description" : "Event lasted: 3000ms.  Exceeded threshold: 2000ms",
              "refRecord" : 3,
              "severity" : 2
          }
        ],
        "description" : "Should trigger a critical alarm."
    };
  }-*/;
}
