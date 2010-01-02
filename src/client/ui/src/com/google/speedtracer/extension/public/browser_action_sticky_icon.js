function isRecordDump() {
  return (document.documentElement.getAttribute("isDump") == "true");
}

// We run this in every page load so that we can make the browser action
// button sticky (http://code.google.com/p/chromium/issues/detail?id=30113)
if (window == top && !isRecordDump()) {
  chrome.extension.connect({name: "STICKY_ICON"});
}