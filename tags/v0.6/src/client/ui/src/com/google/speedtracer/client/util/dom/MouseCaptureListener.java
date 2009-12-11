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
package com.google.speedtracer.client.util.dom;

import com.google.gwt.events.client.Event;
import com.google.gwt.topspin.ui.client.MouseDownEvent;
import com.google.gwt.topspin.ui.client.MouseDownListener;
import com.google.speedtracer.client.util.dom.MouseEventCapture.CaptureReleaser;

/**
 * Listener that can be used for event capture. Simply add this as a
 * MouseDownListener and then you will get mouse capture for moves and mouseup
 * for that element.
 */
public abstract class MouseCaptureListener implements MouseDownListener {

  private CaptureReleaser releaser;

  public void handleEvent(Event evt) {
    if (evt.getType().equals("mousemove")) {
      this.onMouseMove(evt);
    } else if (evt.getType().equals("mouseup")) {
      this.onMouseUp(evt);
    }
  }

  /**
   * Starts capture for you on mouse down.
   */
  public void onMouseDown(MouseDownEvent evt) {
    MouseEventCapture.capture(this);
    evt.preventDefault();
  }

  public abstract void onMouseMove(Event evt);

  /**
   * Default implementation of mouseup simply releases capture.
   * 
   * @param evt
   */
  public void onMouseUp(Event evt) {
    release();
  }

  protected void release() {
    if (releaser != null) {
      releaser.release();
    }
  }
  
  void setCaptureReleaser(CaptureReleaser releaser) {
    this.releaser = releaser;
  }
}
