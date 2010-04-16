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
 * LayoutEvent overlay.
 */
public class PaintEvent extends UiEvent {
  public static final int TYPE = EventRecordType.PAINT_EVENT;

  protected PaintEvent() {
  }

  public final int getHeight() {
    return getData().getIntProperty("height");
  }

  public final int getWidth() {
    return getData().getIntProperty("width");
  }

  public final int getX() {
    return getData().getIntProperty("x");
  }

  public final int getY() {
    return getData().getIntProperty("y");
  }
}
