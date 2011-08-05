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

  /**
   * Set default data for these tests
   */
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
 
  /**
   * Run a test with a hint
   * Using default start and end events
   */
  private void runCacheTest(
      NetworkResponseReceivedEvent responseEvent, String url, HintRecord expectedHint) {
    HintletTestCase test = HintletTestCase.getHintletTestCase();
    test.addInput(HintletEventRecordBuilder.createResourceSendRequest(url));
    test.addInput(responseEvent);
    test.addInput(HintletEventRecordBuilder.createResourceFinish());
    if (expectedHint != null) {
      test.addExpectedHint(expectedHint);
    }
    HintletTestHelper.runTest(new HintletCacheControl(), test);
  }

  /**
   * Run a test with no expected hint
   */
  private void runCacheTestNoHint(NetworkResponseReceivedEvent responseEvent, String url) {
    runCacheTest(responseEvent, url, null);
  }

  /**
   * Create a HintRecord with the default information for these tests
   */
  private static HintRecord createHintRecord(String description, int severity) {
    return HintRecord.create("ResourceCaching", HintletEventRecordBuilder.DEFAULT_TIME, severity,
        description, HintletEventRecordBuilder.DEFAULT_SEQUENCE);
  }

  /**
   * Expiration rule
   * missing cache expiration
   */
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
    runCacheTest(responseBuilder.getEvent(), DEFAULT_URL, expectedHint);
  }

  /**
   * Expiration rule
   * Marked explicitly not to cache
   */
  public void testExpirationRuleNoCache() {
    responseBuilder.setResponseHeaderCacheControl("no-cache,no-store");
    responseBuilder.setResponseHeaderContentEncoding("gzip");
    responseBuilder.setResponseHeaderContentType("text/css;charset=UTF-8");
    runCacheTestNoHint(responseBuilder.getEvent(), DEFAULT_URL);
  }

  /**
   * Expiration rule
   * has a cookie
   */
  public void testExpirationRuleHasCookie() {
    responseBuilder.setResponseHeaderCacheControl("private");
    responseBuilder.setResponseHeaderContentEncoding("gzip");
    responseBuilder.setResponseHeaderContentType("text/css;charset=UTF-8");
    responseBuilder.setResponseHeaderSetCookie("TESTCOOKIE");
    runCacheTestNoHint(responseBuilder.getEvent(), DEFAULT_URL);
  }

  /**
   * Expiration rule
   * Non cacheable response
   */
  public void testExpirationRuleNonCacheableResponse() {
    responseBuilder.setResponseHeaderCacheControl("private");
    responseBuilder.setResponseHeaderContentEncoding("gzip");
    responseBuilder.setResponseHeaderContentType("text/css;charset=UTF-8");
    responseBuilder.setResponseStatus(NON_CACHEABLE_RESPONSE);
    runCacheTestNoHint(responseBuilder.getEvent(), DEFAULT_URL);
  }

  /**
   * Expiration rule
   * Non cacheable response
   */
  public void testExpirationRuleNonCacheableType() {
    responseBuilder.setResponseHeaderCacheControl("private");
    responseBuilder.setResponseHeaderContentEncoding("gzip");
    responseBuilder.setResponseHeaderContentType("text/css;charset=UTF-8");
    responseBuilder.setResponseHeaderContentType("text/html; charset=UTF-8");
    runCacheTestNoHint(responseBuilder.getEvent(), DEFAULT_URL);
  }

  /**
   * Expiration rule
   * Has Expiration
   */
  public void testExpirationRuleHasExpiration() {
    responseBuilder.setResponseHeaderCacheControl("private");
    responseBuilder.setResponseHeaderContentEncoding("gzip");
    responseBuilder.setResponseHeaderContentType("text/css;charset=UTF-8");
    responseBuilder.setResponseHeaderExpires(FUTURE_DATE);
    runCacheTestNoHint(responseBuilder.getEvent(), DEFAULT_URL);
  }

  /**
   * Vary rule
   * Vary tag is acceptable to IE
   */
  public void testVaryRuleAcceptable() {
    responseBuilder.setResponseHeaderVary("Accept-Encoding");
    responseBuilder.setResponseHeaderCacheControl("public");
    responseBuilder.setResponseHeaderContentType("image/gif");
    responseBuilder.setResponseHeaderExpires(FUTURE_DATE);
    responseBuilder.setResponseHeaderLastModified("Fri, 04 Sep 1998 11:43:00 GMT");
    runCacheTestNoHint(responseBuilder.getEvent(), DEFAULT_URL);
  }

  /**
   * Vary Rule
   * Gibberish vary tag, public
   */
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

    runCacheTest(responseBuilder.getEvent(), DEFAULT_URL, hint);
  }

  /**
   * Vary rule
   * Junk Vary header and no cache
   */
  public void testVaryRuleNoCache() {
    responseBuilder.setResponseHeaderCacheControl("private, no-cache");
    responseBuilder.setResponseHeaderContentType("image/gif");
    responseBuilder.setResponseHeaderExpires(FUTURE_DATE);
    responseBuilder.setResponseHeaderLastModified("Fri, 04 Sep 1998 11:43:00 GMT");
    runCacheTestNoHint(responseBuilder.getEvent(), DEFAULT_URL);
  }

  /**
   * Vary rule
   * Acceptable Vary and past expiration
   */
  public void testVaryRulePastExpires() {
    responseBuilder.setResponseHeaderVary("Accept-Encoding");
    responseBuilder.setResponseHeaderCacheControl("public");
    responseBuilder.setResponseHeaderContentType("image/gif");
    responseBuilder.setResponseHeaderExpires("Wed, 17 Sep 1975 21:32:10 GMT");
    responseBuilder.setResponseHeaderLastModified("Fri, 04 Sep 1998 11:43:00 GMT");
    runCacheTestNoHint(responseBuilder.getEvent(), DEFAULT_URL);
  }

  /**
   * Vary rule
   * Contains a Vary tag that should trigger hint
   */
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

    runCacheTest(responseBuilder.getEvent(), DEFAULT_URL, expectedHint);
  }

  /**
   * Freshness rule
   * Marked cacheable but short freshness
   */
  public void testFreshnessRuleShortFreshnessFromExpires() {
    responseBuilder.setResponseHeaderCacheControl("public");
    responseBuilder.setResponseHeaderContentType("image/png; charset=utf-8");
    responseBuilder.setResponseHeaderExpires("Tue, 08 Sep 1998 17:43:37 GMT");

    String hintDescription = "The following cacheable resources have a short"
        + " freshness lifetime. Specify an expiration at least one month"
        + " in the future for the following resources: http://www.google.com";
    HintRecord expectedHint = createHintRecord(hintDescription, HintRecord.SEVERITY_WARNING);

    runCacheTest(responseBuilder.getEvent(), DEFAULT_URL, expectedHint);
  }

  /**
   * Freshness rule
   * Short freshness according to max-age - throws hint
   */
  public void testFreshnessRuleShortFreshnessFromMaxAge() {
    responseBuilder.setResponseHeaderCacheControl("public, max-age=1");
    responseBuilder.setResponseHeaderContentType("image/png; charset=utf-8");

    String hintDescription = "The following cacheable resources have a short"
        + " freshness lifetime. Specify an expiration at least one month"
        + " in the future for the following resources: http://www.google.com";
    HintRecord expectedHint = createHintRecord(hintDescription, HintRecord.SEVERITY_WARNING);

    runCacheTest(responseBuilder.getEvent(), DEFAULT_URL, expectedHint);
  }

  /**
   * Freshness rule
   * max-age more than a month
   */
  public void testFreshnessRuleMoreThanMonthFreshnessFromMaxAge() {
    double age = HintletCacheControl.SECONDS_IN_A_MONTH + 1;
    responseBuilder.setResponseHeaderCacheControl("public, max-age=" + age);
    responseBuilder.setResponseHeaderContentType("image/png; charset=utf-8");

    String hintDescription = "To further improve cache hit rate, specify an expiration"
        + " one year in the future for the following cacheable resources: http://www.google.com";
    HintRecord expectedHint = createHintRecord(hintDescription, HintRecord.SEVERITY_INFO);

    runCacheTest(responseBuilder.getEvent(), DEFAULT_URL, expectedHint);
  }

  /**
   * Freshness rule
   * Expires a year after date
   */
  public void testFreshnessRuleMoreThanMonthFreshnessFromExpires() {
    responseBuilder.setResponseHeaderCacheControl("public");
    responseBuilder.setResponseHeaderContentType("image/png; charset=utf-8");
    responseBuilder.setResponseHeaderDate("Fri, 01 Jan 2010 00:00:00 GMT");
    responseBuilder.setResponseHeaderExpires("Mon, 01 Mar 2010 00:00:00 GMT");

    String hintDescription = "To further improve cache hit rate, specify an expiration"
        + " one year in the future for the following cacheable resources: http://www.google.com";
    HintRecord expectedHint = createHintRecord(hintDescription, HintRecord.SEVERITY_INFO);

    runCacheTest(responseBuilder.getEvent(), DEFAULT_URL, expectedHint);
  }

  /**
   * Freshness rule
   * Expires a year after date
   */
  public void testFreshnessRuleYearExpires() {
    responseBuilder.setResponseHeaderCacheControl("public");
    responseBuilder.setResponseHeaderContentType("image/png; charset=utf-8");
    responseBuilder.setResponseHeaderDate("Fri, 01 Jan 2010 00:00:00 GMT");
    responseBuilder.setResponseHeaderExpires("Sat, 01 Jan 2011 00:00:00 GMT");
    runCacheTestNoHint(responseBuilder.getEvent(), DEFAULT_URL);
  }

  /**
   * Favicon rule
   * Favicon without header and caching on
   */
  public void testFaviconRuleCachingNoExpires() {
    responseBuilder.setResponseHeaderCacheControl("private");
    responseBuilder.setResponseHeaderContentType(FAVICON_TYPE);

    String hintDescription = "Favicons should have an expiration at least one month"
        + " in the future: http://www.google.com";
    HintRecord expectedHint = createHintRecord(hintDescription, HintRecord.SEVERITY_WARNING);

    runCacheTest(responseBuilder.getEvent(), DEFAULT_URL, expectedHint);
  }

  /**
   * Favicon rule
   * Favicon with expiration in the future
   */
  public void testFaviconRuleCachingFutureExpires() {
    responseBuilder.setResponseHeaderExpires(FUTURE_DATE);
    responseBuilder.setResponseHeaderCacheControl("private");
    responseBuilder.setResponseHeaderContentType(FAVICON_TYPE);
    runCacheTestNoHint(responseBuilder.getEvent(), DEFAULT_URL);
  }

  /**
   * Favicon rule
   * Favicon with cookie set
   */
  public void testFaviconRuleCookieSet() {
    responseBuilder.setResponseHeaderSetCookie("TESTCOOKIE");
    responseBuilder.setResponseHeaderCacheControl("private");
    responseBuilder.setResponseHeaderContentType(FAVICON_TYPE);
    runCacheTestNoHint(responseBuilder.getEvent(), DEFAULT_URL);
  }

  /**
   * Favicon rule
   * Favicon with short expiration
   */
  public void testFaviconRuleShortExpiration() {
    responseBuilder.setResponseHeaderCacheControl("private");
    responseBuilder.setResponseHeaderContentType(FAVICON_TYPE);
    responseBuilder.setResponseHeaderDate("Mon, 07 Sep 1998 17:43:37 GMT");
    responseBuilder.setResponseHeaderExpires("Mon, 14 Sep 1998 17:43:37 GMT");

    String hintDescription = "Favicons should have an expiration at least one month"
        + " in the future: http://www.google.com";
    HintRecord expectedHint = createHintRecord(hintDescription, HintRecord.SEVERITY_WARNING);

    runCacheTest(responseBuilder.getEvent(), DEFAULT_URL, expectedHint);
  }

  /**
   * Query rule
   * Cacheable resource has a ? in URL
   */
  public void testQueryRuleCacheableWithQuery() {
    responseBuilder.setResponseHeaderCacheControl("public");
    responseBuilder.setResponseHeaderContentType("image/png");
    responseBuilder.setResponseHeaderExpires(FUTURE_DATE);
    String url = "http://www.google.com/image.png?param=1";

    String hintDescription = "Resources with a '?' in the URL are not cached by most"
        + " proxy caching servers. Remove the query string and encode the"
        + " parameters into the URL for the following resources: " + url;
    HintRecord expectedHint = createHintRecord(hintDescription, HintRecord.SEVERITY_WARNING);
    runCacheTest(responseBuilder.getEvent(), url, expectedHint);
  }

  /**
   * Query rule
   * Has query but private caching
   */
  public void testQueryRulePrivateCacheWithQuery() {
    responseBuilder.setResponseHeaderCacheControl("private");
    responseBuilder.setResponseHeaderContentType("image/png");
    responseBuilder.setResponseHeaderContentEncoding("gzip"); // added so Public Rule won't fire
    responseBuilder.setResponseHeaderExpires(FUTURE_DATE);
    String url = "http://www.google.com/image.png?param=1";
    runCacheTestNoHint(responseBuilder.getEvent(), url);
  }

  /**
   * Query rule
   * Publicly cacheable with query and cookie.
   * Does not fire query rule, but will fire cookie rule
   */
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

    runCacheTest(responseBuilder.getEvent(), url, expectedHint);
  }

  /**
   * Proxy Bug rule
   * cache-control private, could be public
   */
  public void testProxyBugRulePrivate() {
    responseBuilder.setResponseHeaderCacheControl("private");
    responseBuilder.setResponseHeaderContentType("image/png");
    responseBuilder.setResponseHeaderExpires(FUTURE_DATE);

    String hintDescription = "Consider adding a 'Cache-Control: public' header to the"
        + " following resource: http://www.google.com";
    HintRecord expectedHint = createHintRecord(hintDescription, HintRecord.SEVERITY_INFO);

    runCacheTest(responseBuilder.getEvent(), DEFAULT_URL,
        expectedHint);
  }

  /**
   * Proxy Bug Rule
   * publicly cacheable
   */
  public void testProxyBugRulePublic() {
    responseBuilder.setResponseHeaderCacheControl("public");
    responseBuilder.setResponseHeaderContentType("image/png");
    responseBuilder.setResponseHeaderExpires(FUTURE_DATE);
    runCacheTestNoHint(responseBuilder.getEvent(), DEFAULT_URL);
  }

  /**
   * Proxy Bug Rule
   * private but contains a cookie
   */
  public void testProxyBugRuleContainsCooke() {
    responseBuilder.setResponseHeaderCacheControl("private");
    responseBuilder.setResponseHeaderContentType("image/png");
    responseBuilder.setResponseHeaderExpires(FUTURE_DATE);
    responseBuilder.setResponseHeaderSetCookie("TESTCOOKIE");
    runCacheTestNoHint(responseBuilder.getEvent(), DEFAULT_URL);
  }

  /**
   * Proxy Bug Rule
   * private but compressed resource
   */
  public void testProxyBugRuleCompressedResource() {
    responseBuilder.setResponseHeaderCacheControl("private");
    responseBuilder.setResponseHeaderContentType("image/css");
    responseBuilder.setResponseHeaderContentEncoding("gzip");
    responseBuilder.setResponseHeaderExpires(FUTURE_DATE);
    runCacheTestNoHint(responseBuilder.getEvent(), DEFAULT_URL);
  }

  /**
   * Proxy Bug Rule
   * publicly cacheable with compressed data
   */
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

    runCacheTest(responseBuilder.getEvent(), DEFAULT_URL, expectedHint);
  }

  /**
   * Proxy Bug rule
   * privately cacheable with compressed data
   */
  public void testProxyBugRulePrivateGzip() {
    responseBuilder.setResponseHeaderCacheControl("private");
    responseBuilder.setResponseHeaderContentEncoding("gzip");
    responseBuilder.setResponseHeaderContentType("text/javascript; charset=utf-8");
    responseBuilder.setResponseHeaderExpires(FUTURE_DATE);
    runCacheTestNoHint(responseBuilder.getEvent(), DEFAULT_URL);
  }

  /**
   * Proxy Bug Rule
   * publicy cacheable with vary header
   */
  public void testProxyBugRulePublicVary() {
    responseBuilder.setResponseHeaderCacheControl("public");
    responseBuilder.setResponseHeaderContentEncoding("gzip");
    responseBuilder.setResponseHeaderVary("Accept-Encoding");
    responseBuilder.setResponseHeaderContentType("text/javascript; charset=utf-8");
    responseBuilder.setResponseHeaderExpires(FUTURE_DATE);
    runCacheTestNoHint(responseBuilder.getEvent(), DEFAULT_URL);
  }

  /**
   * Proxy Bug rule
   * publicly cacheable with cookie
   */
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

    runCacheTest(responseBuilder.getEvent(), DEFAULT_URL, expectedHint);
  }

  /**
   * Cookie rule
   * Contains a Set-Cookie along with Cache-Control: public header
   */
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

    runCacheTest(responseBuilder.getEvent(), DEFAULT_URL, expectedHint);
  }

  /**
   * Cookie rule
   * public cache control but no cookie
   */
  public void testCookieRulePublicNoCookie() {
    responseBuilder.setResponseHeaderCacheControl("public");
    responseBuilder.setResponseHeaderContentType("image/gif");
    responseBuilder.setResponseHeaderDate("Mon, 07 Sep 1998 17:43:37 GMT");
    responseBuilder.setResponseHeaderExpires("Tue, 08 Jun 2020 17:43:37 GMT");
    responseBuilder.setResponseHeaderLastModified("Fri, 04 Sep 1998 00:00:00 GMT");
    runCacheTestNoHint(responseBuilder.getEvent(), DEFAULT_URL);
  }

  /**
   * Cookie rule
   * No caching and cookie set
   */
  public void testCookieRulePrivateCacheWithCooke() {
    responseBuilder.setResponseHeaderCacheControl("private");
    responseBuilder.setResponseHeaderContentType("image/gif");
    responseBuilder.setResponseHeaderDate("Mon, 07 Sep 1998 17:43:37 GMT");
    responseBuilder.setResponseHeaderExpires("Tue, 08 Jun 2020 17:43:37 GMT");
    responseBuilder.setResponseHeaderLastModified("Fri, 04 Sep 1998 00:00:00 GMT");
    responseBuilder.setResponseHeaderSetCookie("TESTCOOKIE");
    runCacheTestNoHint(responseBuilder.getEvent(), DEFAULT_URL);
  }

}
