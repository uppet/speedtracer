/**
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
package com.google.speedtracer.latencydashboard.shared;

import junit.framework.TestCase;

import java.util.Map;

/**
 * Test for the {@link CustomDashboardRecord} class.
 */
public class CustomDashboardRecordTests extends TestCase {

  public void testCustomDashboardRecord1() {
    CustomDashboardRecord customRecord = new CustomDashboardRecord(456L, "Foo",
        "r123");
    assertEquals("timestamp", 456L, customRecord.getTimestamp());
    assertEquals("name", "Foo", customRecord.getName());
    assertEquals("revision", "r123", customRecord.getRevision());
  }

  public void testCustomDashboardRecord2() {
    CustomDashboardRecord customRecord = new CustomDashboardRecord(456L, "Foo",
        "r123");
    customRecord.addCustomMeasure("measure_one", 100.0);
    customRecord.addCustomMeasure("measure two", 1000.0);
    Map<String, Double> customMeasures = customRecord.getCustomMetrics();
    Double data;
    data = customMeasures.get("measure_one");
    assertNotNull("data from measure_one", data);
    assertEquals("measure_one", 100.0, data, .001);
    data = customMeasures.get("measure two");
    assertEquals("measure two", 1000.0, data, .001);
  }
}
