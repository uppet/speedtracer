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
import com.google.speedtracer.client.model.HintRecord;
import com.google.speedtracer.client.model.NetworkResponseReceivedEvent;
import com.google.speedtracer.hintletengine.client.HintletEventRecordBuilder;
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
  private static String FAVICON_TYPE = "image/vnd.microsoft.icon";

  private NetworkResponseReceivedEventBuilder responseBuilder;

  @Override
  protected void gwtSetUp() {
    responseBuilder =
        new NetworkResponseReceivedEventBuilder(HintletEventRecordBuilder.DEFAULT_ID,
            HintletEventRecordBuilder.DEFAULT_TIME, HintletEventRecordBuilder.DEFAULT_SEQUENCE);
    responseBuilder.setResponseHeaderDate(PAST_DATE);
    responseBuilder.setResponseStatus(CACHEABLE_RESPONSE);
    responseBuilder.setResponseFromDiskCache(false);
    responseBuilder.setResponseHeaderContentLength("6104");
  }

  @Override
  public String getModuleName() {
    return "com.google.speedtracer.hintletengine.HintletEngineTest";
  }
  
  public HintletTestCase createTestCase(String description,
      NetworkResponseReceivedEvent response, String url, HintRecord expectedHint) {
    JSOArray<EventRecord> inputs = JSOArray.create();
    inputs.push(HintletEventRecordBuilder.createResourceSendRequest(url));
    inputs.push(response);
    inputs.push(HintletEventRecordBuilder.createResourceFinish());
    if(expectedHint == null) {
      return HintletTestCase.createTestCase(description, inputs);
    } else {
      return HintletTestCase.createTestCase(description, inputs, expectedHint);
    }
  }

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

  private static HintRecord createHintRecord(String description, int severity) {
    return HintRecord.create("ResourceCaching", HintletEventRecordBuilder.DEFAULT_TIME, severity,
        description, HintletEventRecordBuilder.DEFAULT_SEQUENCE);
  }

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

  public void testVaryRuleAcceptable() {
    responseBuilder.setResponseHeaderVary("Accept-Encoding");
    responseBuilder.setResponseHeaderCacheControl("public");
    responseBuilder.setResponseHeaderContentType("image/gif");
    responseBuilder.setResponseHeaderExpires(FUTURE_DATE);
    responseBuilder.setResponseHeaderLastModified("Fri, 04 Sep 1998 11:43:00 GMT");
    runCacheTestNoHint("Vary tag is acceptable to IE", responseBuilder.getEvent(), DEFAULT_URL);
  }

  public void testVaryRuleGibberish() {
    responseBuilder.setResponseHeaderVary("Random Gibbersih");
    responseBuilder.setResponseHeaderCacheControl("public");
    responseBuilder.setResponseHeaderContentType("image/gif");
    responseBuilder.setResponseHeaderExpires(FUTURE_DATE);
    responseBuilder.setResponseHeaderLastModified("Fri, 04 Sep 1998 11:43:00 GMT");

    String hintDescription =
        "The following resources specify a 'Vary' header"
            + " that disables caching in most versions of Internet Explorer."
            + " Fix or remove the 'Vary' header for the following resources:"
            + " http://www.google.com";
    HintRecord hint = createHintRecord(hintDescription, HintRecord.SEVERITY_CRITICAL);

    runCacheTest(hintDescription, responseBuilder.getEvent(), DEFAULT_URL, hint);
  }

  public void testVaryRuleNoCache() {
    responseBuilder.setResponseHeaderCacheControl("private, no-cache");
    responseBuilder.setResponseHeaderContentType("image/gif");
    responseBuilder.setResponseHeaderExpires(FUTURE_DATE);
    responseBuilder.setResponseHeaderLastModified("Fri, 04 Sep 1998 11:43:00 GMT");
    runCacheTestNoHint("Junk Vary header and no cache", responseBuilder.getEvent(), DEFAULT_URL);
  }

  public void testVaryRulePastExpires() {
    responseBuilder.setResponseHeaderVary("Accept-Encoding");
    responseBuilder.setResponseHeaderCacheControl("public");
    responseBuilder.setResponseHeaderContentType("image/gif");
    responseBuilder.setResponseHeaderExpires("Wed, 17 Sep 1975 21:32:10 GMT");
    responseBuilder.setResponseHeaderLastModified("Fri, 04 Sep 1998 11:43:00 GMT");
    runCacheTestNoHint(
        "Acceptable Vary and past expiration", responseBuilder.getEvent(), DEFAULT_URL);
  }

  public void testVaryRuleSomeAcceptable() {
    responseBuilder.setResponseHeaderVary("Accept-Encoding, BOGUS");
    responseBuilder.setResponseHeaderCacheControl("public");
    responseBuilder.setResponseHeaderContentType("image/gif");
    responseBuilder.setResponseHeaderExpires(FUTURE_DATE);
    responseBuilder.setResponseHeaderLastModified("Fri, 04 Sep 1998 11:43:00 GMT");

    String hintDescription =
        "The following resources specify a 'Vary' header"
            + " that disables caching in most versions of Internet Explorer."
            + " Fix or remove the 'Vary' header for the following resources:"
            + " http://www.google.com";
    HintRecord expectedHint = createHintRecord(hintDescription, HintRecord.SEVERITY_CRITICAL);

    runCacheTest("Contains a Vary tag that should trigger hint", responseBuilder.getEvent(),
        DEFAULT_URL, expectedHint);
  }

  public void testFreshnessRuleShortFreshnessFromExpires() {
    responseBuilder.setResponseHeaderCacheControl("public");
    responseBuilder.setResponseHeaderContentType("image/png; charset=utf-8");
    responseBuilder.setResponseHeaderExpires("Tue, 08 Sep 1998 17:43:37 GMT");

    String hintDescription = "The following cacheable resources have a short"
        + " freshness lifetime. Specify an expiration at least one month"
        + " in the future for the following resources: http://www.google.com";
    HintRecord expectedHint = createHintRecord(hintDescription, HintRecord.SEVERITY_WARNING);

    runCacheTest("Marked cacheable but short freshness", responseBuilder.getEvent(), DEFAULT_URL,
        expectedHint);
  }

  public void testFreshnessRuleShortFreshnessFromMaxAge() {
    responseBuilder.setResponseHeaderCacheControl("public, max-age=1");
    responseBuilder.setResponseHeaderContentType("image/png; charset=utf-8");

    String hintDescription = "The following cacheable resources have a short"
        + " freshness lifetime. Specify an expiration at least one month"
        + " in the future for the following resources: http://www.google.com";
    HintRecord expectedHint = createHintRecord(hintDescription, HintRecord.SEVERITY_WARNING);

    runCacheTest(hintDescription, responseBuilder.getEvent(), DEFAULT_URL, expectedHint);
  }

  public void testFreshnessRuleMoreThanMonthFreshnessFromMaxAge() {
    double age = HintletCacheControl.SECONDS_IN_A_MONTH + 1;
    responseBuilder.setResponseHeaderCacheControl("public, max-age=" + age);
    responseBuilder.setResponseHeaderContentType("image/png; charset=utf-8");

    String hintDescription = "To further improve cache hit rate, specify an expiration"
        + " one year in the future for the following cacheable resources: http://www.google.com";
    HintRecord expectedHint = createHintRecord(hintDescription, HintRecord.SEVERITY_INFO);

    runCacheTest(
        "max-age more than a month", responseBuilder.getEvent(), DEFAULT_URL, expectedHint);
  }

  public void testFreshnessRuleMoreThanMonthFreshnessFromExpires() {
    responseBuilder.setResponseHeaderCacheControl("public");
    responseBuilder.setResponseHeaderContentType("image/png; charset=utf-8");
    responseBuilder.setResponseHeaderDate("Fri, 01 Jan 2010 00:00:00 GMT");
    responseBuilder.setResponseHeaderExpires("Mon, 01 Mar 2010 00:00:00 GMT");

    String hintDescription = "To further improve cache hit rate, specify an expiration"
        + " one year in the future for the following cacheable resources: http://www.google.com";
    HintRecord expectedHint = createHintRecord(hintDescription, HintRecord.SEVERITY_INFO);

    runCacheTest(
        "Expires a year after date", responseBuilder.getEvent(), DEFAULT_URL, expectedHint);
  }

  public void testFreshnessRuleYearExpires() {
    responseBuilder.setResponseHeaderCacheControl("public");
    responseBuilder.setResponseHeaderContentType("image/png; charset=utf-8");
    responseBuilder.setResponseHeaderDate("Fri, 01 Jan 2010 00:00:00 GMT");
    responseBuilder.setResponseHeaderExpires("Sat, 01 Jan 2011 00:00:00 GMT");
    runCacheTestNoHint("Expires a year after date", responseBuilder.getEvent(), DEFAULT_URL);
  }

  public void testFaviconRuleCachingNoExpires() {
    responseBuilder.setResponseHeaderCacheControl("private");
    responseBuilder.setResponseHeaderContentType(FAVICON_TYPE);

    String hintDescription = "Favicons should have an expiration at least one month"
        + " in the future: http://www.google.com";
    HintRecord expectedHint = createHintRecord(hintDescription, HintRecord.SEVERITY_WARNING);

    runCacheTest("Favicon without header and caching on", responseBuilder.getEvent(), DEFAULT_URL,
        expectedHint);
  }

  public void testFaviconRuleCachingFutureExpires() {
    responseBuilder.setResponseHeaderExpires(FUTURE_DATE);
    responseBuilder.setResponseHeaderCacheControl("private");
    responseBuilder.setResponseHeaderContentType(FAVICON_TYPE);
    runCacheTestNoHint(
        "Favicon with expiration in the future", responseBuilder.getEvent(), DEFAULT_URL);
  }

  public void testFaviconRuleCookieSet() {
    responseBuilder.setResponseHeaderSetCookie("TESTCOOKIE");
    responseBuilder.setResponseHeaderCacheControl("private");
    responseBuilder.setResponseHeaderContentType(FAVICON_TYPE);
    runCacheTestNoHint("Favicon with cookie set", responseBuilder.getEvent(), DEFAULT_URL);
  }

  public void testFaviconRuleShortExpiration() {
    responseBuilder.setResponseHeaderCacheControl("private");
    responseBuilder.setResponseHeaderContentType(FAVICON_TYPE);
    responseBuilder.setResponseHeaderDate("Mon, 07 Sep 1998 17:43:37 GMT");
    responseBuilder.setResponseHeaderExpires("Mon, 14 Sep 1998 17:43:37 GMT");

    String hintDescription = "Favicons should have an expiration at least one month"
        + " in the future: http://www.google.com";
    HintRecord expectedHint = createHintRecord(hintDescription, HintRecord.SEVERITY_WARNING);

    runCacheTest(
        "Favicon with short expiration", responseBuilder.getEvent(), DEFAULT_URL, expectedHint);
  }

  public void testQueryRuleCacheableWithQuery() {
    responseBuilder.setResponseHeaderCacheControl("public");
    responseBuilder.setResponseHeaderContentType("image/png");
    responseBuilder.setResponseHeaderExpires(FUTURE_DATE);
    String url = "http://www.google.com/image.png?param=1";

    String hintDescription = "Resources with a '?' in the URL are not cached by most"
        + " proxy caching servers. Remove the query string and encode the"
        + " parameters into the URL for the following resources: " + url;
    HintRecord expectedHint = createHintRecord(hintDescription, HintRecord.SEVERITY_WARNING);
    runCacheTest(
        "Cacheable resource has a ? in URL", responseBuilder.getEvent(), url, expectedHint);
  }

  public void testQueryRulePrivateCacheWithQuery() {
    responseBuilder.setResponseHeaderCacheControl("private");
    responseBuilder.setResponseHeaderContentType("image/png");
    responseBuilder.setResponseHeaderContentEncoding("gzip"); // added so Public Rule won't fire
    responseBuilder.setResponseHeaderExpires(FUTURE_DATE);
    String url = "http://www.google.com/image.png?param=1";
    runCacheTestNoHint("Has query but private caching", responseBuilder.getEvent(), url);
  }

  public void testQueryRulePublicCacheWithQueryAndCookie() {
    responseBuilder.setResponseHeaderCacheControl("public");
    responseBuilder.setResponseHeaderSetCookie("TESTCOOKIE");
    responseBuilder.setResponseHeaderContentType("image/png");
    responseBuilder.setResponseHeaderExpires(FUTURE_DATE);
    String url = "http://www.google.com/image.png?param=1";

    // does fire a rule, but should not fire the query related rule since
    // a cookie is set
    String hintDescription = "The following publicly cacheable resources contain"
        + " a Set-Cookie header. This security vulnerability can cause cookies"
        + " to be shared by multiple users: " + url;
    HintRecord expectedHint = createHintRecord(hintDescription, HintRecord.SEVERITY_CRITICAL);

    runCacheTest("Publicly cacheable with query and cookie."
        + " Does not fire query rule, but will fire cookie rule", responseBuilder.getEvent(), url,
        expectedHint);
  }

  public void testProxyBugRulePrivate() {
    responseBuilder.setResponseHeaderCacheControl("private");
    responseBuilder.setResponseHeaderContentType("image/png");
    responseBuilder.setResponseHeaderExpires(FUTURE_DATE);

    String hintDescription = "Consider adding a 'Cache-Control: public' header to the"
        + " following resource: http://www.google.com";
    HintRecord expectedHint = createHintRecord(hintDescription, HintRecord.SEVERITY_INFO);

    runCacheTest("cache-control private, could be public", responseBuilder.getEvent(), DEFAULT_URL,
        expectedHint);
  }

  public void testProxyBugRulePublic() {
    responseBuilder.setResponseHeaderCacheControl("public");
    responseBuilder.setResponseHeaderContentType("image/png");
    responseBuilder.setResponseHeaderExpires(FUTURE_DATE);
    runCacheTestNoHint("publicly cacheable", responseBuilder.getEvent(), DEFAULT_URL);
  }

  public void testProxyBugRuleContainsCooke() {
    responseBuilder.setResponseHeaderCacheControl("private");
    responseBuilder.setResponseHeaderContentType("image/png");
    responseBuilder.setResponseHeaderExpires(FUTURE_DATE);
    responseBuilder.setResponseHeaderSetCookie("TESTCOOKIE");
    runCacheTestNoHint("private but contains a cookie", responseBuilder.getEvent(), DEFAULT_URL);
  }

  public void testProxyBugRuleCompressedResource() {
    responseBuilder.setResponseHeaderCacheControl("private");
    responseBuilder.setResponseHeaderContentType("image/css");
    responseBuilder.setResponseHeaderContentEncoding("gzip");
    responseBuilder.setResponseHeaderExpires(FUTURE_DATE);
    runCacheTestNoHint("private but compressed resource", responseBuilder.getEvent(), DEFAULT_URL);
  }

  public void testProxyBugRulePublicGzip() {
    responseBuilder.setResponseHeaderCacheControl("public");
    responseBuilder.setResponseHeaderContentEncoding("gzip");
    responseBuilder.setResponseHeaderContentType("text/javascript; charset=utf-8");
    responseBuilder.setResponseHeaderExpires(FUTURE_DATE);

    String hintDescription = "Due to a bug, some proxy caching servers do not detect the presence"
      + " of the Content-Encoding response header. This can result in compressed"
      + " versions being delivered to client browsers that cannot properly"
      + " decompress the files. Therefore, use either 'Cache-Control: private'"
      + " or 'Vary: Accept-Encoding' for the following resource: http://www.google.com";
    HintRecord expectedHint = createHintRecord(hintDescription, HintRecord.SEVERITY_WARNING);

    runCacheTest("publicly cacheable with compressed data", responseBuilder.getEvent(), DEFAULT_URL,
        expectedHint);
  }

  public void testProxyBugRulePrivateGzip() {
    responseBuilder.setResponseHeaderCacheControl("private");
    responseBuilder.setResponseHeaderContentEncoding("gzip");
    responseBuilder.setResponseHeaderContentType("text/javascript; charset=utf-8");
    responseBuilder.setResponseHeaderExpires(FUTURE_DATE);
    runCacheTestNoHint(
        "privately cacheable with compressed data", responseBuilder.getEvent(), DEFAULT_URL);
  }

  public void testProxyBugRulePublicVary() {
    responseBuilder.setResponseHeaderCacheControl("public");
    responseBuilder.setResponseHeaderContentEncoding("gzip");
    responseBuilder.setResponseHeaderVary("Accept-Encoding");
    responseBuilder.setResponseHeaderContentType("text/javascript; charset=utf-8");
    responseBuilder.setResponseHeaderExpires(FUTURE_DATE);
    runCacheTestNoHint(
        "publicy cacheable with vary header", responseBuilder.getEvent(), DEFAULT_URL);
  }

  public void testProxyBugRulePublicCookie() {
    responseBuilder.setResponseHeaderCacheControl("public");
    responseBuilder.setResponseHeaderContentEncoding("gzip");
    responseBuilder.setResponseHeaderSetCookie("TESTCOOKIE");
    responseBuilder.setResponseHeaderContentType("text/javascript; charset=utf-8");
    responseBuilder.setResponseHeaderExpires(FUTURE_DATE);

    // does fire a rule, but should not fire a rule related to the proxy bug
    String hintDescription = "The following publicly cacheable resources contain"
        + " a Set-Cookie header. This security vulnerability can cause cookies"
        + " to be shared by multiple users: http://www.google.com";
    HintRecord expectedHint = createHintRecord(hintDescription, HintRecord.SEVERITY_CRITICAL);

    runCacheTest(
        "publicly cacheable with cookie", responseBuilder.getEvent(), DEFAULT_URL, expectedHint);
  }

  public void testCookieRulePublicWithCookie() {
    responseBuilder.setResponseHeaderCacheControl("public");
    responseBuilder.setResponseHeaderContentType("image/gif");
    responseBuilder.setResponseHeaderSetCookie("TESTCOOKIE");
    responseBuilder.setResponseHeaderDate("Mon, 07 Sep 1998 17:43:37 GMT");
    responseBuilder.setResponseHeaderExpires("Tue, 08 Jun 2020 17:43:37 GMT");
    responseBuilder.setResponseHeaderLastModified("Fri, 04 Sep 1998 00:00:00 GMT");

    String hintDescription = "The following publicly cacheable resources contain"
        + " a Set-Cookie header. This security vulnerability can cause cookies"
        + " to be shared by multiple users: http://www.google.com";
    HintRecord expectedHint = createHintRecord(hintDescription, HintRecord.SEVERITY_CRITICAL);

    runCacheTest("Contains a Set-Cookie along with Cache-Control: public header",
        responseBuilder.getEvent(), DEFAULT_URL, expectedHint);
  }

  public void testCookieRulePublicNoCookie() {
    responseBuilder.setResponseHeaderCacheControl("public");
    responseBuilder.setResponseHeaderContentType("image/gif");
    responseBuilder.setResponseHeaderDate("Mon, 07 Sep 1998 17:43:37 GMT");
    responseBuilder.setResponseHeaderExpires("Tue, 08 Jun 2020 17:43:37 GMT");
    responseBuilder.setResponseHeaderLastModified("Fri, 04 Sep 1998 00:00:00 GMT");
    runCacheTestNoHint(
        "public cache control but no cookie", responseBuilder.getEvent(), DEFAULT_URL);
  }

  public void testCookieRulePrivateCacheWithCooke() {
    responseBuilder.setResponseHeaderCacheControl("private");
    responseBuilder.setResponseHeaderContentType("image/gif");
    responseBuilder.setResponseHeaderDate("Mon, 07 Sep 1998 17:43:37 GMT");
    responseBuilder.setResponseHeaderExpires("Tue, 08 Jun 2020 17:43:37 GMT");
    responseBuilder.setResponseHeaderLastModified("Fri, 04 Sep 1998 00:00:00 GMT");
    responseBuilder.setResponseHeaderSetCookie("TESTCOOKIE");
    runCacheTestNoHint("No caching and cookie set", responseBuilder.getEvent(), DEFAULT_URL);
  }

}
