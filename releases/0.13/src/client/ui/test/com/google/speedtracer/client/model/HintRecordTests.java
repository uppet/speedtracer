/*
 * Copyright 2009 Google Inc.
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

import com.google.gwt.junit.client.GWTTestCase;
import com.google.speedtracer.client.util.JSOArray;

/**
 * Exercises the HintRecord class. 
 */
public class HintRecordTests extends GWTTestCase {
  @Override
  public String getModuleName() {
    return "com.google.speedtracer.Common";
  }

  public void testCreateHintRecord() {
    HintRecord hint = HintRecord.create("testRuleName", 123.4,
        HintRecord.SEVERITY_WARNING, "testDescription", 456);

    assertEquals("getHintletRuleRule()", hint.getHintletRule(), "testRuleName");
    assertEquals("getTimestamp()", hint.getTimestamp(), 123.4, .1);
    assertEquals("getSeverity()", hint.getSeverity(),
        HintRecord.SEVERITY_WARNING);
    assertEquals("getDescription()", hint.getDescription(), "testDescription");
    assertEquals("getRefRecord()", hint.getRefRecord(), 456);
  }

  public void testMostSevere() {
    HintRecord hint1 = HintRecord.create("testRuleName", 123.6,
        HintRecord.SEVERITY_INFO, "testDescription3", 456);
    HintRecord hint2 = HintRecord.create("testRuleName", 123.4,
        HintRecord.SEVERITY_WARNING, "testDescription1", 456);
    HintRecord hint3 = HintRecord.create("testRuleName", 123.5,
        HintRecord.SEVERITY_CRITICAL, "testDescription2", 456);

    JSOArray<HintRecord> hintRecords = JSOArray.createArray().cast();
    hintRecords.push(hint1);
    
    int mostSevere = HintRecord.mostSevere(hintRecords);
    assertEquals("mostSevere 1", mostSevere, HintRecord.SEVERITY_INFO);
    
    hintRecords.push(hint2);
    mostSevere = HintRecord.mostSevere(hintRecords);
    assertEquals("mostSevere 2", mostSevere, HintRecord.SEVERITY_WARNING);
    hintRecords.push(hint3);
    mostSevere = HintRecord.mostSevere(hintRecords);
    assertEquals("mostSevere 3", mostSevere, HintRecord.SEVERITY_CRITICAL);        
  }
}
