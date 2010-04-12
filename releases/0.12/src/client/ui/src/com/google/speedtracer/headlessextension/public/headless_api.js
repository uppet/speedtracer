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
 * API injected by content script into a web page for using the SpeedTracer 
 * headless extension.
 *
 * A <script> tag for this script must be included in the page by the page that wants to use
 * the headless extension.
 *
 * <script language="javascript" 
 *         src="chrome-extension://jolleknjmmglebfldiogepklbacoohni/headless_api.js">
 * </script>
 */
(function() { // begin anonymous closure

// Handshake message sent to content script.
var API_READY_MSG = 1;
// Constant values defined in MessageType.java and headless_content_script.js
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

var TO_API_DIV_ID = '__speedtracerToApiElement';
var FROM_API_DIV_ID = '__speedtracerFromApiElement';
var EVENT_NAME = 'SpeedTracer Headless Event';
var fromApiDiv;
var toApiDiv;
var getDumpCallbacks = [];
var sendDumpCallbacks = [];
var monitoringOnCallbacks = [];
var monitoringOffCallbacks = [];
var contentScriptEvent = document.createEvent('Event');
contentScriptEvent.initEvent(EVENT_NAME, true, true);
var toDispatch = [];
var onDOMContentLoadedDelayed = false;

// Optional debug loggging
var loggingEnabled = false;
var logDiv;
var toLog = [];

// Hook into GWT's lightweight metrics.
if (!window.__gwtStatsEvent) {
  window.__gwtStatsEvent = function(event) {
   console.markTimeline("__gwtStatsEvent: " + JSON.stringify(event)); 
  }
} else {
  var origStatsFunc = window.__gwtStatsEvent;
  window.__gwtStatsEvent = function(event) {
   origStatsFunc(event);
   console.markTimeline("__gwtStatsEvent: " + JSON.stringify(event)); 
  }
}

function onDOMContentLoaded() {
  fromApiDiv = document.getElementById(FROM_API_DIV_ID);
  if (fromApiDiv == null && onDOMContentLoadedDelayed == false) {
    // There is a race witht he headless_content_script.js to add the divs
    onDOMContentLoadedDelayed = true;
    window.setTimeout(onDOMContentLoaded, 1);
    return;
  }
  toApiDiv = document.getElementById(TO_API_DIV_ID);
  toApiDiv.addEventListener(EVENT_NAME, handleMessagesToApi);

  if (loggingEnabled) {
    logDiv = document.createElement('div');
    document.body.appendChild(logDiv);
    while (toLog.length > 0) {
      log(toLog.shift());
    }
  }

  // Send any queued messages waiting for DOMContentLoaded
  while (toDispatch.length > 0) {
    sendMsg(toDispatch.shift());
  }
}

// Create the SpeedTracer API object
// TODO(zundel): If the speedtracer object already exists, might as well 
// bomb out and print an error.
if (!window.speedtracer) {
  window.speedtracer = {};
  if (document.readyState != 'complete' && document.readyState != 'loaded'){ 
    document.addEventListener("DOMContentLoaded", onDOMContentLoaded, false);
  } else {
    onDOMContentLoaded();
  }
}

// A DOM event listener on the toApiDiv
function handleMessagesToApi() {
  var tmpRec = JSON.parse(toApiDiv.innerText);
  log("Received message from Content Script: " + tmpRec.type);
  toApiDiv.innerText = "";
  switch (tmpRec.type) {
  case PORT_HEADLESS_GET_DUMP_ACK:
    var callback = getDumpCallbacks.shift();
    if (callback) { 
      callback(tmpRec.data);
    }
    break;
  case PORT_HEADLESS_SEND_DUMP_ACK:
    var callback = sendDumpCallbacks.shift();
    if (callback) { 
      callback(tmpRec.success);
    }
    break;
  case PORT_HEADLESS_MONITORING_ON_ACK:
    var monitoringOnCallback = monitoringOnCallbacks.shift();
    if (tmpRec.reload) {
      window.location.href = tmpRec.reload;
    } else {
      if (monitoringOnCallback) {
        monitoringOnCallback();
      } else {
        log("Expected monitoringOnCallback and got null instead.")
      }
    }
    break;
  case PORT_HEADLESS_MONITORING_OFF_ACK:
    var monitoringOffCallback = monitoringOffCallbacks.shift();
    if (monitoringOffCallback) {
      monitoringOffCallback();
    } else {
      log("Expected monitoringOffCallback and got null instead.")
    }
    break;
  default:
    // Unhandled message
  }
}

// TODO(zundel): Refactor this code such that the speedtracer object is 
// defined with all its functions and properties, and then assigned to 
// window.speedtracer

// This function clears any previously stored speed trace data.
// Useful if you are performing multiple runs on the same URL.
window.speedtracer.clearData = function() {
  sendMsg({'type':PORT_HEADLESS_CLEAR_DATA}); // 
}

// Turn on the SpeedTracer monitoring function
// Valid properties for options:
//   clearData - clears any previously recorded timeline data if true
//   reload - a url to load in this tab after turning monitoring on
// cb - optional function to be called after monitoring has been enabled
window.speedtracer.startMonitoring = function(options, cb) {
  sendMsg({'type':PORT_HEADLESS_MONITORING_ON, 'options':options}); 
  monitoringOnCallbacks.push(function() { 
    if (cb) {
      cb();
    }
  });
}

// Stop the SpeedTracer monitoring function
// cb - optional function to be called after monitoring has been disabled
window.speedtracer.stopMonitoring = function(cb) {
  sendMsg({'type':PORT_HEADLESS_MONITORING_OFF});
  monitoringOffCallbacks.push(function() { 
    if (cb) {
      cb();
    }
  });
}

// Retrieve the dump.
//    callback will be invoked when the data is ready.
//       callback (String data)
window.speedtracer.getDump = function(callback) {
  sendMsg({'type':PORT_HEADLESS_GET_DUMP});
  getDumpCallbacks.push(callback);
}

// Use cross site XHR to publish the dump
//    the callback will be invoked when the transmission is completed.
//       callback (number responseCode)
window.speedtracer.sendDump = function(url, header, callback) {
  sendMsg({'type':PORT_HEADLESS_SEND_DUMP, 'header': header, 'url':url});
  sendDumpCallbacks.push(callback);
}

// This function is internal to the API
// The API uses a hidden div with a special ID to communicate with
// the content script as per the Chrome Extension docs.
function sendMsg(message) {
  if (fromApiDiv == null) {
    toDispatch.push(message);
  } else {
    fromApiDiv.innerText = JSON.stringify(message);
    fromApiDiv.dispatchEvent(contentScriptEvent);
  }
}

function log(message) {
  if (loggingEnabled) {
    if (logDiv) {
     logDiv.innerHTML = logDiv.innerHTML + "<br/>\nAPI: " + message;
    } else {
      toLog.push(message);
    }
  }
}

// Notify the content script that the API is ready to play.
sendMsg({'type':API_READY_MSG});

})(); // end anonymous closure