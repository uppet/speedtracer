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

package com.google.speedtracer.hintletengine.client.rules;

import com.google.gwt.coreext.client.JSOArray;
import com.google.speedtracer.client.model.EventRecord;
import com.google.speedtracer.client.model.HintRecord;
import com.google.speedtracer.client.model.UiEvent;
import com.google.speedtracer.client.util.TimeStampFormatter;
import com.google.speedtracer.hintletengine.client.HintletOnHintListener;
import com.google.speedtracer.shared.EventRecordType;

/**
 * Frequent layout hintlet
 * 
 */
public final class HintletFrequentLayout extends HintletRule {

  // Number of layouts to trigger a record
  private static final int NUMBER_THRESHOLD = 3;
  // Number of ms in layout to trigger a record
  private static final int TIME_THRESHOLD = 70;

  public HintletFrequentLayout() {
  }

  public HintletFrequentLayout(HintletOnHintListener onHint) {
    setOnHintCallback(onHint);
  }

  private static class Results {
    public int layoutsFound;
    public double layoutTime;
  }

  /**
   * Look through this array of children and its descendants for any events of
   * type hintlet.types.LAYOUT_EVENT
   * 
   * @param events array of child events to analyze
   * @param results object containing the number of layout events and the time
   *        spent in layout
   */
  private void findLayouts(JSOArray<UiEvent> events, Results results) {

    for (int i = 0; i < events.size(); i++) {
      UiEvent event = events.get(i);

      if (event.getType() == EventRecordType.LAYOUT_EVENT) {
        results.layoutsFound++;
        results.layoutTime += event.getSelfTime();
      }

      // recursively search all children
      if (event.getChildren() != null) {
        findLayouts(event.getChildren(), results);
      }
    }
  }

  @Override
  public void onEventRecord(EventRecord eventRecord) {

    if (!UiEvent.isUiEvent(eventRecord)) {
      return;
    }

    UiEvent uiEvent = eventRecord.cast();
    JSOArray<UiEvent> children = uiEvent.getChildren();
    if (children == null || children.size() == 0) {
      return;
    }

    // Look through children for layout activity.
    Results results = new Results();
    findLayouts(children, results);
    if (results.layoutsFound >= NUMBER_THRESHOLD && results.layoutTime >= TIME_THRESHOLD) {
      addHint(getHintletName(), uiEvent.getTime(), "Event triggered " + results.layoutsFound
          + " layouts taking " + TimeStampFormatter.formatMilliseconds(results.layoutTime) + ".",
          uiEvent.getSequence(), HintRecord.SEVERITY_WARNING);
    }
  }

  @Override
  public String getHintletName() {
    return "Frequent Layout activity";
  }

}
