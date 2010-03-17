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

import com.google.gwt.dom.client.Element;
import com.google.speedtracer.client.util.JSOArray;
import com.google.speedtracer.client.util.JsIntegerDoubleMap;

import java.util.Comparator;

/**
 * Base class for our Overlay types wrapping Event JSO payloads passed up from
 * the plugin.
 */
public class UiEvent extends EventRecord {
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

  protected UiEvent() {
  }

  public final void acceptVisitor(EventVisitor visitor) {
    visitor.visitUiEvent(this);
  }

  public final native String getBackTrace() /*-{
    return (this.data && this.data.backTrace) ? this.data.backTrace : null;
  }-*/;

  /**
   * Returns the line number of the top JS call frame.
   * 
   * @return the line number
   */
  public final native int getCallerScriptLine() /*-{
    return this.callerScriptLine;
  }-*/;

  /**
   * Returns the script name for the JS code of the top JS call frame.
   * 
   * @return the script name
   */
  public final native String getCallerScriptName() /*-{
    return this.callerScriptName;
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

  /**
   * Pulls the cached canvas element corresponding to the rendered
   * {@link com.google.speedtracer.client.visualizations.view.EventTraceBreakdown.MasterEventTraceGraph}
   * from a {@link UiEvent} or null if it hasn't been rendered yet.
   * 
   * @return the {@link Element} corresponding to the canvas for the rendered
   *         master bar graph
   */
  public final native Element getRenderedMasterEventTraceGraph() /*-{
    return this.masterGraph;
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
    return !(this.hasUserLogs === undefined);
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
   * Marks this event as having been inspected for log messages. We reuse the
   * hasUserLogs property. It is normally unset, or true. Setting it to false
   * allows us to explicitly state that this has at least been inspected.
   */
  public final native void setHasBeenCheckedForLogs() /*-{
    this.hasUserLogs = false;
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
   * Indicates that profile data exists for record but is still being processed.
   */
  public final native void setProcessingJavaScriptProfile() /*-{
    this.javaScriptProfileState = "Processing";
  }-*/;

  /**
   * Caches a canvas element corresponding to the rendered
   * {@link com.google.speedtracer.client.visualizations.view.EventTraceBreakdown.MasterEventTraceGraph}
   * for a {@link UiEvent}.
   * 
   * @param frameBuffer the {@link Element} corresponding to the rendered canvas
   *          tag
   */
  public final native void setRenderedMasterEventTraceGraph(Element frameBuffer) /*-{
    this.masterGraph = frameBuffer;
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
