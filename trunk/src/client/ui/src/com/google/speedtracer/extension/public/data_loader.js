function isRecordDump() {
  return (document.documentElement.getAttribute("isDump") == "true");
}

function sendData(port, dataContainer) {
  // 0.8 was the last version to not version saved files.
  var version = dataContainer.getAttribute("version") || "0.8";
  var allData = dataContainer.innerHTML;
  for (var start = 0, end = allData.indexOf('\n', 0);
       end != -1; end = allData.indexOf('\n', start)) {
    var recordStr = allData.slice(start, end);
    // Make sure the recordStr is not simply all whitespace.
    if (!/^\s*$/.test(recordStr)) {
      port.postMessage({
        version: version,
        record : recordStr
      });
    }
    start = end + 1;
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
  return (window.location.href.toLowerCase().indexOf("file://") == 0) &&
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