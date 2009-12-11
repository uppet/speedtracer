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

// Example hintlet that flags a long duration event.

// Make a namespace for this rule using a closure
(function() {  // Begin closure

var HINTLET_NAME = "Long Duration Events";
// 100 milliseconds, threshold of human perception
var HINTLET_LOW_DURATION_THRESHOLD = 100;
// 2 seconds - a very long running event
var HINTLET_LONG_DURATION_THRESHOLD = 2000;

hintlet.register(HINTLET_NAME, function(dataRecord){
  if (dataRecord['duration'] == null) {
    return;
  }

  var duration = dataRecord.duration;
  if (duration > HINTLET_LONG_DURATION_THRESHOLD) {
    hintlet.addHint(HINTLET_NAME, dataRecord.time,
      "Event lasted: " + hintlet.formatMilliseconds(duration) 
      + ".  Exceeded threshold: " 
      + HINTLET_LONG_DURATION_THRESHOLD + "ms", 
      dataRecord.sequence, hintlet.SEVERITY_WARNING);        
 } else if (duration > HINTLET_LOW_DURATION_THRESHOLD) {
   hintlet.addHint(HINTLET_NAME, dataRecord.time,
      "Event lasted: " + hintlet.formatMilliseconds(duration) 
      + ".  Exceeded threshold: " 
      + HINTLET_LOW_DURATION_THRESHOLD + "ms",
      dataRecord.sequence, hintlet.SEVERITY_INFO);
  } 
});

})();  // End closure
