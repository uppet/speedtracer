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

// Detect a Google Web Toolkit compiled application and emit GWT specific hints

// Make a namespace for this rule using a closure
(function() {  // Begin closure

var HINTLET_NAME = "GWT Application Detection";

// Emit hintlets based on the size of the initial download
var CODE_SPLIT_INFO_SIZE_THRESHOLD = 250000;
var CODE_SPLIT_WARNING_SIZE_THRESHOLD = 500000;
var CODE_SPLIT_CRITICAL_SIZE_THRESHOLD = 1000000;

function initHintletData() {
  var data = new Object();
  data.isGWT = false;
  data.noCacheRecord = undefined;
  data.resourceData = undefined;
  data.resourceId = undefined;
  data.analyzedNoCache = false;
  return data;
}

var hintletData = initHintletData();

function analyzeNoCacheRecord() {
  var resourceData = hintletData.resourceData;  
  if (!cache_lib.isExplicitlyNonCacheable(resourceData.responseHeaders,
		  resourceData.url, resourceData.statusCode)) {
    hintlet.addHint(HINTLET_NAME, resourceData.responseReceivedTime,
      "GWT selection script '.nocache.js' file should be set as non-cacheable",
      hintletData.noCacheRecord.sequence, hintlet.SEVERITY_CRITICAL);
  }
}

function analyzeDownloadSize(dataRecord, expectedContentLength) {
  var size = expectedContentLength;
  size = (size === undefined) ? 0 : size;

  var severity = null;
  if (size > CODE_SPLIT_CRITICAL_SIZE_THRESHOLD) {
    severity = hintlet.SEVERITY_CRITICAL;
  } else if (size > CODE_SPLIT_WARNING_SIZE_THRESHOLD) {
    severity = hintlet.SEVERITY_WARNING;
  } else if (size > CODE_SPLIT_INFO_SIZE_THRESHOLD) {
    severity = hintlet.SEVERITY_INFO;
  }

  if (severity !== null) {
    hintlet.addHint(HINTLET_NAME, resourceData.responseReceivedTime,
      "The size of the initial GWT download (" + hintletData.strongNameUrl 
      + ") is " + size + " bytes.  Consider using GWT.runAsync() code splitting "
      + "and the Compile Report to reduce the size of the initial download.",
    dataRecord.sequence, severity);
  }
}

function isStrongName(url) {
  return (url.search("[0-9A-F]{32}\\.cache\\.html$") >= 0);
}

hintlet.register(HINTLET_NAME, function(dataRecord){

  if (dataRecord.type === hintlet.types.TAB_CHANGED) {
    // Reset state after a page transition
    hintletData = initHintletData();
    return;
  }

  // TODO(zundel): Take into account the time to download/parse the strong name
  // to suggest code splitting opportunity. Be sure to omit Script Eval time.

  if (dataRecord.type !== hintlet.types.RESOURCE_FINISH) {
    return;
  }

  var resourceData = hintlet.getResourceData(dataRecord.data.identifier);
  if (!resourceData) {
    return;
  }

  hintletData.resourceData = resourceData;

  var url = resourceData.url;

  // The first file loaded should be the .nocache.js file - the selection 
  // script
  if (url.search("\\.nocache\\.js$") >= 0) {
    hintletData.noCacheRecord = dataRecord;
    return;
  }

  if (hintletData.isGWT || !hintletData.noCacheRecord) {
    // Either we've already processed this page, or haven't seen the
    // selection script yet.
    return;
  }

  if (isStrongName(url) && !hintletData.analyzedNoCache) {
    // Keep track of the start of the initial strong name fetch.
    hintletData.strongNameTime = resourceData.responseReceivedTime;
    hintletData.strongNameUrl = url;
    hintletData.resourceId = resourceData.identifier;
    analyzeNoCacheRecord();
    hintletData.analyzedNoCache = true;
    // Keep the hintlet from firing again.
    hintletData.isGWT = true;
    analyzeDownloadSize(dataRecord, resourceData.expectedContentLength);
  }
});

})();  // End closure

// Make sure the cache_lib is loaded.  
hintlet.load("rules/cache_lib.js")
