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
package com.google.speedtracer.client.visualizations.view;

import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.SpanElement;
import com.google.gwt.events.client.EventListenerRemover;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.resources.client.CssResource.Strict;
import com.google.gwt.topspin.ui.client.ClickEvent;
import com.google.gwt.topspin.ui.client.ClickListener;
import com.google.gwt.topspin.ui.client.Container;
import com.google.gwt.topspin.ui.client.DefaultContainerImpl;
import com.google.gwt.topspin.ui.client.Div;
import com.google.gwt.topspin.ui.client.MouseOutEvent;
import com.google.gwt.topspin.ui.client.MouseOutListener;
import com.google.gwt.topspin.ui.client.MouseOverEvent;
import com.google.gwt.topspin.ui.client.MouseOverListener;
import com.google.speedtracer.client.model.NetworkResource;
import com.google.speedtracer.client.timeline.Constants;
import com.google.speedtracer.client.util.TimeStampFormatter;
import com.google.speedtracer.client.util.dom.DocumentExt;
import com.google.speedtracer.client.util.dom.LazilyCreateableElement;

import java.util.ArrayList;
import java.util.List;

/**
 * The portion of a ResourceRow corresponding to the pillbox aligned on the
 * TimeLine.
 */
public class NetworkPillBox extends Div {
  /**
   * CSS.
   */
  public interface Css extends CssResource {

    String calloutLeft();

    int calloutOffset();

    String calloutRight();

    int pillBoxHeight();

    String pillBoxLeft();

    String pillBoxRight();

    String pillBoxTimeLine();

    String pillBoxWrapper();

    String pillBoxWrapperSelected();

    String timeOverlay();

    int timeOverlayPadding();
  }

  /**
   * Externalized interface.
   */
  public interface Resources extends RequestDetails.Resources,
      HintletRecordsTree.Resources {
    @Source("resources/graphLabelCalloutLeft.png")
    ImageResource leftCallout();

    @Source("resources/NetworkPillBox.css")
    @Strict
    NetworkPillBox.Css networkPillBoxCss();

    @Source("resources/graphLabelCalloutRight.png")
    ImageResource rightCallout();
  }

  /**
   * The left time overlay.
   */
  private class LeftOverlay extends TimeOverlay {

    protected LeftOverlay(Element parent, String value) {
      super(parent, value);
    }

    public void moveAbove() {
      int leftShift = Constants.GRAPH_PIXEL_OFFSET
          - getParentElem().getAbsoluteLeft();
      getIconElem().getStyle().setProperty("display", "none");
      getElement().getStyle().setPropertyPx("top",
          -resources.networkPillBoxCss().timeOverlayPadding());
      getElement().getStyle().setPropertyPx("marginLeft", leftShift);
    }

    @Override
    public void moveOut() {
      getIconElem().getStyle().setProperty("display", "");
      int leftShift = (overHang - getWidth())
          + resources.networkPillBoxCss().calloutOffset();
      getElement().getStyle().setPropertyPx("marginLeft", leftShift);
    }

    @Override
    protected Element createElement() {
      Element elem = super.createElement();
      Element iconElem = getIconElem();
      iconElem.setClassName(resources.networkPillBoxCss().calloutLeft());
      elem.appendChild(getIconElem());

      return elem;
    }
  }

  /**
   * The right time overlay.
   */
  private class RightOverlay extends TimeOverlay {
    protected RightOverlay(Element parent, String value) {
      super(parent, value);
    }

    @Override
    public void moveOut() {
      getIconElem().getStyle().setProperty("display", "");
      int leftShift = getParentWidth()
          - (overHang + resources.networkPillBoxCss().calloutOffset());
      getElement().getStyle().setPropertyPx("marginLeft", leftShift);
    }

    @Override
    protected Element createElement() {
      Element elem = super.createElement();
      Element iconElem = getIconElem();
      iconElem.setClassName(resources.networkPillBoxCss().calloutRight());
      elem.appendChild(getIconElem());
      elem.insertBefore(getIconElem(), elem.getFirstChild());

      return elem;
    }
  }

  /**
   * The time overlay that gets shown for one of the sides of the pillbox when
   * hovering over it.
   */
  private abstract class TimeOverlay extends LazilyCreateableElement {
    protected static final int overHang = 4;
    private SpanElement iconElem;
    private final Element parent;

    private final String value;

    protected TimeOverlay(Element parent, String value) {
      super(resources.networkPillBoxCss().timeOverlay());
      this.parent = parent;
      this.value = value;
    }

    public boolean fitsInParent() {
      return (getWidth() <= getParentWidth());
    }

    public SpanElement getIconElem() {
      ensureIconElem();
      return iconElem;
    }

    public Element getParentElem() {
      return parent;
    }

    public int getParentWidth() {
      return parent.getOffsetWidth();
    }

    public int getWidth() {
      return getElement().getOffsetWidth();
    }

    public void moveIn() {
      int myWidth = getWidth();
      int leftShift = (getParentWidth() - myWidth) / 2;
      getIconElem().getStyle().setProperty("display", "none");
      getElement().getStyle().setPropertyPx("marginLeft", leftShift);
    }

    public abstract void moveOut();

    @Override
    protected Element createElement() {
      Element elem = DocumentExt.get().createDivWithClassName(getClassName());
      parent.appendChild(elem);
      Element b = Document.get().createElement("b");
      b.setInnerText(value);
      elem.appendChild(b);
      return elem;
    }

    protected void ensureIconElem() {
      if (iconElem == null) {
        iconElem = DocumentExt.get().createSpanElement();
      }
    }
  }

  /**
   * Manages the left and right time overlays.
   */
  private class TimeOverlayController {
    private final LeftOverlay leftOverlay;
    private final RightOverlay rightOverlay;

    public TimeOverlayController(Element leftPb, String leftValue,
        Element rightPb, String rightValue) {
      leftOverlay = new LeftOverlay(leftPb, leftValue);
      rightOverlay = new RightOverlay(rightPb, rightValue);
    }

    public void hide() {
      leftOverlay.getElement().getStyle().setProperty("opacity", "0");
      rightOverlay.getElement().getStyle().setProperty("opacity", "0");
    }

    public void show() {
      if (!leftOverlay.fitsInParent()) {
        leftOverlay.moveOut();
      } else {
        leftOverlay.moveIn();
      }

      if (!rightOverlay.fitsInParent()) {
        rightOverlay.moveOut();
      } else {
        rightOverlay.moveIn();
      }

      // Now figure out if our left overlay even fits on the screen. For now we
      // let the right overlay hang off since you most likely will have room to
      // zoom out. And it still wouldnt solve the "appearance of scrollbars"
      // issue.
      if (hangsOffLeftSide(leftOverlay)) {
        leftOverlay.moveAbove();
      }

      leftOverlay.getElement().getStyle().setProperty("opacity", "1");
      rightOverlay.getElement().getStyle().setProperty("opacity", "1");
    }

    private boolean hangsOffLeftSide(TimeOverlay overlay) {
      return (overlay.getElement().getAbsoluteLeft() < Constants.GRAPH_PIXEL_OFFSET);
    }
  }

  private final DefaultContainerImpl container;

  private int leftOffset;

  private final ResourceRow parentRow;

  private final List<EventListenerRemover> removers;

  private final NetworkPillBox.Resources resources;

  private RequestDetails details;

  public NetworkPillBox(Container container, ResourceRow parentRow,
      NetworkPillBox.Resources resources) {
    super(container);
    this.parentRow = parentRow;
    this.resources = resources;
    Element elem = getElement();
    elem.setClassName(resources.networkPillBoxCss().pillBoxTimeLine());
    elem.getStyle().setPropertyPx("left", Constants.GRAPH_HEADER_WIDTH);
    this.container = new DefaultContainerImpl(elem);
    removers = new ArrayList<EventListenerRemover>();
  }

  public void createPillBox(NetworkResource resource, double start,
      double middle, double end, int pixelWidth, double domainLeft,
      double domainRight) {

    double domainWidth = domainRight - domainLeft;
    double screenConversion = (double) pixelWidth / domainWidth;

    Element elem = getElement();
    NetworkPillBox.Css css = resources.networkPillBoxCss();
    DivElement pillBoxContainer = DocumentExt.get().createDivWithClassName(
        css.pillBoxWrapper());

    Element pbLeft = DocumentExt.get().createDivWithClassName(css.pillBoxLeft());
    Element pbRight = DocumentExt.get().createDivWithClassName(
        css.pillBoxRight());

    leftOffset = (int) ((start - domainLeft) * screenConversion);
    pillBoxContainer.getStyle().setPropertyPx("left", leftOffset);

    double cappedMiddle = middle;
    double cappedEnd = end;

    // We cap the middle and end to be at the right side of the screen if they
    // are not yet set
    if (Double.isNaN(end)) {
      cappedEnd = domainRight;
    }

    if (Double.isNaN(middle)) {
      cappedMiddle = cappedEnd;
    }

    double leftTime = cappedMiddle - start;
    double rightTime = cappedEnd - cappedMiddle;
    int leftWidth = (int) (leftTime * screenConversion);
    int rightWidth = (int) (rightTime * screenConversion);
    leftWidth = Math.max(leftWidth, 2);
    rightWidth = Math.max(rightWidth, 2);
    pbLeft.getStyle().setPropertyPx("width", leftWidth);
    pbRight.getStyle().setPropertyPx("width", rightWidth);
    pbRight.getStyle().setPropertyPx("left", leftWidth);
    pillBoxContainer.getStyle().setPropertyPx("width", leftWidth + rightWidth);

    pillBoxContainer.appendChild(pbLeft);
    pillBoxContainer.appendChild(pbRight);
    elem.appendChild(pillBoxContainer);

    details = new RequestDetails(getContainer(), pillBoxContainer, resource,
        resources, removers);

    removers.add(ClickEvent.addClickListener(parentRow, parentRow.getElement(),
        new ClickListener() {
          public void onClick(ClickEvent event) {
            details.toggleVisibility();
          }
        }));

    // We want to stop the annoying issue of clicking inside the details view
    // collapsing the expansion.
    removers.add(ClickEvent.addClickListener(details, details.getElement(),
        new ClickListener() {
          public void onClick(ClickEvent event) {
            event.getNativeEvent().cancelBubble(true);
          }
        }));

    // Setup the overlay hover
    final TimeOverlayController timeOverlayController = new TimeOverlayController(
        pbLeft, TimeStampFormatter.formatMilliseconds(leftTime), pbRight,
        TimeStampFormatter.formatMilliseconds(rightTime));

    removers.add(MouseOverEvent.addMouseOverListener(pillBoxContainer,
        parentRow.getElement(), new MouseOverListener() {

          public void onMouseOver(MouseOverEvent event) {
            timeOverlayController.show();
          }

        }));

    removers.add(MouseOutEvent.addMouseOutListener(pillBoxContainer,
        parentRow.getElement(), new MouseOutListener() {

          public void onMouseOut(MouseOutEvent event) {
            timeOverlayController.hide();
          }

        }));
  }

  public void detachEventListeners() {
    for (int i = 0, n = removers.size(); i < n; i++) {
      removers.get(i).remove();
    }
    removers.clear();
  }

  public DefaultContainerImpl getContainer() {
    return container;
  }

  public int getLeftOffset() {
    return leftOffset;
  }

  public void refresh() {
    if (details != null) {
      details.refresh();
    }
  }

}
