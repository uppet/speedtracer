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

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.coreext.client.JSOArray;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.speedtracer.client.model.EventRecord;
import com.google.speedtracer.hintletengine.client.NetworkResponseReceivedEventBuilder;
import static com.google.speedtracer.hintletengine.client.HintletEventRecordBuilder.createResourceSendRequest;
import static com.google.speedtracer.hintletengine.client.HintletEventRecordBuilder.createResourceFinish;
import static com.google.speedtracer.hintletengine.client.HintletEventRecordBuilder.createResourceReceiveResponse;

/**
 * Tests {@link HintletStaticNoCookie}.
 */
public class HintletStaticNoCookieTests extends GWTTestCase {

  @Override
  public String getModuleName() {
    return "com.google.speedtracer.hintletengine.HintletEngineTest";
  }

  public void testImageNoCookieHeader() {
    HintletTestHelper.runTest(new HintletStaticNoCookie(), getCaseImageNoCookieHeader());
  }

  public void testImageWithCookieHeader() {
    HintletTestHelper.runTest(new HintletStaticNoCookie(), getCaseImageWithCookieHeader());
  }

  public void testHtmlWithCookieHeader() {
    HintletTestHelper.runTest(new HintletStaticNoCookie(), getCaseHtmlWithCookieHeader());
  }
  
  private static EventRecord createNetworkResponseReceived(String identifier, int sequence,
      JavaScriptObject headers) {
    NetworkResponseReceivedEventBuilder builder =
        new NetworkResponseReceivedEventBuilder(identifier, sequence, sequence);
    builder.setResponseFromDiskCache(false)
      .setResponseStatus(200)
      .setResponseHeaders(headers);
    return builder.getEvent();
  }
  

  /**
   * Get a sequence of events for a single resource.
   * 
   * @param url The url of the resource
   * @param mimeType
   * @param headers
   * @return a sequence of events
   */
  private static JSOArray<EventRecord> getInputs(String url, String mimeType,
      JavaScriptObject headers) {
    final String identifier = "1";
    int sequence = 1;

    JSOArray<EventRecord> inputs = JSOArray.create();
    inputs.push(createResourceSendRequest(identifier, url, sequence++));
    inputs.push(createResourceReceiveResponse(identifier, sequence++, mimeType));
    inputs.push(createNetworkResponseReceived(identifier, sequence++, headers));
    inputs.push(createResourceFinish(identifier, sequence++));
    return inputs;
  }

  private native static HintletTestCase getCaseImageNoCookieHeader()/*-{
    return {
      "description" : "Should not fire the rule because no Cookie header",
      "inputs" : @com.google.speedtracer.hintletengine.client.rules.HintletStaticNoCookieTests::getInputs(Ljava/lang/String;Ljava/lang/String;Lcom/google/gwt/core/client/JavaScriptObject;)
                  ("http://example.org/foo.png", "image/png", { "Content-Type" : "image/png" } ),
      "expectedHints" : []
    };
  }-*/;

  private native static HintletTestCase getCaseImageWithCookieHeader()/*-{
    return {
      "description" : "Cookie Header and type is image. Expect hint",
      "inputs" : @com.google.speedtracer.hintletengine.client.rules.HintletStaticNoCookieTests::getInputs(Ljava/lang/String;Ljava/lang/String;Lcom/google/gwt/core/client/JavaScriptObject;)
                  ("http://example.org/foo.png", "image/png", { "Cookie":"TESTCOOKIE", "Content-Type" : "image/png" } ),
      "expectedHints" : [
        {
          "hintletRule" : "Static Resource served from domains with cookies",
          "timestamp" : 2,
          "description" : "URL http://example.org/foo.png is static content that should be served from a domain that does not set cookies.  Found 18 extra bytes from cookie.",
          "refRecord" : 4,
          "severity" : 3
        }    
      ]
    };
  }-*/;

  private native static HintletTestCase getCaseHtmlWithCookieHeader()/*-{
    return {
      "description" : "Do not fire - Cookie Header but type is plain html",
      "inputs" : @com.google.speedtracer.hintletengine.client.rules.HintletStaticNoCookieTests::getInputs(Ljava/lang/String;Ljava/lang/String;Lcom/google/gwt/core/client/JavaScriptObject;)
                  ("http://example.org/foo.png", "text/html", { "Cookie":"TESTCOOKIE", "Content-Type" : "text/html"  } ),
      "expectedHints" : []
    };
  }-*/;
}
