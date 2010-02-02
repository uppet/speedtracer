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

    int sideMargins();

    int widgetWidth();
  }

  /**
   * Base class for colored bars next to nodes in the Event Trace Tree.
   */
  public abstract class EventTraceGraph {
    // The Y axis does not need to have much detail. We would make it 1 if not
    // for LoLEvents needing a strokeRect().
    protected static final int COORD_HEIGHT = 10;
    private final Canvas canvas;
    private final double domainToCoords;

    private final UiEvent event;

    protected EventTraceGraph(UiEvent event, Element element, int coordWidth) {
      this.canvas = new Canvas(element);
      this.canvas.setLineWidth(2);
      this.event = event;
      this.domainToCoords = coordWidth / event.getDuration();
    }

    protected EventTraceGraph(UiEvent event, int coordWidth) {
      this.canvas = new Canvas(coordWidth, COORD_HEIGHT);
      this.canvas.setLineWidth(2);
      this.event = event;
      this.domainToCoords = coordWidth / event.getDuration();
    }

    public Element getElement() {
      return canvas.getElement();
    }

    /**
     * Renders this bar in its entirety, showing time in children as well.
     */
    public abstract void render();

    /**
     * Only shows the time spent in self.
     */
    public void renderOnlyTimeInSelf() {
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

    protected Canvas getCanvas() {
      return canvas;
    }

    protected UiEvent getEvent() {
      return event;
    }

    protected double getMyDomainToCoords() {
      return domainToCoords;
    }
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
   * {@link Canvas} that represents the colored bar for the top level event
   * dispatch node. This is rendered once only and is sampled to draw the reset
   * of the event graph bars.
   */
  public class MasterEventTraceGraph extends EventTraceGraph {
    /**
     * Simply utility class for aggregating information in two
     * JsIntegerDoubleMaps by iterating over one of them.
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

    // We have larger space in the X axis so that we can sample at sub pixel
    // precision.
    private static final int MASTER_COORD_WIDTH = 1000;

    // Nodes that are sub pixel may eventually add up to something significant.
    // We also do no want to render a bunch of subpixel fills since the end
    // result will be anti-aliased and the colors will be averaged. We want
    // discrete blocks of colors... even at the expense of absolute correctness.
    private final JsIntegerDoubleMap littleNodes = JsIntegerDoubleMap.create();

    private MasterEventTraceGraph() {
      super(rootEvent, MASTER_COORD_WIDTH);
      render();
    }

    private MasterEventTraceGraph(Element element) {
      super(rootEvent, element, MASTER_COORD_WIDTH);
    }

    /**
     * Renders the master event bar by simply replaying the pre-order traversal
     * history stored in the root {@link UiEvent}. Nodes should render back to
     * front with the right z-ordering.
     */
    @Override
    public void render() {
      getCanvas().clear();
      traverseAndRender(null, getEvent());
      // Cache rendered framebuffer on UiEvent
      getEvent().setRenderedMasterEventTraceGraph(getElement());
    }

    /**
     * Post order traversal. At each visit, we know that the typeMap for all our
     * children should be up to date for that subtree. We simply update our own
     * typemap with the information contained in the children's typeMap, and
     * then set our own dominant color if it matters.
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
     * Renders a sub-rectangle for a LoLEvent showing the contributing
     * proportions of its components.
     */
    private void renderLoLEvent(UiEvent parent, double startX,
        double totalWidth, LotsOfLittleEvents lolEventsNode) {
      JSOArray<TypeCountDurationTuple> typeCountDurationTuples = lolEventsNode.getTypeCountDurationTuples();
      double currStartX = startX;
      for (int i = 0, n = typeCountDurationTuples.size(); i < n; i++) {
        TypeCountDurationTuple tuple = typeCountDurationTuples.get(i);
        getCanvas().setFillStyle(
            EventRecordColors.getColorForType(tuple.getType()));
        double width = tuple.getDuration() * getMyDomainToCoords();
        getCanvas().fillRect(currStartX, 0, width, COORD_HEIGHT);
        currStartX += width;
      }

      // fill in the remaining gap space at the end.
      // Draw a border to show that ordering guarantees dont matter, and to
      // imply that the remaining time is time spent in the parent.
      getCanvas().setFillStyle(
          EventRecordColors.getColorForType(LotsOfLittleEvents.TYPE));
      getCanvas().setStrokeStyle(
          EventRecordColors.getColorForType(parent.getType()));
      double remainingWidth = totalWidth - (currStartX - startX);
      getCanvas().fillRect(currStartX, 0, remainingWidth, COORD_HEIGHT);
      getCanvas().strokeRect(currStartX, 0, remainingWidth, COORD_HEIGHT);
    }

    private void renderNode(UiEvent parent, UiEvent node) {
      double startX = (node.getTime() - getEvent().getTime())
          * getMyDomainToCoords();
      double width = node.getDuration() * getMyDomainToCoords();
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
          width = insignificanceThreshold * getMyDomainToCoords();
          // Reset the type specific aggregation.
          littleNodes.put(nodeType, 0);
        }
      }

      // For LotsOfLittleEvents we have a separate rendering path.
      if (!isAggregate && nodeType == LotsOfLittleEvents.TYPE) {
        LotsOfLittleEvents lolEventsNode = node.cast();
        renderLoLEvent(parent, startX, width, lolEventsNode);
      } else {
        getCanvas().setFillStyle(EventRecordColors.getColorForType(nodeType));
        getCanvas().fillRect(startX, 0, width, COORD_HEIGHT);
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
    private void traverseAndRender(UiEvent prev, UiEvent node) {
      renderNode(prev, node);
      JSOArray<UiEvent> children = node.getChildren();
      for (int i = 0, n = children.size(); i < n; i++) {
        traverseAndRender(node, children.get(i));
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
   * {@link Canvas} that represents the colored bar for nodes in the event trace
   * tree that are children of the top level node. This event bar simply samples
   * from the master bar.
   */
  public class SubEventTraceGraph extends EventTraceGraph {
    private final MasterEventTraceGraph masterBar;

    protected SubEventTraceGraph(MasterEventTraceGraph masterBar,
        UiEvent event, int pixelWidth) {
      super(event, pixelWidth);
      this.masterBar = masterBar;
    }

    /**
     * Samples from the {@link MasterEventTraceGraph} to draw this sub bar.
     */
    @Override
    public void render() {
      getCanvas().clear();
      Color dominantColor = getDominantColor(getEvent());
      // If this node has a dominant color set, then it is one of several
      // important ones... show it with a 1 pixel bar.
      if (dominantColor != null) {
        // Simple fill with this color.
        getCanvas().setFillStyle(dominantColor);
        getCanvas().fillRect(0, 0, getCanvas().getCoordWidth(),
            getCanvas().getCoordHeight());
      } else {
        double sx = (getEvent().getTime() - rootEvent.getTime())
            * masterDomainToCoords;
        double sw = (getEvent().getDuration()) * masterDomainToCoords;
        // Calling drawImage with a width of exactly 0 throws an exception in
        // JavaScript. No need to even make the draw call in this situation.
        // This is a defensive guard since we have guarantees earlier on that
        // this will be non-zero. But leave it in just in case.
        if (sw > 0) {
          getCanvas().drawImage(masterBar.getElement().<ImageHandle> cast(),
              sx, 0, sw, COORD_HEIGHT, 0, 0, getCanvas().getCoordWidth(),
              COORD_HEIGHT);
        }
      }
    }
  }

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
    this.masterDomainToCoords = MasterEventTraceGraph.MASTER_COORD_WIDTH
        / rootEvent.getDuration();
    this.insignificanceThreshold = rootEvent.getDuration()
        / (resources.eventTraceBreakdownCss().widgetWidth());

    // An important color is one that on aggregate should occupy at least 5
    // pixels. Meaning if you combined the contribution of that event type over
    // the entire event, it would occupy 5 pixels.
    this.aggregateThreshold = 5 * insignificanceThreshold;
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
    int width = getWidth(event.getDuration());
    // Set positioning information.
    barGuides.getStyle().setPropertyPx("left", leftOffset);
    barGuides.getStyle().setPropertyPx("width", width);
    parentElement.appendChild(barGuides);
    return barGuides;
  }

  /**
   * Creates and returns a new {@link MasterEventTraceGraph}. If the specified
   * {@link Element} is not null, it will use that for the backing frame buffer
   * WITHOUT rendering a new one. If it is null, it will create and render a new
   * one.
   * 
   * @param frameBuffer {@link Element} that corresponds to a rendered canvas
   *          tag.
   * @return the newly created {@link MasterEventTraceGraph}
   */
  public MasterEventTraceGraph createMasterEventTraceGraph(Element frameBuffer) {
    MasterEventTraceGraph masterGraph;
    if (frameBuffer == null) {
      masterGraph = new MasterEventTraceGraph();
    } else {
      masterGraph = new MasterEventTraceGraph(frameBuffer);
    }

    int width = (int) (rootEvent.getDuration() * domainToPixels);
    Css css = resources.eventTraceBreakdownCss();
    masterGraph.getElement().getStyle().setPropertyPx("marginLeft",
        css.listMargin());
    masterGraph.getElement().getStyle().setPropertyPx("marginBottom",
        css.listMargin());

    // Set positioning information.
    masterGraph.getElement().getStyle().setPropertyPx("height",
        css.masterHeight());
    masterGraph.getElement().getStyle().setPropertyPx("width", width);

    return masterGraph;
  }

  /**
   * Creates a {@link SubEventTraceGraph}, renders it and sets its position.
   * 
   * @param parentElement the {@link Element} that this new bar will attach to.
   * @param master the {@link MasterEventTraceGraph} that this bar will sample
   *          its colors from.
   * @param event
   * @param nodeDepth
   * @return
   */
  public SubEventTraceGraph createSubEventTraceGraph(Element parentElement,
      MasterEventTraceGraph master, UiEvent event, int nodeDepth) {
    int leftOffset = getLeftOffset(event, nodeDepth);
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
    SubEventTraceGraph subBar = new SubEventTraceGraph(master, event, width);
    subBar.getElement().setClassName(css.eventGraph());

    // Set positioning information.
    subBar.getElement().getStyle().setPropertyPx("left", leftOffset);
    subBar.getElement().getStyle().setPropertyPx("width", width);
    parentElement.appendChild(subBar.getElement());
    return subBar;
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

  private int getWidth(double duration) {
    double width = Math.max(1, domainToPixels * duration);
    return (int) width;
  }
}
