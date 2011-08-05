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

  public native HintletTestCase setDescription(String description) /*-{
    this.description = description;
    return this;
  }-*/;

  public native JSOArray<EventRecord> getInputs()/*-{
    return this.inputs;
  }-*/;

  public native HintletTestCase setInputs(JSOArray<EventRecord> inputs) /*-{
    this.inputs = inputs;
    return this;
  }-*/;

  public native HintletTestCase addInput(EventRecord input) /*-{
    this.inputs.push(input);
    return this;
  }-*/;

  public native JSOArray<HintRecord> getExpectedHints()/*-{
    return this.expectedHints;
  }-*/;

  public native HintletTestCase setExpectedHints(JSOArray<HintRecord> expectedHints) /*-{
    this.expectedHints = expectedHints;
    return this;
  }-*/;

  public native HintletTestCase addExpectedHint(HintRecord expectedHint) /*-{
    this.expectedHints.push(expectedHint);
    return this;
  }-*/;

  public static native HintletTestCase getHintletTestCase() /*-{
    return {
      "description" : "",
      "inputs" : [],
      "expectedHints" : []
    }
  }-*/;

}
