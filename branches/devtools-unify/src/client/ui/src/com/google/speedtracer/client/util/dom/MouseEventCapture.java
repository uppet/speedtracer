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
package com.google.speedtracer.client.util.dom;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.events.client.Event;
import com.google.gwt.events.client.EventListener;
import com.google.gwt.events.client.EventListenerRemover;
import com.google.gwt.topspin.ui.client.MouseMoveEvent;
import com.google.gwt.topspin.ui.client.MouseUpEvent;
import com.google.gwt.topspin.ui.client.Window;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class which allows simulation of Event Capture.
 */
public class MouseEventCapture {
  /**
   * Interface defining a handle to an object for releasing Capture.
   */
  interface CaptureReleaser {
    void release();
  }

  private static class RemoverImpl implements EventListenerRemover {

    private static native void removeImpl(String type, JavaScriptObject source,
        JavaScriptObject fn) /*-{
      source.removeEventListener(type, fn, false);
    }-*/;

    private final JavaScriptObject function;
    private final JavaScriptObject source;

    private final String type;

    public RemoverImpl(String type, JavaScriptObject source,
        JavaScriptObject listener) {
      this.type = type;
      this.source = source;
      this.function = listener;
    }

    public void remove() {
      removeImpl(type, source, function);
    }
  }

  /**
   * Current mouse capture owner.
   */
  private static MouseCaptureListener captureOwner;

  /**
   * These are the EventRemover objects for the hooks into Window that fire to
   * this class. We have exactly one listener per event type that supports
   * capture. When no UI component possesses capture (meaning that the
   * captureOwner stack is empty) we can disconnect these.
   */
  private static final List<EventListenerRemover> mouseRemovers = new ArrayList<EventListenerRemover>();

  /**
   * Capture happens on a per listener instance basis. We throw the
   * CaptureListener on the top of the capture stack and then provide a handle
   * to an object that can be used to
   * 
   * @param listener
   */
  public static void capture(final MouseCaptureListener listener) {

    // Lazily initialize event hookups
    if (mouseRemovers.size() == 0) {
      registerEventCaptureHookups();
    }

    // Make sure to release the previous capture owner.
    if (captureOwner != null) {
      captureOwner.release();
    }

    captureOwner = listener;
    listener.setCaptureReleaser(new CaptureReleaser() {
      public void release() {
        // nuke the reference to this releaser in the listener (which should
        // still be the capture owner).
        listener.setCaptureReleaser(null);
        // nuke the captureOwner
        captureOwner = null;

        // Release the event listeners.
        for (int i = 0, n = mouseRemovers.size(); i < n; i++) {
          mouseRemovers.get(i).remove();
        }
        mouseRemovers.clear();
      }
    });
  }

  /**
   * Convenience method.
   * 
   * @param event
   */
  public static native void stopPropagation(Event event) /*-{
    event.stopPropagation();
  }-*/;

  private static EventListenerRemover addCaptureEventListener(String type,
      JavaScriptObject source, EventListener listener) {
    return new RemoverImpl(type, source, addCaptureEventListenerImpl(type,
        source, listener));
  }

  private static native JavaScriptObject addCaptureEventListenerImpl(
      String type, JavaScriptObject source, EventListener listener) /*-{
    var f = function(event) {
      listener.
      @com.google.gwt.events.client.EventListener::handleEvent(Lcom/google/gwt/events/client/Event;)
      (event);
    }; 
    source.addEventListener(type, f, true);
    return f;
  }-*/;

  private static void forwardToCaptureOwner(Event event) {
    if (captureOwner != null) {
      captureOwner.handleEvent(event);
      stopPropagation(event);
    }
  }

  /**
   * TODO: Add hookups for the other event types. Currently I found capture
   * useful only for drag interactions.
   * 
   * Registers for every event type on the Window.
   * 
   * Capture should be lazily initialized, and then destroyed each time nothing
   * has capture (as to not cause useless event dispatch and handling for events
   * like moveevents).
   */
  private static void registerEventCaptureHookups() {

    // MOUSEMOVE
    mouseRemovers.add(addCaptureEventListener(MouseMoveEvent.NAME,
        Window.get(), new EventListener() {
          public void handleEvent(Event event) {
            forwardToCaptureOwner(event);
          }
        }));

    // MOUSEUP
    mouseRemovers.add(addCaptureEventListener(MouseUpEvent.NAME, Window.get(),
        new EventListener() {
          public void handleEvent(Event event) {
            forwardToCaptureOwner(event);
          }
        }));
  }
}
