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

import com.google.gwt.junit.client.GWTTestCase;

/**
 * Tests {@link ModelData}.
 */
public class ModelDataTests extends GWTTestCase {

  @Override
  public String getModuleName() {
    return "com.google.speedtracer.ModelTests";
  }

  public void testModelDataAdd() {
    ModelData data = new ModelData();
    data.add(new DataPoint(1.0, 0.0)); // 0
    data.add(new DataPoint(2.0, 1.0)); // 1
    data.add(new DataPoint(3.0, 1.0)); // 2
    data.add(new DataPoint(3.1, 5.0)); // 3
    data.add(new DataPoint(4.0, 0.0)); // 4

    DataPoint val3 = data.get(3);
    DataPoint val0 = data.get(0);
    DataPoint val1 = data.get(1);
    DataPoint val4 = data.get(4);
    DataPoint val2 = data.get(2);

    assertEquals("val0.getX()", 1.0, val0.getX(), .00001);
    assertEquals("val0.getY()", 0.0, val0.getY(), .00001);
    assertEquals("val1.getX()", 2.0, val1.getX(), .00001);
    assertEquals("val1.getY()", 1.0, val1.getY(), .00001);
    assertEquals("val2.getX()", 3.0, val2.getX(), .00001);
    assertEquals("val2.getY()", 1.0, val2.getY(), .00001);
    assertEquals("val3.getX()", 3.1, val3.getX(), .00001);
    assertEquals("val3.getY()", 5.0, val3.getY(), .00001);
    assertEquals("val4.getX()", 4.0, val4.getX(), .00001);
    assertEquals("val4.getY()", 0.0, val4.getY(), .00001);
  }

  public void testModelDataMax() {
    ModelData data = new ModelData();
    data.add(new DataPoint(1.0, 0.0)); // 0
    data.add(new DataPoint(2.0, 1.0)); // 1
    data.add(new DataPoint(3.0, 1.0)); // 2
    data.add(new DataPoint(3.1, 5.0)); // 3
    data.add(new DataPoint(4.0, 0.0)); // 4

    double max = data.getMaxEncounteredValue();
    assertEquals("max encountered value 1", 5.0, max, .00001);

    data.add(new DataPoint(4.5, 11.0));
    max = data.getMaxEncounteredValue();
    assertEquals("max encountered value 2", 11.0, max, .00001);

    data.truncateBy(3);
    max = data.getMaxEncounteredValue();
    // TODO: Should the max encountered value be recomputed?
    // assertEquals("max encountered value 3", 1.0, max, .00001);
    assertEquals("max encountered value 3", 11.0, max, .00001);
  }

  public void testModelDataTruncateBy() {
    ModelData data = new ModelData();
    data.add(new DataPoint(1.0, 0.0));
    data.add(new DataPoint(2.0, 1.0));
    data.add(new DataPoint(3.0, 1.0));
    data.add(new DataPoint(3.1, 5.0));
    data.add(new DataPoint(4.0, 0.0));

    assertEquals("data.size()", 5, data.size());
    data.truncateBy(1);
    assertEquals("data.size()", 4, data.size());
    data.truncateBy(4);
    assertEquals("data.size()", 0, data.size());
  }
}
