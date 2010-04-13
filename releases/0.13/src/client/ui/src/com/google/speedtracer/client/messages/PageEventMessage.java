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

import com.google.gwt.chrome.crx.client.Port.Message;
import com.google.gwt.chrome.crx.client.events.DevToolsPageEvent.PageEvent;
import com.google.speedtracer.client.util.JSON;

/**
 * Overlay type associated with sending an Inspector PageEvent JSON strings over
 * postMessage from a content script or some other entity over a chromium
 * extension Port.
 */
public class PageEventMessage extends Message {
  public static final int TYPE = MessageType.PORT_PAGE_EVENT_TYPE;

  public static PageEventMessage create(String recordStr) {
    return setFields(Message.create(TYPE), recordStr);
  }

  private static native PageEventMessage setFields(Message msg, String recordStr) /*-{
    msg["record"] = recordStr;
    return msg;
  }-*/;

  protected PageEventMessage() {
  }

  public final PageEvent getPageEvent() {
    return JSON.parse(getRecordString()).cast();
  }

  public final native String getRecordString() /*-{
    return this.record;
  }-*/;

  public final native String getVersion() /*-{
    return this.version;
  }-*/;
}