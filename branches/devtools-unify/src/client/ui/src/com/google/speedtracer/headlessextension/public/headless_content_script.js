/*
 * Copyright 2010 Google Inc.
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

/*
 * Chrome Extension Content script that provides a bridge between the 
 * background page and the API embedded in the page to control SpeedTracer.  
 * This script injects the API when it first comes up, then waits for 
 * messages from either the Background Page or API.
 */

// Handshake message received from API
var API_READY_MSG = 1;
// Constant values defined in MessageType.java and headless_api.js
// TODO(zundel): Create a way to automatically sync these definitions
var PORT_HEADLESS_CLEAR_DATA = 102;  
var PORT_HEADLESS_MONITORING_ON = 103;
var PORT_HEADLESS_MONITORING_OFF = 104;
var PORT_HEADLESS_GET_DUMP = 105;
var PORT_HEADLESS_GET_DUMP_ACK = 106;
var PORT_HEADLESS_SEND_DUMP = 107;
var PORT_HEADLESS_SEND_DUMP_ACK = 108;
var PORT_HEADLESS_MONITORING_ON_ACK = 109;
var PORT_HEADLESS_MONITORING_OFF_ACK = 110;

var extensionPort;
var TO_API_DIV_ID = '__speedtracerToApiElement';
var FROM_API_DIV_ID = '__speedtracerFromApiElement';
var EVENT_NAME = 'SpeedTracer Headless Event';
var apiReady = false;
var toApiDiv;
var fromApiDiv;
var logDiv;
var contentScriptEvent = document.createEvent('Event');
contentScriptEvent.initEvent(EVENT_NAME, true, true);
var eventRecordData = [];
var toDispatch = [];
var xhrUrl;
var xhrTime = 10000; //default time to send dump is 10 seconds

// Optional debug loggging
var loggingEnabled = false;
var logDiv;
var toLog = [];

// A Port style listener function
function handleMessagesFromBackgroundPage(backgroundMsg) {
  log("Received record from Background Page type: " + backgroundMsg.type);
  switch (backgroundMsg.type) {
  case PORT_HEADLESS_GET_DUMP_ACK:
  case PORT_HEADLESS_SEND_DUMP_ACK:
  case PORT_HEADLESS_MONITORING_ON_ACK:
  case PORT_HEADLESS_MONITORING_OFF_ACK:
    // Forward results to the API
    sendMsgToApi(JSON.stringify(backgroundMsg));
    break;
  default:
    // message not implemented
    log("Message type " + backgroundMsg.type + " not handled.");
  }
}

// A DOM event listener on the special fromApiDiv
function handleMessagesFromApi() {
  var tmpRecString = fromApiDiv.innerText;
  var tmpRec = JSON.parse(tmpRecString);
  fromApiDiv.innerText = "";
  log("Received record from API type: " + tmpRec.type);
  switch(tmpRec.type) {
  case API_READY_MSG:
    // API is ready.
    apiReady = true;
    window.setTimeout(function() {
      while (toDispatch.length > 0) {
        sendMsgToApi(toDispatch.shift());
      }
    }, 1);
  break;
  case PORT_HEADLESS_CLEAR_DATA:
  case PORT_HEADLESS_MONITORING_ON:
  case PORT_HEADLESS_MONITORING_OFF:
  case PORT_HEADLESS_GET_DUMP:
  case PORT_HEADLESS_SEND_DUMP:
    // Simply forward to background page
    sendMsgToBackground(tmpRec);
    break;
  default:
    // Unhandled message.
  }
}

// Creates 2 divs in the page for communication between the API embedded
// in the page and this script.
function addHiddenDivsImpl() {
  toApiDiv =  document.createElement('div');
  fromApiDiv =  document.createElement('div');
  toApiDiv.style.display = 'none';
  fromApiDiv.style.display = 'none';
  toApiDiv.id = TO_API_DIV_ID;
  fromApiDiv.id = FROM_API_DIV_ID;
  document.body.appendChild(toApiDiv);
  document.body.appendChild(fromApiDiv);
  fromApiDiv.addEventListener(EVENT_NAME, handleMessagesFromApi);

  if (loggingEnabled) {
    logDiv = document.createElement('div');
    document.body.appendChild(logDiv);
    while (toLog.length > 0) {
      log(toLog.shift());
    }
  }
}

function addHiddenDivs() {
  // The DOM might not be ready when this function is called
  if (document.readyState != 'complete') {
    window.addEventListener("DOMContentLoaded", addHiddenDivsImpl, false);
  } else {
    addHiddenDivsImpl();
  }
}

// Send a message to the API embedded in the main page.
function sendMsgToApi(message) {
  if (apiReady) {
    toApiDiv.innerText = message;
    toApiDiv.dispatchEvent(contentScriptEvent);
  } else {
    toDispatch.push(message);
  }
}

// Send a message to the Extension's Background Page.
function sendMsgToBackground(message) {
  extensionPort.postMessage(message);
}

function log(message) {
  if (loggingEnabled) {
    if (logDiv) {
      logDiv.innerHTML = logDiv.innerHTML + "<br/>\nCONTENT_SCRIPT: " 
          + message;
    } else {
      toLog.push(message);
    }
  }
}

// Triggers monitoring to turn on from the query string. 
// originalUrl - pass String(window.location)
// queryString - pass window.location.search
// (These above arguments are externalized for test purposes.)

// TODO(zundel): Encountering escaped URLs could wreak havoc here.  ":" and "/" 
//   are reserved and both would be used as parameters to SpeedTracer 
//   configuration.  Determine if this will ever be a problem, and if so, run
//   the string through a URL de-escaper.
function parseQueryString(originalUrl, queryString) {
  if (queryString.charAt(0) == "?") {
    var args = queryString.substring(1).split("&");
    for (var i = 0; i < args.length; ++i) {
      var queryArg = args[i];
      if (queryArg.search(/^SpeedTracer=/) == 0) {
        parseSpeedTracerQueryString(originalUrl, queryArg);
        break;
      }
    }
  }
}

// Take a string like this 
// SppedTracer=monitor,header(foo:bar),xhr(http://nowhere.com),timeout(5000)
// and parses it
//
// values:
// -monitor
//   Turns on monitoring when the content script is loaded.  After the 
//   ack for turning on monitoring is received, the current URL minus the 
//   arguments parsed are used to reload the page.  
// -header(name:value)
//  Sets a name/value pair for a header to send in the xhr. Multiple headers can
//  be set by using multiple instances (e.g.
//  SpeedTracer=header(foo:bar),header(blah:asdf)
// -xhr(url)
//  Sets where the dump XHR should go
// -timeout(value)
//  Sets the timeout to send the dump to value ms
//
function parseSpeedTracerQueryString(originalUrl, queryArg) {
  var header = {name:'', revision:''};
  var turnOnMonitoring = false;
  var persistentValues = [];
  var values=queryArg.substring(12).split(",");

  for (var i = 0; i < values.length; ++i) {
    var value = values[i];
    if (!value) {
      // handle empty value strings
      continue;
    }

    var lcValue = value.toLowerCase();
    if (lcValue === "monitor") {
      turnOnMonitoring = true;
    } else if (lcValue.search(/^header\(/) == 0) {
      var headerPair = value.substr(7, value.length - 8);
      var pair = headerPair.split(":");
      if (pair.length === 2) {
        header[pair[0]]=pair[1];
      }
      persistentValues.push(value);
    } else if (lcValue.search(/^xhr\(/) == 0) {
      xhrUrl = value.substr(4, value.length - 5);
      persistentValues.push(value);
    } else if (lcValue.search(/^timeout\(/) == 0) {
      xhrTime = value.substr(8, value.length - 9);
      persistentValues.push(value);
    } else {
      log("unknown parameter: " + value);
    }
  }

  if (turnOnMonitoring) {
    var newUrl = "";
    if (persistentValues.length == 0) {
      newUrl = removeQuerySubString(originalUrl, queryArg);
    } else {
      var startIndex = originalUrl.indexOf(queryArg);
      var prefix = originalUrl.slice(0, startIndex);
      var suffix = originalUrl.slice(startIndex + queryArg.length);
      var persistentValuesString = "";
      for(var i = 0, n = persistentValues.length; i < n; i++) {
        persistentValuesString += persistentValues[i];
        if (i + 1 < n) {
          persistentValuesString += ",";
        }
      }
      newUrl = prefix + "SpeedTracer=" + persistentValuesString + suffix;
    }
    log("Turning on monitoring and setting to reload:" + newUrl);
    sendMsgToBackground({'type':PORT_HEADLESS_MONITORING_ON, 
      'options':{'clearData':true,'reload':newUrl, 'header':{}}});
  } else if (xhrUrl) {
    setTimeout(function(xhrUrl, header) {
      log("sending xhr to " + xhrUrl);
      sendMsgToBackground({'type':PORT_HEADLESS_SEND_DUMP,
        'url':xhrUrl, 'header':header});
      //turn off monitoring since we're done
      sendMsgToBackground({'type':PORT_HEADLESS_MONITORING_OFF});
    }, xhrTime, xhrUrl, header);
  }
}

// Removes 'querySubString' from the middle of 'queryString'
// Intended to strip out the speedtracer portion of the query so
// the page can be reloaded without it.
function removeQuerySubString(queryString, querySubString) {
  var startIndex = queryString.indexOf(querySubString);
  if (startIndex >= 0) {
    var prefix = queryString.slice(0, startIndex);
    var suffix = queryString.slice(startIndex + querySubString.length);
    var prefix_tail = prefix.slice(-1);
    if ((prefix_tail === "&" || prefix_tail === "?")
        && (suffix.length > 0 && suffix.slice(0,1) === "&")) {
      // peel off the extra "&"
      suffix = suffix.slice(1);
    } else if (suffix.length === 0 && prefix_tail === "&") {
      prefix = prefix.slice(0, -1);
    }
    return prefix + suffix;
  }

  throw new Error("Couldn't find query substring " + querySubString + " to extract");
}


// Only execute if this code if it is running at the top level window
// in an extension, or running in a unit test
if (window == top || window.__isTest) {
  extensionPort = chrome.extension.connect( {
    name : "HEADLESS_API"
  });
  addHiddenDivs();
  extensionPort.onMessage.addListener(handleMessagesFromBackgroundPage);
}

// Only execute if this is actually running in an extension
if (window == top && !window.__isTest) {
  parseQueryString(String(window.location), window.location.search);
}

