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
 * Transition for float styles.
 */
public class CssTransitionFloat extends Animation {

  /**
   * Callback invoked when the transition is finished.
   */
  public interface CallBack {
    void onTransitionEnd();
  }

  private static final CssTransitionFloat INSTANCE = new CssTransitionFloat();

  public static CssTransitionFloat get() {
    return INSTANCE;
  }

  private CallBack callBack;
  private String cssProperty;
  private Element elem;

  private double startValue, endValue;

  private CssTransitionFloat() {
    super();
  }

  @Override
  public void onCancel() {
  }

  @Override
  public void onComplete() {
    if (callBack != null) {
      callBack.onTransitionEnd();
    }
  }

  @Override
  public void onStart() {
  }

  @Override
  public void onUpdate(double progress) {
    float value = (float) (startValue + (endValue - startValue) * progress);
    elem.getStyle().setProperty(this.cssProperty, value + "");
  }

  public void setCallBack(CallBack cb) {
    this.onComplete();
    callBack = cb;
  }

  public void transition(Element elem, String property, double start,
      double end, int duration) {

    this.elem = elem;
    this.cssProperty = property;
    this.startValue = start;
    this.endValue = end;

    run(duration);
  }
}
