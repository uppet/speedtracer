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

import com.google.gwt.chrome.crx.client.Port.Message;
import com.google.gwt.core.client.JavaScriptObject;

/**
 * Message sent from the Content Script to the Background page. This message
 * requests that the stored profiling data be sent to the content script.
 */
public class HeadlessSendDataMessage extends Message {
  public static final int TYPE = MessageType.PORT_HEADLESS_SEND_DUMP;

  protected HeadlessSendDataMessage() {
  }

  /**
   * Header information to send with the data. The Background Page will fill in
   * the header with a timeStamp field to indicate the base time for the data.
   */
  public final native JavaScriptObject getHeader() /*-{
    return this.header;
  }-*/;

  public final native String getUrl() /*-{
    return this.url;
  }-*/;
}
