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
package com.google.speedtracer.client.model;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.coreext.client.DataBag;
import com.google.gwt.coreext.client.JSOArray;
import com.google.speedtracer.shared.EventRecordType;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Base type for recorded data.
 */
public class EventRecord extends JavaScriptObject {
  /**
   * Comparator object for comparing UiEvent objects.
   */
  public static final class EventRecordComparator implements
      Comparator<EventRecord> {
    public int compare(EventRecord first, EventRecord second) {
      return Double.compare(first.getTime(), second.getTime());
    }
  }

  public static final int INVALID_TYPE = -1;

  private static EventRecordComparator comparatorInstance;

  /**
   * Singleton getter for our comparator.
   * 
   * @return
   */
  public static final EventRecordComparator getComparator() {
    if (comparatorInstance == null) {
      comparatorInstance = new EventRecordComparator();
    }
    return comparatorInstance;
  }

  public static int getIndexOfRecord(List<? extends EventRecord> eventList,
      double keyValue) {
    return getIndexOfRecord(eventList, createKey(keyValue));
  }

  public static int getIndexOfRecord(List<? extends EventRecord> eventList,
      EventRecord key) {
    int insertionPoint = Collections.binarySearch(eventList, key,
        EventRecord.getComparator());
    // Should almost always be an insertionPoint
    if (insertionPoint < 0) {
      return (-insertionPoint) - 1;
    } else {
      // we hit a node on the head.
      // simply return it
      return insertionPoint;
    }
  }

  /**
   * Returns a short user facing string that describes the event associated with
   * the given type int.
   */
  public static String typeToString(int type) {
    if (CustomEvent.isCustomEvent(type)) {
      return CustomEvent.getCustomTypeName(type);
    }

    return EventRecordType.typeToString(type);
  }

  private static native EventRecord createKey(double keyValue) /*-{
    return {time: keyValue};
  }-*/;

  /**
   * The data field in a record. TODO: Type checking on return values. For now
   * the run time errors are still pretty descriptive.
   */

  protected EventRecord() {
  }

  /**
   * Associate a HintRecord with this EventRecord.
   * 
   * @param hint a hint record to append to the list of hints associated with
   *          this EventRecord.
   */
  public final void addHint(HintRecord hint) {
    if (!hasHintRecords()) {
      this.setHintRecords(JSOArray.createArray());
    }
    this.getHintRecords().push(hint);
  }

  /**
   * Ensures that we have a type value set.
   */
  public final native void ensureType() /*-{
    if (!this.hasOwnProperty("type")) {
      // Invalid record!
      this.type = @com.google.speedtracer.client.model.EventRecord::INVALID_TYPE;
    }
  }-*/;

  /**
   * Returns a more verbose description of the event than
   * {@link #getTypeString()}.
   * 
   * @return a more verbose description of the event than getString().
   */
  public final String getHelpString() {
    int type = getType();
    if (CustomEvent.isCustomEvent(type)) {
      return "Custom Event Type: " + typeToString(type);
    }

    return EventRecordType.typeToHelpString(type);
  }

  /**
   * Retrieves the list of currently associated Hint records.
   * 
   * @return the list of currently associated Hint records.
   */
  public final native JSOArray<HintRecord> getHintRecords() /*-{
    return this.hints;
  }-*/;

  /**
   * Gets the sequence number for the record in this session.
   * 
   * @return the sequence number for the record in this session.
   */
  public final native int getSequence() /*-{
    return this.sequence || 0;
  }-*/;

  /**
   * @return JavaScript stack trace associated with this record.
   */
  public final native JSOArray<StackFrame> getStackTrace() /*-{
    return this.stackTrace;
  }-*/;

  /**
   * Gets the time value for this record. It is an integral type, but since JS
   * doesn't have an int type, we do not want to incur extra GWT generated
   * bounds checking, overflow handling, so we leave it a double.
   * 
   * @return the time for this record.
   */
  public final native double getTime() /*-{
    return this.time || 0;
  }-*/;

  /**
   * Gets the numeric type value record. If there is no type, use -1 so that it
   * can propagate to the UI
   * 
   * @return the number that represents this type.
   */
  public final native int getType() /*-{
    // This will always exist. We force it to.
    return this.type;
  }-*/;

  /**
   * Returns a short user facing string that describes this event type.
   * 
   * @return a short user facing string that describes this event type.
   */
  public final String getTypeString() {
    return typeToString(getType());
  }

  /**
   * Returns <code>true</code> if this record has associated Hint records.
   * 
   * @return <code>true</code> if this record has associated Hint records.
   */
  public final native boolean hasHintRecords() /*-{
    return !!this.hints;
  }-*/;

  /**
   * Setter for installing the sequence number for a record.
   */
  public final native void setSequence(int seq) /*-{
    this.sequence = seq;
  }-*/;

  /**
   * This is callable only by derived classes. Getter for the data bag.
   * 
   * @return the data bag for this record.
   */
  protected final native DataBag getData() /*-{
    return this.data || {};
  }-*/;

  /**
   * Replaces the list of currently associated Hint records.
   */
  private native void setHintRecords(JavaScriptObject hintRecordArray) /*-{
    this.hints = hintRecordArray;
  }-*/;
}
