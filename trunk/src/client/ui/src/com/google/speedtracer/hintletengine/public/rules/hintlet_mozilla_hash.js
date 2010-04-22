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

// Firefox has a feeble hash algorithm that is prone to collisions. Check
// for collisions among resources on the same page here.  THis problem has been
// addressed, but not included in all supported versions of Firefox.
//
//   http://hg.mozilla.org/mozilla-central/rev/999999125311
//   https://bugzilla.mozilla.org/show_bug.cgi?id=290032
//
// TODO: If possible, it would be nice to determine if IE has the
// same problem. However, it could be impossible to figure out its hashing
// algorithm.

// Make a namespace for this rule
(function() {  // Begin closure

var HINTLET_NAME = "Mozilla URL Cache Hash Collision";
// Stores previous URLs encountered indexed by their hash code.
var HASHED_URLS = {};
// To detect moving to a new page
var PREVIOUS_URL = "";

/** 
 * Left rotates x by the specified number of bits in a 32 bit field. 
 * @param {number} x The 32 bit unsigned int to rotate.
 * @param {number} bits The number of bits to rotate.
 * @return {number} The rotated number.
 */
function rotateLeft(x, bits) {
  return ((x << bits) & 0xffffffff) | ((x >>> (32 - bits)) & 0xffffffff);
}

/**
 * Returns the hash key that Mozilla uses for the given URL.
 * See original implementations:
 * http://mxr.mozilla.org/seamonkey/source/netwerk/cache/src/nsDiskCacheDevice.cpp#240
 * http://mxr.mozilla.org/mozilla1.8/source/netwerk/cache/src/nsDiskCacheDevice.cpp#270
 * @param {string} url The URL for which to generate the cache key.
 * @return {number} The hash key for the given URL.
 */
function generateMozillaHashKey(url) {
  var h = 0;
  for (var i = 0, len = url.length; i < len; ++i) {
    h = rotateLeft(h, 4) ^ url.charCodeAt(i);
  }
  return h;
}

/**
 * Given a list of URLs, divide them into groups such that
 * all members of each group have the same hash in firefox's
 * cache, and each group has more than one member.  Add the specified
 * url to the persistent store and return any new collisions.
 *
 * @param {String} url The url to check
 * @return {Array.Array.string} Each element of this array is a set
 *    of urls which have the same hash.
 */
function findCacheCollisions(url) {
  var newHash = generateMozillaHashKey(url);
  var results = [];
  if (!HASHED_URLS.hasOwnProperty(newHash)) {
    HASHED_URLS[newHash] = [ url ];
    return results;
  }
  var urls = HASHED_URLS[newHash];
  // Found a potential collision
  var found = false;
  for (var i = 0, j = urls.length; i < j; i++) {
    // Check to make sure we aren't adding the same URL as before
    if (urls[i] == url) {
      found = true;
      break;
    }
  }
  if (!found) {
    // A real collision, keep track of it and emit results.
    HASHED_URLS[newHash].push(url);
    results = HASHED_URLS[newHash];
  }
  return results;
}

hintlet.register(HINTLET_NAME, function(dataRecord) {
  // Reset after a new page is loaded.
  if (dataRecord.type == hintlet.types.TAB_CHANGED 
        && PREVIOUS_URL != dataRecord.data.url) {
    HASHED_URLS = {};
    return;
  } else if (dataRecord.type != hintlet.types.RESOURCE_FINISH) {
    return;
  }

  var resourceData = hintlet.getResourceData(dataRecord.data.identifier);
  if (!resourceData) {
    return;
  }
  
  var url = resourceData.url;
  PREVIOUS_URL = url;

  var responseCode = resourceData.responseCode;
  if (!cache_lib.isCacheableResponseCode(responseCode)) {
    return;
  }
  var resourceType =
      hintlet.getResourceType(resourceData);
  if (cache_lib.isNonCacheableResourceType(resourceType)) {
    return;
  }

  var urlCacheCollisions = findCacheCollisions(url);

  if (urlCacheCollisions.length) {
    var kCacheCollisionWarning = [
        'Due to a URL conflict, the Firefox browser cache can store only ',
        'one of these resources at a time. Changing the URLs of some ',
        'resources can fix this problem. Consult the Page Speed ',
        'documentation for information on how to disambiguate these URLs.'
        ].join('');

    hintlet.addHint(HINTLET_NAME, resourceData.responseReceivedTime,
      "The following URLs cause a conflict in the Firefox browser cache: "
      + urlCacheCollisions.join(" "), dataRecord.sequence, 
      hintlet.SEVERITY_CRITICAL);
  }
});

})();  // End closure

// Make sure the cache_lib is loaded.
hintlet.load("rules/cache_lib.js")
