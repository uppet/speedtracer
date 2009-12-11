/*
 * Copyright 2008 Google Inc.
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

import com.google.gwt.junit.client.GWTTestCase;

/**
 * Tests {@link TimeStampFormatter}.
 */
public class TimeStampFormatterTests extends GWTTestCase {

  @Override
  public String getModuleName() {
    return "com.google.speedtracer.Common";
  }

  /**
   * Tests {@link TimeStampFormatter#format(double, int, double)}.
   */
  public void testFormat() {
    assertEquals("1000ms", TimeStampFormatter.format(1000, 3, 2000));
    assertEquals("2.000s", TimeStampFormatter.format(2000, 3, 2000));
    assertEquals("2.500s", TimeStampFormatter.format(2500, 3, 2000));
  }

  /**
   * Tests {@link TimeStampFormatter#formatMilliseconds(double)}.
   */
  public void testFormatMilliseconds() {
    assertEquals("1000ms", TimeStampFormatter.formatMilliseconds(1000));
    assertEquals("0ms", TimeStampFormatter.formatMilliseconds(0.3333));
  }

  /**
   * Tests {@link TimeStampFormatter#formatSeconds(double, int)}.
   */
  public void testFormatSeconds() {
    assertEquals("1s", TimeStampFormatter.formatSeconds(1000, 0));
    assertEquals("1.0s", TimeStampFormatter.formatSeconds(1000, 1));
    assertEquals("1.000s", TimeStampFormatter.formatSeconds(1000, 3));

    assertEquals("1.300s", TimeStampFormatter.formatSeconds(1300, 3));
    assertEquals("1.345s", TimeStampFormatter.formatSeconds(1345, 3));
    assertEquals("1.346s", TimeStampFormatter.formatSeconds(1345.6, 3));
  }
}
