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
import com.google.speedtracer.client.model.V8SymbolTable.V8Symbol;

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
    V8Symbol symbolOne = new V8Symbol("test1", 1, 0x100, 0x10);
    assertEquals("test1 : 0x100-0x110", symbolOne.toString());
    symbolTable.add(symbolOne);
    V8Symbol symbolTwo = new V8Symbol("test2", 1, 0x120, 0x5);
    assertEquals("test2 : 0x120-0x125", symbolTwo.toString());
    symbolTable.add(symbolTwo);
    V8Symbol symbolThree = new V8Symbol("test3", 1, 0x90, 0x5);
    assertEquals("test3 : 0x90-0x95", symbolThree.toString());
    symbolTable.add(symbolThree);
    
    assertEquals(symbolOne.toString(), symbolTable.lookup(0x100).toString());
    assertEquals(symbolOne.toString(), symbolTable.lookup(0x101).toString());
    assertEquals(symbolOne.toString(), symbolTable.lookup(0x110).toString());
    assertEquals(symbolTwo.toString(), symbolTable.lookup(0x120).toString());
    assertEquals(symbolTwo.toString(), symbolTable.lookup(0x121).toString());
    assertEquals(symbolTwo.toString(), symbolTable.lookup(0x125).toString());
    assertEquals(symbolThree.toString(), symbolTable.lookup(0x90).toString());
    assertEquals(symbolThree.toString(), symbolTable.lookup(0x94).toString());
    assertEquals(symbolThree.toString(), symbolTable.lookup(0x95).toString());

    // Make sure address lookups outside the range fail
    assertNull(symbolTable.lookup(0x50));
    assertNull(symbolTable.lookup(0x96));
    assertNull(symbolTable.lookup(0x99));
    assertNull(symbolTable.lookup(0x111));
    assertNull(symbolTable.lookup(0x119));
    assertNull(symbolTable.lookup(0x126));
    assertNull(symbolTable.lookup(0x1000));
  }

  @Override
  protected void gwtSetUp() throws Exception {
    Logging.createListenerLogger(null);
  }
}
