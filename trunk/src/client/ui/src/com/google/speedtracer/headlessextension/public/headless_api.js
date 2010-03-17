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

var TO_API_DIV_ID = '__speedtracerToApiElement';
var FROM_API_DIV_ID = '__speedtracerFromApiElement';
var EVENT_NAME = 'SpeedTracer Headless Event';
var fromApiDiv;
var toApiDiv;
var logDiv;
var getDumpCallbacks = [];
var sendDumpCallbacks = [];
var contentScriptEvent = document.createEvent('Event');
contentScriptEvent.initEvent(EVENT_NAME, true, true);
var toDispatch = [];
var onDOMContentLoadedDelayed = false;

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

  // Uncomment the following two lines to enable debugging
  // logDiv = document.createElement('div');
  // document.body.appendChild(logDiv);

  // Send any queued messages waiting for DOMContentLoaded
  while (toDispatch.length > 0) {
    log("Sending deferred message");
    sendMsg(toDispatch.shift());
  }
}

// Create the SpeedTracer API object
// TODO(zundel): If the speedtracer object already exists, might as well 
// bomb out and print an error.
if (!window.speedtracer) {
  window.speedtracer = {};
  document.addEventListener("DOMContentLoaded", onDOMContentLoaded, false);
}

// A DOM event listener on the toApiDiv
function handleMessagesToApi() {
  var tmpRec = JSON.parse(toApiDiv.innerText);
  log("Received message from Content Script: " + tmpRec.type);
  toApiDiv.innerText = "";

  switch (tmpRec.type) {
  case 107: // PORT_HEADLESS_GET_DUMP_ACK 
    var callback = getDumpCallbacks.pop();
    if (callback) { 
      log("Calling getDumpCallback");
      callback(tmpRec.data);
    }
    break;
  case 109: // PORT_HEADLESS_SEND_DUMP_ACK 
    var callback = sendDumpCallbacks.pop();
    if (callback) { 
      log("Calling sendDumpCallback");
      callback(tmpRec.success);
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
     log("Calling clearData");
  sendMsg({'type':103}); // PORT_HEADLESS_CLEAR_DATA
}

// Turn on the SpeedTracer monitoring function
// Valid properties for options:
//   profiling - enable CPU profiling if set to true
//   stackTrace = enable Stack Traces if set to true
window.speedtracer.startMonitoring = function(options) {
  sendMsg({'type':104, 'options':options}); // PORT_HEADLESS_MONITORING_ON
}

// Stop the SpeedTracer monitoring function
window.speedtracer.stopMonitoring = function() {
  sendMsg({'type':105}); // PORT_HEADLESS_MONITORING_OFF
}

// Retrieve the dump.
//    callback will be invoked when the data is ready.
//       callback (String data)
window.speedtracer.getDump = function(callback) {
  sendMsg({'type':106}); // PORT_HEADLESS_GET_DUMP
  getDumpCallbacks.push(callback);
}

// Use cross site XHR to publish the dump
//    the callback will be invoked when the transmission is completed.
//       callback (number responseCode)
window.speedtracer.sendDump = function(url, header, callback) {
  sendMsg({'type':108, 'header': header, 'url':url}); // PORT_HEADLESS_SEND_DUMP
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
    log("Sending message to Content Script: " + message.type);
    fromApiDiv.dispatchEvent(contentScriptEvent);
  }
}

function log(message) {
  if (logDiv) {
   logDiv.innerHTML = logDiv.innerHTML + "<br/>\nAPI: " + message;
  }
}

})(); // end anonymous closure