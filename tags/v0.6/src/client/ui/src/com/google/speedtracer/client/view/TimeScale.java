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
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.resources.client.CssResource.Strict;
import com.google.gwt.resources.client.ImageResource.ImageOptions;
import com.google.gwt.resources.client.ImageResource.RepeatStyle;
import com.google.gwt.topspin.ui.client.Container;
import com.google.gwt.topspin.ui.client.Div;
import com.google.gwt.topspin.ui.client.MouseDownEvent;
import com.google.gwt.topspin.ui.client.MouseDownListener;
import com.google.speedtracer.client.util.TimeStampFormatter;
import com.google.speedtracer.client.util.dom.DocumentExt;

/**
 * Gradiated scale at top of TimeLine.
 */
public class TimeScale extends Div {

  /**
   * CSS.
   */
  public interface Css extends CssResource {
    String scale();

    String scaleValue();

    String timeScaleBase();
  }

  /**
   * Externalized interface.
   */
  public interface Resources extends ClientBundle {
    @Source("resources/scaleBg.png")
    @ImageOptions(repeatStyle = RepeatStyle.Horizontal)
    ImageResource scaleBg();

    @Source("resources/scale.png")
    @ImageOptions(repeatStyle = RepeatStyle.Horizontal)
    ImageResource scaleLines();

    @Source("resources/TimeScale.css")
    @Strict
    Css timeScaleCss();
  }

  // The padding between the text and its marking
  private static final int SCALE_PADDING = 2;
  // The space between scale markings in pixels.
  private static final int SCALE_SELECTION_WIDTH = 100;

  protected Element[] scaleLabels;

  private final TimeScale.Css css;
  private int oldScaleWidth = 0;
  private final DivElement scale;

  public TimeScale(Container container, TimeScale.Resources resources) {
    super(container);
    this.css = resources.timeScaleCss();
    Element elem = getElement();
    elem.setClassName(css.timeScaleBase());
    scale = DocumentExt.get().createDivWithClassName(css.scale());
    elem.appendChild(scale);
    
    // Suppress native selections by accidentally dragging on the time scale.
    MouseDownEvent.addMouseDownListener(elem, elem, new MouseDownListener() {
      public void onMouseDown(MouseDownEvent event) {
        event.preventDefault();
      }
    });
  }

  /**
   * Updates the labels on the scale based on inputed left and right bounds.
   * 
   * @param leftBound left bound of the scale
   * @param rightBound right bound of the scale
   */
  public void updateScaleLabels(int xAxisPixels, double leftBound,
      double rightBound) {
    // Lazily create the scale labels each time we have a width change
    if (oldScaleWidth != xAxisPixels) {
      scaleLabels = createScaleLabels(xAxisPixels);
      oldScaleWidth = xAxisPixels;
    }

    double domainRange = rightBound - leftBound;
    double domainOffset = (domainRange / (double) xAxisPixels);

    // Change scale values
    for (int i = 0; i < scaleLabels.length; i++) {
      // Truncate to 2 decimals
      double value = (leftBound + (domainOffset * (double) i * SCALE_SELECTION_WIDTH));
      scaleLabels[i].setInnerText(TimeStampFormatter.format(value));
    }
  }

  protected Element[] createScaleLabels(int scaleWidth) {
    Element[] labels = new Element[(scaleWidth / SCALE_SELECTION_WIDTH) + 1];
    scale.setInnerHTML("");
    for (int i = 0; i < labels.length; i++) {
      Element label = labels[i] = Document.get().createElement("div");
      label.setClassName(css.scaleValue());
      label.getStyle().setPropertyPx("left",
          SCALE_PADDING + (i * SCALE_SELECTION_WIDTH));
      scale.appendChild(label);
    }

    return labels;
  }

}