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
import com.google.gwt.topspin.ui.client.WidgetEventListener;

/**
 * Simple utility class for lazily attaching an event listener. Useful for
 * lazily created components where we do not have a DOM element constructed at
 * the time of Widget construction.
 * 
 * @param <T> the type of the listener specified at construction time
 */
public abstract class LazyEventListenerAttacher<T extends WidgetEventListener> {

  /**
   * Note that this need not be what we actually attach the listener to.
   */
  private final Object eventSource;

  /**
   * The listener that we eventually connect.
   */
  private final T listener;

  public LazyEventListenerAttacher(Object eventSource, T listener) {
    this.eventSource = eventSource;
    this.listener = listener;
  }

  /**
   * The implementation of this method does the event hookup. The invoker MUST
   * specify the eventTarget that we attach to.
   * 
   * This method is abstract because in order to connect the listener we need to
   * know the type of listener. This gets specified at construction time.
   * 
   * @param eventTarget
   */
  public abstract void attach(JavaScriptObject eventTarget);

  public Object getEventSource() {
    return eventSource;
  }

  public T getListener() {
    return listener;
  }
}
