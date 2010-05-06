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

/**
 * Base type for recorded data.
 */
public class EventRecord extends JavaScriptObject {
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
   * Returns a more verbose description of the event than
   * {@link #getTypeString()}.
   * 
   * @return a more verbose description of the event than getString().
   */
  public final String getHelpString() {
    return EventRecordType.typeToHelpString(getType());
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
   * TODO(conroy): Port this class to use DataBag and put the expensive code
   * path that checks hasOwnProperty() behind the Debug permutation.
   * 
   * Gets the numeric type value record. If there is no type, use -1 so that it
   * can propagate to the UI
   * 
   * @return the number that represents this type.
   */
  public final native int getType() /*-{
    return this.hasOwnProperty("type") ? this.type : -1;
  }-*/;

  /**
   * Returns a short user facing string that describes this event type.
   * 
   * @return a short user facing string that describes this event type.
   */
  public final String getTypeString() {
    return EventRecordType.typeToString(getType());
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
   * This is callable only by derived classes. Getter for the data bag.
   * 
   * @return the data bag for this record.
   */
  protected final native DataBag getData() /*-{
    return this.data || {};
  }-*/;

  /**
   * Setter for installing the sequence number for a record.
   */
  final native void setSequence(int seq) /*-{
    this.sequence = seq;
  }-*/;

  /**
   * Replaces the list of currently associated Hint records.
   */
  private native void setHintRecords(JavaScriptObject hintRecordArray) /*-{
    this.hints = hintRecordArray;
  }-*/;
}
