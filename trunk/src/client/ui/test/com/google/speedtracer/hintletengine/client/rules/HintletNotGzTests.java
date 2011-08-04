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
import com.google.speedtracer.hintletengine.client.NetworkResponseReceivedEventBuilder;
import static com.google.speedtracer.hintletengine.client.HintletEventRecordBuilder.createResourceSendRequest;
import static com.google.speedtracer.hintletengine.client.HintletEventRecordBuilder.createResourceFinish;
import static com.google.speedtracer.hintletengine.client.HintletEventRecordBuilder.createResourceReceiveResponse;
import static com.google.speedtracer.hintletengine.client.HintletEventRecordBuilder.createNetworkDataRecieved;

/**
 * Tests {@link HintletNotGz}.
 */
public class HintletNotGzTests extends GWTTestCase {

  @Override
  public String getModuleName() {
    return "com.google.speedtracer.hintletengine.HintletEngineTest";
  }

  public void testHtmlFileWithHint() {
    HintletTestHelper.runTest(new HintletNotGz(), getCaseHtmlFileWithHint());
  }

  public void testCssFileWithHint() {
    HintletTestHelper.runTest(new HintletNotGz(), getCaseCssFileWithHint());
  }

  public void testJavaScriptFileWithHint() {
    HintletTestHelper.runTest(new HintletNotGz(), getCaseJavaScriptFileWithHint());
  }
  
  public void testImageFileNoHint() {
    HintletTestHelper.runTest(new HintletNotGz(), getCaseImageFileNoHint());
  }
  
  public void testSmallHtmlFileNoHint() {
    HintletTestHelper.runTest(new HintletNotGz(), getCaseSmallHtmlFileNoHint());
  }
  
  public void testGzipedHtmlFileNoHint() {
    HintletTestHelper.runTest(new HintletNotGz(), getCaseGzipedHtmlFileNoHint());
  }
  
  public void testBzip2edHtmlFileNoHint() {
    HintletTestHelper.runTest(new HintletNotGz(), getCaseBzip2edHtmlFileNoHint());
  }
  
  private static EventRecord createNetworkResponseReceived(String identifier, int sequence,
      String contentType, String contentEncoding) {
    NetworkResponseReceivedEventBuilder builder =
        new NetworkResponseReceivedEventBuilder(identifier, sequence, sequence);
    builder.setResponseFromDiskCache(false)
      .setResponseStatus(200)
      .setResponseHeaderContentType(contentType);
    if (contentEncoding != null) {
      builder.setResponseHeaderContentEncoding(contentEncoding);
    }
    return builder.getEvent();
  }
  
  /**
   * Get a sequence of events for a single resource.
   * 
   * @param url The url of the resource
   * @param mimeType
   * @param contentEncoding if not {@code null}, "Content-Encoding" is added to response header.
   * @param totalDataLength resource size
   * @return a sequence of events
   */
  @SuppressWarnings("unused")
  private static JSOArray<EventRecord> getInputs(String url, String mimeType,
      String contentEncoding, int totalDataLength) {
    final String identifier = "1";
    int sequence = 1;

    JSOArray<EventRecord> inputs = JSOArray.create();
    inputs.push(createResourceSendRequest(identifier, url, sequence++));
    inputs.push(createResourceReceiveResponse(identifier, sequence++, mimeType));
    inputs.push(createNetworkResponseReceived(identifier, sequence++, mimeType, contentEncoding));
    // resource may be split and transfered via several events
    // use 2 network-data-received events here.
    // omit RESOURCE_DATA_RECEIVED event
    inputs.push(createNetworkDataRecieved(identifier, sequence++, totalDataLength / 2));
    inputs.push(createNetworkDataRecieved(identifier, sequence++, totalDataLength - totalDataLength / 2));
    inputs.push(createResourceFinish(identifier, sequence++));
    return inputs;
  }
  
  private native static HintletTestCase getCaseHtmlFileWithHint()/*-{
    return {
      "inputs" : @com.google.speedtracer.hintletengine.client.rules.HintletNotGzTests::getInputs(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;I)
                  ("http://example.org/foo.html", "text/html", null, 200),
      "expectedHints" : [
        {
          "hintletRule" : "Uncompressed Resource",
          "timestamp" : 2,
          "description" : "URL http://example.org/foo.html was not compressed with gzip or bzip2",
          "refRecord" : 6,
          "severity" : 3
        }
      ]
    };
  }-*/; 

  private native static HintletTestCase getCaseCssFileWithHint()/*-{
    return {
      "inputs" : @com.google.speedtracer.hintletengine.client.rules.HintletNotGzTests::getInputs(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;I)
                  ("http://example.org/foo.css", "text/css", null, 9875),
      "expectedHints" : [
        {
          "hintletRule" : "Uncompressed Resource",
          "timestamp" : 2,
          "description" : "URL http://example.org/foo.css was not compressed with gzip or bzip2",
          "refRecord" : 6,
          "severity" : 3
        }
      ]
    };
  }-*/; 

  private native static HintletTestCase getCaseJavaScriptFileWithHint()/*-{
    return {
      "inputs" : @com.google.speedtracer.hintletengine.client.rules.HintletNotGzTests::getInputs(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;I)
                  ("http://example.org/foo.js", "text/javascript", null, 9875),
      "expectedHints" : [
        {
          "hintletRule" : "Uncompressed Resource",
          "timestamp" : 2,
          "description" : "URL http://example.org/foo.js was not compressed with gzip or bzip2",
          "refRecord" : 6,
          "severity" : 3
        }
      ]
    };
  }-*/; 

  private native static HintletTestCase getCaseImageFileNoHint()/*-{
    return {
      "inputs" : @com.google.speedtracer.hintletengine.client.rules.HintletNotGzTests::getInputs(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;I)
                  ("http://example.org/foo.png", "image/png", null, 9875),
      "expectedHints" : []
    };
  }-*/; 

  private native static HintletTestCase getCaseSmallHtmlFileNoHint()/*-{
    return {
      "inputs" : @com.google.speedtracer.hintletengine.client.rules.HintletNotGzTests::getInputs(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;I)
                  ("http://example.org/foo.html", "text/html", null, 149),
      "expectedHints" : []
    };
  }-*/; 

  private native static HintletTestCase getCaseGzipedHtmlFileNoHint()/*-{
    return {
      "inputs" : @com.google.speedtracer.hintletengine.client.rules.HintletNotGzTests::getInputs(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;I)
                  ("http://example.org/foo.html", "text/html", "gzip", 6436),
      "expectedHints" : []
    };
  }-*/; 

  private native static HintletTestCase getCaseBzip2edHtmlFileNoHint()/*-{
    return {
      "inputs" : @com.google.speedtracer.hintletengine.client.rules.HintletNotGzTests::getInputs(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;I)
                  ("http://example.org/foo.html", "text/html", "bzip2", 6436),
      "expectedHints" : []
    };
  }-*/; 
  
}
