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
import com.google.gwt.core.client.JsArrayString;

/**
 * A lightweight implementation of a String to Object map.
 * 
 * TODO(knorton): Add other primitive specialization maps.
 * 
 * @see JsStringBooleanMap
 * 
 * @param <T>
 */
public final class JsStringMap<T> extends JavaScriptObject {
  /**
   * Creates a new map.
   * 
   * @param <T>
   * @return
   */
  public static <T> JsStringMap<T> create() {
    return JavaScriptObject.createObject().cast();
  }

  static native void erase(JavaScriptObject object, String key) /*-{
    var p = @com.google.gwt.coreext.client.JsStringMap::getPropertyForKey(Ljava/lang/String;)(key);
    delete object[p];
  }-*/;

  static native boolean getBoolean(JavaScriptObject object, String key) /*-{
    var p = @com.google.gwt.coreext.client.JsStringMap::getPropertyForKey(Ljava/lang/String;)(key);
    return object[p];
  }-*/;

  static native JavaScriptObject getKeys(JavaScriptObject object) /*-{
    var data = [];
    for (var item in object) {
      if (object.hasOwnProperty(item)) {
        var key = @com.google.gwt.coreext.client.JsStringMap::getKeyForProperty(Ljava/lang/String;)(item);
        data.push(key);
      }
    }
    return data;
  }-*/;

  static native JavaScriptObject getValues(JavaScriptObject object) /*-{
    var data = [];
    for (var item in object) {
      if (object.hasOwnProperty(item)) {
        data.push(object[item]);
      }
    }
    return data;
  }-*/;

  static native boolean hasKey(JavaScriptObject object, String key) /*-{
    var p = @com.google.gwt.coreext.client.JsStringMap::getPropertyForKey(Ljava/lang/String;)(key);
    return object.hasOwnProperty(p);
  }-*/;

  static native void put(JavaScriptObject object, String key, boolean value) /*-{
    var p = @com.google.gwt.coreext.client.JsStringMap::getPropertyForKey(Ljava/lang/String;)(key);
    object[p] = value;
  }-*/;

  @SuppressWarnings("unused")
  // Called from JSNI.
  private static String getKeyForProperty(String property) {
    assert property.charAt(0) == ':' : "map contains a property without the proper prefix";
    return property.substring(1);
  }

  @SuppressWarnings("unused")
  // Called from JSNI.
  private static String getPropertyForKey(String key) {
    assert key != null : "native maps do not allow null key values.";
    return ":" + key;
  }

  protected JsStringMap() {
  }

  /**
   * Removes an existing key from the map.
   * 
   * @param key
   */
  public void erase(String key) {
    erase(this, key);
  }

  /**
   * Retrieves the value stored for the given key.
   * 
   * @param key
   * @return the value, <code>null</code> if the key doesn't exist
   */
  public native T get(String key) /*-{
    var p = @com.google.gwt.coreext.client.JsStringMap::getPropertyForKey(Ljava/lang/String;)(key);
    return this[p];
  }-*/;

  /**
   * Gets all keys. The order is unspecified.
   * 
   * @return
   */
  public JsArrayString getKeys() {
    return getKeys(this).cast();
  }

  /**
   * Gets all values. The order is unspecified.
   * 
   * @return
   */
  public JSOArray<T> getValues() {
    return getValues(this).cast();
  }

  /**
   * Indicates whether there is a value stored for the given key.
   * 
   * @param key
   * @return
   */
  public boolean hasKey(String key) {
    return hasKey(this, key);
  }

  /**
   * Store a value for the given key.
   * 
   * @param key
   * @param value
   */
  public native void put(String key, T value) /*-{
    var p = @com.google.gwt.coreext.client.JsStringMap::getPropertyForKey(Ljava/lang/String;)(key);
    this[p] = value;
  }-*/;
}
