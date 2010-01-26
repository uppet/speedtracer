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
package com.google.speedtracer.client.model;

import com.google.gwt.junit.client.GWTTestCase;
import com.google.speedtracer.client.Logging;
import com.google.speedtracer.client.model.V8SymbolTable.Symbol;

/**
 * Tests for parsing profile data from the v8 JavaScript engine.
 */
public class V8SymbolTableTests extends GWTTestCase {

  @Override
  public String getModuleName() {
    return "com.google.speedtracer.Common";
  }

  public void testSymbolTable() {
    V8SymbolTable symbolTable = new V8SymbolTable();
    Symbol symbolOne = new Symbol("test1", 1, 100, 10);
    assertEquals("test1 : 100-110", symbolOne.toString());
    symbolTable.add(symbolOne);
    Symbol symbolTwo = new Symbol("test2", 1, 120, 5);
    assertEquals("test2 : 120-125", symbolTwo.toString());
    symbolTable.add(symbolTwo);
    Symbol symbolThree = new Symbol("test3", 1, 90, 5);
    assertEquals("test3 : 90-95", symbolThree.toString());
    symbolTable.add(symbolThree);
    
    assertEquals(symbolOne.toString(), symbolTable.lookup(100).toString());
    assertEquals(symbolOne.toString(), symbolTable.lookup(101).toString());
    assertEquals(symbolOne.toString(), symbolTable.lookup(110).toString());
    assertEquals(symbolTwo.toString(), symbolTable.lookup(120).toString());
    assertEquals(symbolTwo.toString(), symbolTable.lookup(121).toString());
    assertEquals(symbolTwo.toString(), symbolTable.lookup(125).toString());
    assertEquals(symbolThree.toString(), symbolTable.lookup(90).toString());
    assertEquals(symbolThree.toString(), symbolTable.lookup(94).toString());
    assertEquals(symbolThree.toString(), symbolTable.lookup(95).toString());

    // Make sure address lookups outside the range fail
    assertNull(symbolTable.lookup(50));
    assertNull(symbolTable.lookup(96));
    assertNull(symbolTable.lookup(99));
    assertNull(symbolTable.lookup(111));
    assertNull(symbolTable.lookup(119));
    assertNull(symbolTable.lookup(126));
    assertNull(symbolTable.lookup(1000));
  }

  @Override
  protected void gwtSetUp() throws Exception {
    Logging.createListenerLogger(null);
  }
}
