/*
 * Copyright 2010 Google Inc.
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

import com.google.gwt.coreext.client.JSOArray;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.graphics.client.Color;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.topspin.ui.client.Container;
import com.google.gwt.topspin.ui.client.Div;
import com.google.gwt.topspin.ui.client.MouseOutEvent;
import com.google.gwt.topspin.ui.client.MouseOutListener;
import com.google.gwt.topspin.ui.client.MouseOverEvent;
import com.google.gwt.topspin.ui.client.MouseOverListener;
import com.google.speedtracer.client.model.GraphCalloutModel;
import com.google.speedtracer.client.timeline.Constants;
import com.google.speedtracer.client.util.TimeStampFormatter;
import com.google.speedtracer.client.util.dom.EventListenerOwner;

/**
 * Class responsible for marking vertical lines on the timeline. These are not
 * to be confused with the per-Visualization highlight lines that are used to
 * show the presence of hints.
 */
public class TimelineMarks extends Div {

  /**
   * Styles for this widget.
   */
  public interface Css extends CssResource {
    String base();

    String mark();
  }

  /**
   * Client resources.
   */
  public interface Resources extends ClientBundle {
    @Source("resources/TimelineMarks.css")
    Css timelineMarksCss();
  }

  private class Mark {
    final Color color;
    final String shortDescription;
    final String longDescription;
    final double offset;

    Mark(double offset, Color color, String shortDescription,
        String longDescription) {
      this.offset = offset;
      this.color = color;
      this.shortDescription = shortDescription;
      this.longDescription = longDescription;
    }

    void render(double left, double right, Element parent) {
      Element markElem = parent.getOwnerDocument().createDivElement();
      markElem.setClassName(resources.timelineMarksCss().mark());
      markElem.setTitle("@" + TimeStampFormatter.format(offset) + " - "
          + longDescription);
      double total = right - left;
      double fraction = (offset - left) / total;
      markElem.getStyle().setLeft(fraction * 100, Unit.PCT);
      markElem.getStyle().setProperty("borderColor", color.toString());
      parent.appendChild(markElem);

      listenerOwner.manageEventListener(MouseOverEvent.addMouseOverListener(
          this, markElem, new MouseOverListener() {
            public void onMouseOver(MouseOverEvent event) {
              calloutModel.update(offset, 0, shortDescription, 0, true);
            }
          }));
    }
  }

  private final GraphCalloutModel calloutModel;

  private final EventListenerOwner listenerOwner = new EventListenerOwner();

  private JSOArray<Mark> marks = JSOArray.create();

  private final Resources resources;

  public TimelineMarks(Container container, GraphCalloutModel calloutModel,
      Resources resources) {
    super(container);
    this.resources = resources;
    this.calloutModel = calloutModel;
    setStyleName(resources.timelineMarksCss().base());
    getElement().getStyle().setLeft(Constants.GRAPH_PIXEL_OFFSET, Unit.PX);
    sinkEvents();
  }

  public void addMark(double offset, Color color, String shortDescription,
      String longDescription) {
    marks.push(new Mark(offset, color, shortDescription, longDescription));
  }

  public void clear() {
    getElement().setInnerHTML("");
    marks = JSOArray.create();
  }

  public void drawMarksInBounds(double left, double right) {
    listenerOwner.remove();
    getElement().setInnerHTML("");
    // Simple linear search should work for now.
    for (int i = 0, n = marks.size(); i < n; i++) {
      Mark m = marks.get(i);
      if (m.offset > left && m.offset < right) {
        m.render(left, right, getElement());
      }
    }
  }

  private void sinkEvents() {
    // Any mouse out should cause us to hide any displayed callouts.
    // This listener is sunk to an element that exists for the lifecycle of the
    // page, so we dont need to unhook it.
    MouseOutEvent.addMouseOutListener(this, getElement(),
        new MouseOutListener() {
          public void onMouseOut(MouseOutEvent event) {
            calloutModel.setSelected(false);
          }
        });
  }
}
