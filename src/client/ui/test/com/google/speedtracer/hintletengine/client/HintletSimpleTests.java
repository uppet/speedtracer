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

import com.google.gwt.junit.client.GWTTestCase;
import com.google.speedtracer.hintletengine.client.rules.HintletFrequentLayout;
import com.google.speedtracer.hintletengine.client.rules.HintletLongDuration;
import com.google.speedtracer.hintletengine.client.rules.HintletTotalBytes;

/**
 * Class for testing hintlets that are simple to test
 * using the HintletTestHelper
 */
public class HintletSimpleTests extends GWTTestCase {

  private HintletTestHelper testAssistant;
  
  @Override
  public String getModuleName() {
    return "com.google.speedtracer.hintletengine.HintletEngineTest";
  }
  
  @Override
  protected void gwtSetUp() {
    testAssistant = new HintletTestHelper();
  }
  
  public void testFrequentLayout() {
    testAssistant.runTest(
        new HintletFrequentLayout(), 
        HintletTestData.getFrequentLayoutInput(),
        HintletTestData.getFrequentLayoutOutput()
      );
  }

  public void testLongDuration() {    
    testAssistant.runTest(
        new HintletLongDuration(), 
        HintletTestData.getLongDurationInput(),
        HintletTestData.getLongDurationOutput()
      );
  }
  
  public void testTotalBytes() {
    testAssistant.runTest(
        new HintletTotalBytes(), 
        HintletTestData.getTotalBytesInput(),
        HintletTestData.getTotalBytesOutput()
      );
  }
}
