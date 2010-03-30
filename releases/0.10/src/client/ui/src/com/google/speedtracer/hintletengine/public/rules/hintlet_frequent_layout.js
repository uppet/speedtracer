/*
 * Copyright 2009 Google Inc.
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

// Analyzes a single event, looking for excessive layout

// Make a namespace for this rule using a closure
(function() {  // Begin closure

var HINTLET_NAME = "Frequent Layout activity";
var NUMBER_THRESHOLD = 3;  // Number of layouts to trigger a record
var TIME_THRESHOLD = 70;  // Number of ms in layout to trigger a record

/**
 * Look through this array of children and its descendants for 
 * any events of type  hintlet.types.LAYOUT_EVENT
 *
 * @param children array of child events to analyze
 * @param results_obj object containing current tally
 *   properties:
 *     layouts_found number of layout_events
 *     layout_time time spent in layout
 * @return a results_obj object
 */
function find_layouts(children, results_obj) {
  for (var i = 0, j = children.length; i < j; i++) {
    var event = children[i];
    if (event.type == hintlet.types.LAYOUT_EVENT) {
      results_obj.layouts_found++;
      results_obj.layout_time += event.selfDuration;
    } else if (event.type == hintlet.types.AGGREGATED_EVENTS) {
      for (var i = 0, j = event.data.events.length; i < j; i++) {
        var aggr_tuple = event.data.events[i];
        if (aggr_tuple[0] == hintlet.types.LAYOUT_EVENT) {
          results_obj.layouts_found += aggr_tuple[1];
          results_obj.layout_time += aggr_tuple[2];
        }
      }
    }
    if (event.children && event.children.length > 0) {
      results_obj = find_layouts(event.children, results_obj);
    }
  }
  return results_obj;
}

hintlet.register(HINTLET_NAME, function(dataRecord){
  if (dataRecord.children == null || dataRecord.children.length == 0) {
    return;
  }
  var results_obj = find_layouts(dataRecord.children, 
    { layouts_found : 0, layout_time : 0});
  
  // Look through children for layout activity.
  if (results_obj.layouts_found >= NUMBER_THRESHOLD &&
      results_obj.layout_time >= TIME_THRESHOLD) {	
    hintlet.addHint(HINTLET_NAME, dataRecord.time,
        "Event triggered " + results_obj.layouts_found + " layouts taking " + 
        hintlet.formatMilliseconds(results_obj.layout_time) + ".",
        dataRecord.sequence, hintlet.SEVERITY_WARNING);
  }
});

})();  // End closure

