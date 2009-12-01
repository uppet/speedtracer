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

// Detect a GWT application and emit GWT specific hints

// Make a namespace for this rule using a closure
(function() {  // Begin closure

var HINTLET_NAME = "GWT Application Detection";
var isGWT = false;
var noCacheRecord = undefined;

function analyzeNoCacheRecord() {
  if (!cache_lib.isExplicitlyNonCacheable(noCacheRecord)) {
    hintlet.addHint(HINTLET_NAME, noCacheRecord.time,
      "GWT selection script '.nocache.js' file should be set as non-cacheable",
      noCacheRecord.sequence, hintlet.SEVERITY_CRITICAL);
  }
}

hintlet.register(HINTLET_NAME, function(dataRecord){

  if (dataRecord.type === hintlet.types.TAB_CHANGED) {
    // reset state after a page transition
    isGWT = false;
    noCacheRecord = undefined;
  }

  if (dataRecord.type != hintlet.types.NETWORK_RESOURCE_RESPONSE) {
    return;
  }
  var url = dataRecord.data.url;
  if (!url) {
    return;
  }

  // The first file loaded should be the .nocache.js file - the selection script
  if (url.search("\\.nocache\\.js$") >= 0) {
    noCacheRecord = dataRecord;
    return;
  }

  // Look for cached application logic with strong name
  if (!isGWT && noCacheRecord && url.search("[0-9A-F]{32}\\.cache\\.html$") >= 0) {
    isGWT = true;
    analyzeNoCacheRecord();
    return;
  }

});

})();  // End closure

// Make sure the cache_lib is loaded.  
hintlet.load("rules/cache_lib.js")
