
// This entire script block should get removed before we save to disk.
function getVisitedUrlsString(visitedUrls) {
  var urlString = "&nbsp;&nbsp;";
  for (urlKey in visitedUrls) {
    var url = visitedUrls[urlKey];
    urlString += url + "<br/>&nbsp;&nbsp;"
  }
  var optionalS = (visitedUrls.length > 1) ? "s" : "";
  return visitedUrls.length + " url" + optionalS + " visited:<br/> " +
      urlString; 
}

function format2(arg) {
  return (arg.length == 1) ? "0" + arg : arg; 
}

// This gets called by the opening view.
var doSave = function (version, visitedUrls, traceData) {
  var date = new Date();
  // Files should be names like SpeedTrace-YYYYMMDDHHMSS
  var fileName = "SpeedTrace-" + date.getFullYear() +
      format2(date.getMonth() + 1) + format2(date.getDate()) +
      format2(date.getHours()) + format2(date.getMinutes()) +
      format2(date.getSeconds());

  // Set the title to be like SpeedTrace-6-40-41pm
  document.title = fileName;

  // List the pages visited
  document.getElementById("urls").innerHTML =
      getVisitedUrlsString(visitedUrls);

  // TraceData is an array of traces. Make it a String.
  // TODO(jaimeyap): If this is too expensive, we can do it incrementally.
  var traceDataString = traceData.join('\n');
  
  //Inject the data
  var traceDataElem = document.getElementById("traceData");
  traceDataElem.setAttribute("version", version);
  traceDataElem.innerHTML = traceDataString;

  // Set the save instructions
  var keys = "CTRL+S";
  if (navigator.platform == "Mac" ||
      navigator.platform == "MacIntel") {
    keys = '\u2318' + "+S";
  }
 
  var uiHtmlString ="(" + keys + " to save)";
  var infoElem = document.getElementById("info");
  infoElem.innerHTML = uiHtmlString;
  var timeElem = document.getElementById("time");
  var prettyTime = date.toLocaleDateString() + " " + date.toLocaleTimeString();
  timeElem.innerHTML = prettyTime;
};

function getParentView() {
  // Look at our query string. It should match our parent view.
  var queryString = window.location.search;

  //Look up the view that opened us.
  var views = chrome.extension.getViews();
  for (var i = 0, n = views.length; i < n; i++) {
    var view = views[i];
    // Make sure not to include any save page templates.
    if (view.location.search == queryString &&
        view.document.documentElement.getAttribute("isDump") != "true") {
      return view;
    }
  }

  return null;
}

// This script injects records stashed on the global object into the div above
// ... and then removes this script tag from the DOM to save space when saved 
// to disk. Does this ONLY if we are in the extensions process (meaning saving).
if (this.chrome && this.chrome.extension) {
  // Look up the view that opened us.
  var parentView = getParentView();

  if (parentView) {
    // Call into our opening view and plunk in our save function.
    parentView._onSaveReady(doSave);

    // Queue a task to remove ourselves.
    setTimeout(function() {
      document.body.removeChild(window.document.getElementById("injector"));
    }, 0);
  }
}