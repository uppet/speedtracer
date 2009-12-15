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

// Example hintlet that flags lots of bytes downloaded

// We are looking for NetworkResourceFinish events 
//{
//   "data": {
//      "resourceId": "1NetworkResourceEvent1",
//      "contentLength": 200
//   },
//   "sequence": 1234,
//   "time": 10549.0,
//   "type": "22"
//},

// Make a namespace for this rule using a closure
(function() {  // Begin closure

var HINTLET_NAME = "Total Bytes Downloaded";
var INFO_ALARM_THRESHOLD = 500000;
var WARNING_ALARM_THRESHOLD = 2000000;
var CRITICAL_ALARM_THRESHOLD = 10000000;

var INFO_ALARM_EMITTED = false;
var WARNING_ALARM_EMITTED = false;
var CRITICAL_ALARM_EMITTED = false;
var TOTAL_SIZE = 0;

var previousUrl;

hintlet.register(HINTLET_NAME, function(dataRecord){

    // Reset the count on a page transition event.
    if (dataRecord.type == hintlet.types.TAB_CHANGED 
        && previousUrl != dataRecord.data.url) {
      TOTAL_SIZE = 0;
      INFO_ALARM_EMITTED = false;
      WARNING_ALARM_EMITTED = false;
      CRITICAL_ALARM_EMITTED = false;
      previousUrl = dataRecord.data.url;

      // TODO(zundel): Something needs to be done here to go back and count
      //   events that have already fired that are a part of this page.
      return;
    }

    // TODO(zundel): Modify to trigger one on the document loaded event.

    if (dataRecord.type != hintlet.types.NETWORK_RESOURCE_FINISH) {
      return;
    }

    var contentLength = dataRecord.data.contentLength;
    if (contentLength < 0) {
      return;
    }

    TOTAL_SIZE = TOTAL_SIZE + contentLength;

    if (!INFO_ALARM_EMITTED) {
      if (TOTAL_SIZE > INFO_ALARM_THRESHOLD) {
         hintlet.addHint(HINTLET_NAME, dataRecord.time,
            "More than " + INFO_ALARM_THRESHOLD + " bytes downloaded.",
            dataRecord.sequence, hintlet.SEVERITY_INFO);    
        INFO_ALARM_EMITTED = true;
      }
    } else if (!WARNING_ALARM_EMITTED) {
      if (TOTAL_SIZE > WARNING_ALARM_THRESHOLD) {
         hintlet.addHint(HINTLET_NAME, dataRecord.time,
            "More than " + WARNING_ALARM_THRESHOLD + " bytes downloaded.",
            dataRecord.sequence, hintlet.SEVERITY_WARNING);
        WARNING_ALARM_EMITTED = true;
      }
    } else if (!CRITICAL_ALARM_EMITTED) {
      if (TOTAL_SIZE > CRITICAL_ALARM_THRESHOLD) {
         hintlet.addHint(HINTLET_NAME, dataRecord.time,
            "More than " + CRITICAL_ALARM_THRESHOLD + " bytes downloaded.",
            dataRecord.sequence, hintlet.SEVERITY_CRITICAL);
        CRITICAL_ALARM_EMITTED = true;
      }
    }
  });

})();  // End closure
