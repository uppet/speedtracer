function isRecordDump() {
  return (document.documentElement.getAttribute("isDump") == "true");
}

//Content scripts get run in each frame. We only want to run once in the
//top level window context.
if (window == top && !isRecordDump()) {
  chrome.extension.connect({name: "ENABLE_MONITOR_TAB_BUTTON"});
}