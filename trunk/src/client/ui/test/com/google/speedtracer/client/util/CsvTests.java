/*
 * Copyright 2010 Google Inc.
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
package com.google.speedtracer.client.util;

import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.junit.client.GWTTestCase;

/**
 * Tests the CSV Utility class.
 */
public class CsvTests extends GWTTestCase {

  @Override
  public String getModuleName() {
    return "com.google.speedtracer.Common";
  }

  public void testCsvSplit() {
    JsArrayString results;
    results = Csv.split("1,2,3");
    assertEquals(3, results.length());
    assertEquals("1", results.get(0));
    assertEquals("2", results.get(1));
    assertEquals("3", results.get(2));

    results = Csv.split("1,\"2\",3");
    assertEquals(3, results.length());
    assertEquals("1", results.get(0));
    assertEquals("2", results.get(1));
    assertEquals("3", results.get(2));

    results = Csv.split("1,\"\\\"foo\\\"\",3");
    assertEquals(3, results.length());
    assertEquals("1", results.get(0));
    assertEquals("\\\"foo\\\"", results.get(1));
    assertEquals("3", results.get(2));

    results = Csv.split("1,\"2,2\",3");
    assertEquals(3, results.length());
    assertEquals("1", results.get(0));
    assertEquals("2,2", results.get(1));
    assertEquals("3", results.get(2));

    results = Csv.split("1,,3");
    assertEquals(3, results.length());
    assertEquals("1", results.get(0));
    assertEquals("", results.get(1));
    assertEquals("3", results.get(2));

    results = Csv.split("1,2,");
    assertEquals(3, results.length());
    assertEquals("1", results.get(0));
    assertEquals("2", results.get(1));
    assertEquals("", results.get(2));

    results = Csv.split("1,,\"\"");
    assertEquals(3, results.length());
    assertEquals("1", results.get(0));
    assertEquals("", results.get(1));
    assertEquals("", results.get(2));
    
    results = Csv.split("1,\"\",");
    assertEquals(3, results.length());
    assertEquals("1", results.get(0));
    assertEquals("", results.get(1));
    assertEquals("", results.get(2));
    
    results = Csv.split("code-creation,RegExp,3f830f,566,\"[^+&gt;] [^+&gt;]\"");
    assertEquals(5, results.length());
    assertEquals("code-creation", results.get(0));
    assertEquals("RegExp", results.get(1));
    assertEquals("3f830f", results.get(2));
    assertEquals("566", results.get(3));
    assertEquals("[^+&gt;] [^+&gt;]", results.get(4));
    
    results = Csv.split("code-creation,RegExp,0x1906720,566,\"[\\\\\\\\\\\\\"\\\\x00-\\\\x1f\\\\x80-\\\\uffff]\"");
    assertEquals(5, results.length());    
    
  }
}
