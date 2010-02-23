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
package com.google.speedtracer.client.visualizations.view;

import com.google.speedtracer.client.model.EventRecord;
import com.google.speedtracer.client.model.EventVisitorTraverser;
import com.google.speedtracer.client.model.UiEvent;
import com.google.speedtracer.client.model.EventVisitor.PreOrderVisitor;

/**
 * A class that contains the filter criteria for events.
 */
public class EventFilter {

  private class SubtypeDurationVisitor implements PreOrderVisitor {
    private double duration = 0;

    public SubtypeDurationVisitor() {
    }

    public double getSubtypeDuration() {
      return duration;
    }

    public void postProcess() {
      // nothing to do
    }

    public void reset() {
      duration = 0;
    }

    public void visitUiEvent(UiEvent uiEvent) {
      if (EventFilter.this.eventType == uiEvent.getType()) {
        duration += uiEvent.getSelfTime();
      }
    }
  }

  private int eventType = -1;

  private boolean filterHintlets = false;
  private boolean filterUserLogs = false;
  private double minDuration = 0.0;
  private double minEventTypePercent = 0.0;

  private final PreOrderVisitor[] postOrderVisitors = {new SubtypeDurationVisitor()};

  public EventFilter() {
  }

  public int getEventType() {
    return eventType;
  }

  public boolean getFilterHintlets() {
    return filterHintlets;
  }

  public boolean getFilterLogs() {
    return filterUserLogs;
  }

  public double getMinDuration() {
    return minDuration;
  }

  public double getMinEventTypePercent() {
    return minEventTypePercent;
  }

  /**
   * If set, only events that contain this type at the top level or any child
   * event will pass the filter.
   * 
   * @param eventType the event type to pass. Use one of the constants in
   * @{link EventRecordType}
   */
  public void setEventType(int eventType) {
    this.eventType = eventType;
  }

  /**
   * Whether to display only those events that contain hintlets.
   * 
   * Default is false (do not filter out any events that contain hintlets)
   * 
   * @param filterHintlets <code>true</code> to allow records that contain
   *          hintlets to be filtered if some other filter criteria is not met.
   */
  public void setFilterHints(boolean filterHintlets) {
    this.filterHintlets = filterHintlets;
  }

  /**
   * Whether to display only those events that contain logs.
   * 
   * @param filterUserLogs <code>true</code> to allow records that contain logs
   *          to be filtered if some other filter criteria is not met.
   */
  public void setFilterUserLogs(boolean filterUserLogs) {
    this.filterUserLogs = filterUserLogs;
  }

  /**
   * Filter any event with a top level duration less than the specified number
   * of milliseconds.
   * 
   * @param minDuration Minimum number of milliseconds to pass the filter.
   */
  public void setMinDuration(double minDuration) {
    this.minDuration = minDuration;
  }

  /**
   * Set the minimum allowed percent time for a particular event type.
   * 
   * @param minEventTypePercent a value between 0.0 and 1.0 representing the
   *          minimum amount of time of that type of event required to pass the
   *          filter.
   */
  public void setMinEventTypePercent(double minEventTypePercent) {
    this.minEventTypePercent = minEventTypePercent;
  }

  /**
   * Returns <code>true</code> if the event does not meet all the filter
   * criteria.
   * 
   * @param eventRecord A record to test against the filter criteria.
   * @return <code>true</code> if the event does not meet all the filter
   *         criteria.
   */
  public boolean shouldFilter(UiEvent eventRecord) {
    // Always allow records with logs or hintlets through, unless filtering
    // them has been enabled.
    if (!filterHintlets && eventRecord.hasHintRecords()) {
      return false;
    }
    if (!filterUserLogs && eventRecord.hasUserLogs()) {
      return false;
    }

    double duration = eventRecord.getDuration();
    if (duration < minDuration) {
      return true;
    }
    if (this.eventType > -1 && shouldFilterEventType(duration, eventRecord)) {
      return true;
    }
    return false;
  }

  // Walks children and adds up duration of all child self-times that match
  // the filter's event type.
  private double getSubtypeDuration(UiEvent eventRecord) {
    SubtypeDurationVisitor visitor = (SubtypeDurationVisitor) postOrderVisitors[0];
    visitor.reset();
    EventVisitorTraverser.traversePreOrder(eventRecord, postOrderVisitors);
    return visitor.getSubtypeDuration();
  }

  private boolean shouldFilterEventType(double duration, EventRecord eventRecord) {
    if (duration > 0) {
      // For any record with duration, you've got to check the child types.
      double typeDuration = getSubtypeDuration((UiEvent) eventRecord);
      if (minEventTypePercent > 0) {
        if ((typeDuration / duration) < minEventTypePercent) {
          return true;
        }
      } else if (typeDuration <= 0) {
        return true;
      }
    } else if (eventRecord.getType() != eventType) {
      return true;
    }
    return false;
  }
}
