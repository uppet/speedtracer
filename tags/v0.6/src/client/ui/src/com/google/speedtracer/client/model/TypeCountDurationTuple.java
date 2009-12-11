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
package com.google.speedtracer.client.model;

import com.google.gwt.core.client.JavaScriptObject;

/**
 * Overlay type for a record type / count / duration tuple represented as a
 * JSArray. Used in LotsOfLittleEvents objects.
 */
public class TypeCountDurationTuple extends JavaScriptObject {
  protected TypeCountDurationTuple() {
  }

  /**
   * Gets the number of events of this type.
   * 
   * @return count
   */
  public final native int getCount() /*-{
    return this[1];
  }-*/;

  /**
   * Gets the total time spent by events of this type.
   * 
   * @return duration in milliseconds
   */
  public final native double getDuration() /*-{
    return this[2];
  }-*/;

  /**
   * Gets the type of event this tuple wraps. Will always be in range of the
   * EventRecordType enum.
   * 
   * @return
   */
  public final native int getType() /*-{
    return this[0];
  }-*/;
}
