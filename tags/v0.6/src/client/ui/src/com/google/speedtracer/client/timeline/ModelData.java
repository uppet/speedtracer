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

import java.util.ArrayList;

/**
 * A simple ArrayList extension that provides truncation and max value
 * tracking.
 */
@SuppressWarnings("serial")
public class ModelData extends ArrayList<DataPoint> {

  private double maxEncounteredValue = 0;

  @Override
  public boolean add(DataPoint val) {
    if (val.getY() > maxEncounteredValue) {
      maxEncounteredValue = val.getY();
    }
    return super.add(val);
  }

  public double getMaxEncounteredValue() {
    return maxEncounteredValue;
  }

  public void truncateBy(int indicesToAxe) {
    removeRange(size() - indicesToAxe, size());
  }
}
