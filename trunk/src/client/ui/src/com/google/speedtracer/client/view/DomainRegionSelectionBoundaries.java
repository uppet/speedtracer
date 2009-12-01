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
import com.google.gwt.topspin.ui.client.Container;
import com.google.gwt.topspin.ui.client.Div;
import com.google.speedtracer.client.util.TimeStampFormatter;
import com.google.speedtracer.client.util.dom.DocumentExt;

/**
 * The grippies on the OverViewGraph.
 */
public class DomainRegionSelectionBoundaries {
  private double selectionDomainWidth = 0;
  private int selectionPixelWidth = 0;
  private final DomainRegionSelectionBound leftBound;
  private final DomainRegionSelectionBound rightBound;

  public DomainRegionSelectionBoundaries(Container container,
      DomainRegionSelection.Resources resources) {
    DomainRegionSelection.Css css = resources.domainRegionSelectionCss();
    leftBound = new LeftSelectionBoundImpl(container, css);
    rightBound = new RightSelectionBoundImpl(container, css);
  }

  public DomainRegionSelectionBound getLeftSelectionBound() {
    return leftBound;
  }

  public DomainRegionSelectionBound getRightSelectionBound() {
    return rightBound;
  }

  public double getSelectionDomainWidth() {
    return selectionDomainWidth;
  }

  public int getSelectionPixelWidth() {
    return selectionPixelWidth;
  }

  public void updateSelectionBounds(double leftDomain, double rightDomain,
      int leftOffset, int rightOffset, int screenWidth) {

    leftBound.setDomainValue(leftDomain, leftOffset, screenWidth);
    rightBound.setDomainValue(rightDomain, rightOffset, screenWidth);
  }

  /**
   * Abstract base class for one of the Boundaries.
   */
  public abstract class DomainRegionSelectionBound extends Div {

    protected double domainValue;
    protected int pixelOffset;
    protected final Element textOverlay;
    protected final int defaultOffset;

    // The width of the text field in pixels.
    protected final int textFieldWidth = 55;

    public DomainRegionSelectionBound(Container container, String cssClassName,
        Element textOverlay, int defaultTextOffset) {
      super(container);
      domainValue = 0;
      pixelOffset = 0;
      defaultOffset = defaultTextOffset;
      Element elem = getElement();
      elem.setClassName(cssClassName);
      this.textOverlay = textOverlay;
      elem.appendChild(textOverlay);
    }

    public double getDomainValue() {
      return domainValue;
    }

    public int getPixelOffset() {
      return pixelOffset;
    }

    public abstract void setDomainValue(double val, int pixelOff,
        int screenWidth);
  }

  /**
   * The Impl for the Left Bound.
   */
  public class LeftSelectionBoundImpl extends DomainRegionSelectionBound {

    /**
     * The default offset for the left text overlay in pixels.
     */
    private static final int defaultTextOffset = -45;

    public LeftSelectionBoundImpl(Container container,
        DomainRegionSelection.Css css) {
      super(container, css.scaleSelectionLeft(),
          DocumentExt.get().createDivWithClassName(css.scaleSelectionValue()),
          defaultTextOffset);
    }

    @Override
    public void setDomainValue(double val, int pixelOff, int screenWidth) {
      domainValue = val;
      pixelOffset = pixelOff;

      getElement().getStyle().setPropertyPx("left", pixelOffset);

      textOverlay.getStyle().setPropertyPx("left", defaultOffset);

      selectionDomainWidth = rightBound.getDomainValue() - getDomainValue();
      selectionPixelWidth = rightBound.getPixelOffset() - getPixelOffset();
      textOverlay.setInnerText("@" + TimeStampFormatter.format(domainValue));
    }
  }

  /**
   * The Impl for the Right Bound.
   */
  public class RightSelectionBoundImpl extends DomainRegionSelectionBound {

    /**
     * The default offset for the right text overlay in pixels.
     */
    private static final int defaultTextOffset = 0;

    public RightSelectionBoundImpl(Container container,
        DomainRegionSelection.Css css) {
      super(container, css.scaleSelectionRight(),
          DocumentExt.get().createDivWithClassName(css.scaleSelectionValue()),
          defaultTextOffset);
    }

    @Override
    public void setDomainValue(double val, int pixelOff, int screenWidth) {

      domainValue = val;
      pixelOffset = pixelOff;

      getElement().getStyle().setPropertyPx("left", pixelOffset);

      // Stick to borders when you try to scoll off
      if (pixelOff > screenWidth - (textFieldWidth)) {
        textOverlay.getStyle().setPropertyPx("left",
            screenWidth - (pixelOffset + textFieldWidth));
      } else {
        textOverlay.getStyle().setPropertyPx("left", defaultOffset);
      }

      selectionDomainWidth = getDomainValue() - leftBound.getDomainValue();
      selectionPixelWidth = getPixelOffset() - leftBound.getPixelOffset();
      textOverlay.setInnerHTML("+"
          + TimeStampFormatter.format(domainValue
              - getLeftSelectionBound().getDomainValue()));
    }
  }
}
