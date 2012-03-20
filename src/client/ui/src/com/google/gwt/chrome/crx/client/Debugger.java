/*
 * Copyright 2011 Google Inc.
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
package com.google.gwt.chrome.crx.client;

import com.google.gwt.chrome.crx.client.events.DebuggerDetachEvent;
import com.google.gwt.chrome.crx.client.events.DebuggerEvent;
import com.google.gwt.core.client.JavaScriptObject;

/**
 * An experimental API for interacting with the Chrome debugging server.
 * 
 * API for chrome.debugger
 * 
 * See documentaion at: <a href=
 * "http://code.google.com/chrome/extensions/trunk/debugger.html"
 * >chrome.experimental.debugger</a>
 */
public class Debugger {

  public interface AttachCallback {
    void onAttach();
  }

  public interface DetachCallback {
    void onDetach();
  }

  public interface SendRequestCallback {
    void onResponse(JavaScriptObject result);
  }

  public static native void attach(int tabId) /*-{
    chrome["debugger"].attach({"tabId" : tabId});
  }-*/;

  public static native void attach(int tabId, AttachCallback callback) /*-{
    chrome["debugger"].attach({"tabId" : tabId}, "1.0", function() {
      callback.@com.google.gwt.chrome.crx.client.Debugger.AttachCallback::onAttach()();
    });
  }-*/;

  public static native void detach(int tabId) /*-{
    chrome["debugger"].detach({"tabId" : tabId});
  }-*/;

  public static native void detach(int tabId, DetachCallback callback) /*-{
    chrome["debugger"].detach({"tabId" : tabId}, function() {
      callback.@com.google.gwt.chrome.crx.client.Debugger.DetachCallback::onDetach()();
    });
  }-*/;

  public native static DebuggerDetachEvent getDetachEvent() /*-{
    return chrome["debugger"].onDetach;
  }-*/;

  public native static DebuggerEvent getEvent() /*-{
    return chrome["debugger"].onEvent;
  }-*/;

  public native static void sendCommand(int tabId, String method) /*-{
    chrome["debugger"].sendCommand({"tabId" : tabId}, method);
  }-*/;

  public native static void sendCommand(int tabId, String method, JavaScriptObject params) /*-{
    chrome["debugger"].sendCommand({"tabId" : tabId}, method, params);
  }-*/;

  public native static void sendRequest(int tabId, String method, JavaScriptObject params,
      SendRequestCallback callback) /*-{
    chrome["debugger"].sendCommand({"tabId" : tabId}, method, params, function(result) {
      callback.@com.google.gwt.chrome.crx.client.Debugger.SendRequestCallback::onResponse(Lcom/google/gwt/core/client/JavaScriptObject;)(result);
    });
  }-*/;
}
