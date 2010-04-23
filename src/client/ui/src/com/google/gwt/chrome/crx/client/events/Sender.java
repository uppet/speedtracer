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
package com.google.gwt.chrome.crx.client.events;

import com.google.gwt.chrome.crx.client.Tabs.Tab;
import com.google.gwt.core.client.JavaScriptObject;

/**
 * The extension that sent the request.
 */
public class Sender extends JavaScriptObject {
  protected Sender() {
  }

  /**
   * The String extension ID of the extension that sent the request.
   * 
   * @return
   */
  public final native String getId() /*-{
    return this.id;
  }-*/;

  /**
   * This will only be set when a request is sent from a Tab or a Content
   * Script.
   * 
   * @return The Tab that sent the request.
   */
  public final native Tab getTab() /*-{
    return this.tab;
  }-*/;
}