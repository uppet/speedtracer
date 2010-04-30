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

/**
 * Test for parsing v8 log decompression.
 */
public class V8LogDecompressorTests extends GWTTestCase {

  @Override
  public String getModuleName() {
    return "com.google.speedtracer.Common";
  }

  public void testEscapedCompressionToken() {
    V8LogDecompressor decompressor = new V8LogDecompressor(4);
    String result;
    result = decompressor.decompressLogEntry("foo");
    assertEquals("foo", result);
    result = decompressor.decompressLogEntry("bar");
    assertEquals("bar", result);

    // should not trigger a decompression - inside a quoted regexp string
    result = decompressor.decompressLogEntry("\"#2:1\"");
    assertEquals("\"#2:1\"", result);
  }

  public void testLineCompression() {
    V8LogDecompressor decompressor = new V8LogDecompressor(4);
    String result;
    result = decompressor.decompressLogEntry("foo");
    assertEquals("foo", result);
    result = decompressor.decompressLogEntry("bar");
    assertEquals("bar", result);
    result = decompressor.decompressLogEntry("baz");
    assertEquals("baz", result);
    result = decompressor.decompressLogEntry("#3");
    assertEquals("foo", result);
    result = decompressor.decompressLogEntry("a#3");
    assertEquals("abar", result);
    result = decompressor.decompressLogEntry("ab#3");
    assertEquals("abbaz", result);
  }

  public void testProfilerInteraction() {
    V8LogDecompressor decompressor = new V8LogDecompressor(4);
    String result;
    result = decompressor.decompressLogEntry("10,foo");
    assertEquals("10,foo", result);
    result = decompressor.decompressLogEntry("10,bar");
    assertEquals("10,bar", result);
    result = decompressor.decompressLogEntry("10,baz");
    assertEquals("10,baz", result);
    result = decompressor.decompressLogEntry("10,foobar");
    assertEquals("10,foobar", result);
    result = decompressor.decompressLogEntry("profiler,\"resume\"");
    assertEquals("profiler,\"resume\"", result);
    result = decompressor.decompressLogEntry("#1");
    assertEquals("10,foobar", result);
  }

  public void testRepeatInteraction() {
    V8LogDecompressor decompressor = new V8LogDecompressor(4);
    String result;
    result = decompressor.decompressLogEntry("repeat,10,foo");
    assertEquals("repeat,10,foo", result);
    result = decompressor.decompressLogEntry("#1");
    assertEquals("foo", result);
    result = decompressor.decompressLogEntry("repeat,10,foo");
    result = decompressor.decompressLogEntry("#1:1");
    assertEquals("oo", result);
  }

  public void testSubLineCompression() {
    V8LogDecompressor decompressor = new V8LogDecompressor(4);
    String result;
    result = decompressor.decompressLogEntry("foo");
    assertEquals("foo", result);
    result = decompressor.decompressLogEntry("bar");
    assertEquals("bar", result);
    result = decompressor.decompressLogEntry("baz");
    assertEquals("baz", result);
    result = decompressor.decompressLogEntry("#3:1");
    assertEquals("oo", result);
    result = decompressor.decompressLogEntry("a#3:2");
    assertEquals("ar", result);
    result = decompressor.decompressLogEntry("abcdefghijklmnopqrstuvwxyz");
    assertEquals("abcdefghijklmnopqrstuvwxyz", result);
    result = decompressor.decompressLogEntry("#1:10");
    assertEquals("klmnopqrstuvwxyz", result);
  }
}
