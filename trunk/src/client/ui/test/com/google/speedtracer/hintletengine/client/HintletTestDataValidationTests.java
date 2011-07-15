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
import com.google.gwt.coreext.client.JSON.JSONParseException;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.speedtracer.breaky.client.DumpValidator;
import com.google.speedtracer.breaky.client.JsonSchema.JsonSchemaResults;

/**
 * Validates the hintlet test data
 */
public class HintletTestDataValidationTests extends GWTTestCase {
  
  private DumpValidator validator;
  
  @Override
  public String getModuleName() {
    return "com.google.speedtracer.hintletengine.HintletEngine";
  }
  
  @Override
  public void gwtSetUp() {
    validator = new DumpValidator();
  }
  
  public void testLongDuration() {
    for(JavaScriptObject record:HintletTestData.getLongDurationInput()) {
      assertTrue("JSON object invalid: " + JSON.stringify(record), validateEntry(record));
    }
  } 
  
  /** 
   * Test that the tests are working.
   */
  public void testSimple() {
    assertTrue(true);
  }
  
  public void testTotalBytes() {
    for(JavaScriptObject record:HintletTestData.getTotalBytesInput()) {
      assertTrue("JSON object invalid: " + JSON.stringify(record), validateEntry(record));
    }
  }
  
  /**
   * Validate a JSON single record against the speedtracer schemas
   */
  private boolean validateEntry(JavaScriptObject record) {
    try {
      JsonSchemaResults results = validator.validate(record);
      assertTrue("Could not validate: \n"+JSON.stringify(record) + "\n" +
          results.getErrors().get(0).getProperty() + " " + 
            results.getErrors().get(0).getMessage(),
          results.isValid());
      
      return results.isValid();
    } catch (JSONParseException e) {
      fail("Got an exception trying to JSON parse the record: " + e.getMessage());
      return false;
    } 
  }
}
