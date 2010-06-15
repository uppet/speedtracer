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
package com.google.speedtracer.client.model;

import com.google.gwt.core.client.JavaScriptObject;

/**
 * Represents a single entry in the whitelist. This is a javascript object that
 * gets stored in HTML local storage.
 */
public class WhitelistEntry extends JavaScriptObject {

  public static WhitelistEntry create(String host, String port) {
    WhitelistEntry entry = JavaScriptObject.createObject().cast();
    return entry.setHost(host).setPort(port);
  }

  protected WhitelistEntry() {
  }

  public final native void clearKey() /*-{
    delete this.key;
  }-*/;

  public final native String getHost() /*-{
    return this.host;
  }-*/;

  public final native String getKey() /*-{
    return this.key;
  }-*/;

  public final native String getPort() /*-{
    return this.port;
  }-*/;

  public final String getValidationErrors() {
    String host = getHost();
    if (null == host || host.isEmpty()) {
      return ("Host is empty");
    }

    String port = getPort();
    if (null == port || port.isEmpty()) {
      return ("Port is empty");
    }
    if (!port.equals("*") && !port.matches("^\\d+$")) {
      return ("Port expected * or a numeric value.");
    }
    return null;
  }

  public final boolean isEqual(WhitelistEntry cmp) {
    return cmp.getHost() == this.getHost() && cmp.getPort() == this.getPort();
  }

  public final native WhitelistEntry setHost(String host) /*-{
    this.host = host;
    return this;
  }-*/;

  public final native WhitelistEntry setKey(String key) /*-{
    this.key = key;
    return this;
  }-*/;

  public final native WhitelistEntry setPort(String port) /*-{
    this.port = port;
    return this;
  }-*/;
}
