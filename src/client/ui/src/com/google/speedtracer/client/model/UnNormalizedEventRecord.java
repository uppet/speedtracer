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

/**
 * An EventRecord that has not yet been time normalized. Normalization involves
 * setting the time field relative to some base time, and establishing a
 * duration if there happens to be one.
 * 
 * Before normalizing, {@link #getTime()} will return NaN.
 */
public class UnNormalizedEventRecord extends EventRecord {
  protected UnNormalizedEventRecord() {
  }

  public final native EventRecord convertToEventRecord(double baseTime) /*-{
    if (this.hasOwnProperty("endTime")) {
      this.duration = this.endTime - this.startTime;  
    }

    this.time = this.startTime - baseTime;

    delete this.startTime;
    delete this.endTime;

    return this;
  }-*/;

  public final native double getStartTime() /*-{
    return this.startTime;
  }-*/;
}