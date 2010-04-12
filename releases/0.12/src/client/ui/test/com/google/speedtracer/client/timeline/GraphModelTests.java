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
import com.google.speedtracer.client.MonitorConstants;
import com.google.speedtracer.client.timeline.GraphModel.RegularGraphModel;
import com.google.speedtracer.client.timeline.GraphModel.SparseGraphModel;

/**
 * Tests {@link GraphModel}.
 */
public class GraphModelTests extends GWTTestCase {

  /**
   * Testable subclass to circumvent scoping restrictions.
   */
  private class TestableRegularGraphModel extends RegularGraphModel {
    public TestableRegularGraphModel() {
      super(new ModelData(), "", "", "", "",
          MonitorConstants.MIN_GRAPH_DATA_RESOLUTION);
    }

    public TestableRegularGraphModel(ModelData data, String xAxisLabel,
        String xUnit, String yAxisLabel, String yUnit, double intervalSize) {
      super(data, xAxisLabel, xUnit, yAxisLabel, yUnit, intervalSize);
    }
  }

  /**
   * Testable subclass to circumvent scoping restrictions.
   */
  private class TestableSparseGraphModel extends SparseGraphModel {
    public TestableSparseGraphModel() {
      super(new ModelData(), "", "", "", "");
    }

    public TestableSparseGraphModel(ModelData data, String xLabel,
        String xUnit, String yAxisLabel, String yUnit) {
      super(data, xLabel, xUnit, yAxisLabel, yUnit);
    }

    public double findMaxValueInRangeProxy(int closestIndex,
        double targetDomainValue, double sampleRange) {
      return super.findMaxValueInRange(closestIndex, targetDomainValue,
          sampleRange);
    }
  }

  // Simple test data set.
  private double[][] simpleTestData = {
      {0, 1}, {10, 4}, {15, 3}, {40, 10}, {41, 2}};

  @Override
  public String getModuleName() {
    return "com.google.speedtracer.Common";
  }

  /**
   * Tests {@link GraphModel#findMaxValueInRange(int, double, double)}. Sparsity
   * does not affect test.
   */
  public void testFindMaxValueInRange() {
    ModelData data = new ModelData();
    data.add(new DataPoint(1.0, 0.0)); // 0
    data.add(new DataPoint(1.0, 1.0)); // 1
    data.add(new DataPoint(1.0, 1.0)); // 2
    data.add(new DataPoint(1.0, 5.0)); // 3
    data.add(new DataPoint(1.0, 0.0)); // 4

    TestableSparseGraphModel graphModel = new TestableSparseGraphModel(data,
        "", "", "", "");

    double found = graphModel.findMaxValueInRangeProxy(0, 0.0, 3.0);
    assertEquals("searching for max value from 0.0 to 3.0", 5.0, found, .001);

    found = graphModel.findMaxValueInRangeProxy(0, -5.0, 3.0);
    assertEquals("Expected null in search for max value from -5.0 to -2.0",
        found, 0.0, .001);

    found = graphModel.findMaxValueInRangeProxy(4, 5.0, 3.0);
    assertEquals("Expected null in search for max value from 5.0 to 7.0",
        found, 0.0, .001);
  }

  /**
   * Tests that a GraphModel gets initialized correctly on construction.
   * Sparsity is irrelevant for this test.
   */
  public void testGraphModelConstruction() {
    ModelData modelData = new ModelData();
    GraphModel graphModel = GraphModel.createGraphModel(modelData, "Time",
        "ms", "domain", "crackers", true);

    // Sanity Check
    assertTrue(graphModel.getXAxisLabel().equals("Time"));
    assertTrue(graphModel.getXAxisUnit().equals("ms"));
    assertTrue(graphModel.getYAxisLabel().equals("domain"));
    assertTrue(graphModel.getYAxisUnit().equals("crackers"));

    // With no data added yet. minX should be initialized to Double.MAX_VALUE;
    assertTrue(graphModel.getMinX() == Double.MAX_VALUE);
  }

  /**
   * Tests {@link RegularGraphModel#addData(double, double)} and
   * {@link RegularGraphModel#getRangeValue(double)}.
   */
  public void testRegularAddingDataPoints() {
    TestableRegularGraphModel graphModel = new TestableRegularGraphModel();
    // Add test data to graph.
    for (int i = 0; i < simpleTestData.length; i++) {
      graphModel.addData(simpleTestData[i][0], simpleTestData[i][1]);
    }

    // Make sure that querying the test data gives us what we want.
    for (int i = 0; i < simpleTestData.length; i++) {
      assertTrue(graphModel.getRangeValue(simpleTestData[i][0]) == simpleTestData[i][1]);
    }

    // Check getters for min and max domain values (ranges already tested
    // above) after adding some data.
    assertTrue(graphModel.getMinX() == simpleTestData[0][0]);
    assertTrue(graphModel.getMaxX() == simpleTestData[simpleTestData.length - 1][0]);

    // Test negative time. Should always give 0.
    assertTrue(graphModel.getRangeValue(-1) == 0);
  }

  /**
   * Tests the heuristic closestIndex search for {@link RegularGraphModel}.
   */
  public void testRegularFindClosestIndex() {
    ModelData data = new ModelData();
    data.add(new DataPoint(1.0, 0.0)); // 0
    data.add(new DataPoint(2.0, 1.0)); // 1
    data.add(new DataPoint(3.0, 1.0)); // 2
    data.add(new DataPoint(3.1, 5.0)); // 3
    data.add(new DataPoint(4.0, 0.0)); // 4

    TestableRegularGraphModel graphModel = new TestableRegularGraphModel(data,
        "", "", "", "", 35);

    int foundIndex;

    foundIndex = graphModel.findClosestIndex(-999);
    assertEquals("search for -999", -1, foundIndex);
    foundIndex = graphModel.findClosestIndex(.5);
    assertEquals("search for .5", -1, foundIndex);
    foundIndex = graphModel.findClosestIndex(1.0);
    assertEquals("search for 1.0", 0, foundIndex);
    foundIndex = graphModel.findClosestIndex(1.5);
    assertEquals("search for 1.5", 0, foundIndex);
    foundIndex = graphModel.findClosestIndex(2.0);
    assertEquals("search for 2.0", 1, foundIndex);
    foundIndex = graphModel.findClosestIndex(2.9);
    assertEquals("search for 2.9", 1, foundIndex);
    foundIndex = graphModel.findClosestIndex(3.0);
    assertEquals("search for 3.0", 2, foundIndex);
    foundIndex = graphModel.findClosestIndex(3.09);
    assertEquals("search for 3.09", 2, foundIndex);
    foundIndex = graphModel.findClosestIndex(3.19);
    assertEquals("search for 3.19", 3, foundIndex);
    foundIndex = graphModel.findClosestIndex(4.0);
    assertEquals("search for 4.0", 4, foundIndex);
    foundIndex = graphModel.findClosestIndex(4.1);
    assertEquals("search for 4.1", 4, foundIndex);
    foundIndex = graphModel.findClosestIndex(999);
    assertEquals("search for 999", 4, foundIndex);

    // In an empty data model
    graphModel = new TestableRegularGraphModel();

    foundIndex = graphModel.findClosestIndex(-999);
    assertEquals("search for -999", -1, foundIndex);
    foundIndex = graphModel.findClosestIndex(.5);
    assertEquals("search for .5", -1, foundIndex);
    foundIndex = graphModel.findClosestIndex(4.1);
    assertEquals("search for 4.1", -1, foundIndex);
    foundIndex = graphModel.findClosestIndex(999);
    assertEquals("search for 999", -1, foundIndex);

    // A single entry in the data model
    data = new ModelData();
    data.add(new DataPoint(-100.0, 0.0)); // 0
    graphModel = new TestableRegularGraphModel(data, "", "", "", "", 35);

    foundIndex = graphModel.findClosestIndex(-999.0);
    assertEquals("search for -999", -1, foundIndex);
    foundIndex = graphModel.findClosestIndex(-100.0);
    assertEquals("search for -100", 0, foundIndex);
    foundIndex = graphModel.findClosestIndex(1.0);
    assertEquals("search for 1.0", 0, foundIndex);
    foundIndex = graphModel.findClosestIndex(999.0);
    assertEquals("search for 999", 0, foundIndex);
  }

  /**
   * Tests querying for data on time boundaries in between data points for
   * {@link RegularGraphModel}.
   */
  public void testRegularInterpolation() {
    TestableRegularGraphModel graphModel = new TestableRegularGraphModel();

    int dataPoints = simpleTestData.length;
    // Add test data to graph.
    for (int i = 0; i < dataPoints; i++) {
      graphModel.addData(simpleTestData[i][0], simpleTestData[i][1]);
    }

    // Let us check the default interpolator which should return the average
    // of two surrounding points.
    for (int i = 0; i < dataPoints - 1; i++) {
      double domainToQuery = (simpleTestData[i][0] + simpleTestData[i + 1][0]) / 2;
      double rangeValueInBetween = (simpleTestData[i][1] + simpleTestData[i + 1][1]) / 2;
      assertTrue(graphModel.getRangeValue(domainToQuery) == rangeValueInBetween);
    }

    // We want to also test a query off to the right of the data set.
    // We say max domain value + 1
    // and + 55
    assertTrue(graphModel.getRangeValue(simpleTestData[dataPoints - 1][0] + 1) == simpleTestData[dataPoints - 1][1]);
    assertTrue(graphModel.getRangeValue(simpleTestData[dataPoints - 1][0] + 55) == simpleTestData[dataPoints - 1][1]);
  }

  /**
   * Tests {@link SparseGraphModel#addData(double, double)} and
   * {@link SparseGraphModel#getRangeValue(double)}.
   */
  public void testSparseAddingDataPoints() {
    TestableSparseGraphModel graphModel = new TestableSparseGraphModel();
    // Add test data to graph.
    for (int i = 0; i < simpleTestData.length; i++) {
      graphModel.addData(simpleTestData[i][0], simpleTestData[i][1]);
    }

    // Make sure that querying the test data gives us what we want.
    for (int i = 0; i < simpleTestData.length; i++) {
      assertTrue(graphModel.getRangeValue(simpleTestData[i][0]) == simpleTestData[i][1]);
    }

    // Check getters for min and max domain values (ranges already tested
    // above) after adding some data.
    assertTrue(graphModel.getMinX() == simpleTestData[0][0]);
    assertTrue(graphModel.getMaxX() == simpleTestData[simpleTestData.length - 1][0]);

    // Test negative time. Should always give 0.
    assertTrue(graphModel.getRangeValue(-1) == 0);
  }

  /**
   * Tests the closestIndex search for {@link SparseGraphModel}. This search
   * uses an iterative binary search.
   */
  public void testSparseFindClosestIndex() {
    ModelData data = new ModelData();
    data.add(new DataPoint(1.0, 0.0)); // 0
    data.add(new DataPoint(2.0, 1.0)); // 1
    data.add(new DataPoint(3.0, 1.0)); // 2
    data.add(new DataPoint(3.1, 5.0)); // 3
    data.add(new DataPoint(4.0, 0.0)); // 4

    TestableSparseGraphModel graphModel = new TestableSparseGraphModel(data,
        "", "", "", "");

    int foundIndex;

    foundIndex = graphModel.findClosestIndex(-999);
    assertEquals("search for -999", -1, foundIndex);
    foundIndex = graphModel.findClosestIndex(.5);
    assertEquals("search for .5", -1, foundIndex);
    foundIndex = graphModel.findClosestIndex(1.0);
    assertEquals("search for 1.0", 0, foundIndex);
    foundIndex = graphModel.findClosestIndex(1.5);
    assertEquals("search for 1.5", 0, foundIndex);
    foundIndex = graphModel.findClosestIndex(2.0);
    assertEquals("search for 2.0", 1, foundIndex);
    foundIndex = graphModel.findClosestIndex(2.9);
    assertEquals("search for 2.9", 1, foundIndex);
    foundIndex = graphModel.findClosestIndex(3.0);
    assertEquals("search for 3.0", 2, foundIndex);
    foundIndex = graphModel.findClosestIndex(3.09);
    assertEquals("search for 3.09", 2, foundIndex);
    foundIndex = graphModel.findClosestIndex(3.19);
    assertEquals("search for 3.19", 3, foundIndex);
    foundIndex = graphModel.findClosestIndex(4.0);
    assertEquals("search for 4.0", 4, foundIndex);
    foundIndex = graphModel.findClosestIndex(4.1);
    assertEquals("search for 4.1", 4, foundIndex);
    foundIndex = graphModel.findClosestIndex(999);
    assertEquals("search for 999", 4, foundIndex);

    // In an empty data model
    graphModel = new TestableSparseGraphModel();

    foundIndex = graphModel.findClosestIndex(-999);
    assertEquals("search for -999", -1, foundIndex);
    foundIndex = graphModel.findClosestIndex(.5);
    assertEquals("search for .5", -1, foundIndex);
    foundIndex = graphModel.findClosestIndex(4.1);
    assertEquals("search for 4.1", -1, foundIndex);
    foundIndex = graphModel.findClosestIndex(999);
    assertEquals("search for 999", -1, foundIndex);

    // A single entry in the data model
    data = new ModelData();
    data.add(new DataPoint(-100.0, 0.0)); // 0
    graphModel = new TestableSparseGraphModel(data, "", "", "", "");

    foundIndex = graphModel.findClosestIndex(-999.0);
    assertEquals("search for -999", -1, foundIndex);
    foundIndex = graphModel.findClosestIndex(-100.0);
    assertEquals("search for -100", 0, foundIndex);
    foundIndex = graphModel.findClosestIndex(1.0);
    assertEquals("search for 1.0", 0, foundIndex);
    foundIndex = graphModel.findClosestIndex(999.0);
    assertEquals("search for 999", 0, foundIndex);

    // Irregularly spaced data with multiple entries on the same time
    graphModel = new TestableSparseGraphModel();

    graphModel.addData(0, 1);
    graphModel.addData(10, 4);
    graphModel.addData(15, 0);
    graphModel.addData(40, 10);
    // Note that we have two entries at the same time!
    // Should return right most.
    graphModel.addData(40, 2);
    graphModel.addData(41, 2);
    
    foundIndex = graphModel.findClosestIndex(-100.0);
    assertEquals("search for -100", -1, foundIndex);
    foundIndex = graphModel.findClosestIndex(1.0);
    assertEquals("search for 1.0", 0, foundIndex);
    foundIndex = graphModel.findClosestIndex(999.0);
    assertEquals("search for 999", 5, foundIndex);
    foundIndex = graphModel.findClosestIndex(5.0);
    assertEquals("search for 5.0", 0, foundIndex);
    foundIndex = graphModel.findClosestIndex(40.3);
    assertEquals("search for 40.3", 4, foundIndex);
    foundIndex = graphModel.findClosestIndex(30.3);
    assertEquals("search for 30.3", 2, foundIndex);
    foundIndex = graphModel.findClosestIndex(20.0);
    assertEquals("search for 20.0", 2, foundIndex);
  }

  /**
   * Tests querying for data on time boundaries in between data points for
   * {@link SparseGraphModel}.
   */
  public void testSparseInterpolation() {
    TestableSparseGraphModel graphModel = new TestableSparseGraphModel();

    int dataPoints = simpleTestData.length;
    // Add test data to graph.
    for (int i = 0; i < dataPoints; i++) {
      graphModel.addData(simpleTestData[i][0], simpleTestData[i][1]);
    }

    // Let us check the default interpolator... which doesn't really
    // interpolate. It
    // just rounds down to the nearest index.
    for (int i = 0; i < dataPoints - 1; i++) {
      double domainToQuery = (simpleTestData[i][0] + simpleTestData[i + 1][0]) / 2;
      double rangeValueForLowerIndex = simpleTestData[i][1];
      assertTrue(graphModel.getRangeValue(domainToQuery) == rangeValueForLowerIndex);
    }

    // We want to also test a query off to the right of the data set.
    // We say max domain value + 1
    // and + 55
    assertTrue(graphModel.getRangeValue(simpleTestData[dataPoints - 1][0] + 1) == simpleTestData[dataPoints - 1][1]);
    assertTrue(graphModel.getRangeValue(simpleTestData[dataPoints - 1][0] + 55) == simpleTestData[dataPoints - 1][1]);
  }
}
