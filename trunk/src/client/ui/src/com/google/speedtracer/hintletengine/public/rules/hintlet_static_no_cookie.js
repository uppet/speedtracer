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
 * Rule to find static content served from a domain that sets cookies.
 * 
 * Cloned from Page Speed: staticNoCookieLint.js
 */

// Make a namespace for this rule using a closure
(function() {  // Begin closure
var HINTLET_NAME = "Static Resource served from domains with cookies";
// Stores Network Resource Start events to pair with the Response events later.
var RESOURCE_MAP = {};

// Returns the cookie header if found, undefined if not found
var hasCookie = function(dataRecord) {
  var headers = dataRecord.data.headers;
  var cookie = hintlet.hasHeader(headers, 'Set-Cookie');
  if (cookie !== undefined && cookie.length > 0) {
    return cookie;
  }
  cookie = hintlet.hasHeader(headers, 'Cookie');
  if (cookie !== undefined && cookie.length > 0) {
    return cookie;
  }
  return undefined;
}

// TODO(zundel): Right now I know that the START resources are not going 
// to pass the resourceType test.  There could be some logic to test the
// filename extensions...
hintlet.register(HINTLET_NAME, function(dataRecord){

    if (dataRecord.type == hintlet.types.NETWORK_RESOURCE_START) {
      // Add to the map to work on later.
      RESOURCE_MAP[dataRecord.data.resourceId] = dataRecord;
      return;
    }

    if (dataRecord.type != hintlet.types.NETWORK_RESOURCE_RESPONSE) {
      return;
    }

    var resourceType = hintlet.getResourceType(dataRecord);
    var resourceId = dataRecord.data.resourceId;
    var startRecord = RESOURCE_MAP[resourceId];
    // Clean up memory
    delete RESOURCE_MAP[resourceId];

    // Make sure this is a static resource
    switch(resourceType) {
      case hintlet.RESOURCE_TYPE_SCRIPT:
      case hintlet.RESOURCE_TYPE_IMAGE:
      case hintlet.RESOURCE_TYPE_CSSIMAGE:
      case hintlet.RESOURCE_TYPE_STYLESHEET:
      case hintlet.RESOURCE_TYPE_OBJECT:
        break;
      default:
        return;
    }
        
    var cookie = hasCookie(dataRecord);
    var sequence = dataRecord.sequence;
    if (cookie === undefined) {
      // See if the start record matches.
      if (startRecord !== undefined) {
        cookie = hasCookie(startRecord);
        sequence = startRecord.sequence;
      }
    }

    if (cookie !== undefined) {
      hintlet.addHint(HINTLET_NAME, dataRecord.time,
          "URL " + dataRecord.data.url + " is static content that should be "
          + "served from a domain that does not set cookies.  Found " 
          + (cookie.length + 8) + " extra bytes from cookie.",
          sequence, hintlet.SEVERITY_INFO);
    }
  }); // End hintlet.register()

})();  // End closure
