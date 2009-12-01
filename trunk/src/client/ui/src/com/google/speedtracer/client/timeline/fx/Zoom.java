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
package com.google.speedtracer.client.timeline.fx;

import com.google.gwt.animation.client.Animation;
import com.google.speedtracer.client.view.MainTimeLine;

/**
 * The class that drives the zoom transitions for navigating the timeline.
 */
public class Zoom extends Animation {

  /**
   * CallBack interface for animation chaining.
   */
  public interface CallBack {
    void onAnimationComplete();
  }

  private CallBack cb = null;
  private boolean inProgress = false;
  private double leftEnd;
  private double leftStart;
  private double rightEnd;
  private double rightStart;

  private MainTimeLine timeLine;

  public Zoom(MainTimeLine timeLine) {
    this.timeLine = timeLine;
  }

  public boolean isInProgress() {
    return inProgress;
  }

  @Override
  public void onCancel() {
    if (inProgress) {
      onComplete();
    }
  }

  @Override
  public void onComplete() {
    inProgress = false;
    timeLine.toggleGraphPrecision();
    onUpdate(1.0);

    if (cb != null) {
      cb.onAnimationComplete();
    }
  }

  @Override
  public void onStart() {
  }

  @Override
  public void onUpdate(double progress) {
    double newLeftBound = leftStart + ((leftEnd - leftStart) * progress);
    double newRightBound = rightStart + ((rightEnd - rightStart) * progress);

    if (newLeftBound < newRightBound) {
      timeLine.getModel().updateBounds(newLeftBound, newRightBound);
    } else {
      timeLine.getModel().updateBounds(newRightBound, newLeftBound);
    }
  }

  public void setCallBack(CallBack callBack) {
    onCancel();
    this.cb = callBack;
  }

  public void zoom(int dur, double lEnd, double rEnd) {
    leftStart = timeLine.getModel().getLeftBound();
    leftEnd = lEnd;
    rightStart = timeLine.getModel().getRightBound();
    rightEnd = rEnd;
    inProgress = true;
    timeLine.toggleGraphPrecision();
    this.run(dur);
  }
}
