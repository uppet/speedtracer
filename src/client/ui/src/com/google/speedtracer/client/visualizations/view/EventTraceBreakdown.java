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

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style;
import com.google.gwt.graphics.client.Canvas;
import com.google.gwt.graphics.client.Color;
import com.google.gwt.graphics.client.ImageHandle;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.speedtracer.client.model.UiEvent;
import com.google.speedtracer.client.util.JSOArray;
import com.google.speedtracer.client.util.JsIntegerDoubleMap;

/**
 * Class used to position and style the event graph bar UI that lives next to
 * the Event Trace Tree.
 */
public class EventTraceBreakdown {

  /**
   * Css selectors.
   */
  public interface Css extends CssResource {
    int borderThickness();

    String eventGraph();

    String eventGraphGuides();

    int itemMargin();

    int listMargin();

    int masterHeight();

    String masterRender();

    int sideMargins();

    int widgetWidth();
  }

  /**
   * Overlay type that represents the {@link Element} associated the guide lines
   * for the event bar graph.
   */
  public static class EventTraceGraphGuides extends Element {
    protected EventTraceGraphGuides() {
    }
  }

  /**
   * Allows clients to control the appearance and presentation of events in the
   * breakdown graphs.
   */
  public interface Presenter {
    /**
     * Gets the color to use when rendering this event in a time breakdown.
     * 
     * @param event
     * @return
     */
    Color getColor(UiEvent event);

    /**
     * Get an event's dominant color. For insignifcant events that do not have a
     * dominant color and signifcant events, implementations should return
     * <code>null</code>. A dominant color is a color that replaces an
     * insignificant event's primary color when rendering an event breakdown.
     * 
     * NOTE: {@link Presenter#hasDominantType(UiEvent, UiEvent, double)} will
     * always be called on an event prior to this method, so it is recommended
     * that any computation for calculating the dominant color be done there.
     * 
     * @param event
     * @return
     */
    Color getDominantTypeColor(UiEvent event);

    /**
     * Gets the theshold, in milliseconds, that determines if events are
     * considered insignificant. <code>msPerPixel</code> is provided to allow
     * implementers to base signifcance on pixel resolution.
     * 
     * @param msPerPixel the number of milliseconds in each pixel used to render
     *          the event breakdown
     * @return
     */
    double getInsignificanceThreshold(double msPerPixel);

    /**
     * Indicates whether an event contains a dominant type. This method will
     * only be called on events where the duration is less than
     * {@link Presenter#getInsignificanceThreshold(double)}. This method will
     * always be called before {@link Presenter#getDominantTypeColor(UiEvent)}
     * and should generally be used to carry out the computation needed to
     * determine the dominant type.
     * 
     * @see Presenter#getDominantTypeColor(UiEvent)
     * 
     * @param event
     * @param rootEvent the top-level event that contains this event
     * @param msPerPixel the number of milliseconds in each pixel
     * @return
     */
    boolean hasDominantType(UiEvent event, UiEvent rootEvent, double msPerPixel);
  }

  /**
   * Capable of updating the canvas rendering for a {@link UiEvent} inside of an
   * event tree.
   */
  public class Renderer {
    private final Canvas canvas;
    private final UiEvent event;

    private Renderer(Canvas canvas, UiEvent event) {
      this.event = event;
      this.canvas = canvas;
    }

    public Element getElement() {
      return canvas.getElement();
    }

    /**
     * Renders only time spent in self leaving gaps where time was spent in
     * children.
     */
    public void renderOnlySelf() {
      final double domainToCoords = canvas.getCoordWidth()
          / event.getDuration();
      canvas.clear();
      canvas.setFillStyle(presenter.getColor(event));
      canvas.fillRect(0, 0, canvas.getCoordWidth(), canvas.getCoordHeight());

      // now cut out the immediate children.
      JSOArray<UiEvent> children = event.getChildren();
      for (int i = 0, n = children.size(); i < n; i++) {
        // The simple act of getting children takes a long time. That is crazy.
        UiEvent child = children.get(i);
        double startX = (child.getTime() - event.getTime()) * domainToCoords;
        canvas.clearRect(startX, 0, domainToCoords * child.getDuration(),
            canvas.getCoordWidth());
      }
    }

    /**
     * Renders time in self and overlays time spent in children.
     */
    public void renderSelfAndChildren() {
      canvas.clear();
      final Color dominantColor = presenter.getDominantTypeColor(event);

      // If this node has a dominant color set, then it is one of several
      // important ones... show it with a 1 pixel bar.
      if (dominantColor != null) {
        // Simple fill with this color.
        canvas.setFillStyle(dominantColor);
        canvas.fillRect(0, 0, canvas.getCoordWidth(), canvas.getCoordHeight());
      } else {
        double sx = (event.getTime() - rootEvent.getTime())
            * masterDomainToCoords;
        double sw = event.getDuration() * masterDomainToCoords;
        // Prevent exception due to rounding errors that slightly exceed
        // MASTER_COORD_WIDTH
        sw = Math.min(sw, MASTER_COORD_WIDTH);
        // Calling drawImage with a width of exactly 0 throws an exception in
        // JavaScript. No need to even make the draw call in this situation.
        // This is a defensive guard since we have guarantees earlier on that
        // this will be non-zero. But leave it in just in case.
        if (sw > 0) {
          canvas.drawImage(getMasterImageHandle(), sx, 0, sw, COORD_HEIGHT, 0,
              0, canvas.getCoordWidth(), COORD_HEIGHT);
        }
      }
    }
  }

  /**
   * Externalized resource interface for accessing Css.
   */
  public interface Resources extends ClientBundle {
    @Source("resources/EventTraceBreakdown.css")
    Css eventTraceBreakdownCss();
  }

  private static final int COORD_HEIGHT = 10;

  private static final int MASTER_COORD_WIDTH = 1000;

  // conversion factor for converting from domain space to pixel space.
  private final double domainToPixels;

  private final double insignificanceThreshold;

  private final double masterDomainToCoords;

  private final Resources resources;

  private final UiEvent rootEvent;

  private final Presenter presenter;

  /**
   * This element is used to display the full rendering for the rootEvent and
   * all its children. It is also used as the master copy so that
   * {@link Renderer}s can sample their region to avoid doing a full redraw.
   */
  private Element masterCanvasElement;

  /**
   * Constructor.
   * 
   * @param rootEvent the root event of the tree for which we are showing the
   *          breakdown bar graphs.
   * @param resources the {@link EventTraceBreakdown.Resources} for the
   *          breakdown.
   */
  public EventTraceBreakdown(UiEvent rootEvent, Presenter presenter,
      EventTraceBreakdown.Resources resources) {
    this.presenter = presenter;
    this.resources = resources;
    this.rootEvent = rootEvent;
    domainToPixels = resources.eventTraceBreakdownCss().widgetWidth()
        / rootEvent.getDuration();
    masterDomainToCoords = MASTER_COORD_WIDTH
        / ((rootEvent.getDuration() == 0) ? 1 : rootEvent.getDuration());
    insignificanceThreshold = presenter.getInsignificanceThreshold(domainToPixels);
  }

  public Element cloneRenderedCanvasElement() {
    ensureMasterIsRendered();
    int width = (int) (rootEvent.getDuration() * domainToPixels);
    final Canvas canvas = new Canvas(width, COORD_HEIGHT);
    canvas.setLineWidth(2);
    final Element element = canvas.getElement();
    element.setClassName(resources.eventTraceBreakdownCss().masterRender());
    element.getStyle().setPropertyPx("width", width);
    new Renderer(canvas, rootEvent).renderSelfAndChildren();
    return element;
  }

  /**
   * Creates a {@link EventTraceGraphGuides} and sets its and position.
   * 
   * @param parentElement the {@link Element} that this new guide will attach
   *          to.
   * @param event the {@link UiEvent} associate with the node that this guide
   *          represents.
   * @param nodeDepth the depth in the event trace tree for the associate node.
   * @return returns the newly created {@link EventTraceGraphGuides}.
   */
  public EventTraceGraphGuides createBarGraphGuides(Element parentElement,
      UiEvent event, int nodeDepth) {
    Css css = resources.eventTraceBreakdownCss();
    EventTraceGraphGuides barGuides = parentElement.getOwnerDocument().createDivElement().cast();
    barGuides.setClassName(css.eventGraphGuides());
    int leftOffset = getLeftOffset(event, nodeDepth);
    // the guides are attached as a child of the <ul> element. So we have
    // to also account for the border and margin for the list that contains us.
    leftOffset -= (css.listMargin() + css.borderThickness());
    int width = getPixelWidth(event.getDuration());
    // Set positioning information.
    barGuides.getStyle().setPropertyPx("left", leftOffset);
    barGuides.getStyle().setPropertyPx("width", width);
    parentElement.appendChild(barGuides);
    return barGuides;
  }

  public Renderer createRenderer(UiEvent event, int depth) {
    ensureMasterIsRendered();
    int width = (int) (event.getDuration() * domainToPixels);
    // We may have truncated something that, on aggregate may matter.
    // If this node has a dominant color set, then it contains a child that is
    // one of the important ones... show it with a 1 pixel bar.
    if (width == 0
        && presenter.hasDominantType(event, rootEvent, domainToPixels)) {
      width = 1;
    }

    Css css = resources.eventTraceBreakdownCss();

    final Canvas canvas = new Canvas(width, COORD_HEIGHT);
    canvas.setLineWidth(2);
    final Element element = canvas.getElement();
    final Style style = element.getStyle();
    element.setClassName(css.eventGraph());
    style.setPropertyPx("left", getLeftOffset(event, depth));
    style.setPropertyPx("width", width);

    return new Renderer(canvas, event);
  }

  public Element getRenderedCanvasElement() {
    ensureMasterIsRendered();
    return masterCanvasElement;
  }

  private void ensureMasterIsRendered() {
    if (masterCanvasElement != null) {
      return;
    }

    // See comment in renderNode.
    final JsIntegerDoubleMap accumlatedErrorByType = JsIntegerDoubleMap.create();
    final Canvas canvas = new Canvas(MASTER_COORD_WIDTH, COORD_HEIGHT);
    traverseAndRender(canvas, null, rootEvent, accumlatedErrorByType);
    masterCanvasElement = canvas.getElement();
  }

  /**
   * Returns the offset in pixels from the left of the EventTraceBreakdown
   * widget that we want to place an element. The offset is determined by how
   * deep a node is in the tree, and the start time of the node in the tree.
   * 
   * We use negative positioning absolute positioning. So we also have to
   * account for margins and border thickness to get the things lined up.
   */
  private int getLeftOffset(UiEvent event, int nodeDepth) {
    Css css = resources.eventTraceBreakdownCss();
    // How far should the bar be offset from the left?
    double domainOffset = event.getTime() - rootEvent.getTime();
    int offsetPixels = (css.widgetWidth() + css.sideMargins())
        - (int) (domainOffset * domainToPixels);

    // include margins and borders
    offsetPixels += (nodeDepth * (css.listMargin() + css.itemMargin() + css.borderThickness()));

    return -offsetPixels;
  }

  private ImageHandle getMasterImageHandle() {
    return masterCanvasElement.cast();
  }

  private int getPixelWidth(double duration) {
    return (int) Math.max(1, domainToPixels * duration);
  }

  private void renderNode(Canvas canvas, UiEvent parent, UiEvent node,
      JsIntegerDoubleMap accumulatedErrorByType) {
    double startX = (node.getTime() - rootEvent.getTime())
        * masterDomainToCoords;
    double width = node.getDuration() * masterDomainToCoords;
    final int nodeType = node.getType();
    // Insignificance is tricky. If we have lots of insignificant things, they
    // can add up to a significant thing.
    if (node.getDuration() < insignificanceThreshold) {
      // When the sub-pixel strokes are composited, we get a misleading color
      // blend. We use a unique aliasing scheme here where we suppress short
      // duration events but keep up with the total time we've suppressed for
      // each suppressed type. If the total suppressed time for a type ends up
      // being significant, we will synthesize a single aggregate event to
      // correct our accounting.
      double correctedTime = node.getSelfTime();
      if (accumulatedErrorByType.hasKey(nodeType)) {
        correctedTime += accumulatedErrorByType.get(nodeType);
      }

      if (correctedTime < insignificanceThreshold) {
        accumulatedErrorByType.put(nodeType, correctedTime);
        return;
      }

      // We want to draw a discrete bar.
      width = insignificanceThreshold * masterDomainToCoords;
      // Reset the type specific aggregation.
      accumulatedErrorByType.put(nodeType, 0);
    }

    canvas.setFillStyle(presenter.getColor(node));
    canvas.fillRect(startX, 0, width, COORD_HEIGHT);
  }

  /**
   * Should render back to front using a simple pre-order traversal.
   * 
   * @param node the current node in the traversal
   */
  private void traverseAndRender(Canvas canvas, UiEvent prev, UiEvent node,
      JsIntegerDoubleMap accumulatedErrorByType) {
    renderNode(canvas, prev, node, accumulatedErrorByType);
    JSOArray<UiEvent> children = node.getChildren();
    for (int i = 0, n = children.size(); i < n; i++) {
      traverseAndRender(canvas, node, children.get(i), accumulatedErrorByType);
    }
  }
}
