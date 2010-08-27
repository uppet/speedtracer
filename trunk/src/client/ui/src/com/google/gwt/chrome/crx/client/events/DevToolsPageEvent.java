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
package com.google.gwt.chrome.crx.client.events;

import com.google.gwt.core.client.JavaScriptObject;

/**
 * Part of the experimental timeline api. An event object that makes it possible
 * to listen for events from the devtools timeline agent.
 */
public class DevToolsPageEvent extends Event {
  /**
   * Called when a page event is received.
   */
  public interface Listener {
    void onPageEvent(PageEvent event);
  }

  /**
   * The record that gets passed back onPageEvent.
   */
  public static final class PageEvent extends JavaScriptObject {
    public static PageEvent create(JavaScriptObject event) {
      final JavaScriptObject types = getEventTypes();

      // If the event is array-based, it first needs to be upgraded to
      // an object-based representation.
      // See http://trac.webkit.org/changeset/65809.
      if (isLegacyArrayBasedEvent(event)) {
        return upgradeEvent(types, event);
      }

      // Only return the event if it is a known type.
      final PageEvent pageEvent = event.<PageEvent> cast();
      if (isKnownEventType(types, pageEvent.getMethod())) {
        return pageEvent;
      }

      return null;
    }

    private static void assertMethod(PageEvent event, String expected) {
      assert expected.equals(event.getMethod()) : "PageEvent method should be "
          + expected + " but it's " + event.getMethod();
    }

    protected PageEvent() {
    }

    public native String getMethod() /*-{
      return this.event;
    }-*/;

    public native JavaScriptObject getRecord() /*-{
      @com.google.gwt.chrome.crx.client.events.DevToolsPageEvent.PageEvent::assertMethod(Lcom/google/gwt/chrome/crx/client/events/DevToolsPageEvent$PageEvent;Ljava/lang/String;)(this, "addRecordToTimeline");
      return this.data.record;
    }-*/;

    public native JavaScriptObject getResource() /*-{
      @com.google.gwt.chrome.crx.client.events.DevToolsPageEvent.PageEvent::assertMethod(Lcom/google/gwt/chrome/crx/client/events/DevToolsPageEvent$PageEvent;Ljava/lang/String;)(this, "updateResource");
      return this.data.resource;
    }-*/;

    public native int getResourceId() /*-{
      @com.google.gwt.chrome.crx.client.events.DevToolsPageEvent.PageEvent::assertMethod(Lcom/google/gwt/chrome/crx/client/events/DevToolsPageEvent$PageEvent;Ljava/lang/String;)(this, "updateResource");
      return this.data.resource.id;
    }-*/;
  }

  /**
   * A map of event names to a function capable of upgrading an event of that
   * type from an array-based representation to an object-based representation.
   */
  private static JavaScriptObject eventTypes;

  private static native JavaScriptObject getEventTypes() /*-{
    if (!@com.google.gwt.chrome.crx.client.events.DevToolsPageEvent::eventTypes) {
      @com.google.gwt.chrome.crx.client.events.DevToolsPageEvent::eventTypes = {
        updateResource : @com.google.gwt.chrome.crx.client.events.DevToolsPageEvent::upgradeUpdateResourceEvent(Lcom/google/gwt/core/client/JavaScriptObject;),
        addRecordToTimeline : @com.google.gwt.chrome.crx.client.events.DevToolsPageEvent::upgradeAddRecordToTimelineEvent(Lcom/google/gwt/core/client/JavaScriptObject;)
      };
    }
    return @com.google.gwt.chrome.crx.client.events.DevToolsPageEvent::eventTypes;
  }-*/;

  private static native boolean isKnownEventType(JavaScriptObject eventTypes,
      String type) /*-{
    return !!eventTypes[type];
  }-*/;

  private static native boolean isLegacyArrayBasedEvent(JavaScriptObject event) /*-{
    return event instanceof Array;
  }-*/;

  private static native PageEvent upgradeAddRecordToTimelineEvent(
      JavaScriptObject event) /*-{
    return { "event" : event[0], "data" : { "record" : event[1] } };
  }-*/;

  private static native PageEvent upgradeEvent(JavaScriptObject eventTypes,
      JavaScriptObject event) /*-{
    var upgradeFunction = eventTypes[event[0]];
    if (upgradeFunction) {
      return upgradeFunction(event); 
    }
    return null;
  }-*/;

  private static native PageEvent upgradeUpdateResourceEvent(
      JavaScriptObject event) /*-{
    var resource = event[2];
    resource.id = event[1];
    return { "event" : event[0], "data" : { "resource" :  resource } };
  }-*/;

  protected DevToolsPageEvent() {
  }

  public final ListenerHandle addListener(Listener listener) {
    return new ListenerHandle(this, addListenerImpl(listener));
  }

  private native JavaScriptObject addListenerImpl(Listener listener) /*-{
    var handle = function(event) {
      var event = @com.google.gwt.chrome.crx.client.events.DevToolsPageEvent.PageEvent::create(Lcom/google/gwt/core/client/JavaScriptObject;)(event);
      if (event) {
        listener.@com.google.gwt.chrome.crx.client.events.DevToolsPageEvent$Listener::onPageEvent(Lcom/google/gwt/chrome/crx/client/events/DevToolsPageEvent$PageEvent;)(event);
      }
    };

    this.addListener(handle);
    return handle;
  }-*/;
}
