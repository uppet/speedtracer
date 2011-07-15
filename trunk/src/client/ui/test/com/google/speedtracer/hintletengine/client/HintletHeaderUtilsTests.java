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

package com.google.speedtracer.hintletengine.client;

import com.google.gwt.junit.client.GWTTestCase;
import com.google.speedtracer.client.model.NetworkResource.HeaderMap;

/**
 * Tests {@link HintletHeaderUtilsTests}.
 * 
 */
public class HintletHeaderUtilsTests extends GWTTestCase {

  @Override
  public String getModuleName() {
    return "com.google.speedtracer.hintletengine.HintletEngineTest";
  }

  public void testHasHeader() {
    HeaderMap testData = makeHeaderMap("text/html", "gzip");
    assertEquals("text/html", HintletHeaderUtils.hasHeader(testData, "Content-Type"));
    assertEquals("text/html", HintletHeaderUtils.hasHeader(testData, "content-type"));
    assertNull(HintletHeaderUtils.hasHeader(testData, "bogus"));
  }

  public void testIsCompressed() {
    assertTrue(HintletHeaderUtils.isCompressed(makeHeaderMap("text/html", "gzip")));
    assertFalse(HintletHeaderUtils.isCompressed(makeHeaderMap("text/html", "bogus")));
    assertFalse(HintletHeaderUtils.isCompressed(makeHeaderMapWithoutContentEncoding()));
  }

  public void testHeaderContains() {
    HeaderMap testData = makeHeaderMap("text/html", "gzip");
    assertTrue(HintletHeaderUtils.headerContains(testData, "Content-Type", "text/html"));
    assertTrue(HintletHeaderUtils.headerContains(testData, "Content-Type", "TEXT/HTML"));
    assertTrue(HintletHeaderUtils.headerContains(testData, "content-Type", "TEXT/HTML"));
    assertFalse(HintletHeaderUtils.headerContains(testData, "content-Type", "TEXT/SGML"));
    assertFalse(HintletHeaderUtils.headerContains(testData, "bogus", "text/html"));
  }

  private static native HeaderMap makeHeaderMap(String contentType, String contentEncoding) /*-{
    return {
        "Content-Type" : contentType,
        "Content-Encoding" : contentEncoding
    };
  }-*/;

  private static native HeaderMap makeHeaderMapWithoutContentEncoding()/*-{
    return {
      "Content-Type" : "text/html"
    };
  }-*/;

}
