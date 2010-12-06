function isRecordDump() {
  return (document.documentElement.getAttribute("isDump") == "true");
}


var STATE_START = 0;
var STATE_IN_JSO = 1;
var STATE_IN_QUOTE = 2;

// Examines the string starting at the specified offset to find complete
// JSON object strings starting and ending with a curly brace: { ... }
//   curr_offset - the character position to begin scanning the string
//   data - the raw data string to split into separate jso strings
// On success, returns an array:
//   [ curr_offset, jso_string ]
// If there is no more data in the string, returns null
function nextJsonObject(curr_offset, data) {
  var result = [];
  var curr = curr_offset;
  var curr_state = STATE_START;
  var quote_type;
  var curr_jso = [];
  for (start = 0; curr < data.length ; curr++) {
    var curr_char = data.charAt(curr);
    curr_jso.push(curr_char);
    
    switch (curr_state) {
    case STATE_START:
      // drop any previously encountered characters preceding '{'
      var nested_level = 0;
      if (curr_char == "{") {
        curr_state = STATE_IN_JSO;
      } else {
        curr_jso = [];
      }
    break;

    case STATE_IN_JSO:
      if (curr_char == "'") {
        curr_state = STATE_IN_QUOTE;
        quote_type = "'";
      } else if (curr_char == '"') {
        curr_state = STATE_IN_QUOTE;
        quote_type = '"';
      } else if (curr_char == '{') {
        nested_level++;
      } else if (curr_char == '}') {
        if (nested_level < 0) {
          throw new Error("Nested level should be > 0");
        }
        nested_level--;
        if (nested_level == -1) {
          return [curr + 1, curr_jso.join("")];
        }
      }
    break;

    case STATE_IN_QUOTE:
      if (curr_char == "\\") {
        curr++;
        curr_char = data.charAt(curr);
        curr_jso.push(curr_char);
      } else if (curr_char == quote_type) {
        curr_state = STATE_IN_JSO;
      }
    break;
    }
  }
  // All valid records should have been spit out from STATE_IN_JSO
  return null;
}

function sendData(port, dataContainer) {
  // 0.8 was the last version to not version saved files.
  var version = dataContainer.getAttribute("version") || "0.8";
  var allData = dataContainer.innerHTML;
  
  var json_result = [0]
  while (true) {
    var json_result = nextJsonObject(json_result[0], allData);
    if (json_result == null) {
      break;
    }
    recordStr = json_result[1];
    if (!/^\s*$/.test(recordStr)) {
      port.postMessage({
        version: version,
        record : recordStr
      });
    }
  }
  var info = document.getElementById("info");
  info.innerHTML = "(loading... complete!)";
}

function loadData() {
  var dataContainer = document.getElementById("traceData");
  if (dataContainer) {
    var portName = 
      (dataContainer.getAttribute("isRaw") == "true") ? "RAW_DATA_LOAD" : "DATA_LOAD";
    var port = chrome.extension.connect({
      name : portName
    });
    // We send the data when the monitor is ready for it.
    port.onMessage.addListener(function(msg) {
      if (msg.ready) {
        sendData(port, dataContainer);
      }
    });
  }
}

function injectLoadUi() {  
  // Put a button in there that asks if they want to view us.
  var info = document.getElementById("info");
  info.innerHTML = "";
  if (info) {
    var viewButton = document.createElement("input");
    viewButton.type = "button";
    viewButton.value = "Open Monitor!";
    viewButton.style["cursor"] = "pointer";
    viewButton.addEventListener("click", function(evt){
        info.innerHTML = "(loading...)";
        loadData();
    }, false);
    info.appendChild(viewButton);
  }
}

function isTrampoline() {
  return ((window.location.href.toLowerCase().indexOf("file://") == 0) ||
      (window.location.href.toLowerCase().indexOf("http://localhost") == 0)) &&
      (document.documentElement.getAttribute("openSpeedTracer") == "true");
}

function maybeAutoOpen() {
  if (!isTrampoline()) {
    return;
  }
  var redirectUrl = document.documentElement.getAttribute("redirectUrl");
  chrome.extension.sendRequest({autoOpen: true}, function(response) {
    if (response.ready && window.location.href != redirectUrl) {
      window.location.href = redirectUrl;
    }
  });
}

if (window == top) {
  if (isRecordDump()) {
    injectLoadUi();
  } else {
    // Race condition with messaging the background page.
    setTimeout(function() {maybeAutoOpen();}, 100);
  }
}