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
 
/**
 * hintlet_main.js - bootstraps the API for hintlet rules.
 * 
 * This file defines internal functions that are not builtin to the plugin.
 *   These functions are written in JavaScript, but but are not intended 
 *   for direct access by hintlet rules.
 *
 * See hintlet_api.js for functions usable by hintlet rules.
 *
 * Routines that are builtin when in the testing environment:
 *  hintlet.__hAddHintlet(String) - Sends a hint record (as JSON) to the 
 *      user interface.
 *  hintlet.__hLoad(String) - loads a JavaScript file relative to the hintlet 
 *      directory.
 *  hintlet.__hLog(String) - prints a record to the debug log
 *  hintlet.__hVersion() - returns the version of Chrome
 */
var isTestingEnviron = !!this.hintlet;

if (isTestingEnviron) {
  // Running under the C++ Hintlet Engine testing environment
  hintlet.__hLoad("hintlet_record_type.js");
  hintlet.__hLoad("hintlet_api.js");

  //Redefine some of hintlet_api.js if we are in a testing environment.
  hintlet.addHint = function(hintletRule, timestamp, description,
      refRecord, severity) {
    var value = hintlet._formatHint(hintletRule, timestamp, description,
        refRecord, severity);
    hintlet.__hAddHint(JSON.stringify(value));
  };
  
  hintlet.log = function(value) {
    hintlet.__hLog(value);
  };
  
  hintlet.load = function(path) {
    hintlet.__hLoad(path);
  };
  
  hintlet._newRecord = function (sequence, dataRecordString) {
    var dataRecord = JSON.parse(dataRecordString);
    // Populate the sequence number in the data record.
    dataRecord.sequence = sequence;
    // Calculate the time spent in each event/sub-event exclusive of children 
    hintlet._addSelfDuration(dataRecord); 
    for (var i = 0, j = hintlet.rules.length; i < j; i++) {
      hintlet.rules[i].callback(dataRecord);
    }
  }
} else {
  // Running as a webworker inside a browser
  hintlet = {};
  importScripts("hintlet_record_type.js");
  importScripts("hintlet_api.js");
  
  // Sets the onmessage handler when the host posts a message to us.
  self.onmessage = function(messageEvent) {
    var record = JSON.parse(messageEvent.data);
    hintlet._addRecord(record);
  }
}

hintlet.log("Loaded Hintlet API (hintlet_api.js)");

/**
 * Recieve a new record of browser data and forward it to registered hintlet
 * rules.
 *  dataRecord (JSO) - record to send to all hintlets
 */
hintlet._addRecord = function (dataRecord) {
  // Calculate the time spent in each event/sub-event exclusive of children 
  hintlet._addSelfDuration(dataRecord);

  // Potentially keep state for a network resource.
  if (dataRecord.type == hintlet.types.RESOURCE_UPDATED) {
    hintlet._updateResource(dataRecord);
    hintlet._maybeForgetResource(dataRecord);
    
    // TODO(jaimeyap): Is there a legit reason to deliver inspector 
    // updateResource messages to hintlets rules?
  } else {
    for (var i = 0, j = hintlet.rules.length; i < j; i++) {
      hintlet.rules[i].callback(dataRecord);
    }
  }
}
 
/** 
 * Recursively calculate the amount of time spent in a record exclusive of 
 * the time spent in its children.  Adds the property 'selfDuration' to this  
 * record and each child record. 
 *  
 * @param {Object} dataRecord a top level record or child record.
 * @return {Number} the duration of this event in milliseconds. 
 */ 
hintlet._addSelfDuration = function (record) { 
  if (record.duration === undefined) { 
    return 0; 
  }
  if (record.type == hintlet.types.AGGREGATED_EVENTS) {
    // Aggregate events are special.  The 'self time' is the time spent by
    // the aggregated events, and so (duration - selfDuration) in an aggregate
    // event is all time that should be attributed to the parent.  Returning
    // selfDuration instead of duration makes the math work out since the parent
    // will only subtract selfDuration.
    var aggrTime = 0;
    for (var i = 0, j = record.data.events.length; i < j; i++) {
      var aggr_tuple = record.data.events[i];
      aggrTime += aggr_tuple[2];
    }
    record.selfDuration = aggrTime;
    return record.selfDuration;
  } else {
    var childTime = 0;
    if (record.children) {
      for (var i = 0, j = record.children.length ; i < j; i++) {
         childTime += hintlet._addSelfDuration(record.children[i]);
      }
    }
    record.selfDuration = record.duration - childTime;
    return record.duration;
  }
}

// In the testing environment, the plugin will take care of running the hintlet
// rule scripts.
if (!isTestingEnviron) {
  // Get the list of rules we want to load.
  // TODO(jaimeyap): Eventually generate this list.
  hintlet.load("hintlet_rule_whitelist.js");

  //Pull in all white listed hintlets.
  var numRules = hintlet.whiteList.length;
  for (var index = 0; index < numRules; ++index) {
    try {
      hintlet.load(hintlet.whiteList[index]);
    } catch (e) {
      hintlet.log("Exception loading " + hintlet.whiteList[index]
                  + " : " + e);
    }
  }
}

hintlet.log("Loaded Hintlet API Bootstrap (hintlet_main.js)");

