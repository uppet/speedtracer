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
package com.google.speedtracer.client.messages;

import com.google.speedtracer.client.WindowChannel;
import com.google.speedtracer.client.util.dom.WindowExt;

/**
 * Message sent by the {@link Monitor} when it comes online to request that
 * it be initialized.
 */
public class RequestInitializationMessage extends WindowChannel.Message {
  public static final int TYPE = MessageType.REQUEST_INITIALIZATION_TYPE;

  /**
   * Create an instance of this message.
   * 
   * @param tabId the associate tab id;
   * @return a message that can be sent with
   *         {@link WindowChannel#sendMessage(int, JavaScriptObject)}
   */
  public static native RequestInitializationMessage create(int tabId,
      int browserId, WindowExt wind) /*-{
    return {tabId: tabId, browserId: browserId, wind: wind};
  }-*/;

  protected RequestInitializationMessage() {
  }

  /**
   * Gets the tab id from the message.
   * 
   * @return the associated Tab id.
   */
  public final native int getBrowserId()/*-{
    return this.browserId;
  }-*/;

  /**
   * Gets the owning {@link WindowExt} for the Monitor.
   * 
   * @return the monitor's window.
   */
  public final native WindowExt getMonitorWindow() /*-{
    return this.wind;
  }-*/;

  /**
   * Gets the tab id from the message.
   * 
   * @return the associated Tab id.
   */
  public final native int getTabId()/*-{
    return this.tabId;
  }-*/;
}
