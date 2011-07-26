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

import com.google.gwt.junit.client.GWTTestCase;
import com.google.speedtracer.client.model.HintRecord;
import com.google.speedtracer.client.model.NetworkResponseReceivedEvent;
import com.google.speedtracer.client.model.ResourceFinishEvent;
import com.google.speedtracer.client.model.ResourceWillSendEvent;
import com.google.speedtracer.hintletengine.client.NetworkResponseReceivedEventBuilder;
import com.google.speedtracer.hintletengine.client.rules.HintletCacheControl;

/**
 * Tests {@link HintletCacheControl} HintletCacheControl has a set of rules for which it fires
 * hintlets. Each rule has an associated set of tests which are grouped accordingly. The methods are
 * named test or get (private method to get the test's resource data) plus a key word for the rule
 * being tested.
 */
public class HintletCacheControlTests extends GWTTestCase {

  @SuppressWarnings("unused")
  private static int CACHEABLE_RESPONSE = 200;
  private static int NON_CACHEABLE_RESPONSE = 404;
  private static String PAST_DATE = "Mon, 07 Sep 1998 17:43:35 GMT";
  private static String FUTURE_DATE = "Mon, 01 Jan 2030 00:00:00 GMT";
  private static String DEFAULT_URL = "http://www.google.com";
  
  private NetworkResponseReceivedEventBuilder responseBuilder;

  @Override
  protected void gwtSetUp() {
    responseBuilder = new NetworkResponseReceivedEventBuilder(1, 1, 1);
    responseBuilder.setResponseHeaderDate(PAST_DATE);
    responseBuilder.setResponseStatus(CACHEABLE_RESPONSE);
    responseBuilder.setResponseFromDiskCache(false);
    responseBuilder.setResponseHeaderContentLength("6104");
  }
  
  @Override
  public String getModuleName() {
    return "com.google.speedtracer.hintletengine.HintletEngine";
  }

  /**
   * Create a HintletTestCase using the default 
   * start and finish events, the given resource,
   * and the expectedHint
   */
  public native HintletTestCase createTestCase(String description,
      NetworkResponseReceivedEvent response, String url, HintRecord expectedHint) /*-{
    var testCase = {
        "description" : description,
        "inputs" : [
            @com.google.speedtracer.hintletengine.client.rules.HintletCacheControlTests::getResourceStart(Ljava/lang/String;)(url),
            response,
            @com.google.speedtracer.hintletengine.client.rules.HintletCacheControlTests::getResourceFinish()()
        ],
        "expectedHints" : []
    }
    if (expectedHint) {
      testCase.expectedHints.push(expectedHint);
    }
    return testCase;
  }-*/;
  
  /**
   * Run a test with a hint
   */
  private void runCacheTest(String description, NetworkResponseReceivedEvent responseEvent,
      String url, HintRecord expectedHint) {
    HintletTestCase test = createTestCase(description, responseEvent, url, expectedHint);
    HintletTestHelper.runTest(new HintletCacheControl(), test);
  }
  
  /**
   * Run a test with no expected hint
   */
  private void runCacheTestNoHint(
      String description, NetworkResponseReceivedEvent responseEvent, String url) {
    HintletTestCase test = createTestCase(description, responseEvent, url, null);
    HintletTestHelper.runTest(new HintletCacheControl(), test);
  }

  /**
   * Get the default start event
   */
  public static native ResourceWillSendEvent getResourceStart(String url) /*-{
    return {
        "type" : @com.google.speedtracer.shared.EventRecordType::RESOURCE_SEND_REQUEST,
        "time" : 1,
        "sequence" : 1,
        "data" : {
            "identifier" : 1,
            "url" : url,
            "requestMethod" : "GET"
        }
    };
  }-*/;

  /**
   * Get the default finish event. Necessary for hints to fire
   */
  @SuppressWarnings("unused")
  private static native ResourceFinishEvent getResourceFinish() /*-{
    return {
        "type" : @com.google.speedtracer.shared.EventRecordType::RESOURCE_FINISH,
        "time" : 1,
        "sequence" : 1,
        "data" : {
            "identifier" : 1,
            "didFail" : false,
            "networkTime" : 100
        }
    }
  }-*/;
  
  private static native HintRecord createHintRecord(String description, int severity) /*-{
    return {
        "hintletRule" : "ResourceCaching",
        "timestamp" : 1,
        "description" : description,
        "refRecrod" : 1,
        "severity" : severity
    };
  }-*/;

  public void testExpirationRuleMissingExpiration() {    
    responseBuilder.setResponseHeaderCacheControl("private");
    responseBuilder.setResponseHeaderContentEncoding("gzip");
    responseBuilder.setResponseHeaderContentType("text/css;charset=UTF-8");
        String hintDescription =
        "The following resources are missing a cache expiration."
            + " Resources that do not specify an expiration may not be cached by browsers."
            + " Specify an expiration at least one month in the future for resources that"
            + " should be cached, and an expiration in the past for resources that should not be cached:" + " http://www.google.com";
    HintRecord expectedHint = createHintRecord(hintDescription, HintRecord.SEVERITY_CRITICAL);
    runCacheTest("missing cache expiration", responseBuilder.getEvent(), DEFAULT_URL, expectedHint);
  }

  public void testExpirationRuleNoCache() {    
    responseBuilder.setResponseHeaderCacheControl("no-cache,no-store");
    responseBuilder.setResponseHeaderContentEncoding("gzip");
    responseBuilder.setResponseHeaderContentType("text/css;charset=UTF-8");
    runCacheTestNoHint("Marked explicitly not to cache", responseBuilder.getEvent(), DEFAULT_URL);
  }

  public void testExpirationRuleHasCookie() {    
    responseBuilder.setResponseHeaderCacheControl("private");
    responseBuilder.setResponseHeaderContentEncoding("gzip");
    responseBuilder.setResponseHeaderContentType("text/css;charset=UTF-8");
    responseBuilder.setResponseHeaderSetCookie("TESTCOOKIE");
    runCacheTestNoHint("has a cookie", responseBuilder.getEvent(), DEFAULT_URL);
  }

  public void testExpirationRuleNonCacheableResponse() {    
    responseBuilder.setResponseHeaderCacheControl("private");
    responseBuilder.setResponseHeaderContentEncoding("gzip");
    responseBuilder.setResponseHeaderContentType("text/css;charset=UTF-8");
    responseBuilder.setResponseStatus(NON_CACHEABLE_RESPONSE);
    runCacheTestNoHint("Non cacheable response", responseBuilder.getEvent(), DEFAULT_URL);
  }

  public void testExpirationRuleNonCacheableType() {    
    responseBuilder.setResponseHeaderCacheControl("private");
    responseBuilder.setResponseHeaderContentEncoding("gzip");
    responseBuilder.setResponseHeaderContentType("text/css;charset=UTF-8");
    responseBuilder.setResponseHeaderContentType("text/html; charset=UTF-8");
    runCacheTestNoHint("Non cacheable response", responseBuilder.getEvent(), DEFAULT_URL);
  }

  public void testExpirationRuleHasExpiration() {    
    responseBuilder.setResponseHeaderCacheControl("private");
    responseBuilder.setResponseHeaderContentEncoding("gzip");
    responseBuilder.setResponseHeaderContentType("text/css;charset=UTF-8");
    responseBuilder.setResponseHeaderExpires(FUTURE_DATE);
    runCacheTestNoHint("Has Expiration", responseBuilder.getEvent(), DEFAULT_URL);
  }
}
