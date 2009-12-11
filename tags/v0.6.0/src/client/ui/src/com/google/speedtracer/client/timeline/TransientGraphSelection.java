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

import com.google.gwt.dom.client.Element;
import com.google.gwt.events.client.Event;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.CssResource.Strict;
import com.google.gwt.topspin.ui.client.DefaultContainerImpl;
import com.google.gwt.topspin.ui.client.Div;
import com.google.gwt.topspin.ui.client.MouseDownEvent;
import com.google.speedtracer.client.util.dom.MouseCaptureListener;

/**
 * Transient selection region based on drag interactions. A highlighted region
 * on the graph from dragging with the mouse.
 */
public class TransientGraphSelection extends Div {

  /**
   * CSS class names used in {@link TransientGraphSelection}.
   */
  public interface Css extends CssResource {
    String transientSelection();
  }

  /**
   * Externalized interface.
   */
  public interface Resources extends ClientBundle {
    @Source("resources/TransientGraphSelection.css")
    @Strict()
    TransientGraphSelection.Css transientGraphSelectionCss();
  }

  /**
   * Handles Mouse Events over Each graph when the TransientGraphSelection is
   * attached to it.
   */
  private class MouseListener extends MouseCaptureListener {
    @Override
    public void onMouseDown(MouseDownEvent event) {
      super.onMouseDown(event);
      mouseX = event.getNativeEvent().getClientX();
      setPinnedBoundPx(mouseX);
    }

    @Override
    public void onMouseMove(Event event) {
      int newX = event.getClientX();

      if (!isVisible()) {
        show();
      }
      setDragBoundPx(newX);

      mouseX = newX;
    }

    @Override
    public void onMouseUp(Event event) {
      applySelection(timeLine);
      super.onMouseUp(event);
    }
  }

  protected int mouseX = 0;

  private boolean isVisible = false;

  private int leftOffset = 0;

  private int pinnedX = 0;

  private int rightOffset = 0;

  private final TimeLine timeLine;

  public TransientGraphSelection(TimeLine timeLine,
      TransientGraphSelection.Resources resources) {
    super(new DefaultContainerImpl(timeLine.getGraphContainerElement()));
    this.timeLine = timeLine;
    Element elem = getElement();
    elem.setClassName(resources.transientGraphSelectionCss().transientSelection());

    // Sink mouse events
    MouseListener mouseListener = new MouseListener();
    // We can ignore the remover since the lifecycle is tied to the lifecycle
    // of the MainTimeLine. Which is... the lifecycle of the Monitor.
    MouseDownEvent.addMouseDownListener(timeLine,
        timeLine.getGraphContainerElement(), mouseListener);
  }

  /**
   * Updates the graph bounds to the current selection bounds.
   * 
   * Can be overridden to allow for arbitrary actions to take place before
   * forwarding to this method via a call to super.applySelection.
   * 
   * Typical use case would be to block until some menu input invokes this
   * function to update the graph.
   */
  public void applySelection(TimeLine timeLine) {
    int correctedRight = getRightOffset();
    int correctedLeft = getLeftOffset();

    // Simple guard to not apply small selections
    if (Math.abs(correctedRight - correctedLeft) < 7) {
      this.hide();
      return;
    }

    if (this.isVisible() && (correctedRight > correctedLeft)) {
      int screenWidth = timeLine.getCurrentGraphWidth();
      double fractionalOffsetLeft = (double) correctedLeft
          / (double) screenWidth;
      double fractionalOffsetRight = (double) correctedRight
          / (double) screenWidth;

      double leftBound = timeLine.getModel().getLeftBound();
      double rightBound = timeLine.getModel().getRightBound();

      double domainWidth = rightBound - leftBound;
      double domainDeltaLeft = fractionalOffsetLeft * domainWidth;
      double domainDeltaRight = fractionalOffsetRight * domainWidth;
      // The new rightbound is the left of the timeline + the offset from the
      // left.
      rightBound = leftBound + domainDeltaRight;
      // The new leftbound is the left of the timeline + the offset from the
      // left. But we dont want to support selections outside the left bound of
      // the window. So we cap at the left edge (negative left delta).
      leftBound = (domainDeltaLeft < 0) ? leftBound : leftBound
          + domainDeltaLeft;
      timeLine.transitionTo(leftBound, rightBound);
      this.hide();
    }
  }

  public int getLeftOffset() {
    return leftOffset - Constants.GRAPH_PIXEL_OFFSET;
  }

  public int getRightOffset() {
    return rightOffset - Constants.GRAPH_PIXEL_OFFSET;
  }

  public void hide() {
    getElement().getStyle().setProperty("display", "none");
    isVisible = false;
    reset();
  }

  public boolean isVisible() {
    return isVisible;
  }

  public void reset() {
    rightOffset = leftOffset;
    getElement().getStyle().setPropertyPx("width", 0);
  }

  public void setDragBoundPx(int dragPx) {
    if (dragPx > pinnedX) {
      rightOffset = dragPx;
    } else {
      leftOffset = dragPx;
      getElement().getStyle().setPropertyPx("left", getLeftOffset());
    }
    int width = rightOffset - leftOffset;
    getElement().getStyle().setPropertyPx("width", width - 3 /* fudge factor */);
  }

  public void setPinnedBoundPx(int pinnedPx) {
    // assume left bound is pinned on mouse down
    leftOffset = pinnedPx;
    // Remember the pinned X value
    this.pinnedX = pinnedPx;
    getElement().getStyle().setPropertyPx("left", getLeftOffset());
    reset();
  }

  public void show() {
    getElement().getStyle().setProperty("display", "block");
    isVisible = true;
  }

}