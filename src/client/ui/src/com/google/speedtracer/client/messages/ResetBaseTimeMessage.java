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
package com.google.speedtracer.client.messages;

import com.google.speedtracer.client.WindowChannel;

/**
 * Message sent by the {@link com.google.speedtracer.client.Monitor} when it
 * wants to reset the time normalization in its associated data instance.
 */
public class ResetBaseTimeMessage extends WindowChannel.Message {
  public static final int TYPE = MessageType.RESET_BASE_TIME_TYPE;

  /**
   * Create an instance of this message.
   * 
   * @param tabId the associate tab id;
   * @return a message that can be sent with {@link WindowChannel#sendMessage}
   */
  public static native ResetBaseTimeMessage create(int tabId, int browserId) /*-{
    return {tabId: tabId,  browserId: browserId};
  }-*/;

  protected ResetBaseTimeMessage() {
  }

  /**
   * Gets the Browser id from the message.
   * 
   * @return the associated Browser id.
   */
  public final native int getBrowserId()/*-{
    return this.browserId;
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
