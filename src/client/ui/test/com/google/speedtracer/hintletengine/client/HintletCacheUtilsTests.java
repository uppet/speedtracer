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
package com.google.speedtracer.hintletengine.client;

import com.google.gwt.junit.client.GWTTestCase;
import com.google.speedtracer.client.model.NetworkResource.HeaderMap;
import static com.google.speedtracer.hintletengine.client.HintletCacheUtils.freshnessLifetimeGreaterThan;
import static com.google.speedtracer.hintletengine.client.HintletCacheUtils.hasExplicitExpiration;
import static com.google.speedtracer.hintletengine.client.HintletCacheUtils.isCacheableResourceType;
import static com.google.speedtracer.hintletengine.client.HintletCacheUtils.isCacheableResponseCode;
import static com.google.speedtracer.hintletengine.client.HintletCacheUtils.isPubliclyCacheable;
import static com.google.speedtracer.hintletengine.client.HintletCacheUtils.isExplicitlyNonCacheable;

/**
 * Tests {@link HintletCacheUtils}
 */
public class HintletCacheUtilsTests extends GWTTestCase {

  private static final int DEFAULT_CACHEABLE_RESPONSE = 200;
  private static final int DEFAULT_NON_CACHEABLE_RESPONSE = 0;

  @Override
  public String getModuleName() {
    return "com.google.speedtracer.hintletengine.HintletEngineTest";
  }

  public void testFreshnessLifetimeGreaterThan() {
    assertFalse("no date in header should fail",
        freshnessLifetimeGreaterThan(createHeaderNoDate(), 0));
    assertFalse("invalid date should fail",
        freshnessLifetimeGreaterThan(createHeaderInvalidDate(), 0));
    assertFalse("no cache control header should fail",
        freshnessLifetimeGreaterThan(createHeaderDateOnly(), 0));
    assertFalse("no max age or Expires should fail",
        freshnessLifetimeGreaterThan(createHeaderDateNoExpiresNoMaxAge(), 0));

    // checking max age
    assertTrue("max-age=1 > 0 should pass",
        freshnessLifetimeGreaterThan(createHeaderPublicMaxAge1(), 0));
    assertFalse("max-age=1 > 1500 should fail",
        freshnessLifetimeGreaterThan(createHeaderPublicMaxAge1(), 1500));

    // checking Expires
    assertTrue("Expires in the future should pass",
        freshnessLifetimeGreaterThan(createHeaderFutureExpiration(), 1000));
    assertFalse("Expires in the past should fail",
        freshnessLifetimeGreaterThan(createHeaderPastExpiration(), 1000));
  }
  
  public void testHasExplicitExpiration() {
    assertFalse("Header without date should fail",
        hasExplicitExpiration(createHeaderNoDate()));
    assertFalse("Empty header should fail", 
        hasExplicitExpiration(createEmptyHeader()));
    assertFalse("date but no Expires or max-age should fail",
        hasExplicitExpiration(createHeaderDateNoExpiresNoMaxAge()));
    assertTrue("has date and Cache-Control:max-age should pass",
        hasExplicitExpiration(createHeaderPublicMaxAge1()));
    assertTrue("has date and Expires should pass",
        hasExplicitExpiration(createHeaderNoCacheControlFutureExpiration()));
  }

  public void testIsCacheableResponseCode() {
    int[] cacheable = {200, 203, 206, 300, 301, 410, 304};
    int[] notCacheable = {0, -1, 100, 201, 1000};
    for (int val : cacheable) {
      assertTrue(isCacheableResponseCode(val));
    }
    for (int val : notCacheable) {
      assertFalse(isCacheableResponseCode(val));
    }
  }

  public void testIsExplicitlyNonCacheable() {
    String url = "http://www.google.com";
    assertTrue("Cache-Control no-cache is specified should pass",
        isExplicitlyNonCacheable(
            createHeaderNoCache(), url, DEFAULT_CACHEABLE_RESPONSE));
    assertTrue("Cache-Control no-store is specified should pass",
        isExplicitlyNonCacheable(
            createHeaderNoStore(), url, DEFAULT_CACHEABLE_RESPONSE));
    assertTrue("Cache-Control proxy-revalidate is specified should pass",
        isExplicitlyNonCacheable(
            createHeaderProxyRevalidate(), url, DEFAULT_CACHEABLE_RESPONSE));
    assertTrue("Pragma no-cache is specified should pass",
        isExplicitlyNonCacheable(
            createHeaderPragmaNoCache(), url, DEFAULT_CACHEABLE_RESPONSE));
    assertTrue("no Expires and Cache-Control max-age:0 should pass",
        isExplicitlyNonCacheable(
            createHeaderCachePrivateZeroMaxAgeNoExpires(), url, DEFAULT_CACHEABLE_RESPONSE));
    assertTrue("no explicit expiration containing ? should pass",
        isExplicitlyNonCacheable(
            createHeaderDateNoExpiresNoMaxAge(), url + "/?val=0", DEFAULT_CACHEABLE_RESPONSE));
    assertTrue("no explicit expiration and non-cacheable response code should pass",
        isExplicitlyNonCacheable(
            createHeaderDateNoExpiresNoMaxAge(), url, DEFAULT_NON_CACHEABLE_RESPONSE));
    assertFalse("no explicit expiration and cacheable response code should fail",
        isExplicitlyNonCacheable(
            createHeaderDateNoExpiresNoMaxAge(), url, DEFAULT_CACHEABLE_RESPONSE));
    assertFalse("cacheable resource should fail", isExplicitlyNonCacheable(
        createHeaderPublicMaxAge1(), url, DEFAULT_CACHEABLE_RESPONSE));
  }

  public void testIsPubliclyCacheable() {
    String url = "http://www.google.com";
    assertFalse("is explicitly non cacheable should fail", isPubliclyCacheable(
        createHeaderNoCache(), url, DEFAULT_CACHEABLE_RESPONSE));
    assertTrue("Cache-Control public should pass", isPubliclyCacheable(
        createHeaderPublicMaxAge1(), url, DEFAULT_CACHEABLE_RESPONSE));
    assertTrue("no cache-control (not explicitly private) and no ? in URL should pass",
        isPubliclyCacheable(
            createHeaderNoCacheControlFutureExpiration(), url, DEFAULT_CACHEABLE_RESPONSE));
    assertFalse("private cache control should fail", isPubliclyCacheable(
        createHeaderCachePrivateZeroMaxAgeNoExpires(), url, DEFAULT_CACHEABLE_RESPONSE));
  }

  public static native HeaderMap createEmptyHeader() /*-{
    return {};
  }-*/;

  public static native HeaderMap createHeaderCachePrivateZeroMaxAgeNoExpires() /*-{
    return {
        "Date" : "Thu, 14 Jul 2011 21:07:34 GMT",
        "Cache-Control" : "private, max-age=0"
    };
  }-*/;

  public static native HeaderMap createHeaderDateNoExpiresNoMaxAge() /*-{
    return {
        "Date" : "Fri, 1 Jan 2010 1:00:00 GMT",
        "Cache-Control" : "private"
    };
  }-*/;

  public static native HeaderMap createHeaderDateOnly() /*-{
    return {
      "Date" : "Fri, 1 Jan 2010 1:00:00 GMT"
    };
  }-*/;

  public static native HeaderMap createHeaderFutureExpiration() /*-{
    return {
        "Date" : "Thu, 14 Jul 2011 21:07:34 GMT",
        "Expires" : "Sat, 1 Jan 2050 1:00:00 GMT"
    };
  }-*/;

  public static native HeaderMap createHeaderInvalidDate() /*-{
    return {
      "Date" : ""
    };
  }-*/;

  public static native HeaderMap createHeaderNoCache() /*-{
    return {
        "Date" : "Thu, 14 Jul 2011 21:07:34 GMT",
        "Connection" : "keep-alive",
        "Content-Length" : "35",
        "Last-Modified" : "Wed, 21 Jan 2004 19:51:30 GMT",
        "Cache-Control" : "private, no-cache",
        "Expires" : "Wed, 19 Apr 2000 11:43:00 GMT"
    };
  }-*/;

  public static native HeaderMap createHeaderNoCacheControlFutureExpiration() /*-{
    return {
        "Date" : "Thu, 14 Jul 2011 21:07:34 GMT",
        "Expires" : "Sat, 1 Jan 2050 1:00:00 GMT"
    };
  }-*/;

  public static native HeaderMap createHeaderNoDate() /*-{
    return {
        "Vary" : "Accept-Encoding",
        "Cache-Control" : "private, max-age=1000",
        "Expires" : "Fri, 1 Jan 2010 1:00:00 GMT"
    };
  }-*/;

  public static native HeaderMap createHeaderNoStore() /*-{
    return {
        "Date" : "Thu, 14 Jul 2011 21:07:34 GMT",
        "Connection" : "keep-alive",
        "Content-Length" : "35",
        "Last-Modified" : "Wed, 21 Jan 2004 19:51:30 GMT",
        "Cache-Control" : "private, no-store",
        "Expires" : "Wed, 19 Apr 2000 11:43:00 GMT"
    };
  }-*/;

  public static native HeaderMap createHeaderPastExpiration() /*-{
    return {
        "Date" : "Sat, 1 Jan 2011 21:00:00 GMT",
        "Expires" : "Sat, 1 Jan 2011 1:00:00 GMT"
    };
  }-*/;

  public static native HeaderMap createHeaderPragmaNoCache() /*-{
    return {
        "Date" : "Thu, 14 Jul 2011 21:07:34 GMT",
        "Connection" : "keep-alive",
        "Content-Length" : "35",
        "Pragma" : "no-cache",
        "Last-Modified" : "Wed, 21 Jan 2004 19:51:30 GMT",
        "Expires" : "Wed, 19 Apr 2000 11:43:00 GMT"
    };
  }-*/;

  public static native HeaderMap createHeaderProxyRevalidate() /*-{
    return {
        "Date" : "Thu, 14 Jul 2011 21:07:34 GMT",
        "Connection" : "keep-alive",
        "Content-Length" : "35",
        "Last-Modified" : "Wed, 21 Jan 2004 19:51:30 GMT",
        "Cache-Control" : "private, proxy-revalidate",
        "Expires" : "Wed, 19 Apr 2000 11:43:00 GMT"
    };
  }-*/;

  public static native HeaderMap createHeaderPublicMaxAge1() /*-{
    return {
        "Date" : "Thu, 14 Jul 2011 21:07:34 GMT",
        "Cache-Control" : "public, max-age=1"
    };
  }-*/;
  
}
