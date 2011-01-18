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
package com.google.speedtracer.client.util;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.GWT.UncaughtExceptionHandler;

/**
 * Fork of Topspin's Command that does not reference $wnd.
 */
public class Command {

  /**
   * Interface implemented by callers to pass in a new method to be executed.
   */
  public interface Method {
    void execute();
  }

  /**
   * Causes a method to be called in the immediate future, after the current
   * event handler returns control to the browser.
   * 
   * @param method the method to be called.
   */
  public static void defer(Method method) {
    defer(method, 0);
  }

  /**
   * Causes a method to be called at some point in the future, as specified by
   * the 'delay' parameter. This method will not be called before the current
   * event handler terminates.
   * 
   * @param method the method to be called.
   * @param delay the delay, in milliseconds, after which the method will be
   *          called
   */
  public static native void defer(Method method, int delay) /*-{
    window.setTimeout(function() {
      @com.google.speedtracer.client.util.Command::fire(Lcom/google/speedtracer/client/util/Command$Method;)(method);
    }, delay);
  }-*/;

  private static void fire(Method method) {
    UncaughtExceptionHandler handler = GWT.getUncaughtExceptionHandler();
    if (handler != null) {
      fireAndCatch(handler, method);
    } else {
      method.execute();
    }
  }

  private static void fireAndCatch(UncaughtExceptionHandler handler, Method method) {
    try {
      method.execute();
    } catch (Throwable e) {
      handler.onUncaughtException(e);
    }
  }
}
