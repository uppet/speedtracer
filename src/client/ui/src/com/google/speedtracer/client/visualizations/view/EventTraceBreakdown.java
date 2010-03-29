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
import com.google.speedtracer.client.model.LogEvent;
import com.google.speedtracer.client.model.LotsOfLittleEvents;
import com.google.speedtracer.client.model.TypeCountDurationTuple;
import com.google.speedtracer.client.model.UiEvent;
import com.google.speedtracer.client.util.JSOArray;
import com.google.speedtracer.client.util.JsIntegerDoubleMap;
import com.google.speedtracer.client.util.JsIntegerDoubleMap.IterationCallBack;

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
      canvas.setFillStyle(EventRecordColors.getColorForType(event.getType()));
      canvas.fillRect(0, 0, canvas.getCoordWidth(), canvas.getCoordHeight());

      // now cut out the immediate children.
      JSOArray<UiEvent> children = event.getChildren();
      for (int i = 0, n = children.size(); i < n; i++) {
        // The simple act of getting children takes a long time. That is crazy.
        UiEvent child = children.get(i);
        double startX = (child.getTime() - event.getTime()) * domainToCoords;

        // We have to special case stupid LoLEvents. It is wrong to cut out all
        // of a LoLEvent since the gap time is technically time in self. As
        // such... we lie a little in terms of ordering.
        double width = domainToCoords
            * ((child.getType() == LotsOfLittleEvents.TYPE)
                ? child.getSelfTime() : child.getDuration());
        canvas.clearRect(startX, 0, width, canvas.getCoordWidth());
      }
    }

    /**
     * Renders time in self and overlays time spent in children.
     */
    public void renderSelfAndChildren() {
      canvas.clear();
      Color dominantColor = getDominantColor(event);
      // If this node has a dominant color set, then it is one of several
      // important ones... show it with a 1 pixel bar.
      if (dominantColor != null) {
        // Simple fill with this color.
        canvas.setFillStyle(dominantColor);
        canvas.fillRect(0, 0, canvas.getCoordWidth(), canvas.getCoordHeight());
      } else {
        double sx = (event.getTime() - rootEvent.getTime())
            * masterDomainToCoords;
        double sw = (event.getDuration()) * masterDomainToCoords;
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

  /**
   * Simply utility class for aggregating information in two JsIntegerDoubleMaps
   * by iterating over one of them.
   */
  class TypeMapAggregator implements IterationCallBack {
    double maxValue = 0;
    JsIntegerDoubleMap parentTypeMap;
    int typeOfMax;

    public TypeMapAggregator(JsIntegerDoubleMap parentTypeMap) {
      this.parentTypeMap = parentTypeMap;
    }

    public void onIteration(int key, double val) {
      double value = ((parentTypeMap.hasKey(key)) ? parentTypeMap.get(key)
          + val : val);
      parentTypeMap.put(key, value);
      maybeChangeMax(key, value);
    }

    private void maybeChangeMax(int key, double val) {
      if (val >= maxValue) {
        typeOfMax = key;
        maxValue = val;
      }
    }
  }

  private static final int COORD_HEIGHT = 10;

  private static final int MASTER_COORD_WIDTH = 1000;

  private static native Color getDominantColor(UiEvent event) /*-{
    return event.dominantColor;
  }-*/;

  private static native void setDominantColor(UiEvent event, Color color) /*-{
    event.dominantColor = color;
  }-*/;

  // The amount of time that an event type on aggregate is deemed significant.
  private final double aggregateThreshold;

  // conversion factor for converting from domain space to pixel space.
  private final double domainToPixels;

  // The amount of time to occupy a pixel.
  private final double insignificanceThreshold;

  private final double masterDomainToCoords;

  private final Resources resources;

  private final UiEvent rootEvent;

  /**
   * This element is used to display the full rendering for the rootEvent and
   * all its children. It is also used as the master copy so that
   * {@link Renderer}s can sample their region to avoid doing a full redraw.
   */
  private Element masterCanvasElement;

  // Nodes that are sub pixel may eventually add up to something significant.
  // We also do no want to render a bunch of subpixel fills since the end
  // result will be anti-aliased and the colors will be averaged. We want
  // discrete blocks of colors... even at the expense of absolute correctness.
  private final JsIntegerDoubleMap littleNodes = JsIntegerDoubleMap.create();

  /**
   * Constructor.
   * 
   * @param rootEvent the root event of the tree for which we are showing the
   *          breakdown bar graphs.
   * @param resources the {@link EventTraceBreakdown.Resources} for the
   *          breakdown.
   */
  public EventTraceBreakdown(UiEvent rootEvent,
      EventTraceBreakdown.Resources resources) {
    this.resources = resources;
    this.rootEvent = rootEvent;
    this.domainToPixels = resources.eventTraceBreakdownCss().widgetWidth()
        / rootEvent.getDuration();
    this.masterDomainToCoords = MASTER_COORD_WIDTH / rootEvent.getDuration();
    this.insignificanceThreshold = rootEvent.getDuration()
        / (resources.eventTraceBreakdownCss().widgetWidth());

    // An important color is one that on aggregate should occupy at least 5
    // pixels. Meaning if you combined the contribution of that event type over
    // the entire event, it would occupy 5 pixels.
    this.aggregateThreshold = 5 * insignificanceThreshold;
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
    if (width == 0) {
      // If this node has a dominant color set, then it contains a child that is
      // one of the important ones... show it with a 1 pixel bar.
      if (getDominantColor(event) != null) {
        width = 1;
      }
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

  /**
   * Post order traversal. At each visit, we know that the typeMap for all our
   * children should be up to date for that subtree. We simply update our own
   * typemap with the information contained in the children's typeMap, and then
   * set our own dominant color if it matters.
   */
  private void computeDominantColorForSubtree(UiEvent node) {
    JsIntegerDoubleMap typeMap = JsIntegerDoubleMap.create();
    JSOArray<UiEvent> children = node.getChildren();
    // Leaf node check
    if (children.isEmpty()) {
      markNode(node);
      int nodeType = node.getType();
      // For LotsOfLittleEvents we pick the color of the most contributing
      // member.
      if (nodeType == LotsOfLittleEvents.TYPE) {
        LotsOfLittleEvents lolEventsNode = node.cast();
        nodeType = lolEventsNode.getDominantEventTypeTuple().getType();
      }
      typeMap.put(nodeType, node.getSelfTime());
      // Set the typemap for parent nodes to use in their calculations.
      node.setTypeDurations(typeMap);
      setDominantColorIfImportant(node, nodeType);
      return;
    }

    // Recursive call
    for (int i = 0, n = children.size(); i < n; i++) {
      computeDominantColorForSubtree(children.get(i));
    }

    // Visit the node.
    // A Visit includes an iteration over the children to aggregate their type
    // map information into our own. And then figuring out what the dominant
    // type is.
    markNode(node);
    TypeMapAggregator aggregator = new TypeMapAggregator(typeMap);
    for (int i = 0, n = children.size(); i < n; i++) {
      children.get(i).getTypeDurations().iterate(aggregator);
    }

    // Set the typemap for parent nodes to use in their calculations.
    node.setTypeDurations(typeMap);
    setDominantColorIfImportant(node, aggregator.typeOfMax);
  }

  private void ensureMasterIsRendered() {
    if (masterCanvasElement != null) {
      return;
    }

    final Canvas canvas = new Canvas(MASTER_COORD_WIDTH, COORD_HEIGHT);
    traverseAndRender(canvas, null, rootEvent);
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

  /**
   * Checks to see if a node has been visited during the dominant color
   * computation.
   */
  private native boolean isMarked(UiEvent node) /*-{
    return !!node.typeBreakdownDone;
  }-*/;

  /**
   * Marks a node as visited during the dominant color computation.
   */
  private native void markNode(UiEvent node) /*-{
    node.typeBreakdownDone = true;
  }-*/;

  /**
   * Renders a sub-rectangle for a LoLEvent showing the contributing proportions
   * of its components.
   */
  private void renderLoLEvent(Canvas canvas, UiEvent parent, double startX,
      double totalWidth, LotsOfLittleEvents lolEventsNode) {
    JSOArray<TypeCountDurationTuple> typeCountDurationTuples = lolEventsNode.getTypeCountDurationTuples();
    double currStartX = startX;
    for (int i = 0, n = typeCountDurationTuples.size(); i < n; i++) {
      TypeCountDurationTuple tuple = typeCountDurationTuples.get(i);
      canvas.setFillStyle(EventRecordColors.getColorForType(tuple.getType()));
      double width = tuple.getDuration() * masterDomainToCoords;
      canvas.fillRect(currStartX, 0, width, COORD_HEIGHT);
      currStartX += width;
    }

    // fill in the remaining gap space at the end.
    // Draw a border to show that ordering guarantees dont matter, and to
    // imply that the remaining time is time spent in the parent.
    canvas.setFillStyle(EventRecordColors.getColorForType(LotsOfLittleEvents.TYPE));
    canvas.setStrokeStyle(EventRecordColors.getColorForType(parent.getType()));
    double remainingWidth = totalWidth - (currStartX - startX);
    canvas.fillRect(currStartX, 0, remainingWidth, COORD_HEIGHT);
    canvas.strokeRect(currStartX, 0, remainingWidth, COORD_HEIGHT);
  }

  private void renderNode(Canvas canvas, UiEvent parent, UiEvent node) {
    double startX = (node.getTime() - rootEvent.getTime())
        * masterDomainToCoords;
    double width = node.getDuration() * masterDomainToCoords;
    int nodeType = node.getType();
    boolean isAggregate = false;
    // Insignificance is tricky. If we have lots of insignificant things, they
    // can add up to a significant thing.
    if (node.getDuration() < insignificanceThreshold) {
      // We now attempt to associate a dominant type with this tiny node.
      // We do not want to have exponential search behavior. So we mark
      // already visited nodes.
      if (!isMarked(node)) {
        // We could have precomputed this in the AggregateTimeVisitor, but it
        // would probably be a waste of memory to compute a valid subtree
        // breakdown for each node in the tree since we only ever care about
        // the root node and small-yet-significant nodes. Also... we want to
        // do this lazily.

        // We run over this small duration subtree and set the dominant color
        // on each sub node when appropriate.
        computeDominantColorForSubtree(node);
      }

      // We want to introduce aliasing on the master graph so that lots of
      // tiny things that add up to something significant dont get blended
      // together.
      double aggregateTime = node.getSelfTime();

      // Again... we have to special case LoLEvents that are tiny since their
      // color depends on the dominant type inside.
      if (nodeType == LotsOfLittleEvents.TYPE) {
        LotsOfLittleEvents lolEventsNode = node.cast();
        TypeCountDurationTuple dominantTypeTuple = lolEventsNode.getDominantEventTypeTuple();
        nodeType = dominantTypeTuple.getType();
        aggregateTime = dominantTypeTuple.getDuration();
      }

      if (littleNodes.hasKey(nodeType)) {
        aggregateTime += littleNodes.get(nodeType);
      }

      if (aggregateTime < insignificanceThreshold) {
        littleNodes.put(nodeType, aggregateTime);
        return;
      } else {
        // We want to draw a discrete bar.
        isAggregate = true;
        width = insignificanceThreshold * masterDomainToCoords;
        // Reset the type specific aggregation.
        littleNodes.put(nodeType, 0);
      }
    }

    // For LotsOfLittleEvents we have a separate rendering path.
    if (!isAggregate && nodeType == LotsOfLittleEvents.TYPE) {
      LotsOfLittleEvents lolEventsNode = node.cast();
      renderLoLEvent(canvas, parent, startX, width, lolEventsNode);
    } else {
      canvas.setFillStyle(EventRecordColors.getColorForType(nodeType));
      canvas.fillRect(startX, 0, width, COORD_HEIGHT);
    }
  }

  private void setDominantColorIfImportant(UiEvent node, int dominantType) {
    // We check to see if this insignificant thing is part of something
    // significant.
    JsIntegerDoubleMap aggregateTimes = rootEvent.getTypeDurations();
    // Visitors should already have run
    assert (aggregateTimes != null);

    // Find the dominant color for this node, and if it belongs to an
    // important color, then set the dominant color on the UiEvent.
    if (aggregateTimes.hasKey(dominantType)
        && (aggregateTimes.get(dominantType) >= aggregateThreshold)
        || (node.getType() == LogEvent.TYPE)) {
      setDominantColor(node, EventRecordColors.getColorForType(dominantType));
    }
  }

  /**
   * Should render back to front using a simple pre-order traversal.
   * 
   * @param node the current node in the traversal
   */
  private void traverseAndRender(Canvas canvas, UiEvent prev, UiEvent node) {
    renderNode(canvas, prev, node);
    JSOArray<UiEvent> children = node.getChildren();
    for (int i = 0, n = children.size(); i < n; i++) {
      traverseAndRender(canvas, node, children.get(i));
    }
  }
}
