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
package com.google.gwt.chrome.crx.client.events;

import com.google.gwt.core.client.JavaScriptObject;

public final class DebuggerEvent extends Event {

  /**
   * Notifications sent by the Chrome Debugger API contain payloads that adhere to this basic
   * format.
   */
  public static final class RawDebuggerEventRecord extends JavaScriptObject {
    protected RawDebuggerEventRecord() {      
    }

    public native String getMethod() /*-{
      return this.method;
    }-*/;
  }

  /**
   * The tab being debugged.
   */
  public static class Debuggee extends JavaScriptObject {
    protected Debuggee() {      
    }

    public final native int getTabId() /*-{
      return this.tabId;
    }-*/;
  }

  public interface Listener {
    void onEvent(Debuggee source, String method, RawDebuggerEventRecord body);
  }

  public final ListenerHandle addListener(Listener listener) {
    return new ListenerHandle(this, addListenerImpl(listener));
  }
  
  private native JavaScriptObject addListenerImpl(Listener listener) /*-{
    var handle = function(source, method, body) {
      listener.
        @com.google.gwt.chrome.crx.client.events.DebuggerEvent.Listener::onEvent(Lcom/google/gwt/chrome/crx/client/events/DebuggerEvent$Debuggee;Ljava/lang/String;Lcom/google/gwt/chrome/crx/client/events/DebuggerEvent$RawDebuggerEventRecord;)
        (source, method, body);
    };
    this.addListener(handle);
    return handle;
  }-*/;
  
  protected DebuggerEvent() {
  }
}
