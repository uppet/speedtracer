/*
 * Copyright 2008 Google Inc.
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
import com.google.speedtracer.client.model.TabDescription;
import com.google.speedtracer.client.model.DataModel.DataInstance;

/**
 * A message to pass the required information to initialize an
 * {@link com.google.speedtracer.client.Monitor}. This type is intended to be
 * exchanged among GWT applications using {@link WindowChannel}.
 * 
 */
public class InitializeMonitorMessage extends WindowChannel.Message {
  public static final int TYPE = MessageType.INITIALIZE_MONITOR_TYPE;

  /**
   * Create an instance of this message.
   * 
   * @param tabDescription information about the tab that is initializing
   * @param handle an opaque handle to a
   *          {@link com.google.speedtracer.client.model.DataModel}
   * @return a message that can be sent with
   *         {@link WindowChannel#sendMessage(int, com.google.gwt.core.client.JavaScriptObject)}
   */
  public static native InitializeMonitorMessage create(
      TabDescription tabDescription, DataInstance handle, String version) /*-{
    return { tabDescription: tabDescription, handle: handle, version: version };
  }-*/;

  protected InitializeMonitorMessage() {
  }

  /**
   * Gets an opaque handle to a
   * {@link com.google.speedtracer.client.model.DataModel}.
   * 
   * @return a handle
   */
  public final native DataInstance getHandle() /*-{
    return this.handle;
  }-*/;

  /**
   * Info about the tab that is to be monitored.
   * 
   * @return tab info
   */
  public final native TabDescription getTabDescription() /*-{
    return this.tabDescription;
  }-*/;

  /**
   * The extensions version string.
   * 
   * @return version
   */
  public final native String getVersion() /*-{
    return this.version;
  }-*/;
}
