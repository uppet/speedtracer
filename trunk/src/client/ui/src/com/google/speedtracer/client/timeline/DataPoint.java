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
package com.google.speedtracer.client.timeline;

/**
 * This class is a simple container for X and Y values.
 * 
 */
public class DataPoint {
  
  public static DataPoint createDataPoint(double xVal, double yVal) {
    return new DataPoint(xVal,yVal);
  }
  
  private final double x;
  private final double y;
  
  protected DataPoint(double xVal, double yVal) {
    x = xVal;
    y = yVal;
  }
  
  public final double getX() {
    return x;
  }
  
  public final double getY() {
    return y;
  }
}
