/*
 * Copyright 2008 Google Inc.
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

// Example hintlet that flags resources that are larger than a certain size.

// Make a namespace for this rule using a closure
(function() {  // Begin closure

var HINTLET_NAME = "Total Bytes Downloaded for a resource";
var INFO_ALARM_THRESHOLD = 50000;
var WARNING_ALARM_THRESHOLD = 100000;

hintlet.register(HINTLET_NAME, function(dataRecord){
  if (dataRecord.type != hintlet.types.RESOURCE_FINISH) {
    return;
  }
  var resourceData = hintlet.getResourceData(dataRecord.data.identifier);
  if (!resourceData) {
    return;
  }

  var url = resourceData.url;
  var contentLength = resourceData.contentLength || 0;
  var severity = hintlet.SEVERITY_INFO;
  
  if (contentLength > WARNING_ALARM_THRESHOLD) {
    severity = hintlet.SEVERITY_WARNING;
  }
  
  if (contentLength > INFO_ALARM_THRESHOLD) {
    hintlet.addHint(HINTLET_NAME, resourceData.responseReceivedTime,
        contentLength + " bytes downloaded for " + "resource " + url,
        dataRecord.sequence, severity);		   
  }
});

})();  // End closure
