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

package com.google.speedtracer.client.model;

/*
 * DO NOT EDIT - Automatically generated file.  Make modifications
 *   to speedtracer/stools/src/py/generate-event-record-type.py and regenerate
 *   by running speedtracer/stools/generate-event-record-type.
 */

/**
 * Primitive integer values for the Types of EventRecords to let us 
 * switch on an int field which faster than an if ladder or string hash.
 */

public class EventRecordType {
  public static final int DOM_EVENT = 0;
  public static final int LAYOUT_EVENT = 1;
  public static final int RECALC_STYLE_EVENT = 2;
  public static final int PAINT_EVENT = 3;
  public static final int PARSE_HTML_EVENT = 4;
  public static final int TIMER_INSTALLED = 5;
  public static final int TIMER_CLEARED = 6;
  public static final int TIMER_FIRED = 7;
  public static final int XHR_READY_STATE_CHANGE = 8;
  public static final int XHR_LOAD = 9;
  public static final int EVAL_SCRIPT_EVENT = 10;
  public static final int LOG_MESSAGE_EVENT = 11;
  public static final int RESOURCE_SEND_REQUEST = 12;
  public static final int RESOURCE_RECEIVE_RESPONSE = 13;
  public static final int RESOURCE_FINISH = 14;
  public static final int JAVASCRIPT_EXECUTION_EVENT = 15;
  public static final int TAB_CHANGED = 16;
  public static final int AGGREGATED_EVENTS = 17;
  public static final int DOM_BINDING_EVENT = 18;
  public static final int JAVASCRIPT_COMPILE_EVENT = 19;
  public static final int WINDOW_EVENT = 20;
  public static final int NETWORK_RESOURCE_ERROR = 21;
  public static final int NETWORK_RESOURCE_FINISH = 22;
  public static final int NETWORK_RESOURCE_RESPONSE = 23;
  public static final int NETWORK_RESOURCE_START = 24;
  public static final int GARBAGE_COLLECT_EVENT = 25;
  public static final int MOUSE_HOVER_STYLE_EVENT = 26;
  public static final int DOM_EVENT_DISPATCH = 27;

  // The highest value key represented by the constants above
  public static final int MAX_KEY = 27;

  private static final String[] typeStrings = {
    "Dom Event",                        // 0 DOM_EVENT
    "Layout",                           // 1 LAYOUT_EVENT
    "Style Recalculation",              // 2 RECALC_STYLE_EVENT
    "Paint",                            // 3 PAINT_EVENT
    "Parse HTML",                       // 4 PARSE_HTML_EVENT
    "Timer Installed",                  // 5 TIMER_INSTALLED
    "Timer Cleared",                    // 6 TIMER_CLEARED
    "Timer Fire",                       // 7 TIMER_FIRED
    "XMLHttpRequest",                   // 8 XHR_READY_STATE_CHANGE
    "XHR Load",                         // 9 XHR_LOAD
    "Script Evaluation",                // 10 EVAL_SCRIPT_EVENT
    "Log Message",                      // 11 LOG_MESSAGE_EVENT
    "Resource Request",                 // 12 RESOURCE_SEND_REQUEST
    "Resource Response",                // 13 RESOURCE_RECEIVE_RESPONSE
    "Resource Finish",                  // 14 RESOURCE_FINISH
    "JavaScript Execution",             // 15 JAVASCRIPT_EXECUTION_EVENT
    "Tab Changed",                      // 16 TAB_CHANGED
    "AGGREGATED Events",                // 17 AGGREGATED_EVENTS
    "Dom Bindings",                     // 18 DOM_BINDING_EVENT
    "JavaScript Compile",               // 19 JAVASCRIPT_COMPILE_EVENT
    "Window",                           // 20 WINDOW_EVENT
    "Network Resource Error",           // 21 NETWORK_RESOURCE_ERROR
    "Network Resource Finished",        // 22 NETWORK_RESOURCE_FINISH
    "Network Resource Response",        // 23 NETWORK_RESOURCE_RESPONSE
    "Network Resource Start",           // 24 NETWORK_RESOURCE_START
    "Garbage Collection",               // 25 GARBAGE_COLLECT_EVENT
    "Mouse Hover for Style",            // 26 MOUSE_HOVER_STYLE_EVENT
    "Dom Dispatch"                      // 27 DOM_EVENT_DISPATCH
  };

  private static final String[] helpStrings = {
    // 0 DOM_EVENT
    "A top level DOM event fired, such as mousemove or DOMContentLoaded fired.",
    // 1 LAYOUT_EVENT
    "The browser's rendering engine performed layout calculations.",
    // 2 RECALC_STYLE_EVENT
    "The renderer recalculated CSS styles.",
    // 3 PAINT_EVENT
    "The browser's rendering engine updated the screen.",
    // 4 PARSE_HTML_EVENT
    "A block of HTML was parsed.",
    // 5 TIMER_INSTALLED
    "A new JavaScript timer was created.",
    // 6 TIMER_CLEARED
    "A JavaScript timer was cancelled.",
    // 7 TIMER_FIRED
    "A block of JavaScript was executed due to a JavaScript timer firing.",
    // 8 XHR_READY_STATE_CHANGE
    "The handler for an XMLHttpRequest ran.  Check the state field to see if this is an intermediate state or the last call for the request.",
    // 9 XHR_LOAD
    "The onload handler for an XMLHttpRequest ran.",
    // 10 EVAL_SCRIPT_EVENT
    "A block of JavaScript was parsed/compiled and executed. This only includes script encountered via an HTML <script> tag.",
    // 11 LOG_MESSAGE_EVENT
    "A log message written using console.markTimeline.",
    // 12 RESOURCE_SEND_REQUEST
    "A network request was queued up to send.",
    // 13 RESOURCE_RECEIVE_RESPONSE
    "A network resource load began to recieve data from the server.",
    // 14 RESOURCE_FINISH
    "A new request for a network resource completed.",
    // 15 JAVASCRIPT_EXECUTION_EVENT
    "A block of JavaScript executed.",
    // 16 TAB_CHANGED
    "Something about the Tab where the page viewed changed.  Usually this is the title string or the location of the page.",
    // 17 AGGREGATED_EVENTS
    "This event represents many short events that have been aggregated to help reduce the total amount of data displayed.",
    // 18 DOM_BINDING_EVENT
    "A DOM property or function was accessed, such as HTMLElement.insertBefore()",
    // 19 JAVASCRIPT_COMPILE_EVENT
    "A block of JavaScript was parsed and compiled by the JavaScript interpreter.",
    // 20 WINDOW_EVENT
    "A top level Window event fired, such as load or unload.",
    // 21 NETWORK_RESOURCE_ERROR
    "A network resource load ended in error.",
    // 22 NETWORK_RESOURCE_FINISH
    "A network resource loaded sucessfully.",
    // 23 NETWORK_RESOURCE_RESPONSE
    "A network resource load began to recieve data from the server.",
    // 24 NETWORK_RESOURCE_START
    "A new request for a network resource started.",
    // 25 GARBAGE_COLLECT_EVENT
    "The JavaScript engine ran garbage collection.",
    // 26 MOUSE_HOVER_STYLE_EVENT
    "The UI changed the currently displayed style for an Element based on a CSS hover rule.",
    // 27 DOM_EVENT_DISPATCH
    "A DOM event dispatch ran.  The event may be in the capture or bubble phase." 
  };

  public static String typeToDetailedTypeString(UiEvent e) {
    switch (e.getType()) {
      case DomEvent.TYPE:
        return "DOM (" + ((DomEvent) e).getDomEventType() + ")";
      case WindowEvent.TYPE:
        return "Window (" + ((WindowEvent) e).getWindowEventType() + ")";
      case DomEventDispatch.TYPE:
        return "Dom Dispatch (" + ((DomEventDispatch) e).getPhase() + ")";
      case DomBindingEvent.TYPE:
        DomBindingEvent domBindingEvent = e.cast();
        return (domBindingEvent.isGetter() ? "get " : "set ")
            + domBindingEvent.getName();
      case TimerFiredEvent.TYPE:
        TimerFiredEvent timerEvent = e.cast();
        return "Timer Fire (" + timerEvent.getTimerId() + ")";
      default:
        return EventRecordType.typeToString(e.getType());
    }
  }

  public static String typeToHelpString(int type) {
    if (type < 0 || type >= helpStrings.length) {
      return "(Unknown Event Type: " + type + ")";
    }
    return helpStrings[type];  
  }

  public static String typeToString(int type) {
    if (type < 0 || type >= typeStrings.length) {
      return "(Unknown Event Type: " + type + ")";
    }
    return typeStrings[type];
  }
}
