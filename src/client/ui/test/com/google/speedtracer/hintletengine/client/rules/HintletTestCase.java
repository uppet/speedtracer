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

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.coreext.client.JSOArray;
import com.google.speedtracer.client.model.EventRecord;
import com.google.speedtracer.client.model.HintRecord;

/**
 * One independent test case
 */
public final class HintletTestCase extends JavaScriptObject {

  protected HintletTestCase() {
  }

  public native String getDescription()/*-{
    return this.description || "";
  }-*/;

  public native JSOArray<EventRecord> getInputs()/*-{
    return this.inputs;
  }-*/;

  public native JSOArray<HintRecord> getExpectedHints()/*-{
    return this.expectedHints;
  }-*/;

  /**
   * Create a HintletTestCase with no expected hints
   */
  public static native HintletTestCase createTestCase(
      String description, JSOArray<EventRecord> inputs)/*-{
    return {
        "description" : description,
        "inputs" : inputs,
        "expectedHints" : []
    }
  }-*/;

  /**
   * Create a HintletTestCase with one expected hint
   */
  public static native HintletTestCase createTestCase(
      String description, JSOArray<EventRecord> inputs, HintRecord expectedHint)/*-{
    return {
        "description" : description,
        "inputs" : inputs,
        "expectedHints" : [
          expectedHint
        ]
    }
  }-*/;

  /**
   * Create a test case with an array of expected hints
   */
  public static native HintletTestCase createTestCase(
      String description, JSOArray<EventRecord> inputs, JSOArray<HintRecord> expectedHints)/*-{
    return {
        "description" : description,
        "inputs" : inputs,
        "expectedHints" : expectedHints
    }
  }-*/;

}
