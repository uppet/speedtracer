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

import com.google.speedtracer.client.util.JSOArray;

/**
 * LayoutEvent overlay.
 */
public class LotsOfLittleEvents extends UiEvent {
  public static final int TYPE = EventRecordType.AGGREGATED_EVENTS;

  protected LotsOfLittleEvents() {
  }

  /**
   * Returns the dominant event type tuple if it has been set.
   * 
   * @return the dominant event type tuple, or null.
   */
  public final native TypeCountDurationTuple getDominantEventTypeTuple() /*-{
    return this.dominantType;
  }-*/;

  /**
   * Gets an array with aggregate data about events collapsed into this one.
   * 
   * @return
   */
  public final JSOArray<TypeCountDurationTuple> getTypeCountDurationTuples() {
    return getData().getJSObjectProperty("events").cast();
  }

  final native void setDominantEventTypeTuple(TypeCountDurationTuple tuple) /*-{
    this.dominantType = tuple;
  }-*/;
}
