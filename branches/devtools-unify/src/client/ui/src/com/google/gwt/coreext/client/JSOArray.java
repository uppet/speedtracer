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
package com.google.gwt.coreext.client;

import com.google.gwt.core.client.JavaScriptObject;

/**
 * Replacement for JSONArray.
 * 
 * @param <T> the type of each entry in the array
 */
public class JSOArray<T> extends JavaScriptObject {

  /**
   * Constructs a new one.
   * 
   * @param <M>
   * @return the array
   */
  public static native <M> JSOArray<M> create() /*-{
    return [];
  }-*/;

  /**
   * Invokes the native string split on a string and returns a JavaScript array.
   * GWT's version of string.split() emulates Java behavior in JavaScript.
   */
  public static native JSOArray<String> splitString(String str, String regexp) /*-{
    return str.split(regexp);
  }-*/;

  protected JSOArray() {
  }

  /**
   * Concatenate 2 arrays.
   * 
   * @param val an array to concatenate onto the end of this array.
   */
  public final native JSOArray<T> concat(JSOArray<T> val) /*-{
    return this.concat(val);
  }-*/;

  /**
   * Standard index accessor.
   * 
   * @param index
   * @return
   */
  public final native T get(int index) /*-{
    return this[index];
  }-*/;

  /**
   * @return
   */
  public final native boolean isEmpty() /*-{
    return (this.length == 0);
  }-*/;

  /**
   * Uses "" as the separator.
   * 
   * @return
   */
  public final String join() {
    return join("");
  }

  public final native String join(String sep) /*-{
    return this.join(sep);
  }-*/;

  /**
   * Returns the last element in the array.
   * 
   * @return the last element
   */
  public final native T peek() /*-{
    return this[this.length - 1];
  }-*/;

  public final native T pop() /*-{
    return this.pop();
  }-*/;

  /*
   * For backwards compatibility with IE5 and because this is marginally faster
   * than push() in IE6.
   */
  public final native void push(T pathStr) /*-{
    this[this.length] = pathStr;
  }-*/;

  public final native void set(int index, T value) /*-{
    this[index] = value;
  }-*/;

  public final native int size() /*-{
    return this.length;
  }-*/;
}
