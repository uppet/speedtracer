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
package com.google.speedtracer.client.visualizations.model;

import com.google.gwt.coreext.client.JSON;
import com.google.gwt.coreext.client.JsIntegerDoubleMap;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.speedtracer.client.model.DataDispatcher;
import com.google.speedtracer.client.model.HintRecord;
import com.google.speedtracer.client.model.ResourceRecord;
import com.google.speedtracer.client.model.ResourceResponseEvent;
import com.google.speedtracer.client.model.UiEvent;
import com.google.speedtracer.client.visualizations.model.ReportDataCollector.ReportData;

import java.util.List;

/**
 * Tests {@link ReportDataCollector}.
 */
public class ReportDataCollectorTests extends GWTTestCase {
  /**
   * Testable DataDispatcher.
   */
  private class MockDataDispatcher extends DataDispatcher {
    protected MockDataDispatcher() {
      super(null);
      initialize();
    }

    @Override
    protected void initialize() {
      // We need the network dispatcher to accumulate NetworkResources for us.
      addDispatcher(getUiEventDispatcher());
      addDispatcher(getNetworkEventDispatcher());
    }
  }

  private static final double ERROR_MARGIN = 0.01;

  @Override
  public String getModuleName() {
    return "com.google.speedtracer.ModelTests";
  }

  /**
   * Tests aggregating type durations for UiEvents.
   */
  public void testAggregatingTypeDurations() {
    MockDataDispatcher mockDispatcher = new MockDataDispatcher();
    dispatchTestUiEvents(mockDispatcher);

    // Test aggregating with window bounds around the entire data set.
    ReportDataCollector dataCollector = new ReportDataCollector(mockDispatcher);
    ReportData data = dataCollector.gatherDataWithinWindow(0, 22);
    JsIntegerDoubleMap typeDurations = data.getAggregatedTypeDurations();
    // Available time should be 4ms;
    assertEquals(4.0, typeDurations.get(-1), ERROR_MARGIN);
    assertEquals(6.0, typeDurations.get(4), ERROR_MARGIN);
    assertEquals(3.0, typeDurations.get(3), ERROR_MARGIN);
    assertEquals(6.0, typeDurations.get(2), ERROR_MARGIN);
    assertEquals(3.0, typeDurations.get(1), ERROR_MARGIN);

    // Tests that aggregating works even when events fall on both window
    // boundaries.
    data = dataCollector.gatherDataWithinWindow(1.5, 20.7);
    typeDurations = data.getAggregatedTypeDurations();
    // Available time should still be 4ms;
    assertEquals(4.0, typeDurations.get(-1), ERROR_MARGIN);
    assertEquals(4.7, typeDurations.get(4), ERROR_MARGIN);
    assertEquals(2.5, typeDurations.get(3), ERROR_MARGIN);
    assertEquals(5.0, typeDurations.get(2), ERROR_MARGIN);
    assertEquals(3.0, typeDurations.get(1), ERROR_MARGIN);
    
    // Tests gathering data with a window completely to the right of everything.
    data = dataCollector.gatherDataWithinWindow(100, 120);
    typeDurations = data.getAggregatedTypeDurations();
    assertEquals(20.0, typeDurations.get(-1), ERROR_MARGIN);
  }

  /**
   * Tests collecting hints attached to UiEvents.
   */
  public void testCollectingHints() {
    MockDataDispatcher mockDispatcher = new MockDataDispatcher();
    dispatchTestUiEvents(mockDispatcher);

    ReportDataCollector dataCollector = new ReportDataCollector(mockDispatcher);
    ReportData data = dataCollector.gatherDataWithinWindow(0, 22);

    List<HintRecord> hints = data.getHints();

    assertEquals(3, hints.size());
  }

  /**
   * Tests collecting hints attached to Network resource related events.
   */
  public void testCollectingNetworkHints() {
    MockDataDispatcher mockDispatcher = new MockDataDispatcher();
    dispatchTestNetworkEvents(mockDispatcher);

    ReportDataCollector dataCollector = new ReportDataCollector(mockDispatcher);
    ReportData data = dataCollector.gatherDataWithinWindow(0, 22);

    List<HintRecord> hints = data.getHints();

    assertEquals(1, hints.size());
  }

  private void dispatchTestNetworkEvents(MockDataDispatcher mockDispatcher) {
    final String[] networkRecords = {
        "{\"data\":{\"identifier\":1,\"url\":\"http://digg.com/\",\"requestMethod\":\"GET\",\"isMainResource\":true},\"type\":12,\"time\":0,\"sequence\":0}",
        "{\"data\":{\"identifier\":1,\"statusCode\":200,\"mimeType\":\"text/html\",\"expectedContentLength\":-1},\"type\":13,\"time\":138.000244140625,\"sequence\":1}",
        "{\"type\":2147483645,\"data\":{\"url\":\"http://www.reddit.com/\",\"documentURL\":\"http://www.reddit.com/\",\"host\":\"www.reddit.com\",\"path\":\"/\",\"lastPathComponent\":\"\",\"requestHeaders\":{\"User-Agent\":\"Mozilla/5.0 (Windows; U; Windows NT 6.0; en-US) AppleWebKit/533.3 (KHTML, like Gecko) Chrome/5.0.356.2 Safari/533.3\"},\"mainResource\":true,\"requestMethod\":\"GET\",\"requestFormData\":\"\",\"didRequestChange\":true,\"cached\":false,\"mimeType\":\"text/html\",\"suggestedFilename\":\"\",\"expectedContentLength\":-1,\"statusCode\":200,\"responseHeaders\":{\"Date\":\"Mon, 22 Mar 2010 20:56:51 GMT\",\"Content-Encoding\":\"gzip\",\"Connection\":\"keep-alive\",\"Content-Length\":\"15021\",\"Server\":\"FriendFeedServer/0.1\",\"Vary\":\"Accept-Encoding\",\"Content-Type\":\"text/html; charset=UTF-8\"},\"didResponseChange\":true,\"type\":0,\"didTypeChange\":true,\"startTime\":6469,\"responseReceivedTime\":7822,\"didTimingChange\":true,\"identifier\":1},\"time\":7822,\"sequence\":2}",
        "{\"data\":{\"identifier\":1,\"didFail\":false},\"type\":14,\"time\":262,\"sequence\":3}",
        "{\"type\":2147483645,\"data\":{\"resourceSize\":99038,\"didLengthChange\":true,\"failed\":false,\"finished\":true,\"didCompletionChange\":true,\"startTime\":6469,\"responseReceivedTime\":7822,\"endTime\":8224,\"didTimingChange\":true,\"identifier\":1},\"time\":8224,\"sequence\":4}"};

    for (int i = 0, n = networkRecords.length; i < n; i++) {
      ResourceRecord networkRecord = JSON.parse(networkRecords[i]).cast();
      mockDispatcher.onEventRecord(networkRecord);
      if (networkRecord.getType() == ResourceResponseEvent.TYPE) {
        // We tack a hint on a response event. Note the response event happens
        // way in the future.
        mockDispatcher.onHint(HintRecord.create("testRuleName",
            networkRecord.getTime(), HintRecord.SEVERITY_INFO,
            "testDescription3", networkRecord.getSequence()));
      }
    }
  }

  /**
   * Populates a MockDataDispatcher with our test data.
   * 
   * 3 test events that have some gaps between them.
   */
  private void dispatchTestUiEvents(MockDataDispatcher mockDispatcher) {
    // Make some events
    UiEvent event1 = makeTestUiEvent(0);
    // Leave 1.23ms of available time and make another one.
    UiEvent event2 = makeTestUiEvent(event1.getDuration() + 1.23);
    // Leave 2.77ms of available time and make another one.
    UiEvent event3 = makeTestUiEvent(event2.getTime() + event2.getDuration()
        + 2.77);
    UiEvent event4 = makeTestUiEvent(event3.getTime() + event3.getDuration()
        + 20);
    
    mockDispatcher.onEventRecord(event1);
    mockDispatcher.onEventRecord(event2);
    mockDispatcher.onEventRecord(event3);
    mockDispatcher.onEventRecord(event4);

    // Add some hints.
    mockDispatcher.onHint(HintRecord.create("testrule", event1.getTime(),
        HintRecord.SEVERITY_INFO, "testrule", event1.getSequence()));
    mockDispatcher.onHint(HintRecord.create("testrule2", event1.getTime(),
        HintRecord.SEVERITY_INFO, "testrule2", event1.getSequence()));
    mockDispatcher.onHint(HintRecord.create("testrule3", event1.getTime(),
        HintRecord.SEVERITY_INFO, "testrule3", event3.getSequence()));
  }

  /**
   * Builds a single event tree with 4 event types.
   * 
   * The tree looks like:
   * 
   * <pre>
   * 1: ------
   * 2: ---
   * 3:  -
   * 4:     --
   * </pre>
   */
  private UiEvent makeTestUiEvent(double startTime) {
    UiEvent root = ReportDataCollector.createUiEvent(1, startTime, 6, null);
    UiEvent child = ReportDataCollector.createUiEvent(2, startTime, 3, null);
    child.addChild(ReportDataCollector.createUiEvent(3, startTime + 1, 1, null));
    root.addChild(child);
    root.addChild(ReportDataCollector.createUiEvent(4, startTime + 4, 2, null));
    return root;
  }
}
