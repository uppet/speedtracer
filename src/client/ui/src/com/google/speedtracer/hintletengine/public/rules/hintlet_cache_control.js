// Ripped off from Page Speed cacheControlLint.js, as much as possible

/*
 * Copyright 2007 Google Inc.
 * All Rights Reserved.
 *
 * @fileoverview A lint rule for determining if Cache-Control headers are
 * appropriately set.
 *
 * @author Kyle Scholz (kylescholz@google.com)
 */

// Example data record:
//{
//   "data": {
//      "headers": {
//         "Cache-Control": "private, max-age=0",
//        "Content-Encoding": "gzip",
//         "Content-Length": "2755",
//         "Content-Type": "text/html; charset=UTF-8",
//         "Date": "Fri, 30 Jan 2009 17:12:38 GMT",
//         "Expires": "-1",
//      },
//      "resourceId": "1NetworkResourceEvent1",
//      "responseCode": 200,
//      "url": "http://www.example.com/foo.html",
//   },
//   "time": 10549.0,
//   "type": "24"
//},

// Make a namespace for this rule
(function() {  // Begin closure

var msInAMonth = 1000 * 60 * 60 * 24 * 30;
var msInElevenMonths = msInAMonth * 11;

/**
 * Constructor for a CacheRule.
 * @param {String} message A message to display for files violating this rule.
 * @param {Number} severity use one of the hintlet.SEVERITY_ constants.
 * @param {Function} exec A function which returns true if the given component
 *     and header are in violation of the rule.
 * @constructor
 */
var CacheRule = function(message, severity, exec) {
  this.message = message;
  this.severity = severity;
  this.exec = exec;
  this.violations = [];
};

var now = (new Date()).getTime();
// Note: To comply with RFC 2616, we don't want to require that Expires is
// a full year in the future. Set the minimum to 11 months. Note that this
// isn't quite right because not all months have 30 days, but we err on the
// side of being conservative, so it works fine for the rule.
var msInAMonth = 1000 * 60 * 60 * 24 * 30;
CacheRule.prototype.oneMonthFromNow = now + msInAMonth;
CacheRule.prototype.elevenMonthsFromNow = now + 11 * msInAMonth;

/**
 * @param {string} type The type of resource.
 * @return {boolean} whether the resource type is known to be
 *     compressable.
 */
function isCompressibleResourceType(type) {
  switch(type) {
  case hintlet.RESOURCE_TYPE_STYLESHEET:
  case hintlet.RESOURCE_TYPE_SCRIPT:
    return true;
  default:
  }
  return false;
}  

// A list of rules to run through for each record encountered.
var rules = [
  //
  // Browser Caching rules
  // 
  new CacheRule('The following resources are missing a cache expiration.' +
                ' Resources that do not specify an expiration may not be' +
                ' cached by browsers.' +
                ' Specify an expiration at least one month in the' +
                ' future for resources that should be cached, and an' +
                ' expiration in the past' +
                ' for resources that should not be cached:',           
                hintlet.SEVERITY_CRITICAL,
                function(url, headers, type, dataRecord) {
                    return cache_lib.isCacheableResourceType(type) &&
                          hintlet.hasHeader(headers, 'Set-Cookie') 
                              === undefined &&
                          !cache_lib.hasExplicitExpiration(headers);
                }),
  new CacheRule('The following resources specify a "Vary" header that' +
                ' disables caching in most versions of Internet Explorer.' +
                ' Fix or remove the "Vary" header for the' +
                ' following resources:',
                 hintlet.SEVERITY_CRITICAL,
                 function(url, headers, type, dataRecord) {
                   var varyHeader = headers['Vary'];
                   if (varyHeader) {
                     // MS documentation indicates that IE will cache
                     // responses with Vary Accept-Encoding or
                     // User-Agent, but not anything else. So we
                     // strip these strings from the header, as well
                     // as separator characters (comma and space),
                     // and trigger a warning if there is anything
                     // left in the header.
                     varyHeader = varyHeader.replace(/User-Agent/gi, '');
                     varyHeader = varyHeader.replace(/Accept-Encoding/gi, '');
                     varyHeader = varyHeader.replace(/[, ]*/g, '');
                   }
                   return cache_lib.isCacheableResourceType(type) &&
                        varyHeader &&
                        varyHeader.length > 0 &&
                        cache_lib.freshnessLifetimeGreaterThan(headers, 0);
                 }),
  new CacheRule('The following cacheable resources have a short' +
                ' freshness lifetime. Specify an expiration at least one' +
                ' month in the future for the following resources:',
                hintlet.SEVERITY_WARNING,
                function(url, headers, type, dataRecord) {
                  // Add an Expires header. Use at least one month in the
                  // future.
                  return cache_lib.isCacheableResourceType(type) &&
                      hintlet.hasHeader(headers, 'Set-Cookie') === undefined &&
                      !cache_lib.freshnessLifetimeGreaterThan(headers, 
                                                              msInAMonth) &&
                      cache_lib.freshnessLifetimeGreaterThan(headers, 0);
                }),

  // Note that I haven't seen any favicon records in the wild.  I don't 
  // think normal instrumentation of the network layer catches them since 
  // they are not referenced by the web page.  The rule makes an assumption
  // that the records look like regular resource responses.
  new CacheRule('Favicons should have an expiration at least one month' +
                ' in the future:',
                hintlet.SEVERITY_WARNING,
                function(url, headers, type) {
                  // Its not reasonable to suggest that the favicon be a year
                  // in the future because sometimes the path cannot be
                  // controlled. However, it is very reasonable to suggest a
                  // month
                  return (type == hintlet.RESOURCE_TYPE_FAVICON)
                    && (hintlet.hasHeader(headers, 'Set-Cookie') === undefined)
                    && !cache_lib.freshnessLifetimeGreaterThan(
                           headers, msInAMonth);
                }),
  new CacheRule('To further improve cache hit rate, specify an expiration' +
                ' one year in the future for the following cacheable' +
                ' resources:',
                hintlet.SEVERITY_INFO,
                function(url, headers, type, dataRecord) {
                  // Add an Expires header. Use at least one year in the
                  // future.
                  return cache_lib.isCacheableResourceType(type) &&
                      hintlet.hasHeader(headers, 'Set-Cookie') === undefined &&
                      !cache_lib.freshnessLifetimeGreaterThan(
                         headers, msInElevenMonths) &&
                      cache_lib.freshnessLifetimeGreaterThan(headers, 
                         msInAMonth);
                }),
  //
  // Proxy caching rules
  // 
  new CacheRule('Due to a bug in some proxy caching servers,' +
                ' the following publicly' +
                ' cacheable, compressible resources should use' +
                ' "Cache-Control: private" or "Vary: Accept-Encoding":',
                hintlet.SEVERITY_WARNING,
                function(url, headers, type, dataRecord) {              
                  // Support for compressed resources is broken on        
                  // some proxies. The HTTP RFC does not call out         
                  // that Content-Encoding should be a part of the        
                  // cache key, which causes clients to break if a        
                  // compressed response is served to a client that       
                  // doesn't support compressed content. Most HTTP        
                  // proxies work around this bug in the spec, but        
                  // some don't. This function detects resources
                  // that are not properly configured for caching by      
                  // these proxies. We do not check for the presence      
                  // of Content-Encoding here because we recommend        
                  // gzipping these resources elsewhere, so we
                  // assume that the client is going to enable
                  // compression for these resources if they haven't      
                  // already.                               
                  return hintlet.hasHeader(headers, 'Set-Cookie')
                           === undefined &&
                      isCompressibleResourceType(type) &&   
                      cache_lib.isPubliclyCacheable(dataRecord) &&  
                      !hintlet.headerContains(headers, 'Vary', 
                          'Accept-Encoding');
                }), 
  new CacheRule('Resources with a "?" in the URL are not cached by most' +
                ' proxy caching servers. Remove the query string and' +
                ' encode the parameters into the URL for the following' +
                ' resources:',
                hintlet.SEVERITY_WARNING,
                function(url, headers, type, dataRecord) {
                  // Static files should be publicly cacheable unless
                  // responses contain a Set-Cookie header.
                  return url.indexOf('?') >= 0 &&
                      hintlet.hasHeader(headers, 'Set-Cookie') === undefined &&
                      cache_lib.isPubliclyCacheable(dataRecord);
                }),
  new CacheRule('Consider adding a "Cache-Control: public" header to the' +
                ' following resource:',
                // We do not know for certain which if any HTTP
                // proxies require CC: public in order to cache 
                // content, so we make this informational for now.
                hintlet.SEVERITY_INFO,
                function(url, headers, type, dataRecord) {
                  // Static files should be publicly cacheable unless
                  // responses contain a Set-Cookie header.
                  return cache_lib.isCacheableResourceType(type) &&
                      !isCompressibleResourceType(type) &&
                      !hintlet.headerContains(headers, 'Cache-Control', 
                                              'public') &&
                      hintlet.hasHeader(headers, 'Set-Cookie') === undefined;
                }),
  new CacheRule('The following publicly cacheable resources contain' +
                ' a Set-Cookie header. This security vulnerability' +
                ' can cause cookies to be shared by multiple users.',
                hintlet.SEVERITY_CRITICAL,
                function(url, headers, type, dataRecord) {
                  // Files with Cookie headers should never be publicly
                  // cached.
                  return hintlet.hasHeader(headers, 'Set-Cookie') &&
                      cache_lib.isPubliclyCacheable(dataRecord);
                })
];

var HINTLET_NAME="Resource Caching"
hintlet.register(HINTLET_NAME, function(dataRecord) {
  if (dataRecord.type != hintlet.types.NETWORK_RESOURCE_RESPONSE) {
    return;
  }

  // For debugging
  // var jsonString = JSON.stringify(dataRecord);
  // hintlet.log(HINTLET_NAME + ": Processing: " + jsonString);

  // TODO(zundel): Figure out a way to exclude the first document fetch from
  //   these rules.
  // Don't run any rules on the main document, iframes, XHRs or redirects.
  // This is because these rules recommend maxing out the cache settings for 
  // all resources.
  // TODO(tonyg): Figure out some good rules around caching XHRs.
  // TODO(tonyg): Figure out if there is any way to cache a redirect.
  if (type == hintlet.RESOURCE_TYPE_DOCUMENT 
      || type == hintlet.RESOURCE_TYPE_OTHER) {
    return;
  }
  var type = hintlet.getResourceType(dataRecord);

  var headers = dataRecord.data.headers;
  var responseCode = dataRecord.data.responseCode;
  var url = dataRecord.data.url;
  // Don't run any rules on URLs that explicitly do not want to be cached
  // (e.g. beacons).
  if (cache_lib.isExplicitlyNonCacheable(dataRecord)) {
    return;
  }

  // Iterate through the rules defined in the rules list.
  var found = 0;
  for (var j = 0; j < rules.length; j++) {
    if (cache_lib.isNonCacheableResourceType(type)) {
      continue;
    }
    if (rules[j].exec.call(rules[j], url, headers, type, dataRecord)) {
      hintlet.addHint(HINTLET_NAME, dataRecord.time,
          rules[j].message + " " + url,
          dataRecord.sequence, rules[j].severity);
     found++;
    }
  }

}); // End hintlet.register()

})();  // End closure

// Make sure the cache_lib is loaded.  
hintlet.load("rules/cache_lib.js")
