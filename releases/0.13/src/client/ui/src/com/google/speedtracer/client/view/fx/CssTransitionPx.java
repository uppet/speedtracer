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
package com.google.speedtracer.client.view.fx;

import com.google.gwt.animation.client.Animation;
import com.google.gwt.dom.client.Element;

/**
 * Reusable class for Animating transitions of Css properties that operate with
 * pixels.
 */
public class CssTransitionPx extends Animation {

  /**
   * Callback invoked when the transition is finished.
   */
  public interface CallBack {
    void onTransitionEnd();
  }

  private static final CssTransitionPx INSTANCE = new CssTransitionPx();

  public static CssTransitionPx get() {
    return INSTANCE;
  }

  private CallBack callBack;
  private String cssProperty;
  private Element elem;

  private double startValue, endValue;

  private CssTransitionPx() {
    super();
  }

  @Override
  public void onCancel() {
  }

  @Override
  public void onComplete() {
    if (callBack != null) {
      callBack.onTransitionEnd();
      callBack = null;
    }
    if (elem != null) {
      // Finalize
      onUpdate(1.0);
    }
  }

  @Override
  public void onStart() {
  }

  @Override
  public void onUpdate(double progress) {
    int value = (int) (startValue + (endValue - startValue) * progress);
    elem.getStyle().setPropertyPx(this.cssProperty, value);
  }

  public void setCallBack(CallBack cb) {
    this.onComplete();
    callBack = cb;
  }

  public void transition(Element elem, String property, int start, int end,
      int duration) {

    this.elem = elem;
    this.cssProperty = property;
    this.startValue = start;
    this.endValue = end;

    run(duration);
  }
}
