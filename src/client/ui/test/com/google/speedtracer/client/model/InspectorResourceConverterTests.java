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
package com.google.speedtracer.client.model;

import com.google.speedtracer.client.model.DevToolsDataInstance.Proxy;
import com.google.speedtracer.client.model.LegacyInspectorResourceConverter.AddResource;
import com.google.speedtracer.client.model.LegacyInspectorResourceConverter.UpdateResource;
import com.google.speedtracer.client.util.JSON;
import com.google.gwt.chrome.crx.client.events.DevToolsPageEvent.PageEvent;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.junit.client.GWTTestCase;

import java.util.ArrayList;
import java.util.List;

/**
 * Tests expected output from the InspectorResourceConverter. Specifically that
 * it synthesized the correct type and order of {@link NetworkResourceRecord}s
 * from inspector style resource records.
 */
public class InspectorResourceConverterTests extends GWTTestCase {
  /**
   * Testable {@link Proxy}. This simply collects output from the
   * LegacyInspectorResourceConverter.
   */
  private static class MockDevToolsDataProxy extends Proxy {
    int currentResource = 0;
    List<EventRecord> recordsReceived = new ArrayList<EventRecord>();

    public MockDevToolsDataProxy(int tabId) {
      super(tabId);
    }

    // We want all normalized times to be the same as the strings in the test
    // resources, so we return 0 here always.
    @Override
    double getBaseTime() {
      return 0;
    }

    EventRecord getNextResource() {
      int i = currentResource;
      currentResource++;
      assertTrue("There is no next resource at index: " + i + "!",
          i < recordsReceived.size());
      return recordsReceived.get(i);
    }

    int getRecordCount() {
      return recordsReceived.size();
    }

    @Override
    void onEventRecord(EventRecord record) {
      recordsReceived.add(record);
    }
  }

  // Equality comparisons with doubles is iffy. 0.1ms is good enough.
  private static double errorMargin = 0.1;

  /**
   * Parses an inspector Add record, sends it to the converter and returns the
   * JSO payload of the inspector record so we can use timing information in
   * that to match against the output of the converter.
   */
  private static AddResource addRecord(String inspectorRecord,
      LegacyInspectorResourceConverter converter) {
    PageEvent add = JSON.parse(inspectorRecord).cast();
    JavaScriptObject payload = getResourcePayload(add);
    converter.onAddResource(getResourceId(add), payload);
    return payload.cast();
  }

  private static native int getResourceId(PageEvent evt) /*-{
    return evt[1];
  }-*/;

  private static native JavaScriptObject getResourcePayload(PageEvent evt) /*-{
    return evt[2];
  }-*/;

  /**
   * Parses an inspector Update record, sends it to the converter and returns
   * the JSO payload of the inspector record so we can use timing information in
   * that to match against the output of the converter.
   */
  private static UpdateResource updateRecord(String inspectorRecord,
      LegacyInspectorResourceConverter converter) {
    PageEvent add = JSON.parse(inspectorRecord).cast();
    JavaScriptObject payload = getResourcePayload(add);
    converter.onUpdateResource(getResourceId(add), payload);
    return payload.cast();
  }

  @Override
  public String getModuleName() {
    return "com.google.speedtracer.Common";
  }

  /**
   * Tests that we correctly generate the record checkpoints for a series of
   * inspector style resources which group both the start time and the response
   * time in one updateResource record. This also happens to be a main resource
   * so it should synthesize a page transition via a TabChange record.
   */
  public void testConvertToCheckpoints() {
    String[] resource = {
        "[\"addResource\",1,{\"requestHeaders\":{},\"requestURL\":\"http://www.cnn.com/\",\"host\":\"www.cnn.com\",\"path\":\"/\",\"lastPathComponent\":\"\",\"isMainResource\":true,\"cached\":false,\"requestMethod\":\"GET\"}]",
        "[\"updateResource\",1,{\"mimeType\":\"text/html\",\"suggestedFilename\":\"\",\"expectedContentLength\":-1,\"statusCode\":200,\"responseHeaders\":{},\"didResponseChange\":true,\"type\":0,\"didTypeChange\":true,\"startTime\":1258059646.920131,\"responseReceivedTime\":1258059646.942131,\"didTimingChange\":true}]",
        "[\"updateResource\",1,{\"failed\":false,\"finished\":true,\"didCompletionChange\":true,\"startTime\":1258059646.920131,\"responseReceivedTime\":1258059646.942131,\"endTime\":1258059646.975131,\"didTimingChange\":true}]"};

    MockDevToolsDataProxy proxy = new MockDevToolsDataProxy(1);
    LegacyInspectorResourceConverter converter = new LegacyInspectorResourceConverter(
        proxy);

    // Send an addResource inspector style resource.
    AddResource add = addRecord(resource[0], converter);

    // Send the update that has the starttime and response. This is also a main
    // record.
    UpdateResource update = updateRecord(resource[1], converter);

    // The id is <redirectCount>-<resourceId>
    String expectedResourceId = "0-1";

    // Both the start and response records should have fired, as well as a
    // TabChanged record (page transition since this is a main resource).
    assertEquals("Wrong record total", 3, proxy.getRecordCount());
    // We should see a start checkpoint.
    checkOutputRecord(expectedResourceId, NetworkResourceStart.TYPE,
        update.getStartTime(), proxy.getNextResource());
    // We should also see a response
    checkOutputRecord(expectedResourceId, NetworkResourceResponse.TYPE,
        update.getResponseReceivedTime(), proxy.getNextResource());

    // We should also see a page transition with a time at the response received
    // time of the update.
    TabChange change = proxy.getNextResource().cast();
    assertEquals("Wrong Type", TabChange.TYPE, change.getType());
    assertEquals("Wrong Time", update.getResponseReceivedTime() * 1000,
        change.getTime(), errorMargin);
    assertEquals("Wrong URL", add.getRequestUrl(), change.getUrl());

    // send the finish
    update = updateRecord(resource[2], converter);
    assertEquals("Wrong record total", 4, proxy.getRecordCount());
    checkOutputRecord(expectedResourceId, NetworkResourceFinished.TYPE,
        update.getEndTime(), proxy.getNextResource());
  }

  /**
   * Tests that we correctly generate the record checkpoints for a series of
   * inspector style resources which contain a redirect (response code 302).
   */
  public void testRedirectCheckpoints() {
    // Resource that contains a redirect.
    String[] redirectResource = {
        "[\"addResource\",34,{\"requestHeaders\":{},\"requestURL\":\"http://ads.cnn.com/event.ng/Type=count&ClientType=2&ASeg=&AMod=&AdID=361252&FlightID=258382&TargetID=77823&SiteID=1588&EntityDefResetFlag=0&Segments=730,2247,2743,2823,3285,3430,9496,9779,9781,9853,10381,13086,14366,16113,17173,17251,18517,18982,19419,20139,21497,23634,26111,29652,30188,30220,30268,30550,30645,30907,30932,31037,31086,31121,31342,31782,32004,32108,32111,32273,32316,32367,32400,32411,32429,32479,32488,32492,32530,32539&Targets=71646,1515,76966,77823,78191&Values=30,60,84,100,150,682,686,917,1163,1285,1588,1601,1604,1678,1690,1696,2677,2746,4445,44138,47457,51073,52263,52897,56058,56872,57005,58702,58920,59986&RawValues=NGUSERID%2Caa57093-30656-1257976597-3%2CTIELID%2C9927469508521&random=bNWgIsb,bfpyKNbctRgs\",\"host\":\"ads.cnn.com\",\"path\":\"/event.ng/Type=count&ClientType=2&ASeg=&AMod=&AdID=361252&FlightID=258382&TargetID=77823&SiteID=1588&EntityDefResetFlag=0&Segments=730,2247,2743,2823,3285,3430,9496,9779,9781,9853,10381,13086,14366,16113,17173,17251,18517,18982,19419,20139,21497,23634,26111,29652,30188,30220,30268,30550,30645,30907,30932,31037,31086,31121,31342,31782,32004,32108,32111,32273,32316,32367,32400,32411,32429,32479,32488,32492,32530,32539&Targets=71646,1515,76966,77823,78191&Values=30,60,84,100,150,682,686,917,1163,1285,1588,1601,1604,1678,1690,1696,2677,2746,4445,44138,47457,51073,52263,52897,56058,56872,57005,58702,58920,59986&RawValues=NGUSERID%2Caa57093-30656-1257976597-3%2CTIELID%2C9927469508521&random=bNWgIsb,bfpyKNbctRgs\",\"lastPathComponent\":\"Type=count&ClientType=2&ASeg=&AMod=&AdID=361252&FlightID=258382&TargetID=77823&SiteID=1588&EntityDefResetFlag=0&Segments=730,2247,2743,2823,3285,3430,9496,9779,9781,9853,10381,13086,14366,16113,17173,17251,18517,18982,19419,20139,21497,23634,26111,29652,30188,30220,30268,30550,30645,30907,30932,31037,31086,31121,31342,31782,32004,32108,32111,32273,32316,32367,32400,32411,32429,32479,32488,32492,32530,32539&Targets=71646,1515,76966,77823,78191&Values=30,60,84,100,150,682,686,917,1163,1285,1588,1601,1604,1678,1690,1696,2677,2746,4445,44138,47457,51073,52263,52897,56058,56872,57005,58702,58920,59986&RawValues=NGUSERID%2Caa57093-30656-1257976597-3%2CTIELID%2C9927469508521&random=bNWgIsb,bfpyKNbctRgs\",\"isMainResource\":false,\"cached\":false,\"requestMethod\":\"GET\"}]",
        "[\"updateResource\",34,{\"startTime\":1258059647.383131,\"didTimingChange\":true}]",
        "[\"updateResource\",34,{\"url\":\"http://i.cdn.turner.com/cnn/images/1.gif\",\"domain\":\"i.cdn.turner.com\",\"path\":\"/cnn/images/1.gif\",\"lastPathComponent\":\"1.gif\",\"requestHeaders\":{},\"mainResource\":false,\"requestMethod\":\"GET\",\"requestFormData\":\"\",\"didRequestChange\":true,\"mimeType\":\"text/html\",\"suggestedFilename\":\"\",\"expectedContentLength\":-1,\"statusCode\":302,\"responseHeaders\":{},\"didResponseChange\":true,\"type\":2,\"didTypeChange\":true,\"startTime\":1258059647.596131,\"didTimingChange\":true}]",
        "[\"updateResource\",34,{\"mimeType\":\"image/gif\",\"suggestedFilename\":\"\",\"expectedContentLength\":43,\"statusCode\":200,\"responseHeaders\":{},\"didResponseChange\":true,\"type\":2,\"didTypeChange\":true,\"startTime\":1258059647.596131,\"responseReceivedTime\":1258059647.812131,\"didTimingChange\":true}]",
        "[\"updateResource\",34,{\"failed\":false,\"finished\":true,\"didCompletionChange\":true,\"startTime\":1258059647.596131,\"responseReceivedTime\":1258059647.812131,\"endTime\":1258059647.817131,\"didTimingChange\":true}]"};

    // Resource the we use to interleave with the redirect to make sure
    // redirecting doesnt screw with ID generation.
    String[] interleavedResource = {
        "[\"addResource\",1,{\"requestHeaders\":{},\"requestURL\":\"http://www.cnn.com/\",\"host\":\"www.cnn.com\",\"path\":\"/\",\"lastPathComponent\":\"\",\"isMainResource\":false,\"cached\":false,\"requestMethod\":\"GET\"}]",
        "[\"updateResource\",1,{\"startTime\":1258059646.920131,\"didTimingChange\":true}]",
        "[\"updateResource\",1,{\"mimeType\":\"text/html\",\"suggestedFilename\":\"\",\"expectedContentLength\":-1,\"statusCode\":200,\"responseHeaders\":{},\"didResponseChange\":true,\"type\":0,\"didTypeChange\":true,\"startTime\":1258059646.920131,\"responseReceivedTime\":1258059646.942131,\"didTimingChange\":true}]",
        "[\"updateResource\",1,{\"failed\":false,\"finished\":true,\"didCompletionChange\":true,\"startTime\":1258059646.920131,\"responseReceivedTime\":1258059646.942131,\"endTime\":1258059646.975131,\"didTimingChange\":true}]"};

    MockDevToolsDataProxy proxy = new MockDevToolsDataProxy(1);
    LegacyInspectorResourceConverter converter = new LegacyInspectorResourceConverter(
        proxy);

    // Send an addResource inspector style resource.
    addRecord(redirectResource[0], converter);

    // Send an update that should trigger a start.
    UpdateResource update = updateRecord(redirectResource[1], converter);

    // The id is <redirectCount>-<resourceId>
    String expectedResourceId = "0-34";

    // just a start is expected so far
    assertEquals("Wrong record total", 1, proxy.getRecordCount());
    checkOutputRecord(expectedResourceId, NetworkResourceStart.TYPE,
        update.getStartTime(), proxy.getNextResource());

    // Interleave a resource so that we can ensure the id's aren't destroyed
    // later.
    addRecord(interleavedResource[0], converter);
    UpdateResource interleavedUpdate = updateRecord(interleavedResource[1],
        converter);
    String interleavedResourceId = "0-1";

    // We expect just the start for the interleaved resource.
    assertEquals("Wrong record total", 2, proxy.getRecordCount());
    checkOutputRecord(interleavedResourceId, NetworkResourceStart.TYPE,
        interleavedUpdate.getStartTime(), proxy.getNextResource());

    // Send in a resource that should be a 302 indicating a redirect.
    update = updateRecord(redirectResource[2], converter);

    // We should see a response, a finished, and then a start for the new
    // resource.
    assertEquals("Wrong record total", 5, proxy.getRecordCount());
    checkOutputRecord(expectedResourceId, NetworkResourceResponse.TYPE,
        update.getStartTime(), proxy.getNextResource());
    checkOutputRecord(expectedResourceId, NetworkResourceFinished.TYPE,
        update.getStartTime(), proxy.getNextResource());

    // We update the expected resource id for the redirect
    expectedResourceId = "1-34";
    checkOutputRecord(expectedResourceId, NetworkResourceStart.TYPE,
        update.getStartTime(), proxy.getNextResource());

    // Finish the rest of the Interleaved resource so that we can ensure the
    // id's aren't destroyed.
    interleavedUpdate = updateRecord(interleavedResource[2], converter);

    assertEquals("Wrong record total", 6, proxy.getRecordCount());
    checkOutputRecord(interleavedResourceId, NetworkResourceResponse.TYPE,
        interleavedUpdate.getResponseReceivedTime(), proxy.getNextResource());

    // Send the finish for the interleaved
    interleavedUpdate = updateRecord(interleavedResource[3], converter);

    assertEquals("Wrong record total", 7, proxy.getRecordCount());
    checkOutputRecord(interleavedResourceId, NetworkResourceFinished.TYPE,
        interleavedUpdate.getEndTime(), proxy.getNextResource());

    // Now finish off the Redirect. We send the response and finish for the rest
    // of it.

    // Send in a response
    update = updateRecord(redirectResource[3], converter);

    assertEquals("Wrong record total", 8, proxy.getRecordCount());
    checkOutputRecord(expectedResourceId, NetworkResourceResponse.TYPE,
        update.getResponseReceivedTime(), proxy.getNextResource());

    // Send in a finish
    update = updateRecord(redirectResource[4], converter);

    assertEquals("Wrong record total", 9, proxy.getRecordCount());
    checkOutputRecord(expectedResourceId, NetworkResourceFinished.TYPE,
        update.getEndTime(), proxy.getNextResource());
  }

  /**
   * Tests that we correctly generate the record checkpoints for a series of
   * inspector style resources. Start comes in its own update. So does response
   * received and finish. This is NOT a main resource.
   */
  public void testSimpleConvertToCheckpoints() {
    String[] resource = {
        "[\"addResource\",1,{\"requestHeaders\":{},\"requestURL\":\"http://www.cnn.com/\",\"host\":\"www.cnn.com\",\"path\":\"/\",\"lastPathComponent\":\"\",\"isMainResource\":false,\"cached\":false,\"requestMethod\":\"GET\"}]",
        "[\"updateResource\",1,{\"startTime\":1258059646.920131,\"didTimingChange\":true}]",
        "[\"updateResource\",1,{\"mimeType\":\"text/html\",\"suggestedFilename\":\"\",\"expectedContentLength\":-1,\"statusCode\":200,\"responseHeaders\":{},\"didResponseChange\":true,\"type\":0,\"didTypeChange\":true,\"startTime\":1258059646.920131,\"responseReceivedTime\":1258059646.942131,\"didTimingChange\":true}]",
        "[\"updateResource\",1,{\"failed\":false,\"finished\":true,\"didCompletionChange\":true,\"startTime\":1258059646.920131,\"responseReceivedTime\":1258059646.942131,\"endTime\":1258059646.975131,\"didTimingChange\":true}]"};

    MockDevToolsDataProxy proxy = new MockDevToolsDataProxy(1);
    LegacyInspectorResourceConverter converter = new LegacyInspectorResourceConverter(
        proxy);

    // The id is <redirectCount>-<resourceId>
    String expectedResourceId = "0-1";

    // Send an addResource inspector style resource.
    addRecord(resource[0], converter);

    // Send the update that has the starttime.
    UpdateResource update = updateRecord(resource[1], converter);

    assertEquals("Wrong record total", 1, proxy.getRecordCount());
    // We should see a start checkpoint.
    checkOutputRecord(expectedResourceId, NetworkResourceStart.TYPE,
        update.getStartTime(), proxy.getNextResource());

    // send the response
    update = updateRecord(resource[2], converter);
    assertEquals("Wrong record total", 2, proxy.getRecordCount());
    checkOutputRecord(expectedResourceId, NetworkResourceResponse.TYPE,
        update.getResponseReceivedTime(), proxy.getNextResource());

    // send the finish
    update = updateRecord(resource[3], converter);
    assertEquals("Wrong record total", 3, proxy.getRecordCount());
    checkOutputRecord(expectedResourceId, NetworkResourceFinished.TYPE,
        update.getEndTime(), proxy.getNextResource());
  }

  private void checkOutputRecord(String id, int type, double time,
      EventRecord record) {
    assertEquals("Wrong Type", type, record.getType());
    assertEquals("Wrong Time", time * 1000, record.getTime(), errorMargin);
    assertEquals("Wrong Resource ID", id,
        record.<NetworkResourceRecord> cast().getResourceId());
  }
}
