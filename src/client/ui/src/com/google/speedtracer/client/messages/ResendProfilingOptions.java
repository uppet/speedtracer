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

import com.google.speedtracer.client.WindowChannel.Message;

/**
 * Message sent from the BackgroundPage to the Monitor to instruct it to re-send
 * the profiling options since they may potentially be out of synch with the
 * target renderer.
 */
public class ResendProfilingOptions extends Message {
  public static final int TYPE = MessageType.WC_RESEND_PROFILING_DATA_TYPE;

  public static native ResendProfilingOptions create() /*-{
    return {};
  }-*/;

  protected ResendProfilingOptions() {
  }
}
