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

  private native static EventRecord resourceSendRequest(int identifier, int sequence, String url)/*-{
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

  private native static EventRecord resourceReceiveResponse(int identifier, int sequence,
      String mimeType)/*-{
    return {
      "data" : {
        "identifier" : identifier,
        "statusCode" : 200,
        "mimeType" : mimeType
      },
      "children" : [],
      "type" : @com.google.speedtracer.shared.EventRecordType::RESOURCE_RECEIVE_RESPONSE,
      "duration" : 0.029052734375,
      "time" : sequence,
      "sequence" : sequence
    };
  }-*/;

  private native static EventRecord networkResponseReceived(int identifier, int sequence,
      JavaScriptObject headers)/*-{
    return {
      "sequence" : sequence,
      "data" : {
        "response" : {
          "statusText" : "OK",
          "fromDiskCache" : false,
          "connectionReused" : true,
          "connectionId" : 751769,
          "status" : 200,
          "headers" : headers
        },
        "identifier" : identifier
      },
      "time" : sequence,
      "type" : @com.google.speedtracer.shared.EventRecordType::NETWORK_RESPONSE_RECEIVED
    };
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
    final int identifier = 1;
    int sequence = 1;

    JSOArray<EventRecord> inputs = JSOArray.create();
    inputs.push(resourceSendRequest(identifier, sequence++, url));
    inputs.push(resourceReceiveResponse(identifier, sequence++, mimeType));
    inputs.push(networkResponseReceived(identifier, sequence++, headers));
    inputs.push(resourceFinish(identifier, sequence++));
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
