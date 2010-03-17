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
var extensionPort;
var TO_API_DIV_ID = '__speedtracerToApiElement';
var FROM_API_DIV_ID = '__speedtracerFromApiElement';
var EVENT_NAME = 'SpeedTracer Headless Event';
var toApiDiv;
var fromApiDiv;
var logDiv;
var contentScriptEvent = document.createEvent('Event');
contentScriptEvent.initEvent(EVENT_NAME, true, true);
var eventRecordData = [];

// A Port style listener function
function handleMessagesFromBackgroundPage(backgroundMsg) {
  switch (backgroundMsg.type) {  // See definitions in MessageType.java
  case 107: // PORT_GET_DUMP_ACK
  case 109: // PORT_SEND_DUMP_ACK
    // Dumps are ready, forward results to the API
    sendMsgToApi(JSON.stringify(backgroundMsg));
    break;
  default:
    // message not implemented
  }
}

// A DOM event listener on the special fromApiDiv
function handleMessagesFromApi() {
  var tmpRecString = fromApiDiv.innerText;
  var tmpRec = JSON.parse(tmpRecString);
  fromApiDiv.innerText = "";

  log("Processing record type: " + tmpRec.type);
  switch(tmpRec.type) {
  case 103: // PORT_HEADLESS_CLEAR_DATA
  case 104: // PORT_HEADLESS_MONITORING_ON
  case 105: // PORT_HEADLESS_MONITORING_OFF
  case 106: // PORT_HEADLESS_GET_DUMP
  case 108: // PORT_HEADLESS_SEND_DUMP
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

  // Uncomment the following two lines to enable debugging
  // logDiv = document.createElement('div');
  // document.body.appendChild(logDiv);
}

function addHiddenDivs() {
  // The DOM might not be ready when this function is called
  if (document.readyState == 'loading') {
    window.addEventListener("DOMContentLoaded", addHiddenDivsImpl, false);
  } else {
    addHiddenDivsImpl();
  }
}

// Send a message to the API embedded in the main page.
function sendMsgToApi(message) {
  toApiDiv.innerText = message;
  toApiDiv.dispatchEvent(contentScriptEvent);
}

// Send a message to the Extension's Background Page.
function sendMsgToBackground(message) {
  extensionPort.postMessage(message);
}

function log(message) {
  if (logDiv) {
   logDiv.innerHTML = logDiv.innerHTML + "<br/>\nAPI: " + message;
  }
}

if (window == top) {
  extensionPort = chrome.extension.connect( {
    name : "HEADLESS_API"
  });

  addHiddenDivs();

  // Wait for messages before continuing further.
  extensionPort.onMessage.addListener(handleMessagesFromBackgroundPage);
}