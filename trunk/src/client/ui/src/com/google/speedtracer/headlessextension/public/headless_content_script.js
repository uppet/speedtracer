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
//
// SpeedTracer=monitor
//   Turns on monitoring when the content script is loaded.  After the 
//   ack for turning on monitoring is received, the current URL minus the 
//   arguments parsed are used to reload the page.  
function parseQueryString() {
  var query = window.location.search;
  if (query.charAt(0) == "?") {
    var args = query.substring(1).split("&");
    for (var i = 0; i < args.length; ++i) {
      var queryArg = args[i];
      if (queryArg.search(/^SpeedTracer=/ == 0)) {
        var value = queryArg.replace(/SpeedTracer=/, "");
        if (value.toLowerCase() === "monitor") {
          var newUrl = String(window.location);
          newUrl = newUrl.replace(/SpeedTracer=monitor(&)?/, "");
          log("Turning on monitoring and setting to reload: " + newUrl);
          sendMsgToBackground({'type':PORT_HEADLESS_MONITORING_ON, 
              'options':{'clearData':true,'reload':newUrl}});
        }
      }
    }
  }
}

if (window == top) {
  extensionPort = chrome.extension.connect( {
    name : "HEADLESS_API"
  });
  addHiddenDivs();
  extensionPort.onMessage.addListener(handleMessagesFromBackgroundPage);
  parseQueryString();
}