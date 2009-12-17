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

// No hintlets for code splitting will be emitted if the size
// is not at least past this threshold
var CODE_SPLIT_SIZE_THRESHOLD = 100000;

// Emit hintlets based on the time to load and parse the initial download
var CODE_SPLIT_INFO_TIME_THRESHOLD = 1000;
var CODE_SPLIT_WARNING_TIME_THRESHOLD = 3000;
var CODE_SPLIT_CRITICAL_TIME_THRESHOLD = 5000;

// Emit hintlets based on the size of the initial download
var CODE_SPLIT_INFO_SIZE_THRESHOLD = 250000;
var CODE_SPLIT_WARNING_SIZE_THRESHOLD = 500000;
var CODE_SPLIT_CRITICAL_SIZE_THRESHOLD = 1000000;

function initHintletData() {
  var data = new Object();
  data.isGWT = false;
  data.noCacheRecord = undefined;
  data.strongNameTime = undefined;
  data.strongNameElapsed = undefined;
  data.parseTimeAnalyzed = false;
  data.resourceId = undefined;
  data.analyzedNoCache = false;
  return data;
}

var hintletData = initHintletData();

function analyzeNoCacheRecord() {
  if (!cache_lib.isExplicitlyNonCacheable(hintletData.noCacheRecord)) {
    hintlet.addHint(HINTLET_NAME, hintletData.noCacheRecord.time,
      "GWT selection script '.nocache.js' file should be set as non-cacheable",
      hintletData.noCacheRecord.sequence, hintlet.SEVERITY_CRITICAL);
  }
}

function analyzeStrongNameFetchAndParseTime(parseHtmlRecord) {
  if (hintletData.strongNameSize < CODE_SPLIT_SIZE_THRESHOLD) {
    return;
  }

  var elapsed = (parseHtmlRecord.time + parseHtmlRecord.duration) 
    - hintletData.strongNameTime;

  var severity = null;
  if (elapsed > CODE_SPLIT_CRITICAL_TIME_THRESHOLD) {
    severity = hintlet.SEVERITY_CRITICAL;
  } else if (elapsed > CODE_SPLIT_WARNING_TIME_THRESHOLD) {
    severity = hintlet.SEVERITY_WARNING;
  } else if (elapsed > CODE_SPLIT_INFO_TIME_THRESHOLD) {
    severity = hintlet.SEVERITY_INFO;
  }

  if (severity !== null) {
    hintlet.addHint(HINTLET_NAME, parseHtmlRecord.time,
      "Initial GWT download (" + hintletData.strongNameUrl +") took " 
      + hintlet.formatMilliseconds(elapsed) 
      + " to load. (" + hintletData.strongNameSize + " bytes). "
      + "Consider using GWT.runAsync() code splitting and the "
      + "Compile Report to reduce the size of the initial download.",
      parseHtmlRecord.sequence, severity);    
  }
}

function analyzeDownloadSize(dataRecord) {
  var size = dataRecord.data.contentLength;
  size = size ? size : 0;
  hintletData.strongNameSize = size;

  var severity = null;
  if (size > CODE_SPLIT_CRITICAL_SIZE_THRESHOLD) {
    severity = hintlet.SEVERITY_CRITICAL;
  } else if (size > CODE_SPLIT_WARNING_SIZE_THRESHOLD) {
    severity = hintlet.SEVERITY_WARNING;
  } else if (size > CODE_SPLIT_INFO_SIZE_THRESHOLD) {
    severity = hintlet.SEVERITY_INFO;
  }

  if (severity !== null) {
    hintlet.addHint(HINTLET_NAME, dataRecord.time,
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
  } else if (!hintletData.parseTimeAnalyzed 
      && dataRecord.type === hintlet.types.PARSE_HTML_EVENT) {
    // This is just a heuristic - there is no actual data tying this event
    // to the strong name file other than its proximity to the end of the
    // resource load.
    analyzeStrongNameFetchAndParseTime(dataRecord);
    hintletData.parseTimeAnalyzed = true; 
  }

  if (dataRecord.type !== hintlet.types.NETWORK_RESOURCE_RESPONSE
      && dataRecord.type !== hintlet.types.NETWORK_RESOURCE_FINISH) {
    return;
  }

  var url = dataRecord.data.url;

  // The first file loaded should be the .nocache.js file - the selection 
  // script
  if (dataRecord.type === hintlet.types.NETWORK_RESOURCE_RESPONSE
      && url.search("\\.nocache\\.js$") >= 0) {
    hintletData.noCacheRecord = dataRecord;
    return;
  }

  if (hintletData.isGWT || !hintletData.noCacheRecord) {
    // Either we've already processed this page, or haven't seen the
    // selection script yet.
    return;
  }

  if (dataRecord.type === hintlet.types.NETWORK_RESOURCE_RESPONSE 
      && isStrongName(url) && !hintletData.analyzedNoCache) {
    // Keep track of the start of the initial strong name fetch.
    hintletData.strongNameTime = dataRecord.time;
    hintletData.strongNameUrl = url;
    hintletData.resourceId = dataRecord.data.resourceId;
    analyzeNoCacheRecord();
    hintletData.analyzedNoCache = true;
  } else if (dataRecord.type === hintlet.types.NETWORK_RESOURCE_FINISH 
      && hintletData.resourceId === dataRecord.data.resourceId) {
    // Keep the hintlet from firing again
    hintletData.isGWT = true;
    analyzeDownloadSize(dataRecord);
  }
});

})();  // End closure

// Make sure the cache_lib is loaded.  
hintlet.load("rules/cache_lib.js")
