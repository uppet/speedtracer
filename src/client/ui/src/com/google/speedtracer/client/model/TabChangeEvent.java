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

import com.google.speedtracer.shared.EventRecordType;

/**
 * EventRecord indicating a page transition or a page refresh.
 */
public class TabChangeEvent extends EventRecord {
  public static final int TYPE = EventRecordType.TAB_CHANGED;

  public static native TabChangeEvent create(double time, String newUrl) /*-{
    return {
      time: time,
      type: @com.google.speedtracer.client.model.TabChangeEvent::TYPE,
      data: {
        url: newUrl
      }
    };
  }-*/;

  public static native UnNormalizedEventRecord createUnNormalized(
      double startTime, String newUrl) /*-{
    return {
      startTime: startTime,
      type: @com.google.speedtracer.client.model.TabChangeEvent::TYPE,
      data: {
        url: newUrl
      }
    };
  }-*/;

  protected TabChangeEvent() {
  }

  public final String getUrl() {
    return getData().getStringProperty("url");
  }
}
