/*
 * Copyright 2008 Google Inc.
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

// Hintlet that flags network resources that are not gzip'ed
// Equivalent to gzipLint.js from Page Speed

// We are looking for resources that do NOT contain 
// Content-Encoding:  that indicates compression (e.g. gzip)
// and have contentLength > 150 bytes
//{
//    "responseHeaders": {
//         "Cache-Control": "private, max-age=0",
//         "Content-Encoding": "gzip",
//         "Content-Length": "2755",
//         "Content-Type": "text/html; charset=UTF-8",
//         "Date": "Fri, 30 Jan 2009 17:12:38 GMT",
//         "Expires": "-1",
//     },
//
//    "contentLength": 200
//}
//

// Make a namespace for this rule using a closure
(function() {  // Begin closure

var HINTLET_NAME = "Uncompressed Resource";

// We consider 150 bytes to be the break-even point for using gzip.
var SIZE_THRESHOLD = 150;

var resourceResponses = {};

hintlet.register(HINTLET_NAME, function(dataRecord){
  if (dataRecord.type != hintlet.types.RESOURCE_FINISH) {
    return;
  }
  var resourceData = hintlet.getResourceData(dataRecord.data.identifier);
  
  if (!resourceData) {
    return;
  }
  
  var url = resourceData.url;
  var headers = resourceData.responseHeaders;

  // Don't suggest compressing very small components.
  var size = resourceData.contentLength;
  if (size < SIZE_THRESHOLD) {
    return;
  }

  var resourceType = hintlet.getResourceType(url, headers);
  switch (resourceType) {
    case hintlet.RESOURCE_TYPE_DOCUMENT:
    case hintlet.RESOURCE_TYPE_STYLESHEET:
    case hintlet.RESOURCE_TYPE_SCRIPT:
    case hintlet.RESOURCE_TYPE_SUBDOCUMENT:
      break;
    default:
      return;
  }

  if (!hintlet.isCompressed(headers)) {
    hintlet.addHint(HINTLET_NAME, resourceData.responseReceivedTime,
          "URL " + url + " was not compressed with gzip or bzip2",
          dataRecord.sequence, hintlet.SEVERITY_INFO);
  }
    
}); // End hintlet.register()

})();  // End closure
