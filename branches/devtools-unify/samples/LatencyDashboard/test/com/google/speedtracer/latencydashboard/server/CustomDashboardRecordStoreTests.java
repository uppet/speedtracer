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
package com.google.speedtracer.latencydashboard.server;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.google.speedtracer.latencydashboard.shared.CustomDashboardRecord;

import junit.framework.TestCase;

import java.util.Iterator;

/**
 * To get this test to work, you need to have extra AppEngine jar files on the
 * class path.
 * 
 * See <a href=
 * "http://code.google.com/appengine/docs/java/tools/localunittesting.html">App
 * Engine Local Unit Testing</a>
 * 
 */
public class CustomDashboardRecordStoreTests extends TestCase {

  private final LocalServiceTestHelper helper = new LocalServiceTestHelper(
      new LocalDatastoreServiceTestConfig());

  public void setUp() {
    helper.setUp();
  }

  public void tearDown() {
    helper.tearDown();
  }

  public void testGetPut() {
    String unitTestName = "UnitTest-testPut";
    DatastoreService store = DatastoreServiceFactory.getDatastoreService();
    CustomDashboardRecordStore.clear(store, unitTestName);

    long now = System.currentTimeMillis();
    CustomDashboardRecord record = new CustomDashboardRecord(now, unitTestName,
        "r1");
    record.addCustomMeasure("measure_one", 100.0);
    record.addCustomMeasure("measure two", 1000.0);
    CustomDashboardRecordStore.put(
        DatastoreServiceFactory.getDatastoreService(), record);

    Iterator<CustomDashboardRecord> results = CustomDashboardRecordStore.getLatest(
        store, 100);

    CustomDashboardRecord found = null;
    while (results.hasNext()) {
      CustomDashboardRecord result = results.next();
      String foundName = result.getName();
      long foundTimeStamp = result.getTimestamp();
      if (foundName.equals(unitTestName) && foundTimeStamp == now) {
        found = result;
        break;
      }
    }
    assertNotNull("no matching result found", found);
    assertEquals("name", unitTestName, found.getName());
    assertEquals("revision", "r1", found.getRevision());
    assertEquals("timeStamp", now, found.getTimestamp());
    assertEquals("measure_one", 100.0, found.getCustomMetrics().get(
        "measure_one"));
    assertEquals("measure two", 1000.0, found.getCustomMetrics().get(
        "measure two"));
  }
}
