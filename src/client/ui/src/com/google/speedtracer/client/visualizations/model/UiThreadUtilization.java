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

import com.google.gwt.user.client.Timer;
import com.google.speedtracer.client.MonitorConstants;
import com.google.speedtracer.client.timeline.GraphModel;

/**
 * Class responsible for calculating Utilization of UI Thread. This provides the
 * data points for driving visualizations like the
 * {@link SluggishnessVisualization}.
 */
public class UiThreadUtilization {
  // We will use a 75ms sliding window. slides at increments of 35ms.
  static final double WINDOW_SLIDE_INCREMENT = MonitorConstants.MIN_GRAPH_DATA_RESOLUTION;

  // Width of a sliding window in ms.
  static final double WINDOW_WIDTH = 75;

  // Constant time between ticks of the warmdown timer.
  private static final int TICK_TIME = 500;

  // The weights for computing the utilization average. These were derived by
  // hand through trial and error using our instrumented data sets, to yield
  // "reasonably" looking graphs.
  private static final double[] weights = {0.1, 0.2, 0.7};

  // The time spent blocked in the current window.
  private double blockedTimeInWindow;

  // Current position within a window.
  private double currentMarker;

  // Current begin point of window.
  private double currentWindowStart;

  // Number of active events.
  private int eventDepth;

  // The model that we populate with data points.
  private final GraphModel graphModel;

  // Utilization values are scaled up to the range [0-maxUtilization].
  private final double maxUtilization;

  // The final compute value for UiThreadUtilization.
  private double scaledWeightedUtilization;

  // The ticks to have utilization converge to a stable value
  // after not getting any events.
  private final Timer warmDownTimer = new Timer() {
    @Override
    public void run() {
      slideWindowTo(currentWindowStart + WINDOW_WIDTH + TICK_TIME);
      // Converge to essentially zero
      if (getThreadUtilization() > 0.005 * maxUtilization) {
        // Keep going until we can be idle.
        // utilization will converge to 0
        warmDownTimer.schedule(TICK_TIME);
      } else {
        // make it 0
        windowHistory[2] = 0;
        windowHistory[1] = 0;
        windowHistory[0] = 0;
        updateWeightedUtilization();
      }
    }
  };

  // Utilization is stored also as a pure fraction to allow for
  // scaling to arbitrary units. [0-1]
  private double weightedUtilizationPercent;

  // We keep history of last 3 windows.
  // windowHistory[2] is treated as most recent in time.
  private double[] windowHistory = {0, 0, 0};

  /**
   * Constructor for UiThreadUtilization.
   * 
   * @param graphModel the model that will be populated with data points
   * @param maxUtilization
   */
  public UiThreadUtilization(GraphModel graphModel, double maxUtilization) {
    this.graphModel = graphModel;
    this.maxUtilization = maxUtilization;
  }

  /**
   * Called when we enter an event that busies the UI thread. We assume that the
   * state of the UI Thread does not change up to this point.
   * 
   * @param at the time that we begin blocking
   */
  public void enterBlocking(double at) {
    slideWindowTo(at);
    // at is now guaranteed to be within the window bounds.
    if (eventDepth > 0) {
      // we are blocking.
      blockedTimeInWindow = at - currentMarker;
    }

    currentMarker = at;

    eventDepth++;
    // schedule the timer again.
    warmDownTimer.schedule(TICK_TIME);
  }

  /**
   * A getter for the current scaled and weighted utilization. Utilization is a
   * weighted average of last 3 windows.
   * 
   * @return the current scaled and weighted utilization
   */
  public double getThreadUtilization() {
    return scaledWeightedUtilization;
  }

  /**
   * Called when an event is done busying up the UI Thread.
   * 
   * @param at the time at which we release blocking the UI thread.
   */
  public void releaseBlocking(double at) {
    assert (currentWindowStart >= 0);

    slideWindowTo(at);
    // at is now guaranteed to be within the window bounds.
    // and blocking
    blockedTimeInWindow = at - currentMarker;
    currentMarker = at;

    eventDepth--;
    // schedule the timer again.
    warmDownTimer.schedule(TICK_TIME);
  }

  /**
   * Computes the utilization percentage for the current window only.
   */
  private double calculateCurrentUtilization() {
    return blockedTimeInWindow / WINDOW_WIDTH;
  }

  /**
   * Computes the dotProduct of our sliding windows and weights vectors.
   * 
   * TODO(jaimeyap): I could make this a general purpose dotProduct function,
   * but for now, this is more performant. Trivial to add later.
   */
  private double dotProduct(double[] windows, double[] weights) {
    assert ((windows.length == 3) && (weights.length == 3));
    return (windows[2] * weights[2]) + (windows[1] * weights[1])
        + (windows[0] * weights[0]);
  }

  /**
   * Slides the window to the specified time, calculating the Sluggishness and
   * updating the graph along the way.
   * 
   * We need to compute all windows that have fully elapsed since the last time
   * we updated. If we were blocking on the last update, all windows will be
   * fully blocking (1.0 utilization). If the last update was not blocking, all
   * windows are fully non-blocking (0.0 utilization). When this method exists,
   * toTime will be in the current window (currentWindowStart < toTime <
   * currentWindowStart + windowWidth).
   */
  private void slideWindowTo(double toTime) {
    /*
     * Our events are buffered up in the NPAPI plugin. So in the case that we get
     * a buffered event whose timestamps are in ranges before the current
     * window, we have to invalidate the data points we added to the graph
     * during said period. We can get into this state if our warm down timer
     * fires and warms utilization down, but an event that should have occurred
     * was buffered and not passed up. When the event arrives, we have to fix
     * things by invalidating the most recent data points.
     */
    if (toTime < currentWindowStart) {
      double timeToGoBack = currentWindowStart - toTime;
      int numIterationsInvalid = (int) Math.ceil(timeToGoBack
          / WINDOW_SLIDE_INCREMENT);
      int dataSize = graphModel.getData().size();

      assert (numIterationsInvalid < dataSize);

      graphModel.getData().truncateBy(numIterationsInvalid);
      currentWindowStart = currentWindowStart
          - (numIterationsInvalid * WINDOW_SLIDE_INCREMENT);
      currentMarker = currentWindowStart;
    }

    double windowEnd;
    // We need to roll up to current time.
    // Slide the window until the toTime is within the bounds of the window.
    while (toTime >= (windowEnd = (currentWindowStart + WINDOW_WIDTH))) {
      // toTime is guaranteed to be off the right edge of the window or exactly
      // on the right edge of the window.
      double remainderInWindow = windowEnd - currentMarker;
      if (eventDepth > 0) {
        // we are blocking.
        // Guaranteed we block all the way.
        blockedTimeInWindow = Math.min(WINDOW_WIDTH, blockedTimeInWindow
            + remainderInWindow);
      } else {
        // we are not previously blocking. so we can assume that the remainder
        // of the new window is unblocked.
        blockedTimeInWindow = Math.max(0, blockedTimeInWindow
            - remainderInWindow);
      }
      // shift window start by the increment
      currentWindowStart += WINDOW_SLIDE_INCREMENT;
      // reset the marker to the window beginning if we pass it on sliding.
      currentMarker = Math.max(currentWindowStart, currentMarker);

      // shift down our iteration history
      windowHistory[0] = windowHistory[1];
      windowHistory[1] = windowHistory[2];
      windowHistory[2] = calculateCurrentUtilization();
      updateWeightedUtilization();

      // Now log a data point in the graph.
      // So that the peaks coincide a with the events in the
      // list, we add it to the graph at the currentMarker.
      graphModel.addData(currentMarker, getThreadUtilization());
    }

    // Sanity check
    assert (toTime < currentWindowStart + WINDOW_WIDTH);
  }

  /*
   * Computes the weighted utilization over the last 3 windows, and scales it up
   * to the max value.
   */
  private void updateWeightedUtilization() {
    weightedUtilizationPercent = dotProduct(windowHistory, weights);
    scaledWeightedUtilization = weightedUtilizationPercent * maxUtilization;
  }
}