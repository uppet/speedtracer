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

package com.google.speedtracer.client.visualizations.model;

import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.user.client.Timer;
import com.google.speedtracer.client.timeline.GraphModel;
import com.google.speedtracer.client.timeline.ModelData;

/**
 * Tests of {@link UiThreadUtilization} used in sluggishness calculation.
 */
public class UiThreadUtilizationTests extends GWTTestCase {

  /**
   * A testable {@link UiThreadUtilization}.
   */
  private static class MockUiThreadUtilization extends UiThreadUtilization {
    /**
     * A static factory method for creating instances of a testable version of
     * {@link UiThreadUtiliation}.
     * 
     * @return a testable version of {@link UiThreadUtilization}
     */
    public static MockUiThreadUtilization create() {
      return create(100);
    }

    public static MockUiThreadUtilization create(double maxUtilization) {
      GraphModel graphModel = GraphModel.createGraphModel(new ModelData(), "",
          "", "", "", false);
      return new MockUiThreadUtilization(graphModel, maxUtilization);
    }

    private GraphModel graphModel;

    private MockUiThreadUtilization(GraphModel graphModel, double maxUtilization) {
      super(graphModel, maxUtilization);
      this.graphModel = graphModel;
    }

    public GraphModel getGraphModel() {
      return this.graphModel;
    }
  }

  @Override
  public String getModuleName() {
    return "com.google.speedtracer.Common";
  }

  /**
   * Tests adding a blocking event which should cause utilization to reach 100%.
   */
  public void testMaxUtilization() {
    double maxUtilization = 100.0;
    MockUiThreadUtilization utilization = MockUiThreadUtilization.create(maxUtilization);
    utilization.enterBlocking(0);

    // The minimum time to reach 100% utilization is to fill 3 sliding windows.
    // Namely. 2 slides + a window width.
    double releaseTime = UiThreadUtilization.WINDOW_WIDTH + 2
        * (UiThreadUtilization.WINDOW_SLIDE_INCREMENT);

    utilization.releaseBlocking(releaseTime);

    assertEquals(utilization.getGraphModel().getMaxEncounteredValue(),
        maxUtilization, 0.0001);
    assertEquals(utilization.getGraphModel().getRangeValue(releaseTime),
        maxUtilization, 0.0001);
  }

  /**
   * Tests to make sure we can handle re-entrant events as well as out of order
   * events. Utilization handles out of order events by invalidating events
   * received during the time the out of order event was in transit.
   */
  public void testReEntrancy() {
    MockUiThreadUtilization utilizationA = MockUiThreadUtilization.create();
    // Test re-entrancy
    utilizationA.enterBlocking(0);
    utilizationA.releaseBlocking(200);
    utilizationA.enterBlocking(150);
    utilizationA.releaseBlocking(190);

    double maxUtilization = utilizationA.getGraphModel().getMaxEncounteredValue();
    // If we didn't crash make sure the data looks like it should
    assertEquals(maxUtilization, 100.0, 0.0001);
    assertEquals(utilizationA.getGraphModel().getRangeValue(200),
        maxUtilization, 0.0001);
    assertEquals(utilizationA.getGraphModel().getRangeValue(150),
        maxUtilization, 0.0001);

    // Test out of order events.
    MockUiThreadUtilization utilizationB = MockUiThreadUtilization.create();
    utilizationB.enterBlocking(0);
    utilizationB.releaseBlocking(100);

    // Simulate warm down timer cooling things off.
    utilizationB.enterBlocking(600);
    utilizationB.releaseBlocking(600);
    // Check to see if we are cooled off.
    assertEquals(utilizationB.getGraphModel().getRangeValue(600), 0, 0.0001);

    // out of order event to invalidate the cool off.
    utilizationB.enterBlocking(150);
    utilizationB.releaseBlocking(400);

    // check to make sure that we are at max utilization.
    assertEquals(utilizationB.getGraphModel().getRangeValue(400), 100.0, 0.0001);
    assertEquals(utilizationB.getGraphModel().getRangeValue(600), 100.0, 0.0001);
  }

  /**
   * Tests adding events which should converge to a utilization that should be
   * within a certain threshold, and not exceed it on average.
   */
  public void testUtilizationThresholds() {
    // Utilization should be within
    MockUiThreadUtilization utilization = MockUiThreadUtilization.create();

    // We are going to shoot for convergence to 50%
    double halfWindow = UiThreadUtilization.WINDOW_WIDTH / 2;

    utilization.enterBlocking(0);
    utilization.releaseBlocking(halfWindow);
    utilization.enterBlocking(2 * halfWindow);
    utilization.releaseBlocking(3 * halfWindow);
    // should converge by now
    utilization.enterBlocking(4 * halfWindow);
    utilization.releaseBlocking(5 * halfWindow);

    // Some wiggle room
    double threshold = 20;
    double target = 50;

    assertTrue(withinThreshold(utilization.getGraphModel().getRangeValue(
        4 * halfWindow), target, threshold));
    assertTrue(withinThreshold(utilization.getGraphModel().getRangeValue(
        5 * halfWindow), target, threshold));
    assertTrue(withinThreshold(
        utilization.getGraphModel().getMaxEncounteredValue(), target, threshold));
  }

  /**
   * Tests the warmdown phase to make sure that utilization converges back to
   * zero.
   */
  public void testWarmDown() {
    // Utilization should be within
    final MockUiThreadUtilization utilization = MockUiThreadUtilization.create();
    utilization.enterBlocking(0);
    // fill it to max utilization
    final double releaseTime = UiThreadUtilization.WINDOW_WIDTH + 2
        * (UiThreadUtilization.WINDOW_SLIDE_INCREMENT);
    utilization.releaseBlocking(releaseTime);

    // We could expose a getter on UiThreadUtilization, but I see no API
    // justification outside of testing to do so.
    final int warmDownTick = 500;

    // Check back in a bit to see if we are finished.
    Timer finishTimer = new Timer() {
      @Override
      public void run() {
        assertTrue(utilization.getGraphModel().getRangeValue(
            releaseTime + warmDownTick) == 0);
        finishTest();
      }
    };
    // Utilization should tick in warmDowntick time. it should already be
    // scheduled. This timer should therefore be scheduled after utilization is
    // done converging. We wait a little more.
    finishTimer.schedule(warmDownTick + 200);

    // Have the test end later
    delayTestFinish(warmDownTick + 400);
  }

  private boolean withinThreshold(double val, double target, double threshold) {
    return (Math.abs(val - target) <= threshold);
  }
}