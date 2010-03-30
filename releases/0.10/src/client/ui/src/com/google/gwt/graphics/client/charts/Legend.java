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

import java.util.List;

/**
 * Attaches to a SimpleChart to display a Legend of the chart's data.
 */
public class Legend extends ColorCodedDataList {

  /**
   * Externalized resources interface.
   */
  public interface Resources extends ColorCodedDataList.Resources {
  }

  public Legend(SimpleChart chart, List<ColorCodedValue> data, double total,
      boolean renderHorizontally, boolean usePercent,
      ColorCodedDataList.Resources resources) {
    super(chart.getContainer(), data, data.size(), total, renderHorizontally,
        usePercent, resources);
  }
}
