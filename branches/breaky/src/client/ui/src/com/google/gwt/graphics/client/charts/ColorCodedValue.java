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
package com.google.gwt.graphics.client.charts;

import com.google.gwt.graphics.client.Color;

/**
 * Simple wrapper object that has key-value pairs and an optional label to drive
 * a simple Chart/Diagram.
 */
public class ColorCodedValue implements Comparable<ColorCodedValue> {

  /**
   * An Identifier and label for this entry.
   */
  public final String key;

  /**
   * The color associated with this entry on Charts.
   */
  public final Color labelColor;

  /**
   * The value of this entry.
   */
  public final double value;

  public ColorCodedValue(String key, double value, Color labelColor) {
    this.key = key;
    this.value = value;
    this.labelColor = labelColor;
  }

  public int compareTo(ColorCodedValue other) {
    return Double.compare(other.value, this.value);
  }
}
