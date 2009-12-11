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
package com.google.speedtracer.client.visualizations.model;

/**
 * Event class for synthetic overlay events.
 */
public class StaticGraphMarkerModel {
  public final double startTime;
  public final double duration;

  /**
   * Base class for a visualization overlays.
   * 
   * @param startTime Timestamp that represents the start of the overlay event
   *          (in milliseconds). This timestamp must be unnormalized.
   * @param duration duration of the event represented by the overlay (in
   *          milliseconds)
   */
  public StaticGraphMarkerModel(double startTime, double duration) {
    // Normalize here
    this.startTime = startTime;
    this.duration = duration;
  }
}
