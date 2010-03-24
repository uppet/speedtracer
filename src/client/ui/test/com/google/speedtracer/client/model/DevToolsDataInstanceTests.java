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

import com.google.gwt.chrome.crx.client.events.DevToolsPageEvent.PageEvent;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.speedtracer.client.model.DataInstance.DataListener;
import com.google.speedtracer.client.model.DataInstance.DataProxy;
import com.google.speedtracer.client.model.DevToolsDataInstance.Proxy;
import com.google.speedtracer.client.util.JSON;

import java.util.ArrayList;
import java.util.List;

/**
 * Tests {@link DevToolsDataInstance}.
 */
public class DevToolsDataInstanceTests extends GWTTestCase {
  /**
   * Testable mock class used to test the record conversion done in
   * {@link DevToolsDataInstance} by its {@link DataProxy}.
   */
  static class MockDataListener implements DataListener {
    private int currentExpectedRecord = 0;
    private List<String> expectedRecords = new ArrayList<String>();

    /**
     * This is called by the DataInstance.
     */
    public void onEventRecord(EventRecord record) {
      assert (currentExpectedRecord < expectedRecords.size()) : "Received a record without a corresponding expected record to compare against";
      String recordStr = getSanitizedString(record);
      String expectedRecordStr = expectedRecords.get(currentExpectedRecord);
      assertEquals(expectedRecordStr, recordStr);
      currentExpectedRecord++;
    }

    /**
     * Sets a series of expected output records that will be triggered by a
     * single dispatch into the DataInstance proxy.
     */
    public void setExpectedRecords(String[] recordsToCompare) {
      this.expectedRecords.clear();
      this.currentExpectedRecord = 0;
      for (int i = 0; i < recordsToCompare.length; i++) {
        expectedRecords.add(recordsToCompare[i]);
      }
    }
  }

  private static final int TAB_ID = 1;

  /**
   * Deletes the "__gwt_Object_Id" property and returns the stringified version
   * of it.
   */
  private static native String getSanitizedString(EventRecord record) /*-{
    // Use a regex to trip the GWT object ID field off the stringified record trace.
    var recordStr = JSON.stringify(record);
    recordStr = recordStr.replace(/,*"__gwt_ObjectId":\d+/g,"");

    // Because we cant have * / in the regex (eclipse thinks I am killing the
    // comment block) we can sometimes have a leading comma.
    recordStr = recordStr.replace("{,","{");
    return recordStr;
  }-*/;

  @Override
  public String getModuleName() {
    return "com.google.speedtracer.ModelTests";
  }

  /**
   * This method tests simple conversion of a sequence of Network Resource
   * Timeline Events, that should result in a page transition event being fired,
   * and UpdateResource events that should synthesize the associate resource
   * update record.
   * 
   * This test also ends with some non-network resource timeline events to
   * ensure that time normalization works when things are started by network
   * resource checkpoints..
   */
  public void testNetworkResources() {
    final String[] networkRecordsIn = {
        "[\"addRecordToTimeline\",{\"startTime\":1269291402424.688,\"data\":{\"identifier\":1,\"url\":\"http://digg.com/\",\"requestMethod\":\"GET\",\"isMainResource\":true},\"type\":12}]",
        "[\"addRecordToTimeline\",{\"startTime\":1269291402562.6882,\"data\":{\"identifier\":1,\"statusCode\":200,\"mimeType\":\"text/html\",\"expectedContentLength\":-1},\"type\":13}]",
        "[\"updateResource\",1,{\"url\":\"http://www.reddit.com/\",\"documentURL\":\"http://www.reddit.com/\",\"host\":\"www.reddit.com\",\"path\":\"/\",\"lastPathComponent\":\"\",\"requestHeaders\":{\"User-Agent\":\"Mozilla/5.0 (Windows; U; Windows NT 6.0; en-US) AppleWebKit/533.3 (KHTML, like Gecko) Chrome/5.0.356.2 Safari/533.3\"},\"mainResource\":true,\"requestMethod\":\"GET\",\"requestFormData\":\"\",\"didRequestChange\":true,\"cached\":false,\"mimeType\":\"text/html\",\"suggestedFilename\":\"\",\"expectedContentLength\":-1,\"statusCode\":200,\"responseHeaders\":{\"Date\":\"Mon, 22 Mar 2010 20:56:51 GMT\",\"Content-Encoding\":\"gzip\",\"Connection\":\"keep-alive\",\"Content-Length\":\"15021\",\"Server\":\"FriendFeedServer/0.1\",\"Vary\":\"Accept-Encoding\",\"Content-Type\":\"text/html; charset=UTF-8\"},\"didResponseChange\":true,\"type\":0,\"didTypeChange\":true,\"startTime\":1269291408.893688,\"responseReceivedTime\":1269291410.246688,\"didTimingChange\":true}]",
        "[\"addRecordToTimeline\",{\"startTime\":1269291402686.688,\"data\":{\"identifier\":1,\"didFail\":false},\"type\":14}]",
        "[\"updateResource\",1,{\"resourceSize\":99038,\"didLengthChange\":true,\"failed\":false,\"finished\":true,\"didCompletionChange\":true,\"startTime\":1269291408.893688,\"responseReceivedTime\":1269291410.246688,\"endTime\":1269291410.648688,\"didTimingChange\":true}]"};

    final String[] networkRecordsOut = {
        "{\"type\":2147483646,\"data\":{\"url\":\"http://digg.com/\"},\"time\":0,\"sequence\":0}",
        "{\"data\":{\"identifier\":1,\"url\":\"http://digg.com/\",\"requestMethod\":\"GET\",\"isMainResource\":true},\"type\":12,\"time\":0,\"sequence\":1}",
        "{\"data\":{\"identifier\":1,\"statusCode\":200,\"mimeType\":\"text/html\",\"expectedContentLength\":-1},\"type\":13,\"time\":138.000244140625,\"sequence\":2}",
        "{\"type\":2147483645,\"data\":{\"url\":\"http://www.reddit.com/\",\"documentURL\":\"http://www.reddit.com/\",\"host\":\"www.reddit.com\",\"path\":\"/\",\"lastPathComponent\":\"\",\"requestHeaders\":{\"User-Agent\":\"Mozilla/5.0 (Windows; U; Windows NT 6.0; en-US) AppleWebKit/533.3 (KHTML, like Gecko) Chrome/5.0.356.2 Safari/533.3\"},\"mainResource\":true,\"requestMethod\":\"GET\",\"requestFormData\":\"\",\"didRequestChange\":true,\"cached\":false,\"mimeType\":\"text/html\",\"suggestedFilename\":\"\",\"expectedContentLength\":-1,\"statusCode\":200,\"responseHeaders\":{\"Date\":\"Mon, 22 Mar 2010 20:56:51 GMT\",\"Content-Encoding\":\"gzip\",\"Connection\":\"keep-alive\",\"Content-Length\":\"15021\",\"Server\":\"FriendFeedServer/0.1\",\"Vary\":\"Accept-Encoding\",\"Content-Type\":\"text/html; charset=UTF-8\"},\"didResponseChange\":true,\"type\":0,\"didTypeChange\":true,\"startTime\":6469,\"responseReceivedTime\":7822,\"didTimingChange\":true,\"identifier\":1},\"time\":7822,\"sequence\":3}",
        "{\"data\":{\"identifier\":1,\"didFail\":false},\"type\":14,\"time\":262,\"sequence\":4}",
        "{\"type\":2147483645,\"data\":{\"resourceSize\":99038,\"didLengthChange\":true,\"failed\":false,\"finished\":true,\"didCompletionChange\":true,\"startTime\":6469,\"responseReceivedTime\":7822,\"endTime\":8224,\"didTimingChange\":true,\"identifier\":1},\"time\":8224,\"sequence\":5}"};

    final MockDataListener mockDataListener = new MockDataListener();
    final Proxy proxy = new Proxy(TAB_ID) {
      // Instead of connecting to DevTools, we will begin sending our test
      // records.
      @Override
      protected void connectToDataSource() {
        // BEGIN THE TESTS

        // Resource start should be buffered.
        testRecord(this, mockDataListener, box("RECORD SHOULD BE BUFFERED"),
            networkRecordsIn[0]);
        // Resource response should cause a the pending records to flush.
        // We should see a page Transition, then a resources start, then the
        // response.
        testRecord(this, mockDataListener, box(networkRecordsOut[0],
            networkRecordsOut[1], networkRecordsOut[2]), networkRecordsIn[1]);
        // We are sending an update resource over which should wrap it into a
        // ResourceUpdateEvent record.
        testRecord(this, mockDataListener, box(networkRecordsOut[3]),
            networkRecordsIn[2]);
        // We are sending finish.
        testRecord(this, mockDataListener, box(networkRecordsOut[4]),
            networkRecordsIn[3]);
        // We are sending an update resource over which should wrap it into a
        // ResourceUpdateEvent record.
        testRecord(this, mockDataListener, box(networkRecordsOut[5]),
            networkRecordsIn[4]);
      }
    };

    DevToolsDataInstance dataInstance = DevToolsDataInstance.create(proxy);
    dataInstance.load(mockDataListener);
  }

  /**
   * This method tests simple conversion of timeline record events. We ensure
   * that the timestamps are normalized correctly.
   */
  public void testSimpleTimeline() {
    final String[] timelineRecordsIn = {
        "[\"addRecordToTimeline\",{\"startTime\":1269291400371.688,\"data\":{},\"children\":[{\"startTime\":1269291400371.688,\"data\":{},\"children\":[],\"endTime\":1269291400372.688,\"type\":2}],\"endTime\":1269291400372.688,\"type\":5}]",
        "[\"addRecordToTimeline\",{\"startTime\":1269291400375.688,\"data\":{},\"children\":[],\"endTime\":1269291400376.688,\"type\":3}]",
        "[\"addRecordToTimeline\",{\"startTime\":1269291400378.688,\"data\":{},\"children\":[],\"endTime\":1269291400378.688,\"type\":4}]"};

    final String[] timelineRecordsOut = {
        "{\"data\":{},\"children\":[{\"data\":{},\"children\":[],\"type\":2,\"duration\":1,\"time\":0}],\"type\":5,\"duration\":1,\"time\":0,\"sequence\":0}",
        "{\"data\":{},\"children\":[],\"type\":3,\"duration\":1,\"time\":4,\"sequence\":1}",
        "{\"data\":{},\"children\":[],\"type\":4,\"duration\":0,\"time\":7,\"sequence\":2}"};

    final MockDataListener mockDataListener = new MockDataListener();
    final Proxy proxy = new Proxy(TAB_ID) {
      // Instead of connecting to DevTools, we will begin sending our test
      // records.
      @Override
      protected void connectToDataSource() {
        // BEGIN THE TESTS

        for (int i = 0; i < timelineRecordsIn.length; i++) {
          String[] expectedOutput = {timelineRecordsOut[i]};
          testRecord(this, mockDataListener, expectedOutput,
              timelineRecordsIn[i]);
        }
      }
    };

    DevToolsDataInstance dataInstance = DevToolsDataInstance.create(proxy);
    dataInstance.load(mockDataListener);
  }

  /**
   * This method tests sending non-network timeline records, and then sending a
   * sequence of network resource related events.
   */
  public void testTimelineThenNetworkResources() {
    final String[] recordsIn = {
        "[\"addRecordToTimeline\",{\"startTime\":1269291402324.688,\"data\":{},\"children\":[],\"endTime\":1269291402424.688,\"type\":3}]",
        "[\"addRecordToTimeline\",{\"startTime\":1269291402424.688,\"data\":{\"identifier\":1,\"url\":\"http://digg.com/\",\"requestMethod\":\"GET\",\"isMainResource\":true},\"type\":12}]",
        "[\"addRecordToTimeline\",{\"startTime\":1269291402562.6882,\"data\":{\"identifier\":1,\"statusCode\":200,\"mimeType\":\"text/html\",\"expectedContentLength\":-1},\"type\":13}]",
        "[\"updateResource\",1,{\"url\":\"http://www.reddit.com/\",\"documentURL\":\"http://www.reddit.com/\",\"host\":\"www.reddit.com\",\"path\":\"/\",\"lastPathComponent\":\"\",\"requestHeaders\":{\"User-Agent\":\"Mozilla/5.0 (Windows; U; Windows NT 6.0; en-US) AppleWebKit/533.3 (KHTML, like Gecko) Chrome/5.0.356.2 Safari/533.3\"},\"mainResource\":true,\"requestMethod\":\"GET\",\"requestFormData\":\"\",\"didRequestChange\":true,\"cached\":false,\"mimeType\":\"text/html\",\"suggestedFilename\":\"\",\"expectedContentLength\":-1,\"statusCode\":200,\"responseHeaders\":{\"Date\":\"Mon, 22 Mar 2010 20:56:51 GMT\",\"Content-Encoding\":\"gzip\",\"Connection\":\"keep-alive\",\"Content-Length\":\"15021\",\"Server\":\"FriendFeedServer/0.1\",\"Vary\":\"Accept-Encoding\",\"Content-Type\":\"text/html; charset=UTF-8\"},\"didResponseChange\":true,\"type\":0,\"didTypeChange\":true,\"startTime\":1269291408.893688,\"responseReceivedTime\":1269291410.246688,\"didTimingChange\":true}]",
        "[\"addRecordToTimeline\",{\"startTime\":1269291402686.688,\"data\":{\"identifier\":1,\"didFail\":false},\"type\":14}]",
        "[\"updateResource\",1,{\"resourceSize\":99038,\"didLengthChange\":true,\"failed\":false,\"finished\":true,\"didCompletionChange\":true,\"startTime\":1269291408.893688,\"responseReceivedTime\":1269291410.246688,\"endTime\":1269291410.648688,\"didTimingChange\":true}]"};

    final String[] recordsOut = {
        "{\"data\":{},\"children\":[],\"type\":3,\"duration\":100,\"time\":0,\"sequence\":0}",
        "{\"type\":2147483646,\"data\":{\"url\":\"http://digg.com/\"},\"time\":100,\"sequence\":1}",
        "{\"data\":{\"identifier\":1,\"url\":\"http://digg.com/\",\"requestMethod\":\"GET\",\"isMainResource\":true},\"type\":12,\"time\":100,\"sequence\":2}",
        "{\"data\":{\"identifier\":1,\"statusCode\":200,\"mimeType\":\"text/html\",\"expectedContentLength\":-1},\"type\":13,\"time\":238.000244140625,\"sequence\":3}",
        "{\"type\":2147483645,\"data\":{\"url\":\"http://www.reddit.com/\",\"documentURL\":\"http://www.reddit.com/\",\"host\":\"www.reddit.com\",\"path\":\"/\",\"lastPathComponent\":\"\",\"requestHeaders\":{\"User-Agent\":\"Mozilla/5.0 (Windows; U; Windows NT 6.0; en-US) AppleWebKit/533.3 (KHTML, like Gecko) Chrome/5.0.356.2 Safari/533.3\"},\"mainResource\":true,\"requestMethod\":\"GET\",\"requestFormData\":\"\",\"didRequestChange\":true,\"cached\":false,\"mimeType\":\"text/html\",\"suggestedFilename\":\"\",\"expectedContentLength\":-1,\"statusCode\":200,\"responseHeaders\":{\"Date\":\"Mon, 22 Mar 2010 20:56:51 GMT\",\"Content-Encoding\":\"gzip\",\"Connection\":\"keep-alive\",\"Content-Length\":\"15021\",\"Server\":\"FriendFeedServer/0.1\",\"Vary\":\"Accept-Encoding\",\"Content-Type\":\"text/html; charset=UTF-8\"},\"didResponseChange\":true,\"type\":0,\"didTypeChange\":true,\"startTime\":6569,\"responseReceivedTime\":7922,\"didTimingChange\":true,\"identifier\":1},\"time\":7922,\"sequence\":4}",
        "{\"data\":{\"identifier\":1,\"didFail\":false},\"type\":14,\"time\":362,\"sequence\":5}",
        "{\"type\":2147483645,\"data\":{\"resourceSize\":99038,\"didLengthChange\":true,\"failed\":false,\"finished\":true,\"didCompletionChange\":true,\"startTime\":6569,\"responseReceivedTime\":7922,\"endTime\":8324,\"didTimingChange\":true,\"identifier\":1},\"time\":8324,\"sequence\":6}"};

    final MockDataListener mockDataListener = new MockDataListener();
    final Proxy proxy = new Proxy(TAB_ID) {
      // Instead of connecting to DevTools, we will begin sending our test
      // records.
      @Override
      protected void connectToDataSource() {
        // BEGIN THE TESTS

        // Send a timeline record to baseline the time.
        testRecord(this, mockDataListener, box(recordsOut[0]), recordsIn[0]);
        // Resource start should trigger a page transition and a send.
        testRecord(this, mockDataListener, box(recordsOut[1], recordsOut[2]),
            recordsIn[1]);
        // Resource response.
        testRecord(this, mockDataListener, box(recordsOut[3]), recordsIn[2]);
        // We are sending an update resource over which should wrap it into a
        // ResourceUpdateEvent record.
        testRecord(this, mockDataListener, box(recordsOut[4]), recordsIn[3]);
        // We are sending finish.
        testRecord(this, mockDataListener, box(recordsOut[5]), recordsIn[4]);
        // We are sending an update resource over which should wrap it into a
        // ResourceUpdateEvent record.
        testRecord(this, mockDataListener, box(recordsOut[6]), recordsIn[5]);
      }
    };

    DevToolsDataInstance dataInstance = DevToolsDataInstance.create(proxy);
    dataInstance.load(mockDataListener);
  }

  /**
   * This tests that normalization works when we encounter an UpdateResource
   * message first.
   */
  public void testUpdateResourceFirst() {
    final String updateResourceIn = "[\"updateResource\",1,{\"url\":\"http://www.reddit.com/\",\"documentURL\":\"http://www.reddit.com/\",\"host\":\"www.reddit.com\",\"path\":\"/\",\"lastPathComponent\":\"\",\"requestHeaders\":{\"User-Agent\":\"Mozilla/5.0 (Windows; U; Windows NT 6.0; en-US) AppleWebKit/533.3 (KHTML, like Gecko) Chrome/5.0.356.2 Safari/533.3\"},\"mainResource\":true,\"requestMethod\":\"GET\",\"requestFormData\":\"\",\"didRequestChange\":true,\"cached\":false,\"mimeType\":\"text/html\",\"suggestedFilename\":\"\",\"expectedContentLength\":-1,\"statusCode\":200,\"responseHeaders\":{\"Date\":\"Mon, 22 Mar 2010 20:56:51 GMT\",\"Content-Encoding\":\"gzip\",\"Connection\":\"keep-alive\",\"Content-Length\":\"15021\",\"Server\":\"FriendFeedServer/0.1\",\"Vary\":\"Accept-Encoding\",\"Content-Type\":\"text/html; charset=UTF-8\"},\"didResponseChange\":true,\"type\":0,\"didTypeChange\":true,\"startTime\":1269291408.893688,\"responseReceivedTime\":1269291410.246688,\"didTimingChange\":true}]";

    final String[] timelineRecordsIn = {
        "[\"addRecordToTimeline\",{\"startTime\":1269291400371.688,\"data\":{},\"children\":[{\"startTime\":1269291400371.688,\"data\":{},\"children\":[],\"endTime\":1269291400372.688,\"type\":2}],\"endTime\":1269291400372.688,\"type\":5}]",
        "[\"addRecordToTimeline\",{\"startTime\":1269291400375.688,\"data\":{},\"children\":[],\"endTime\":1269291400376.688,\"type\":3}]",
        "[\"addRecordToTimeline\",{\"startTime\":1269291400378.688,\"data\":{},\"children\":[],\"endTime\":1269291400378.688,\"type\":4}]"};

    final String[] timelineRecordsOut = {
        "{\"data\":{},\"children\":[{\"data\":{},\"children\":[],\"type\":2,\"duration\":1,\"time\":0}],\"type\":5,\"duration\":1,\"time\":0,\"sequence\":0}",
        "{\"data\":{},\"children\":[],\"type\":3,\"duration\":1,\"time\":4,\"sequence\":1}",
        "{\"data\":{},\"children\":[],\"type\":4,\"duration\":0,\"time\":7,\"sequence\":2}"};

    final MockDataListener mockDataListener = new MockDataListener();
    final Proxy proxy = new Proxy(TAB_ID) {
      // Instead of connecting to DevTools, we will begin sending our test
      // records.
      @Override
      protected void connectToDataSource() {
        // BEGIN THE TESTS

        // Send the updateResource.
        testRecord(this, mockDataListener, box("THIS SHOULD BE DROPPED"),
            updateResourceIn);

        // then send the timeline records.
        for (int i = 0; i < timelineRecordsIn.length; i++) {
          String[] expectedOutput = {timelineRecordsOut[i]};
          testRecord(this, mockDataListener, expectedOutput,
              timelineRecordsIn[i]);
        }
      }
    };

    DevToolsDataInstance dataInstance = DevToolsDataInstance.create(proxy);
    dataInstance.load(mockDataListener);
  }

  /**
   * Utility method for boxing a variable number of Strings.
   */
  private String[] box(String... args) {
    return args;
  }

  private void testRecord(Proxy proxy, MockDataListener mockDataListener,
      String[] expected, String input) {
    // Set the expected output that the MockDataListener will assert on.
    mockDataListener.setExpectedRecords(expected);
    // Send the inputs in and allow the MockDataListener to make the assertions.
    proxy.dispatchPageEvent(JSON.parse(input).<PageEvent> cast());
  }
}