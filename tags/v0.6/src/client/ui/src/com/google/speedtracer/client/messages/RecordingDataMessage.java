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

import com.google.speedtracer.client.Monitor;
import com.google.speedtracer.client.WindowChannel.Message;

/**
 * Message sent from the Monitor to the BackgroundPage to inform the
 * background page to stop/start recording. Also sent from the background page
 * to Monitor when the recording state is changed from another method.
 */
public class RecordingDataMessage extends Message {
  public static final int TYPE = MessageType.RECORD_DATA_TYPE;

  public static RecordingDataMessage create(boolean isRecording) {
    return create(Monitor.DEFAULT_ID, Monitor.DEFAULT_ID, isRecording);
  }
  public static native RecordingDataMessage create(int tabId, int browserId,
      boolean isRecording) /*-{
    return {tabId: tabId, browserId: browserId, isRecording: isRecording};
  }-*/;

  protected RecordingDataMessage() {
  }

  public final native int getBrowserId() /*-{
    return this.browserId;
  }-*/;

  public final native int getTabId() /*-{
    return this.tabId;
  }-*/;

  public final native boolean isRecording() /*-{
    return this.isRecording;
  }-*/;
}
