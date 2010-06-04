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

/**
 * Common functions used for cache related rules.
 * Functions are stored in the 'cache_lib' global object.
 */
cache_lib = {};

/**
 * @param {number} code the response code.
 * @return {boolean} Whether a response with the given response code
 *     is cacheable in the absence of other caching headers.
 */
cache_lib.isCacheableResponseCode = function(code) {
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
 * @param {string} type The type of resource.
 * @return {boolean} whether the resource type is known to be
 *     cacheable.
 */
cache_lib.isCacheableResourceType = function(type) {
  switch(type) {
  case hintlet.RESOURCE_TYPE_STYLESHEET:
  case hintlet.RESOURCE_TYPE_SCRIPT:
  case hintlet.RESOURCE_TYPE_IMAGE:
  case hintlet.RESOURCE_TYPE_STYLESHEET:
  case hintlet.RESOURCE_TYPE_MEDIA:
    return true;
  default:
  }
  return false;
}  

/**
 * @param {string} type The type of resource.
 * @return {boolean} whether the resource type is known to be
 *     uncacheable.
 */
cache_lib.isNonCacheableResourceType = function(type) {
  // TODO: Figure out some good rules around caching XHRs.
  // TODO: Figure out if there is any way to cache a redirect.
  switch(type) {
  case hintlet.RESOURCE_TYPE_DOCUMENT:
  case hintlet.RESOURCE_TYPE_IFRAME:
  case hintlet.RESOURCE_TYPE_OTHER:
    return true;
  default:
  }
  return false;
}

/**
 * @param {object} map of the resource response headers.
 * @param {string} url The URL of the resource.
 * @param {int} HTTP status code.
 * @return {boolean} true iff the headers indicate that this resource may ever
 *     be publicly cacheable.
 */
cache_lib.isPubliclyCacheable = function(headers, url, statusCode) {
  if (cache_lib.isExplicitlyNonCacheable(headers, url, statusCode)) {
    return false;
  }

  if (hintlet.headerContains(headers, 'Cache-Control', 'public')) {
    return true;
  }

  // A response that isn't explicitly marked as private that does not
  // have a query string is cached by most proxies.
  if (url.indexOf('?') == -1 &&
      !hintlet.headerContains(headers, 'Cache-Control', 'private')) {
    return true;
  }

  return false;
}

/**
 * @param {object} map of the resource response headers.
 * @param {string} url The URL of the resource.
 * @param {int} HTTP status code.
 * @return {boolean} whether the resource type is explicitly
 *     uncacheable.
 */
cache_lib.isExplicitlyNonCacheable = function(headers, url, responseCode) {
  // Don't run any rules on URLs that explicitly do not want to be cached
  // (e.g. beacons).
  var hasExplicitExp = cache_lib.hasExplicitExpiration(headers);
  return (hintlet.headerContains(headers, 'Cache-Control', 'no-cache') ||
          hintlet.headerContains(headers, 'Cache-Control', 'no-store') ||
          hintlet.headerContains(headers, 'Cache-Control', 'must-revalidate') ||
          hintlet.headerContains(headers, 'Pragma', 'no-cache') ||
          // Explicit expiration in the past is the HTTP/1.0 equivalent
          // of Cache-Control: no-cache.
          (hasExplicitExp && 
           !cache_lib.freshnessLifetimeGreaterThan(headers, 0)) ||
          // According to the HTTP RFC, responses with query strings
          // and no explicit caching headers must not be cached.
          (!hasExplicitExp && url.indexOf('?') >= 0) ||
          // According to the HTTP RFC, only responses with certain
          // response codes can be cached in the absence of caching
          // headers.
          (!hasExplicitExp &&
           !cache_lib.isCacheableResponseCode(responseCode)));
}


/**
 * @param  {Object} headers An object with a key for each header and a value
 *     containing the contents of that header.
 * @param  {number} timeMs The freshness lifetime to compare with.
 * @return {boolean} true iff the headers indicate that this resource
 *     has a freshness lifetime greater than the specified time.
 */
cache_lib.freshnessLifetimeGreaterThan = function(headers, timeMs) {
  var dateHeader = hintlet.hasHeader(headers, 'Date');
  if (dateHeader === undefined) {
    // HTTP RFC says the date header is required. If not present, we
    // have no reference point to compute the freshness lifetime from,
    // so we assume it has no freshness lifetime.
    return false;
  }

  var dateHdrMs = Date.parse(dateHeader);
  if (isNaN(dateHdrMs)) {
    return false;
  }

  var freshnessLifetimeMs;

  //Check for max-age in the Cache-Control header
  var cacheControlHeader = hintlet.hasHeader(headers, 'Cache-Control');
  var maxAgeMatch = null;
  if (cacheControlHeader !== undefined) {
    maxAgeMatch = cacheControlHeader.match(/max-age=(\d+)/);
  }

  // The max-age overrides Expires in most modern browsers.
  if(maxAgeMatch && maxAgeMatch[1]) {
    freshnessLifetimeMs = 1000 * maxAgeMatch[1]; 
  } else {
    var expiresHeader = hintlet.hasHeader(headers, 'Expires');
    if (expiresHeader !== undefined) {
      var expDate = Date.parse(expiresHeader);
      if (!isNaN(expDate)) {
        freshnessLifetimeMs = expDate - dateHdrMs;
      }
    }
  }

  // Non-numeric freshness lifetime is considered a zero freshness
  // lifetime.                                                
  if (isNaN(freshnessLifetimeMs)) return false;               
                                                              
  return freshnessLifetimeMs > timeMs;                        
}       

/**
 * @param {Object} headers An object with a key for each header and a value
 *     containing the contents of that header.
 * @return {boolean} true iff the headers contain an expiration date
 *     for this resource.
 */
cache_lib.hasExplicitExpiration = function(headers) {
  // HTTP/1.1 RFC says: HTTP/1.1 clients and caches MUST treat
  // invalid date formats, especially including the value "0", as in the
  // past (i.e., "already expired") so we do not need to validate the
  // contents of these headers. We only need to check that they are
  // present.
  return hintlet.hasHeader(headers, 'Date') &&
       (hintlet.hasHeader(headers, 'Expires') ||
        hintlet.headerContains(headers, 'Cache-Control', 'max-age'));
}

hintlet.log("Loaded cache_lib.js");
