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
package com.google.speedtracer.client.visualizations.view;

import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.SpanElement;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.events.client.EventListenerRemover;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.topspin.ui.client.ClickEvent;
import com.google.gwt.topspin.ui.client.ClickListener;
import com.google.gwt.topspin.ui.client.DefaultContainerImpl;
import com.google.gwt.topspin.ui.client.Div;
import com.google.gwt.topspin.ui.client.MouseOutEvent;
import com.google.gwt.topspin.ui.client.MouseOutListener;
import com.google.gwt.topspin.ui.client.MouseOverEvent;
import com.google.gwt.topspin.ui.client.MouseOverListener;
import com.google.gwt.topspin.ui.client.Window;
import com.google.speedtracer.client.model.NetworkResource;
import com.google.speedtracer.client.timeline.Constants;
import com.google.speedtracer.client.util.TimeStampFormatter;
import com.google.speedtracer.client.util.dom.DocumentExt;
import com.google.speedtracer.client.util.dom.EventCleanup;
import com.google.speedtracer.client.util.dom.LazilyCreateableElement;
import com.google.speedtracer.client.util.dom.EventCleanup.ManagesRemovers;

/**
 * The portion of a ResourceRow corresponding to the pillbox aligned on the
 * TimeLine.
 */
public class NetworkPillBox extends Div implements ManagesRemovers {
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
    NetworkPillBox.Css networkPillBoxCss();

    @Source("resources/graphLabelCalloutRight.png")
    ImageResource rightCallout();
  }

  /**
   * The left time overlay.
   */
  private class LeftOverlay extends TimeOverlay {
    protected LeftOverlay(Element parent) {
      super(parent);
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
    protected RightOverlay(Element parent) {
      super(parent);
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

    private Element timeTextElem;

    protected TimeOverlay(Element parent) {
      super(resources.networkPillBoxCss().timeOverlay());
      this.parent = parent;
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
      return elem;
    }

    protected void ensureIconElem() {
      if (iconElem == null) {
        iconElem = DocumentExt.get().createSpanElement();
      }
    }

    protected void setTime(double time) {
      ensureTimeTextElem();
      timeTextElem.setInnerText(TimeStampFormatter.formatMilliseconds(time));
    }

    private void ensureTimeTextElem() {
      if (timeTextElem == null) {
        timeTextElem = getElement().getOwnerDocument().createElement("b");
        getElement().appendChild(timeTextElem);
      }
    }
  }

  /**
   * Manages the left and right time overlays.
   */
  private class TimeOverlayController {
    private final LeftOverlay leftOverlay;

    private final RightOverlay rightOverlay;

    public TimeOverlayController(Element leftPb, Element rightPb,
        NetworkResource networkResource) {
      leftOverlay = new LeftOverlay(leftPb);
      rightOverlay = new RightOverlay(rightPb);
    }

    public void hide() {
      leftOverlay.getElement().getStyle().setProperty("opacity", "0");
      rightOverlay.getElement().getStyle().setProperty("opacity", "0");
    }

    public void show() {
      double[] resourceTimes = capResourceTimes(networkResource);
      double leftTime = resourceTimes[MIDDLE] - resourceTimes[START];
      double rightTime = resourceTimes[END] - resourceTimes[MIDDLE];
      leftOverlay.setTime(leftTime);
      rightOverlay.setTime(rightTime);

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

  private static final int END = 2;

  private static final int MIDDLE = 1;

  private static final int START = 0;

  private RequestDetails details;

  private final double domainLeft;

  private final double domainRight;

  private final EventCleanup eventCleanup = new EventCleanup();

  private final NetworkResource networkResource;

  private final Element parentRowElement;

  private Element pbLeft;

  private Element pbRight;

  private final DivElement pillBoxContainer;

  private final NetworkPillBox.Resources resources;

  public NetworkPillBox(Element parentRowElement,
      NetworkResource networkResource, double windowDomainLeft,
      double windowDomainRight, Resources resources) {
    super(new DefaultContainerImpl(parentRowElement));
    this.parentRowElement = parentRowElement;
    this.networkResource = networkResource;
    this.resources = resources;
    this.domainLeft = windowDomainLeft;
    this.domainRight = windowDomainRight;

    Element elem = getElement();
    elem.setClassName(resources.networkPillBoxCss().pillBoxTimeLine());
    elem.getStyle().setMarginLeft(Constants.GRAPH_HEADER_WIDTH, Unit.PX);

    NetworkPillBox.Css css = resources.networkPillBoxCss();
    this.pillBoxContainer = elem.getOwnerDocument().createDivElement();
    this.pillBoxContainer.setClassName(css.pillBoxWrapper());
    elem.appendChild(this.pillBoxContainer);

    createPillBox(css);
  }

  public void cleanupRemovers() {
    eventCleanup.cleanupRemovers();
  }

  public EventListenerRemover getRemover() {
    return eventCleanup.getRemover();
  }

  public void onResize(int panelWidth) {
    sizePillBox(networkResource, panelWidth);
  }

  public void refresh() {
    if (details != null) {
      details.refresh();
    }
  }

  public void trackRemover(EventListenerRemover remover) {
    eventCleanup.trackRemover(remover);
  }

  /**
   * NetworkResources may have NaN times for responses and ends. This method
   * simply returns reasonable a start, middle, and end. Resonable is defined as
   * "if it is NaN extend to the right edge of the screen".
   */
  private double[] capResourceTimes(NetworkResource networkResource) {
    double start = networkResource.getStartTime();
    double middle = networkResource.getResponseReceivedTime();
    double end = networkResource.getEndTime();

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

    double[] rtn = new double[3];
    rtn[0] = start;
    rtn[1] = cappedMiddle;
    rtn[2] = cappedEnd;
    return rtn;
  }

  private void createPillBox(NetworkPillBox.Css css) {
    pbLeft = DocumentExt.get().createDivWithClassName(css.pillBoxLeft());
    pbRight = DocumentExt.get().createDivWithClassName(css.pillBoxRight());

    pillBoxContainer.appendChild(pbLeft);
    pillBoxContainer.appendChild(pbRight);

    // Create the RequestDetails for this resource. The DOM should be lazily
    // created.
    details = new RequestDetails(getElement(), pillBoxContainer,
        networkResource, this, resources);

    // Add the ClickListener to toggle the visibility
    trackRemover(ClickEvent.addClickListener(parentRowElement,
        parentRowElement, new ClickListener() {
          public void onClick(ClickEvent event) {
            details.toggleVisibility();
          }
        }));

    // Setup the overlay hover
    final TimeOverlayController timeOverlayController = new TimeOverlayController(
        pbLeft, pbRight, networkResource);

    trackRemover(MouseOverEvent.addMouseOverListener(pillBoxContainer,
        parentRowElement, new MouseOverListener() {
          public void onMouseOver(MouseOverEvent event) {
            timeOverlayController.show();
          }
        }));

    trackRemover(MouseOutEvent.addMouseOutListener(pillBoxContainer,
        parentRowElement, new MouseOutListener() {
          public void onMouseOut(MouseOutEvent event) {
            timeOverlayController.hide();
          }
        }));

    sizePillBox(networkResource, Window.getInnerWidth()
        - Constants.GRAPH_HEADER_WIDTH);
  }

  private void sizePillBox(NetworkResource networkResource, int panelWidth) {
    double domainWidth = domainRight - domainLeft;
    double domainToPixels = (double) panelWidth / domainWidth;

    double[] resourceTimes = capResourceTimes(networkResource);
    double start = resourceTimes[START];
    double middle = resourceTimes[MIDDLE];
    double end = resourceTimes[END];

    int leftOffset = (int) ((start - domainLeft) * domainToPixels);
    pillBoxContainer.getStyle().setPropertyPx("left", leftOffset);

    double leftTime = middle - start;
    double rightTime = end - middle;
    int leftWidth = (int) (leftTime * domainToPixels);
    int rightWidth = (int) (rightTime * domainToPixels);
    leftWidth = Math.max(leftWidth, 2);
    rightWidth = Math.max(rightWidth, 2);

    pbLeft.getStyle().setPropertyPx("width", leftWidth);
    pbRight.getStyle().setPropertyPx("width", rightWidth);
    pbRight.getStyle().setPropertyPx("left", leftWidth);
    pillBoxContainer.getStyle().setPropertyPx("width", leftWidth + rightWidth);
  }
}
