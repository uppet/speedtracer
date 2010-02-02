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
import com.google.gwt.topspin.ui.client.Window;

/**
 * Overlay type adds additional functionality to {@link Window}.
 */
public class WindowExt extends Window {

  /**
   * Get an instance of the containing window ($wnd).
   * 
   * @return a reference to $wnd
   */
  public static final WindowExt get() {
    return Window.get().cast();
  }

  protected WindowExt() {
  }

  /**
   * Adds a window.onunload handler.
   * 
   * @param listener a listener object to be called when the underlying event
   *          fires
   * @return a handle to remove the listener
   */
  public final EventListenerRemover addUnloadListener(EventListener listener) {
    return Event.addEventListener("unload", this, listener);
  }

  public final native void close() /*-{
    this.close();
  }-*/;

  /**
   * Returns the {@link LocalStorage} instance tied to the current origin.
   * 
   * @return the {@link LocalStorage} object
   */
  public final native LocalStorage getLocalStorage() /*-{
    return this.localStorage;
  }-*/;

  /**
   * Returns a JavaScriptObject property with the specified name.
   * 
   * @param name the name of the property
   * @return the value of that property as a {@link JavaScriptObject}
   */
  public final native JavaScriptObject getObjectProperty(String name) /*-{
    return this[name];
  }-*/;

  /**
   * Returns the query parameter string portion of the url.
   * 
   * @return the query paramater string portion of the url.
   */
  public final String getQueryParamString() {
    String[] urlPieces = getUrl().split("\\?");
    return (urlPieces.length > 1) ? urlPieces[1] : "";
  }

  public final native void log(String msg) /*-{
    this.console.log(msg);
  }-*/;

  /**
   * Opens a new child window.
   * 
   * @see http://developer.mozilla.org/en/docs/DOM:window.open
   * @param url the URL to load in the new window
   * @param name the window name
   * @param features specifications for the child window, see MDC article for
   *          details
   * @return a reference to the new window
   */
  public final native WindowExt open(String url, String name, String features) /*-{
    return this.open(url, name, features);
  }-*/;

  /**
   * Resize a window.
   * 
   * @param width
   * @param height
   */
  public final native void resizeTo(int width, int height) /*-{
    this.resizeTo(width, height);
  }-*/;

  /**
   * Sets a {@link JavaScriptObject} property with the specified name and value.
   * 
   * @param name the name of the property
   * @param object the object to be stored in that property slot
   */
  public final native void setObjectProperty(String name,
      JavaScriptObject object) /*-{
    this[name] = object;
  }-*/;

  private native String getUrl() /*-{
    return $wnd.location.href;
  }-*/;
}
