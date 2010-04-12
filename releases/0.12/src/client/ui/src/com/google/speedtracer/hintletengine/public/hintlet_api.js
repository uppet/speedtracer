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
 * hintlet_api.js - defines an API for hintlet rule writers.
 * 
 * The functions and data structures in this file serve the following purposes:
 *   - To register record handling methods to receive new data from the browser
 *   - To provide common utility functions for processing browser data. 
 *   - Provide a set of constants for evaluating data records.
 */

// rules - List of objects { name, callback function} used to represent 
// hintlet rules that have called the hintlet.register() method.
hintlet.rules = [];

// Modified from PageSpeed componentsCollectorService.js
// Component Types used by mozilla
// http://www.xulplanet.com/references/xpcomref/ifaces/nsIContentPolicy.html
hintlet.RESOURCE_TYPE_OTHER = 1; // Typically XHR
hintlet.RESOURCE_TYPE_SCRIPT = 2;
hintlet.RESOURCE_TYPE_IMAGE = 3;
hintlet.RESOURCE_TYPE_CSSIMAGE = 31;  
hintlet.RESOURCE_TYPE_FAVICON = 32;  
hintlet.RESOURCE_TYPE_STYLESHEET = 4;
hintlet.RESOURCE_TYPE_OBJECT = 5; // i.e. Flash
hintlet.RESOURCE_TYPE_DOCUMENT = 6;
hintlet.RESOURCE_TYPE_SUBDOCUMENT = 7; // Iframe
hintlet.RESOURCE_TYPE_REDIRECT = 71;  // Custom type
hintlet.RESOURCE_TYPE_JS_REDIRECT = 72;  // Custom type
hintlet.RESOURCE_TYPE_REFRESH = 8;  // Unused
hintlet.RESOURCE_TYPE_XBL = 9;  // Unused
hintlet.RESOURCE_TYPE_PING = 10;  // Unused
hintlet.RESOURCE_TYPE_XMLHTTPREQUEST = 11;
hintlet.RESOURCE_TYPE_OBJECT_SUBREQUEST = 12;  // Unused

/**
 * Severity constant values.
 */
hintlet.SEVERITY_INFO = 3;
hintlet.SEVERITY_WARNING = 2;
hintlet.SEVERITY_CRITICAL = 1;

/**
 * Function used by hintlet rules to register themselves with the hintlet 
 * engine.
 * name (String): Identifier to be associated with the hintlet
 * callback (Function) : function to call back, taking 2 parameters
 *     function(dataRecord)
 */
hintlet.register = function (name, callback) {
  hintlet.rules = hintlet.rules.concat({ "name" : name, "callback": callback});
  hintlet.log("Registered hintlet: " + name);
}

/* 
 * Wrappers for builtin functions:
 */

hintlet._formatHint = function(hintletRule, timestamp, description,
    refRecord, severity) {
  if (timestamp == null) {
    throw new Error( hintletRule + ": timestamp must be defined");
  }
  var severity_out = (severity == null) ? hintlet.SEVERITY_INFO  : severity;
  var value = {"hintletRule" : hintletRule, 
               "timestamp" : timestamp,
               "description" : description,
               "refRecord" : refRecord,
               "severity" : severity_out };
  return value;
}
 
/**
 * Sends a hint record (as JSON) to the user interface.
 *   hintletRule (String) - name of the hintlet rule that generated this record
 *   timestamp (Number) - time associated with the hint
 *   description (String) - human readable description of the hint
 *   refRecord - Sequence number of the record that triggered this hint 
  *      record
 *   severity - Severity of the problem found.  Default is SEVERITY_INFO.
 */
hintlet.addHint = function(hintletRule, timestamp, description,
      refRecord, severity) {
  var value = hintlet._formatHint(hintletRule, timestamp, description,
      refRecord, severity);
  var hintMessage = {
    type: 2,
    payload: value
  };
  // Encode the object as JSON and send it to the UI
  var jsonString = JSON.stringify(hintMessage);
  
  // Send the hint back over to the main page.
  self.postMessage(jsonString);
}

/**
 * Log a message and send it back to the browser.
 *   value (String) - a message to write to the Chrome log.
 */
hintlet.log = function(value) {
  // Send the message back to the plugin to be logged.
  var hintMessage = {
    type: 1,
    payload: value
  };
  // Encode the object as JSON and send it to the UI
  var jsonString = JSON.stringify(hintMessage);

  // Send the hint back over to the main page.
  self.postMessage(jsonString);
}

/**
 * Load external JavaScript from another file.
 *   path (String) - path relative to the hintlet engine root directory.
 */
hintlet.load = function(path) {
  importScripts(path);
}

/*
 * Utility functions:
 */

/**
 * Translate a type number to a string.
 * Returns undefined on failure.
 */
hintlet.typeToString = function(typeNumber) {
  if (typeNumber < 0 || typeNumber > hintlet.webkitTypeList.length) {
    // Normalize by Java's Max Int which is 0x7fffffff, or 2147483647 in decimal
    var maxInt = 2147483647;
    var speedTracerTypeNumber = maxInt - typeNumber;
    if (speedTracerTypeNumber < 0 ||
      speedTracerTypeNumber > hintlet.speedTracerTypeList.length) {
      return undefined;
    }
    return hintlet.speedTracerTypeList[speedTracerTypeNumber];
  }
  return hintlet.webkitTypeList[typeNumber];
}

/** 
 * Translate a type string name to a numeric value.
 * Returns undefined on failure.
 */
hintlet.stringToType = function(typeString) {
  var value = hintlet.types[typeString]; 
  return value;
}

/**
 * Returns the type of resource, given the url and header map,
 * as one of the hintlet.RESOURCE_TYPE_XXX constants.
 *
 * Ripped from Page Speed
 */
// Cached regexps
hintlet.__mime_type_regexp = /^[^\/;]+\/[^\/;]+/;
hintlet.__image_type_regexp = /^image\//;
hintlet.__favicon_regexp = /\/favicon.ico$/;
hintlet.getResourceType = function(url, headers) {

  // Looks in the specified dataRecords at the mime type embedded as the 
  // prefix of the Content-Type header and returns the appropriate resource type.
  var contentTypeHeader = hintlet.hasHeader(headers, 'Content-Type');
  if (contentTypeHeader === undefined) {
    return hintlet.RESOURCE_TYPE_OTHER;
  }
  // hintlet.log("Got contentTypeHeader: " + contentTypeHeader);
  var match = contentTypeHeader.match(hintlet.__mime_type_regexp);
  if (match == null || match.length < 1) {
    return hintlet.RESOURCE_TYPE_OTHER;
  }
  var mimeType = match[0];
  // hintlet.log("Got mimeType: " + mimeType);
  if (mimeType == "text/plain" 
      || mimeType == "text/html" 
      || mimeType == "text/xml" 
      || mimeType == "application/xml" 
      || mimeType == "application/json") {
    return hintlet.RESOURCE_TYPE_DOCUMENT;
  }
  if (mimeType == "text/css") {
    return hintlet.RESOURCE_TYPE_STYLESHEET;
  }
  if (mimeType == "text/javascript") {
    return hintlet.RESOURCE_TYPE_SCRIPT;
  }
  // TODO(zundel): this test is less than complete.
  if (mimeType == "image/vnd.microsoft.icon" || 
      (url &&
       url.match(hintlet.__favicon_regexp))) {
    return hintlet.RESOURCE_TYPE_FAVICON;
  }
  if (mimeType.match(hintlet.__image_type_regexp)) {
    return hintlet.RESOURCE_TYPE_IMAGE;
  }

  return hintlet.RESOURCE_TYPE_OTHER;
}

/**
 * Format a millisecond value to a number of seconds with the specified
 * number of digits following the decimal.
 */
hintlet.formatSeconds = function(ms, decimalPlaces) {
  return Number(ms/1000).toFixed(decimalPlaces) + "s";
}

/**
 * Format a millisecond value with the specified number of digits 
 * following the decimal.
 */
hintlet.formatMilliseconds = function(ms, decimalPlaces) {
  return Number(ms).toFixed(decimalPlaces) + "ms";
}

/**
 * Determines if the given headers contain a header.  Performs a 
 *   case-insensitive comparison.
 * @param {Object} headers An object with a key for each header and a value
 *     containing the contents of that header.
 * @param {string} targetHeader The header to match.
 * @return {Object} actual header value if found.  undefined if the headers do 
 *  not contain the given header.
 */
hintlet.hasHeader = function(headers, targetHeader) {
  var targetHeaderLc = targetHeader.toLowerCase();
  for (var prop in headers) {
    if (prop.toLowerCase() == targetHeaderLc) {
      if (headers[prop] === undefined) {
        return null;
      }
      return headers[prop];
    }
  }
  return undefined;
}

/**
 * Determines if the given headers contain a header that contains the target
 * string (case insensitive).
 * @param {Object} headers An object with a key for each header and a value
 *     containing the contents of that header.
 * @param {string} targetHeader The header to match.
 * @param {string} targetString The string to search for in the header.
 * @return {boolean} true iff the headers contain the given header and it
 *     contains the target string.
 */
hintlet.headerContains = function(headers, targetHeader, targetString) {
  var value = hintlet.hasHeader(headers, targetHeader);
  if (value === undefined) { return false; }
  // TODO(zundel): This could be made more efficient if we didn't create a new 
  // RegExp every time it is called.
  var re = new RegExp(targetString, 'im');
  return (value.match(re) != null);
}

/**
 * Returns 'true' if the 'Content-Encoding' header indicates this request 
 * is compressed.
 */
hintlet.isCompressed = function(headers) {
  if (!headers) {
    return false;
  }
  var prop = hintlet.hasHeader(headers, 'Content-Encoding');
  if (prop === undefined) {
    return false;
  }
  switch (prop.toLowerCase()) {
    case 'compress':
    case 'deflate':
    case 'gzip':
    case 'pack200-gzip': // Java Archives
    case 'bzip2': // Not registered with IANA, but supported by some browsers
    case 'sdch': // Not registered with IANA, but supported by Google Toolbar 
      return true;
  }  
  return false;
}

var resources = {};

/**
 * Updates our backing store of accumulated network resource information by
 * merging in an inspector updateResource object.
 * 
 * NOTE: We do not use store the finished/completion update. We allow the
 * RESOURCE_FINISH timeline record to deliver that information.
 * 
 * @param {Object} the inspector updateResource object used to update our
 *     backing store.
 */
hintlet._updateResource = function(update) {
  var updateData = update.data;
  var resourceData = resources[updateData.identifier];
  
  if (!resourceData) {
    if (updateData.url) {
      resources[updateData.identifier] = updateData;
    }
    return;
  }

  if (updateData.didRequestChange) {
    resourceData.domain = updateData.host;
    resourceData.path = updateData.path;
    resourceData.lastPathComponent = updateData.lastPathComponent;
    resourceData.requestHeaders = updateData.requestHeaders;
    resourceData.cached = updateData.cached;
    resourceData.requestMethod = updateData.requestMethod;
    resourceData.mainResource = updateData.mainResource;
  }
  
  if (updateData.didResponseChange) {
    resourceData.responseHeaders = updateData.responseHeaders;
    resourceData.statusCode = updateData.statusCode;
    resourceData.mimeType = updateData.mimeType;
  }
 
  if (updateData.didLengthChange) {
    resourceData.contentLength = updateData.contentLength;
  }

  if (updateData.didTimingChange) {
    if (updateData.startTime) {
      resourceData.startTime = updateData.startTime;
    }

    if (updateData.responseReceivedTime) {
      resourceData.responseReceivedTime = updateData.responseReceivedTime;
    }
  }
}

/**
 * For the purposes of not leaking memory, we assume hintlets will have used
 * the record by the time an update comes in that signals the resource is 
 * completed. So we can safely remove the associate accumulated state from out
 * backing store.
 * @param {Object} the updateResource object
 */
hintlet._maybeForgetResource = function(update) {
  var updateData = update.data;
  // By now all hintlets should have had a crack at this. The finish checkpoint
  // record gets fired before this completion change record goes out, except in
  // the case of redirects. But arguably, hintlets dont need to see redirects,
  // only the the request that results from the redirect.
  if (updateData.didCompletionChange &&
      (updateData.failed || updateData.finished)) {
    delete resources[updateData.identifier];
  }
}

/**
 * Getter for the accumulated information about a resource.
 */
hintlet.getResourceData = function(identifier) {
  return resources[identifier];
}