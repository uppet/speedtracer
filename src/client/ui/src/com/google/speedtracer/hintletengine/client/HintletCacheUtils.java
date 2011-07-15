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

import com.google.gwt.regexp.shared.MatchResult;
import com.google.gwt.regexp.shared.RegExp;
import com.google.speedtracer.client.model.NetworkResource.HeaderMap;

/**
 * Hitlet Cache Utilities
 */
public final class HintletCacheUtils {

  private static RegExp maxAgeRegExp;

  private HintletCacheUtils() {
  }

  /**
   * @param type The type of resource.
   * @return {@code true} iff the resource type is known to be uncacheable.
   */
  public static boolean isNonCacheableResourceType(WebInspectorType type) {
    // TODO(zundel): Figure out some good rules around caching XHRs.
    // TODO(zundel): Figure out if there is any way to cache a redirect.
    return (type == WebInspectorType.DOCUMENT || type == WebInspectorType.OTHER);
  }

  /**
   * @param code the response code.
   * @return {@code true} iff a response with the given response code is cacheable in the absence of
   *         other caching headers.
   */
  public static boolean isCacheableResponseCode(int code) {
    switch (code) {
      // HTTP/1.1 RFC lists these response codes as cacheable in the
      // absence of explicit caching headers.
      case 200:
      case 203:
      case 206:
      case 300:
      case 301:
      case 410:
        return true;
        // In addition, 304s are sent for cacheable resources. Though
        // the 304 response itself is not cacheable, the underlying
        // resource is, and that's what we care about.
      case 304:
        return true;
      default:
        return false;
    }
  }

  /**
   * @param type The type of resource.
   * @return {@code true} iff the resource type is known to be cacheable.
   */
  public static boolean isCacheableResourceType(WebInspectorType type) {
    return (type == WebInspectorType.SCRIPT || type == WebInspectorType.IMAGE
        || type == WebInspectorType.STYLESHEET || type == WebInspectorType.MEDIA);
  }

  /**
   * @param headers An object with a key for each header and a value containing the contents of that
   *          header.
   * @return {@code true} iff the headers contain an expiration date for this resource.
   */
  public static boolean hasExplicitExpiration(HeaderMap headers) {
    // HTTP/1.1 RFC says: HTTP/1.1 clients and caches MUST treat
    // invalid date formats, especially including the value "0", as in the
    // past (i.e., "already expired") so we do not need to validate the
    // contents of these headers. We only need to check that they are
    // present.
    return HintletHeaderUtils.hasHeader(headers, "Date") != null
        && (HintletHeaderUtils.hasHeader(headers, "Expires") != null || HintletHeaderUtils
            .headerContains(headers, "Cache-Control", "max-age"));
  }

  /**
   * @param headers map of the resource response headers.
   * @param url The URL of the resource.
   * @param responseCode HTTP status code.
   * @return {@code true } iff the resource type is explicitly uncacheable.
   */
  public static boolean isExplicitlyNonCacheable(HeaderMap headers, String url, int responseCode) {
    // Don't run any rules on URLs that explicitly do not want to be cached
    // (e.g. beacons).
    boolean hasExplicitExp = hasExplicitExpiration(headers);
    return (HintletHeaderUtils.headerContains(headers, "Cache-Control", "no-cache")
        || HintletHeaderUtils.headerContains(headers, "Cache-Control", "no-store")
        || HintletHeaderUtils.headerContains(headers, "Cache-Control", "must-revalidate")
        || HintletHeaderUtils.headerContains(headers, "Pragma", "no-cache")
        // Explicit expiration in the past is the HTTP/1.0 equivalent
        // of Cache-Control: no-cache.
        || (hasExplicitExp && !freshnessLifetimeGreaterThan(headers, 0))
        // According to the HTTP RFC, responses with query strings
        // and no explicit caching headers must not be cached.
        || (!hasExplicitExp && url.indexOf("?") >= 0)
        // According to the HTTP RFC, only responses with certain
        // response codes can be cached in the absence of caching headers.
        || (!hasExplicitExp && !isCacheableResponseCode(responseCode)));
  }

  /**
   * @param headers map of the resource response headers.
   * @param url The URL of the resource.
   * @param statusCode HTTP status code.
   * @return {@code true} iff the headers indicate that this resource may ever be publicly
   *         cacheable.
   */
  public static boolean isPubliclyCacheable(HeaderMap headers, String url, int statusCode) {
    if (isExplicitlyNonCacheable(headers, url, statusCode)) {
      return false;
    }

    if (HintletHeaderUtils.headerContains(headers, "Cache-Control", "public")) {
      return true;
    }

    // A response that isn't explicitly marked as private that does not
    // have a query string is cached by most proxies.
    if (url.indexOf("?") == -1
        && !HintletHeaderUtils.headerContains(headers, "Cache-Control", "private")) {
      return true;
    }

    return false;
  }

  /**
   * @param headers An object with a key for each header and a value containing the contents of that
   *          header.
   * @param timeMs The freshness lifetime to compare with (in milliseconds).
   * @return {@code true} iff the headers indicate that this resource has a freshness lifetime
   *         greater than the specified time.
   */
  public static boolean freshnessLifetimeGreaterThan(HeaderMap headers, double timeMs) {
    String dateHeader = HintletHeaderUtils.hasHeader(headers, "Date");
    if (dateHeader == null) {
      // HTTP RFC says the date header is required. If not present, we
      // have no reference point to compute the freshness lifetime from,
      // so we assume it has no freshness lifetime.
      return false;
    }

    double dateHdrMs = getTime(dateHeader);
    if (dateHdrMs == Double.NaN) {
      return false;
    }

    double freshnessLifetimeMs = 0;

    // Check for max-age in the Cache-Control header
    String cacheControlHeader = HintletHeaderUtils.hasHeader(headers, "Cache-Control");
    String maxAgeMatch = null;
    if (cacheControlHeader != null) {
      if (maxAgeRegExp == null) {
        maxAgeRegExp = RegExp.compile("max-age=(\\d+)");
      }
      MatchResult matchResult = maxAgeRegExp.exec(cacheControlHeader);
      if (matchResult != null && matchResult.getGroupCount() > 1) {
        maxAgeMatch = matchResult.getGroup(1);
      }
    }

    // The max-age overrides Expires in most modern browsers.
    if (maxAgeMatch != null) {
      try {
        freshnessLifetimeMs = 1000 * Double.parseDouble(maxAgeMatch);
      } catch (NumberFormatException ex) {
        freshnessLifetimeMs = Double.NaN;
      }
    } else {
      String expiresHeader = HintletHeaderUtils.hasHeader(headers, "Expires");
      if (expiresHeader != null) {
        Double expDate = getTime(expiresHeader);
        if (expDate != Double.NaN) {
          freshnessLifetimeMs = expDate - dateHdrMs;
        }
      }
    }

    // Non-numeric freshness lifetime is considered a zero freshness
    // lifetime.
    if (freshnessLifetimeMs == Double.NaN)
      return false;

    return freshnessLifetimeMs > timeMs;
  }

  /**
   * @param date e.g. "Jul 8, 2005", "Thu, 14 Jul 2011 21:07:34 GMT"
   * @return Return parses a date string and returns the number of milliseconds between the date
   *         string and midnight of January 1, 1970
   */
  private static native double getTime(String date)/*-{
    return Date.parse(date);
  }-*/;

}
