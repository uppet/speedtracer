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
package com.google.gwt.coreext.client;

import com.google.gwt.core.client.JavaScriptObject;

/**
 * DataBag is a utility class to access {@link JavaScriptObject} fields Any JSO
 * can be casted to a DataBag, or you can use it statically.
 */
public class DataBag extends JavaScriptObject {
  /*
   * Static Accessors
   */
  public static final native boolean getBooleanProperty(JavaScriptObject obj,
      String prop) /*-{
    return !!obj[prop];
  }-*/;

  public static final native double getDoubleProperty(JavaScriptObject obj,
      String prop) /*-{
    return obj[prop];
  }-*/;

  public static final native int getIntProperty(JavaScriptObject obj,
      String prop) /*-{
    return obj[prop];
  }-*/;

  public static final native <T extends JavaScriptObject> T getJSObjectProperty(
      JavaScriptObject obj, String prop) /*-{
    return obj[prop];
  }-*/;

  public static final native String getStringProperty(JavaScriptObject obj,
      String prop) /*-{
    return obj[prop];
  }-*/;

  public static final native boolean hasOwnProperty(JavaScriptObject obj,
      String prop) /*-{
    return obj.hasOwnProperty(prop);
  }-*/;

  protected DataBag() {
  }

  public final native boolean getBooleanProperty(String prop) /*-{
    return !!this[prop];
  }-*/;

  public final native double getDoubleProperty(String prop) /*-{
    return this[prop];
  }-*/;

  public final native int getIntProperty(String prop) /*-{
    return this[prop];
  }-*/;

  public final native <T extends JavaScriptObject> T getJSObjectProperty(
      String prop) /*-{
    return this[prop];
  }-*/;

  public final native String getStringProperty(String prop) /*-{
    return this[prop];
  }-*/;

  public final native boolean hasOwnProperty(String prop) /*-{
    return this.hasOwnProperty(prop);
  }-*/;
}