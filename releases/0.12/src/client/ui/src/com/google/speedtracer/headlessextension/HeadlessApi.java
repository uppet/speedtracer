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
package com.google.speedtracer.headlessextension;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.GWT.UncaughtExceptionHandler;

/**
 * Utility class to access the Headless API from a page.
 */
public class HeadlessApi {
  /**
   * Callback to receive the dump data when its ready.
   */
  public interface GetDumpCallback {
    /**
     * Asynchronous retrieval of the data from the extension.
     * 
     * @param dump A string containing newline separated trace records. Each
     *          record is encoded as a separate JSON object.
     */
    void callback(String dump);
  }

  /**
   * Optional callback argument passed to {@link #stopMonitoring} and
   * {@link #stopMonitoring}.
   */
  public interface MonitoringCallback {
    void callback();
  }

  /**
   * Options passed to {@link #startMonitoring}.
   */
  public static class MonitoringOnOptions extends JavaScriptObject {

    protected MonitoringOnOptions() {
    }

    /**
     * Clears any trace data from previous monitoring stored in the extension
     * before turning on monitoring.
     */
    public final native void clearData() /*-{
      this.clearData = true;
    }-*/;

    /**
     * If the content script sends down a URL, it indicates that the API
     * requesting that the current page be reloaded with a new URL after
     * monitoring is enabled.
     */
    public final native void setReloadUrl(String reloadUrl) /*-{
      this.reload = reloadUrl;
    }-*/;
  }

  public static final String API_URI = "chrome-extension://jolleknjmmglebfldiogepklbacoohni/headless_api.js";

  /**
   * Retrieve the trace data stored in the extension (asynchronously).
   * 
   * @param cb callback to be triggered when the action is complete.
   */
  public static void getDump(GetDumpCallback cb) {
    if (cb == null) {
      throw new IllegalArgumentException("callback must not be null");
    }
    nativeGetDump(cb);
  }

  /**
   * Returns true if the API has been loaded. See {@link #loadApi}.
   */
  public static native boolean isLoaded() /*-{
    return !!$wnd.speedtracer;
  }-*/;;

  /**
   * Injects the API into the current page.
   */
  public static void loadApi() {
    nativeLoadApi(API_URI);
  }

  /**
   * Turn monitoring on (asynchronously).
   * 
   * @param cb callback to be triggered when the action is complete.
   */
  public static native void startMonitoring(MonitoringOnOptions options,
      MonitoringCallback cb) /*-{
    $wnd.speedtracer.startMonitoring(options, function() {
      if (cb) {
        @com.google.speedtracer.headlessextension.HeadlessApi::fireMonitoringCb(Lcom/google/speedtracer/headlessextension/HeadlessApi$MonitoringCallback;)(cb);
      }
    });
  }-*/;

  /**
   * Turn monitoring off (asynchronously).
   * 
   * @param cb callback to be triggered when the action is complete.
   */
  public static native void stopMonitoring(MonitoringCallback cb) /*-{
    $wnd.speedtracer.stopMonitoring(function() {
      if (cb) {
        @com.google.speedtracer.headlessextension.HeadlessApi::fireMonitoringCb(Lcom/google/speedtracer/headlessextension/HeadlessApi$MonitoringCallback;)(cb);
      }
    });
  }-*/;

  @SuppressWarnings("unused")
  private static void fireDumpCb(GetDumpCallback cb, String dumpData) {
    UncaughtExceptionHandler ueh = GWT.getUncaughtExceptionHandler();
    if (ueh == null) {
      cb.callback(dumpData);
    } else {
      try {
        cb.callback(dumpData);
      } catch (Throwable ex) {
        ueh.onUncaughtException(ex);
      }
    }
  }

  @SuppressWarnings("unused")
  private static void fireMonitoringCb(MonitoringCallback cb) {
    UncaughtExceptionHandler ueh = GWT.getUncaughtExceptionHandler();
    if (ueh == null) {
      cb.callback();
    } else {
      try {
        cb.callback();
      } catch (Throwable ex) {
        ueh.onUncaughtException(ex);
      }
    }
  }

  private static native void nativeGetDump(GetDumpCallback cb) /*-{
    $wnd.speedtracer.getDump(function(dumpData) {
        @com.google.speedtracer.headlessextension.HeadlessApi::fireDumpCb(Lcom/google/speedtracer/headlessextension/HeadlessApi$GetDumpCallback;Ljava/lang/String;)(cb, dumpData);
    });
  }-*/;

  private static native void nativeLoadApi(String uri) /*-{
    var scriptTag = $doc.createElement("script");
    scriptTag.setAttribute("src", uri);
    $doc.body.appendChild(scriptTag);
  }-*/;

  /**
   * Disallow instantiating this class.
   */
  private HeadlessApi() {
  }
}
