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
package com.google.speedtracer.client.util;

import com.google.gwt.core.client.JavaScriptObject;

/**
 * Overlay type to deal strictly with double primitives and int keys. We can use
 * this instead of JsIntegerMap<T> in scenarios where we do not want to pay the
 * penalty of Double<->double autoboxing semantics. Since Java generics do not
 * support primitive types, JsIntegerMap can only hold values of type Double.
 * Additionally, pulling Doubles out of maps require that we need to do nullity
 * checks, and thus the GWT compiler must compile this to something that is a
 * Class wrapper for a double primitive, when what we really want is just a
 * plain double.
 */
public class JsIntegerDoubleMap extends JavaScriptObject {

  /**
   * Callback interface for int,double key value pairs.
   */
  public interface IterationCallBack {
    void onIteration(int key, double val);
  }

  /**
   * Create a new empty map.
   * 
   * @return an empty map
   */
  public static JsIntegerDoubleMap create() {
    return JavaScriptObject.createObject().cast();
  }

  protected JsIntegerDoubleMap() {
  }

  /**
   * Removes the mapping for this key from the map.
   * 
   * @param key
   */
  public final native void erase(int key) /*-{
    delete this[key];
  }-*/;

  /**
   * Returns the value associated with the specified key.
   * 
   * @param key
   * @return the value associated with the key
   */
  public final native double get(int key) /*-{
    return this[key];
  }-*/;

  /**
   * Returns an array containing all the values in this map.
   * 
   * @return a snapshot of the values contained in the map
   */
  public final native JSOArray<Double> getValues() /*-{
    var data = [];
    for (var i in this) {
      if (this.hasOwnProperty(i)) {
        data.push(this[i]);
      }
    }
    return data;
  }-*/;

  /**
   * Returns true if this map has an entry for the specified key.
   * 
   * @param key
   * @return true if this map has an entry for the given key
   */
  public final native boolean hasKey(int key) /*-{
    return this.hasOwnProperty(key);
  }-*/;

  /**
   * Iterates through the elements and calls back with the proper key value
   * pair.
   * 
   * @param callback
   */
  public final native void iterate(IterationCallBack callback) /*-{
    for (key in this) {
      if (this.hasOwnProperty(key)) {
        callback.
        @com.google.speedtracer.client.util.JsIntegerDoubleMap.IterationCallBack::onIteration(ID)
        (parseInt(key), this[key]);
      }
    }
  }-*/;

  /**
   * Associates the specified value with the specified key in this map.
   * 
   * @param key key with which the value will be associated
   * @param val value to be associated with key
   */
  public final native void put(int key, double val) /*-{
    this[key] = val;
  }-*/;
}
