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
package com.google.speedtracer.client.model;

import com.google.speedtracer.shared.EventRecordType;

/**
 * TimerMetaData overlay. We get one of these when a Timer is installed.
 */
public class TimerInstalled extends TimerRecord {
  public static final int TYPE = EventRecordType.TIMER_INSTALLED;
  
  protected TimerInstalled() {
  }

  public final double getInterval() {
    return getData().getDoubleProperty("timeout");
  }

  public final boolean isSingleShot() {
    return getData().getBooleanProperty("singleShot");
  }
}
