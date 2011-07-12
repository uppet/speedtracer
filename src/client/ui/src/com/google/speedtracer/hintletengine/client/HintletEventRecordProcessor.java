/*
 * Copyright 2011 Google Inc.
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

package com.google.speedtracer.hintletengine.client;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.coreext.client.DataBag;
import com.google.gwt.coreext.client.JSON;
import com.google.speedtracer.client.model.EventRecord;
import com.google.speedtracer.client.model.UiEvent;
import com.google.speedtracer.hintletengine.client.rules.HintletRule;

import java.util.ArrayList;
import java.util.List;

/**
 * Process hintlet event records
 */
public final class HintletEventRecordProcessor {

  private List<HintletRule> rules;

  public HintletEventRecordProcessor(HintletRule rule) {
    rules = new ArrayList<HintletRule>();
    rules.add(rule);
  }

  public HintletEventRecordProcessor(List<HintletRule> rules) {
    this.rules = rules;
  }

  /**
   * Receive a new record of browser data and forward it to registered hintlets
   * 
   * @param dataRecord record to send to all hintlets
   */
  public void onEventRecord(JavaScriptObject dataRecord) {

    if (!DataBag.hasOwnProperty(dataRecord, "type")) {
      throw new IllegalArgumentException("Expecting an EventRecord. Getting "
          + JSON.stringify(dataRecord));
    }
    // TODO(haibinlu): make sure the type is valid
    EventRecord eventRecord = dataRecord.cast();
    // Calculate the time spent in each event/sub-event exclusive of children
    computeSelfTime(eventRecord);

    for (HintletRule rule : rules) {
      rule.onEventRecord(eventRecord);
    }
  }

  /**
   * Recursively calculate the amount of time spent in a record exclusive of the
   * time spent in its children. Adds the property 'selfDuration' to this record
   * and each child record.
   * 
   * @param eventRecord current event
   */
  private static void computeSelfTime(EventRecord eventRecord) {

    if (!UiEvent.isUiEvent(eventRecord)) {
      return;
    }

    UiEvent uiEvent = eventRecord.cast();

    // compute the total duration of all children
    double childTime = 0;
    for (int i = 0, j = uiEvent.getChildren().size(); i < j; i++) {
      childTime += uiEvent.getChildren().get(i).getDuration();
    }
    // add self duration of current node
    uiEvent.setSelfTime(uiEvent.getDuration() - childTime);

    // work on all children then
    for (int i = 0; i < uiEvent.getChildren().size(); i++) {
      computeSelfTime(uiEvent.getChildren().get(i));
    }
  }

}
