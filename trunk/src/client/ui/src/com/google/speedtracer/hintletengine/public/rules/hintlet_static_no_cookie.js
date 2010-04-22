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

// Returns the cookie header if found, undefined if not found
var hasCookie = function(headers) {
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

hintlet.register(HINTLET_NAME, function(dataRecord){
  if (dataRecord.type != hintlet.types.RESOURCE_FINISH) {
    return;
  }
  var resourceData = hintlet.getResourceData(dataRecord.data.identifier);
  if (!resourceData) {
    return;
  }

  var resourceType = hintlet.getResourceType(resourceData);

  // Make sure this is a static resource
  switch(resourceType) {
    case hintlet.RESOURCE_TYPE_STYLESHEET:
    case hintlet.RESOURCE_TYPE_SCRIPT:
    case hintlet.RESOURCE_TYPE_IMAGE:
    case hintlet.RESOURCE_TYPE_MEDIA:
      break;
    default:
      return;
  }

  var cookie = hasCookie(resourceData.responseHeaders);
  var sequence = dataRecord.sequence;

  if (cookie !== undefined) {
    hintlet.addHint(HINTLET_NAME, resourceData.responseReceivedTime,
        "URL " + resourceData.url + " is static content that should be "
        + "served from a domain that does not set cookies.  Found " 
        + (cookie.length + 8) + " extra bytes from cookie.",
        sequence, hintlet.SEVERITY_INFO);
  }
}); // End hintlet.register()

})();  // End closure
