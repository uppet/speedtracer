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

package com.google.speedtracer.hintletengine.client.rules;

import com.google.gwt.coreext.client.JSOArray;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.speedtracer.client.model.EventRecord;

/**
 * Tests {@link HintletGwtDetect}.
 */
public class HintletGwtDetectTests extends GWTTestCase {

  @Override
  public String getModuleName() {
    return "com.google.speedtracer.hintletengine.HintletEngineTest";
  }

  public void testNonCacheableNoHint() {
    HintletTestHelper.runTest(new HintletGwtDetect(), getCaseNonCacheableNoHint());
  }

  public void testNonCacheableWithHints() {
    HintletTestHelper.runTest(new HintletGwtDetect(), getCaseNonCacheableWithHints());
  }

  public void testDownloadSizeWithHints() {
    HintletTestHelper.runTest(new HintletGwtDetect(), getCaseDownloadSizeWithHints());
  }

  private native static EventRecord tabChanged(String url, int sequence)/*-{
  return {
      "data" : {
          "url" : url,
      },
      "type" : @com.google.speedtracer.shared.EventRecordType::TAB_CHANGED,
      "time" : sequence,
      "sequence" : sequence
  };
}-*/;

  private native static EventRecord resourceSendRequest(int identifier, String url, int sequence)/*-{
    return {
        "data" : {
            "identifier" : identifier,
            "url" : url,
            "requestMethod" : "GET"
        },
        "type" : @com.google.speedtracer.shared.EventRecordType::RESOURCE_SEND_REQUEST,
        "time" : sequence,
        "sequence" : sequence
    };
  }-*/;

  private native static EventRecord resourceReceiveResponse(int identifier, int sequence)/*-{
    return {
        "data" : {
            "identifier" : identifier,
            "statusCode" : 200,
            "mimeType" : "application/x-javascript"
        },
        "children" : [],
        "type" : @com.google.speedtracer.shared.EventRecordType::RESOURCE_RECEIVE_RESPONSE,
        "duration" : 0.029052734375,
        "time" : sequence,
        "sequence" : sequence
    };
  }-*/;

  private native static EventRecord resourceDataReceived(int identifier, int sequence)/*-{
    return {
        "data" : {
          "identifier" : identifier
        },
        "children" : [],
        "type" : @com.google.speedtracer.shared.EventRecordType::RESOURCE_DATA_RECEIVED,
        "duration" : 0.02001953125,
        "time" : sequence,
        "sequence" : sequence
    };
  }-*/;

  private native static EventRecord networkResponseReceived(int identifier, int sequence,
      String date, String expires, String cacheControl)/*-{
    var event = {
        "sequence" : sequence,
        "data" : {
            "response" : {
                "statusText" : "OK",
                "fromDiskCache" : false,
                "connectionReused" : true,
                "connectionId" : 751769,
                "status" : 200,
                "headers" : {
                    "Content-Length" : "2349",
                    "Accept-Ranges" : "bytes",

                    "Connection" : "Keep-Alive"
                }
            },
            "identifier" : identifier
        },
        "time" : sequence,
        "type" : @com.google.speedtracer.shared.EventRecordType::NETWORK_RESPONSE_RECEIVED
    };

    if (date != null) {
      event.data.response.headers["Date"] = date;
    }

    if (expires != null) {
      event.data.response.headers["Expires"] = expires;
    }

    if (cacheControl != null) {
      event.data.response.headers["Cache-Control"] = cacheControl;
    }

    return event;
  }-*/;

  private native static EventRecord resourceFinish(int identifier, int sequence)/*-{
    return {
        "data" : {
            "identifier" : identifier,
            "didFail" : false
        },
        "type" : @com.google.speedtracer.shared.EventRecordType::RESOURCE_FINISH,
        "time" : sequence,
        "sequence" : sequence
    };
  }-*/;

  private native static EventRecord networkDataReceived(int identifier, int sequence, int dataLength)/*-{
    return {
        "type" : @com.google.speedtracer.shared.EventRecordType::NETWORK_DATA_RECEIVED,
        "time" : sequence,
        "data" : {
            "identifier" : identifier,
            "dataLength" : dataLength
        },
        "sequence" : sequence
    };
  }-*/;

  /**
   * Get a sequence of events. When {@code selectionScriptNonCacheable} is {@code false},
   * {@code nocache.js} is NOT explicitly non-cacheable and a hint will be fired. When
   * {@code strongNameDataLength1} or {@code strongNameDataLength2} is large enough, strong name
   * fetches will trigger hint for large download size.
   * 
   * @param selectionScriptNonCacheable Selection script is explicitly non-cacheable if
   *          {@code selectionScriptNonCacheable} is true
   * @param strongNameDataLength1 the data length of the first string name fetch.
   * @param strongNameDataLength2 the data length of the second string name fetch.
   * @return
   */
  private static JSOArray<EventRecord> getInputs(boolean selectionScriptNonCacheable,
      int strongNameDataLength1, int strongNameDataLength2) {
    final int hostPageId = 1;
    final String hostPageUrl = "https://www.efgh.com/index.html";
    final int imageId = 2;
    final String imageUrl = "https://www.efgh.com/log.png";
    final int selectionScriptId = 3;
    final String selectionScriptUrl =
        "https://www.efgh.com/gwt.publichome/gwt.publichome.nocache.js";
    String selectionScriptDate = null;
    String selectionScriptExpires = null;
    String selectionScriptCacheControl = null;
    if (selectionScriptNonCacheable) {
      selectionScriptDate = "Wed, 20 Jul 2011 14:04:21 GMT";
      selectionScriptExpires = "Wed, 20 Jul 2011 14:04:21 GMT";
      selectionScriptCacheControl = "no-cache";
    }
    final int strongNameID1 = 4;
    final String strongNameUrl1 =
        "https:/www.efgh.com/gwt.publichome/8E82EC6A261B0BE8394B9AC1BB68A7A9.cache.html";
    final int strongNameID2 = 5;
    final String strongNameUrl2 =
        "https:/www.efgh.com/gwt.publichome/9E82AC6A261B0BE8394B9AC1BB68A7AE.cache.html";
    final String strongNameDate = "Wed, 20 Jul 2011 14:04:22 GMT";
    final String strongNameExpires = "Thu, 19 Jul 2012 14:04:22 GMT";
    final String strongNameCacheControl =
        "public,max-age=31536000,post-check=31536000,pre-check=31536000";

    int sequence = 1;
    JSOArray<EventRecord> inputs = JSOArray.create();
    inputs.push(tabChanged("https:/www.efgh.com", sequence++));
    // Host page
    inputs.push(resourceSendRequest(hostPageId, hostPageUrl, sequence++));
    inputs.push(resourceReceiveResponse(hostPageId, sequence++));
    inputs.push(resourceFinish(hostPageId, sequence++));
    // GWT selection script
    inputs.push(resourceSendRequest(selectionScriptId, selectionScriptUrl, sequence++));
    inputs.push(resourceReceiveResponse(selectionScriptId, sequence++));
    // GWT selection script. Set explicitly non-cacheable here
    inputs.push(networkResponseReceived(selectionScriptId, sequence++, selectionScriptDate,
        selectionScriptExpires, selectionScriptCacheControl));
    // An unrelated interleaved request, just as in real life
    inputs.push(resourceSendRequest(imageId, imageUrl, sequence++));
    inputs.push(resourceReceiveResponse(imageId, sequence++));
    inputs.push(resourceFinish(imageId, sequence++));
    // Back to GWT selection script
    inputs.push(resourceDataReceived(selectionScriptId, sequence++));
    inputs.push(resourceFinish(selectionScriptId, sequence++));
    // Strong name fetch 1
    inputs.push(resourceSendRequest(strongNameID1, strongNameUrl1, sequence++));
    inputs.push(resourceReceiveResponse(strongNameID1, sequence++));
    inputs.push(networkResponseReceived(strongNameID1, sequence++, strongNameDate,
        strongNameExpires, strongNameCacheControl));
    inputs.push(resourceDataReceived(strongNameID1, sequence++));
    inputs.push(networkDataReceived(strongNameID1, sequence++, strongNameDataLength1));
    inputs.push(resourceFinish(strongNameID1, sequence++));
    // Strong name fetch 2
    inputs.push(resourceSendRequest(strongNameID2, strongNameUrl2, sequence++));
    inputs.push(resourceReceiveResponse(strongNameID2, sequence++));
    inputs.push(networkResponseReceived(strongNameID2, sequence++, strongNameDate,
        strongNameExpires, strongNameCacheControl));
    inputs.push(resourceDataReceived(strongNameID2, sequence++));
    inputs.push(networkDataReceived(strongNameID2, sequence++, strongNameDataLength2));
    inputs.push(resourceFinish(strongNameID2, sequence++));
    return inputs;
  }

  private native static HintletTestCase getCaseNonCacheableNoHint()/*-{
    return {
        "inputs" : @com.google.speedtracer.hintletengine.client.rules.HintletGwtDetectTests::getInputs(ZII)
                   (true, 1000, 1000),
        "expectedHints" : []
    };
  }-*/;

  private native static HintletTestCase getCaseNonCacheableWithHints()/*-{
    return {
        "inputs" : @com.google.speedtracer.hintletengine.client.rules.HintletGwtDetectTests::getInputs(ZII)
                   (false, 1000, 1000),
        "expectedHints" : [
            {
                "hintletRule" : "GWT Application Detection",
                "timestamp" : 6,
                "description" : "GWT selection script '.nocache.js' file should be set as non-cacheable",
                "refRecord" : 12,
                "severity" : 1
            },
        ]
    };
  }-*/;

  private native static HintletTestCase getCaseDownloadSizeWithHints()/*-{
    return {
        "inputs" : @com.google.speedtracer.hintletengine.client.rules.HintletGwtDetectTests::getInputs(ZII)
                   (true, 1342730, 1342730),
        "expectedHints" : [
            {
                "description" : "The size of the initial GWT download"
                                + " (https:/www.efgh.com/gwt.publichome/8E82EC6A261B0BE8394B9AC1BB68A7A9.cache.html)"
                                + " is 1342730 bytes.  Consider using GWT.runAsync() code splitting and the Compile Report to"
                                + " reduce the size of the initial download.",
                "hintletRule" : "GWT Application Detection",
                "refRecord" : 18,
                "severity" : 1,
                "timestamp" : 14
            },
            {
                "description" : "The size of the initial GWT download"
                                + " (https:/www.efgh.com/gwt.publichome/9E82AC6A261B0BE8394B9AC1BB68A7AE.cache.html)"
                                + " is 1342730 bytes.  Consider using GWT.runAsync() code splitting and the Compile Report to"
                                + " reduce the size of the initial download.",
                "hintletRule" : "GWT Application Detection",
                "refRecord" : 24,
                "severity" : 1,
                "timestamp" : 20
            }
        ]
    };
  }-*/;

}
