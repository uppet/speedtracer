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
package com.google.speedtracer.client.timeline;

import com.google.gwt.junit.client.GWTTestCase;
import com.google.speedtracer.client.timeline.HighlightModel.HighlightEntry;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Tests the HighlightModel class.
 */
public class HighlightModelTests extends GWTTestCase {

  @Override
  public String getModuleName() {
    return "com.google.speedtracer.Common";
  }

  public void testHighlightModelClear() {
    HighlightModel model = HighlightModel.create();
    model.addData(1.0, 1);
    model.addData(20.0, 20);
    model.addData(10.0, 10);
    model.addData(21.0, 21);
    model.addData(21.0, 22);
    assertEquals("size", 4, model.size());
    model.clear();
    assertEquals("size", 0, model.size());
    // should return an empty iterator
    Iterator<HighlightEntry> it = model.getRangeValues(0.0, 100.0, .5);
    assertFalse(it.hasNext());
  }

  public void testHighlightModelSizeAndMaxX() {
    HighlightModel model = HighlightModel.create();
    assertEquals("size", 0, model.size());
    try {
      model.getMaxX();
      fail("Expected an exception for empty model");
    } catch (NoSuchElementException ex) {
      // expected exception
    }
    model.addData(1.0, 1);
    assertEquals("getMaxX", 1.0, model.getMaxX());
    assertEquals("size", 1, model.size());
    model.addData(20.0, 20);
    assertEquals("getMaxX", 20.0, model.getMaxX());
    assertEquals("size", 2, model.size());
    model.addData(10.0, 10);
    assertEquals("getMaxX", 20.0, model.getMaxX());
    assertEquals("size", 3, model.size());
    model.addData(21.0, 21);
    assertEquals("getMaxX", 21.0, model.getMaxX());
    assertEquals("size", 4, model.size());
    model.addData(21.0, 22);
    assertEquals("getMaxX", 21.0, model.getMaxX());
    assertEquals("size", 4, model.size());
  }

  public void testHightlightModelGetRangeValues1() {
    HighlightModel model = HighlightModel.create();
    model.addData(1.0, 1);
    model.addData(20.0, 20);

    Iterator<HighlightEntry> it = model.getRangeValues(0.0, 100.0, .5);
    assertTrue(it.hasNext());
    HighlightEntry entry = it.next();
    assertEquals("first entry", 1.0, entry.getKey());
    assertEquals("first entry", 1, entry.getValue().intValue());
    assertTrue(it.hasNext());
    entry = it.next();
    assertEquals("second entry", 20.0, entry.getKey());
    assertEquals("second entry", 20, entry.getValue().intValue());
    assertFalse(it.hasNext());
  }
  
  public void testHightlightModelGetRangeValues2() {
    HighlightModel model = HighlightModel.create();
    model.addData(1.0, 1);
    model.addData(2.0, 2);
    model.addData(20.0, 3);
    model.addData(20.0, 1);
    model.addData(50.0, 2);
    
    Iterator<HighlightEntry> it = model.getRangeValues(0.0, 100.0, 10.0);
    assertTrue(it.hasNext());
    HighlightEntry entry = it.next();
    assertEquals("first entry", 1.0, entry.getKey());
    assertEquals("first entry", 2, entry.getValue().intValue());
    assertTrue(it.hasNext());
    entry = it.next();
    assertEquals("second entry", 20.0, entry.getKey());
    assertEquals("second entry", 3, entry.getValue().intValue());
    entry = it.next();
    assertEquals("third entry", 50.0, entry.getKey());
    assertEquals("third entry", 2, entry.getValue().intValue());    
    assertFalse(it.hasNext());
  }  

}
