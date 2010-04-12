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
import com.google.speedtracer.client.util.JSOArray;

/**
 * Base type for recorded data.
 */
public class EventRecord extends JavaScriptObject {
  /**
   * The data field in a record. TODO: Type checking on return values. For now
   * the run time errors are still pretty descriptive.
   */
  protected static class DataBag extends JavaScriptObject {
    protected DataBag() {
    }

    public final native boolean getBooleanProperty(String prop) /*-{
      return !!this[prop];
    }-*/;

    public final native double getDoubleProperty(String prop) /*-{
      return this[prop];
    }-*/;

    public final native int getIntProperty(String prop) /*-{
      return this[prop];
    }-*/;

    public final native JavaScriptObject getJSObjectProperty(String prop) /*-{
      return this[prop];
    }-*/;

    public final native String getStringProperty(String prop) /*-{
      return this[prop];
    }-*/;
  }

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
    return this.sequence;
  }-*/;

  /**
   * Gets the time value for this record. It is an integral type, but since JS
   * doesn't have an int type, we do not want to incur extra GWT generated
   * bounds checking, overflow handling, so we leave it a double.
   * 
   * @return the time for this record.
   */
  public final native double getTime() /*-{
    return this.time;
  }-*/;

  /**
   * Gets the numeric type value record.
   * 
   * @return the number that represents this type.
   */
  public final native int getType() /*-{
    return this.type;
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
