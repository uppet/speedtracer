/*
 * Copyright 2010 Google Inc.
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

import com.google.gwt.core.client.JsArrayNumber;
import com.google.speedtracer.client.util.JSOArray;
import com.google.speedtracer.client.util.JsIntegerDoubleMap;
import com.google.speedtracer.shared.EventRecordType;

import java.util.Comparator;

/**
 * Base class for our Overlay types wrapping Event JSO payloads passed up from
 * the plugin.
 */
public class UiEvent extends EventRecord {

  /**
   * A callback interface for traversing over a {@link UiEvent} in leaf-first
   * order. This type of traversal provides a means to do an upward accumulation
   * of the tree structure.
   * 
   * @see UiEvent#apply(LeafFirstTraversal)
   * @see LeafFirstTraversalNumber
   * @see LeafFirstTraversalVoid
   * 
   * @param <T> the type of the accumulating object
   */
  public interface LeafFirstTraversal<T> {
    /**
     * Called for each event/sub-event contained within a top-level
     * {@link UiEvent}.
     * 
     * @param event the current event/sub-event
     * @param values the accumulated values for each of current events children,
     *          values will be empty for leafs
     * @return the final accumulated value for this event/sub-event
     */
    T visit(UiEvent event, JSOArray<T> values);
  }

  /**
   * A concrete specialization of {@link LeafFirstTraversal} which avoids having
   * to use primitive boxing.
   * 
   * @see UiEvent#apply(LeafFirstTraversalNumber)
   */
  public interface LeafFirstTraversalNumber {

    /**
     * @see LeafFirstTraversal#visit(UiEvent, JSOArray)
     */
    double visit(UiEvent event, JsArrayNumber values);
  }

  /**
   * A callback interface for traversing over a {@link UiEvent}. Use this type
   * of traversal for lower overhead when you do not need to perform upward
   * accumulation.
   * 
   * @see UiEvent#apply(LeafFirstTraversalVoid)
   */
  public interface LeafFirstTraversalVoid {
    /**
     * Called for each event/sub-event contained within a top-level
     * {@link UiEvent}.
     * 
     * @param event the current event/sub-event
     */
    void visit(UiEvent event);
  }

  /**
   * Comparator object for comparing UiEvent objects.
   */
  public static final class UiEventComparator implements Comparator<UiEvent> {
    public int compare(UiEvent first, UiEvent second) {
      return Double.compare(first.getTime(), second.getTime());
    }
  }

  private static UiEventComparator comparatorInstance;

  public static final native UiEvent createKey(double keyValue) /*-{
    return {time: keyValue};
  }-*/;

  /**
   * Singleton getter for our comparator.
   * 
   * @return
   */
  public static final UiEventComparator getComparator() {
    if (comparatorInstance == null) {
      comparatorInstance = new UiEventComparator();
    }
    return comparatorInstance;
  }

  /**
   * Returns true iff this record is a UI event by sniffing the 'duration'
   * field.
   * 
   * @return true iff this record is a UI event
   */
  public static final native boolean isUiEvent(EventRecord data) /*-{
    return data.hasOwnProperty('duration');
  }-*/;

  public static String typeToDetailedTypeString(UiEvent e) {
    switch (e.getType()) {
      case DomEvent.TYPE:
        return "DOM (" + ((DomEvent) e).getDomEventType() + ")";
      case LogEvent.TYPE:
        String logMessage = ((LogEvent) e).getMessage();
        int logLength = logMessage.length();
        logMessage = (logLength > 20) ? logMessage.substring(0, 8) + "..."
            + logMessage.substring(logLength - 8, logLength) : logMessage;
        return "Log: " + logMessage;
      case TimerFiredEvent.TYPE:
        TimerFiredEvent timerEvent = e.cast();
        return "Timer Fire (" + timerEvent.getTimerId() + ")";
      default:
        return EventRecordType.typeToString(e.getType());
    }
  }

  private static <T> T apply(LeafFirstTraversal<T> visitor, UiEvent event) {
    final JSOArray<T> values = JSOArray.create();
    final JSOArray<UiEvent> children = event.getChildren();
    for (int i = 0, n = children.size(); i < n; ++i) {
      values.push(apply(visitor, children.get(i)));
    }
    return visitor.visit(event, values);
  }

  private static double apply(LeafFirstTraversalNumber visitor, UiEvent event) {
    final JsArrayNumber values = JsArrayNumber.createArray().cast();
    final JSOArray<UiEvent> children = event.getChildren();
    for (int i = 0, n = children.size(); i < n; ++i) {
      values.push(apply(visitor, children.get(i)));
    }
    return visitor.visit(event, values);
  }

  private static void apply(LeafFirstTraversalVoid visitor, UiEvent event) {
    final JSOArray<UiEvent> children = event.getChildren();
    for (int i = 0, n = children.size(); i < n; ++i) {
      apply(visitor, children.get(i));
    }
    visitor.visit(event);
  }

  protected UiEvent() {
  }

  /**
   * Applies a {@link LeafFirstTraversal} type visitor to this event and all of
   * its child events.
   * 
   * @param <T> the accumulating type for the visitor
   * @param visitor
   * @return the final accumulated object for the event
   */
  public final <T> T apply(LeafFirstTraversal<T> visitor) {
    return apply(visitor, this);
  }

  /**
   * Applies a {@link LeafFirstTraversalNumber} type visitor to this event and
   * all of its child events.
   * 
   * @param visitor
   * @return the final accumulated value for the event
   */
  public final double apply(LeafFirstTraversalNumber visitor) {
    return apply(visitor, this);
  }

  /**
   * Applies a {@link LeafFirstTraversalVoid} type visitor to this event and all
   * of its child events.
   * 
   * @param visitor
   */
  public final void apply(LeafFirstTraversalVoid visitor) {
    apply(visitor, this);
  }

  public final native String getBackTrace() /*-{
    return (this.data && this.data.backTrace) ? this.data.backTrace : null;
  }-*/;

  /**
   * Returns the script name for the JS code of the top JS call frame.
   * 
   * @return the script name
   */
  public final native String getCallerFunctionName() /*-{
    return this.callerFunctionName || "";
  }-*/;

  /**
   * Returns the line number of the top JS call frame.
   * 
   * @return the line number
   */
  public final native int getCallerScriptLine() /*-{
    return this.callerScriptLine || 0;
  }-*/;

  /**
   * Returns the script name for the JS code of the top JS call frame.
   * 
   * @return the script name
   */
  public final native String getCallerScriptName() /*-{
    return this.callerScriptName || "";
  }-*/;

  public final native JSOArray<UiEvent> getChildren() /*-{
    return this.children || [];
  }-*/;

  /**
   * The duration of event dispatch.
   * 
   * @return the endTime - startTime
   */
  public final native double getDuration() /*-{
    return this.duration || 0;
  }-*/;

  /**
   * The time marking the end of event dispatch.
   * 
   * @return the end time
   */
  public final double getEndTime() {
    return getTime() + getDuration();
  }

  /**
   * Overhead time caused by the act of taking measurements in milliseconds. p *
   * 
   * @return
   */
  public final native double getOverhead() /*-{
    return this.overhead || 0;
  }-*/;

  public final native double getSelfTime() /*-{
    return this.selfTime;
  }-*/;

  /**
   * Returns a map of all the durations for types present in the event context
   * tree. double durations keyed by type key.
   * 
   * @return the durations map.
   */
  public final native JsIntegerDoubleMap getTypeDurations() /*-{
    return this.durationMap;
  }-*/;

  /**
   * This method checks whether or not the this event has been visited and
   * checked for log messages.
   * 
   * @return Whether or not the this event has been visited and checked for log
   *         messages.
   */
  public final native boolean hasBeenCheckedForLogs() /*-{
    return this.hasUserLogs !== undefined;
  }-*/;

  /**
   * Tests whether this UiEvent has a top stack frame from JS. That is, that it
   * was called from JavaScript.
   */
  public final native boolean hasCallLocation() /*-{
    return (this.callerScriptLine !== undefined) &&
        (this.callerScriptName !== undefined);
  }-*/;

  /**
   * Returns whether or not a profile record is associated with this event. Look
   * up the actual profiling data in the {@link JavaScriptProfileModel}
   * 
   * @return whether or not a profile record is associated with this event.
   */
  public final native boolean hasJavaScriptProfile() /*-{
    return this.javaScriptProfileState == "Done";
  }-*/;

  /**
   * Whether or not the user used the markTimeline API to log something.
   * 
   * @return Whether or not the user used the markTimeline API to log something
   */
  public final native boolean hasUserLogs() /*-{
    return !!this.hasUserLogs;
  }-*/;

  /**
   * Returns whether or not a profile record is associated with this event and
   * is still being processed.
   * 
   * @return whether or not a profile record is being processed for this event.
   */
  public final native boolean processingJavaScriptProfile() /*-{
    return this.javaScriptProfileState == "Processing";
  }-*/;

  /**
   * Sets whether or not a profile record is associated with this event.
   */
  public final native void setHasJavaScriptProfile(boolean value) /*-{
    if (value) {
      this.javaScriptProfileState = "Done";
    } else {
      delete this.javaScriptProfileState;
    }
  }-*/;

  /**
   * Setter for whether or not this event has a log message somewhere in the
   * tree.
   */
  public final native void setHasUserLogs(boolean hasUserLogs) /*-{
    this.hasUserLogs = hasUserLogs;
  }-*/;

  /**
   * Indicates that profile data exists for record but is still being processed.
   */
  public final native void setProcessingJavaScriptProfile() /*-{
    this.javaScriptProfileState = "Processing";
  }-*/;

  public final native void setSelfTime(double t) /*-{
    this.selfTime = t;
  }-*/;
  
  /**
   * Caches the durations map on this UiEvent.
   * 
   * @param map the map we want to cache on the event
   */
  public final native void setTypeDurations(JsIntegerDoubleMap map) /*-{
    this.durationMap = map;
  }-*/;
  
}
