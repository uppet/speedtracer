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
public abstract class Command {

  /**
   * Causes a command to be called in the immediate future, after the current
   * event handler returns control to the browser.
   * 
   * @param command the command to be called
   */
  public static void defer(Command command) {
    defer(command, 0);
  }

  /**
   * Causes a command to be called at some point in the future, as specified by
   * the 'delay' parameter. This command will not be called before the current
   * event handler terminates.
   * 
   * @param command the command to be called
   * @param delay the delay, in milliseconds, after which the command will be
   *        called
   */
  public static native void defer(Command command, int delay) /*-{
     window.setTimeout(function() {
       command.@com.google.speedtracer.client.util.Command::fire()();
     }, delay);
   }-*/;

  /**
   * Abstract method that will be called when the command is ready.
   */
  public abstract void execute();

  void fire() {
    UncaughtExceptionHandler handler = GWT.getUncaughtExceptionHandler();
    if (handler != null) {
      fireAndCatch(handler);
    } else {
      execute();
    }
  }

  private void fireAndCatch(UncaughtExceptionHandler handler) {
    try {
      execute();
    } catch (Throwable e) {
      handler.onUncaughtException(e);
    }
  }
}
