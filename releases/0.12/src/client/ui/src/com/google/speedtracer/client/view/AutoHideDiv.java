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
package com.google.speedtracer.client.view;

import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Element;
import com.google.gwt.topspin.client.Command;
import com.google.gwt.topspin.ui.client.Container;
import com.google.gwt.topspin.ui.client.Div;
import com.google.gwt.topspin.ui.client.MouseOutEvent;
import com.google.gwt.topspin.ui.client.MouseOutListener;
import com.google.gwt.topspin.ui.client.MouseOverEvent;
import com.google.gwt.topspin.ui.client.MouseOverListener;

/**
 * Div that auto hides when mouse is not over it.
 */
public abstract class AutoHideDiv extends Div {

  /**
   * Callback interface for when the inline menu disappears.
   */
  public interface OnHideCallBack {
    void onHide();
  }

  protected boolean aboutToHide = false;

  protected OnHideCallBack callBack = null;

  protected boolean isVisible = false;

  private final int delay;

  public AutoHideDiv(Container container, final int delay) {
    super(container);
    this.delay = delay;
    sinkEvents();
  }

  public AutoHideDiv(DivElement myElement, final int delay) {
    super(myElement);
    this.delay = delay;
    sinkEvents();
  }

  public void forceHide() {
    Element elem = getElement();
    elem.getStyle().setProperty("display", "none");
    aboutToHide = false;
    isVisible = false;
    if (callBack != null) {
      callBack.onHide();
      callBack = null;
    }
  }

  public void hide() {
    // in case we mouse back over, don't hide!
    if (aboutToHide) {
      Element elem = getElement();
      elem.getStyle().setProperty("display", "none");
      aboutToHide = false;
      isVisible = false;
      if (callBack != null) {
        callBack.onHide();
        callBack = null;
      }
    }
  }

  public boolean isAboutToHide() {
    return aboutToHide;
  }

  public boolean isVisible() {
    return isVisible;
  }

  public void setAboutToHide(boolean b) {
    aboutToHide = b;
  }

  public void show() {
    getElement().getStyle().setProperty("display", "block");
    isVisible = true;
    deferredHide();
  }

  private void deferredHide() {
    if (!isAboutToHide()) {
      Command.defer(new Command() {
        @Override
        public void execute() {
          hide();
        }
      }, delay);
    }
    setAboutToHide(true);
  }

  private void sinkEvents() {
    addMouseOverListener(new MouseOverListener() {
      public void onMouseOver(MouseOverEvent event) {
        setAboutToHide(false);
      }
    });

    addMouseOutListener(new MouseOutListener() {
      public void onMouseOut(MouseOutEvent event) {
        // To prevent queuing of Deferred commands
        deferredHide();
      }
    });
  }
}
