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

import com.google.gwt.dom.client.Element;
import com.google.gwt.events.client.Event;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.resources.client.CssResource.Strict;
import com.google.gwt.topspin.ui.client.DefaultContainerImpl;
import com.google.gwt.topspin.ui.client.DoubleClickEvent;
import com.google.gwt.topspin.ui.client.DoubleClickListener;
import com.google.gwt.topspin.ui.client.MouseDownEvent;
import com.google.speedtracer.client.timeline.Constants;
import com.google.speedtracer.client.util.dom.DocumentExt;
import com.google.speedtracer.client.util.dom.MouseCaptureListener;
import com.google.speedtracer.client.util.dom.MouseCursor;
import com.google.speedtracer.client.view.DomainRegionSelectionBoundaries.DomainRegionSelectionBound;
import com.google.speedtracer.client.view.OverViewTimeLine.OverViewTimeLineModel;

/**
 * Highlighted region of the Domain on the OverViewGraph.
 */
public class DomainRegionSelection {

  /**
   * CSS.
   */
  public interface Css extends CssResource {
    String hoverable();

    String leftMask();

    String rightMask();

    String scaleSelectionLeft();

    String scaleSelectionRight();

    String scaleSelectionValue();
  }

  /**
   * Externalized interface.
   */
  public interface Resources extends ClientBundle {
    @Source("resources/DomainRegionSelection.css")
    @Strict
    DomainRegionSelection.Css domainRegionSelectionCss();

    @Source("resources/grippy.png")
    ImageResource grippy();
  }

  /**
   * The capture mouse listener for dragging the selection boundaries. This
   * listener sprouts into existence in the MouseDownListener for one of the
   * selection boundaries.
   */
  private class CaptureSelectionBoundMouseListener extends MouseCaptureListener {

    private final DomainRegionSelectionBound selection;

    public CaptureSelectionBoundMouseListener(
        DomainRegionSelectionBound selection) {
      this.selection = selection;
    }

    @Override
    public void onMouseDown(MouseDownEvent event) {
      super.onMouseDown(event);
      boundDragging = true;
      mouseX = getXOffset(event.getNativeEvent().getClientX());
      MouseCursor.setWResize();
    }

    @Override
    public void onMouseMove(Event event) {
      int newX = getXOffset(event.getClientX());
      double domainDelta = getDomainDeltaFromPixelDelta(newX - mouseX);
      selection.setDomainValue(selection.getDomainValue() + domainDelta, newX,
          overViewTimeLine.getCurrentGraphWidth());
      mouseX = newX;
    }

    @Override
    public void onMouseUp(Event event) {
      // We want to make sure we can't accidentally make a negative selection.
      // If the user drags a grippy beyond the left bound of the overview, we
      // simply cap it at the edge on mouseup.
      if (selection.getDomainValue() < overViewTimeLineModel.getLeftBound()) {
        selection.setDomainValue(overViewTimeLineModel.getLeftBound(), 0,
            overViewTimeLine.getCurrentGraphWidth());
      }

      overViewTimeLine.getMainTimeLine().transitionTo(
          selectionBoundaries.getLeftSelectionBound().getDomainValue(),
          selectionBoundaries.getRightSelectionBound().getDomainValue());
      boundDragging = false;

      MouseCursor.setDefault();
      super.onMouseUp(event);
    }
  }

  /**
   * Mouse listener for the domain region selection.
   */
  private class SelectionRegionMouseListener extends MouseCaptureListener {

    @Override
    public void onMouseDown(MouseDownEvent event) {
      if (!boundDragging) {
        super.onMouseDown(event);
        mouseX = getXOffset(event.getNativeEvent().getClientX());
        MouseCursor.setMove();
        event.preventDefault();
      }
    }

    @Override
    public void onMouseMove(Event evt) {
      int newX = getXOffset(evt.getClientX());
      dragging = true;
      int pixelDelta = newX - mouseX;
      double domainDelta = getDomainDeltaFromPixelDelta(pixelDelta);

      DomainRegionSelectionBound leftBound = selectionBoundaries.getLeftSelectionBound();
      DomainRegionSelectionBound rightBound = selectionBoundaries.getRightSelectionBound();
      double leftDomain = leftBound.getDomainValue() + domainDelta;
      double rightDomain = rightBound.getDomainValue() + domainDelta;
      int leftOffset = leftBound.getPixelOffset() + pixelDelta;
      int rightOffset = rightBound.getPixelOffset() + pixelDelta;

      // Bounds capping so normal dragging cant push selection
      // off the sides.
      int screenWidth = overViewTimeLine.getCurrentGraphWidth();
      if (leftDomain < overViewTimeLineModel.getLeftBound()) {
        leftDomain = overViewTimeLineModel.getLeftBound();
        rightDomain = overViewTimeLineModel.getLeftBound()
            + selectionBoundaries.getSelectionDomainWidth();
        leftOffset = 0;
        rightOffset = selectionBoundaries.getSelectionPixelWidth();
      } else {
        if (rightDomain >= overViewTimeLineModel.getRightBound()) {
          leftDomain = overViewTimeLineModel.getRightBound()
              - selectionBoundaries.getSelectionDomainWidth();
          rightDomain = overViewTimeLineModel.getRightBound();
          rightOffset = screenWidth;
          leftOffset = screenWidth
              - selectionBoundaries.getSelectionPixelWidth();
        }
      }

      selectionBoundaries.updateSelectionBounds(leftDomain, rightDomain,
          leftOffset, rightOffset, screenWidth);
      mouseX = newX;
    }

    @Override
    public void onMouseUp(Event evt) {
      if (dragging) {
        overViewTimeLine.getMainTimeLine().transitionTo(
            selectionBoundaries.getLeftSelectionBound().getDomainValue(),
            selectionBoundaries.getRightSelectionBound().getDomainValue());
      }
      dragging = false;
      MouseCursor.setDefault();

      super.onMouseUp(evt);
    }
  }

  protected int mouseX = 0;

  private boolean boundDragging = false;

  private boolean dragging = false;

  private final Element leftMask;

  private final CaptureSelectionBoundMouseListener leftSelectionBoundCaptureListener;

  private final OverViewTimeLine overViewTimeLine;

  private final OverViewTimeLineModel overViewTimeLineModel;

  private final Element rightMask;

  private final CaptureSelectionBoundMouseListener rightSelectionBoundCaptureListener;

  // DomainRegion selectors
  private final DomainRegionSelectionBoundaries selectionBoundaries;

  public DomainRegionSelection(Element parent,
      OverViewTimeLine overViewTimeLine,
      DomainRegionSelection.Resources resources) {
    this.overViewTimeLine = overViewTimeLine;
    this.overViewTimeLineModel = (OverViewTimeLineModel) overViewTimeLine.getModel();
    DomainRegionSelection.Css css = resources.domainRegionSelectionCss();
    this.overViewTimeLine.getElement().getParentElement().addClassName(
        css.hoverable());
    leftMask = DocumentExt.get().createDivWithClassName(css.leftMask());
    rightMask = DocumentExt.get().createDivWithClassName(css.rightMask());

    // Append the masks to the parent
    parent.appendChild(leftMask);
    parent.appendChild(rightMask);

    // Attach the grippies to our mutual parent.
    selectionBoundaries = new DomainRegionSelectionBoundaries(
        new DefaultContainerImpl(parent), resources);

    leftSelectionBoundCaptureListener = new CaptureSelectionBoundMouseListener(
        selectionBoundaries.getLeftSelectionBound());

    rightSelectionBoundCaptureListener = new CaptureSelectionBoundMouseListener(
        selectionBoundaries.getRightSelectionBound());

    sinkEvents();
  }

  /**
   * When the TimeLine is propagating updates, we need to reflect this here. So
   * we update the position of the left side.
   * 
   * @param left
   */
  public void setLeftBound(double left) {
    if (!dragging && !boundDragging) {
      DomainRegionSelectionBound selectionLeft = selectionBoundaries.getLeftSelectionBound();
      double domainWidth = overViewTimeLineModel.getRightBound()
          - overViewTimeLineModel.getLeftBound();
      double pixelDelta = ((double) overViewTimeLine.getCurrentGraphWidth())
          / (domainWidth == 0 ? 1 : domainWidth);
      selectionLeft.setDomainValue(left,
          (int) ((left - overViewTimeLineModel.getLeftBound()) * pixelDelta),
          overViewTimeLine.getCurrentGraphWidth());
      leftMask.getStyle().setPropertyPx("width", selectionLeft.getPixelOffset());
    }
  }

  /**
   * When the TimeLine is propagating updates, we need to reflect this here. So
   * we update the position of the right side.
   * 
   * @param right
   */
  public void setRightBound(double right) {
    if (!dragging && !boundDragging) {
      DomainRegionSelectionBound selectionRight = selectionBoundaries.getRightSelectionBound();
      double domainWidth = overViewTimeLineModel.getRightBound()
          - overViewTimeLineModel.getLeftBound();
      double pixelDelta = ((double) overViewTimeLine.getCurrentGraphWidth())
          / (domainWidth == 0 ? 1 : domainWidth);
      selectionRight.setDomainValue(right,
          (int) ((right - overViewTimeLineModel.getLeftBound()) * pixelDelta),
          overViewTimeLine.getCurrentGraphWidth());
      rightMask.getStyle().setPropertyPx(
          "width",
          overViewTimeLine.getCurrentGraphWidth()
              - (selectionBoundaries.getSelectionPixelWidth() + selectionBoundaries.getLeftSelectionBound().pixelOffset));
    }
  }

  /**
   * Takes in a delta in screen coords and gives the corresponding delta in the
   * domain space.
   * 
   * @param pixelDelta
   * @return
   */
  private double getDomainDeltaFromPixelDelta(int pixelDelta) {
    double fractionalShift = (double) pixelDelta
        / overViewTimeLine.getCurrentGraphWidth();

    return (overViewTimeLineModel.getRightBound() - overViewTimeLineModel.getLeftBound())
        * fractionalShift;
  }

  private int getXOffset(int offset) {
    return offset - Constants.GRAPH_PIXEL_OFFSET;
  }

  private void sinkEvents() {
    // Hook mouse capture on Left selection grippy
    MouseDownEvent.addMouseDownListener(
        selectionBoundaries.getLeftSelectionBound(),
        selectionBoundaries.getLeftSelectionBound().getElement(),
        leftSelectionBoundCaptureListener);

    // Hook mouse capture on Right Selection Grippy
    MouseDownEvent.addMouseDownListener(
        selectionBoundaries.getRightSelectionBound(),
        selectionBoundaries.getRightSelectionBound().getElement(),
        rightSelectionBoundCaptureListener);

    // Drag navigation
    SelectionRegionMouseListener selectionMouseListener = new SelectionRegionMouseListener();
    MouseDownEvent.addMouseDownListener(overViewTimeLine,
        overViewTimeLine.getElement(), selectionMouseListener);

    // Double Click Zoom
    DoubleClickEvent.addDoubleClickListener(overViewTimeLine,
        overViewTimeLine.getElement(), new DoubleClickListener() {

          public void onDoubleClick(DoubleClickEvent event) {
            int newX = getXOffset(event.getNativeEvent().getClientX());
            zoomAndCenterToMouse(newX);
          }

        });
  }

  /**
   * Centers the TimeLine about the specified mouse coordinate. This function,
   * like others here, must map from screen coords to domain coords.
   * 
   * We get lucky in that the OverViewGraph starts at 0 in both pixel and domain
   * coordinates. As such, our pixel delta->Domain delta conversion will convert
   * screen coords to domain values correctly.
   */
  private void zoomAndCenterToMouse(int newX) {
    int selectionPixelWidth = selectionBoundaries.getSelectionPixelWidth();
    int pixelOffset = selectionPixelWidth / 2;
    // We assume the center X coord here is not off the right side of the page
    int newRightPx = newX + pixelOffset;
    int newLeftPx;
    if (newRightPx > overViewTimeLine.getCurrentGraphWidth()) {
      // then we will slide too far off to the right.
      newRightPx = overViewTimeLine.getCurrentGraphWidth();
      double newRightBound = getDomainDeltaFromPixelDelta(newRightPx)
          + overViewTimeLineModel.getLeftBound();
      overViewTimeLine.getMainTimeLine().transitionTo(
          newRightBound - selectionBoundaries.getSelectionDomainWidth(),
          newRightBound);
    } else {
      // clamp the new lower bound
      newLeftPx = Math.max(0, newX - pixelOffset);
      double newLeftBound = getDomainDeltaFromPixelDelta(newLeftPx)
          + overViewTimeLineModel.getLeftBound();
      overViewTimeLine.getMainTimeLine().transitionTo(newLeftBound,
          newLeftBound + selectionBoundaries.getSelectionDomainWidth());
    }
  }
}
