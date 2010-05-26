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

import com.google.gwt.coreext.client.JSOArray;
import com.google.gwt.coreext.client.JsIntegerDoubleMap;
import com.google.gwt.coreext.client.JsIntegerMap;
import com.google.gwt.coreext.client.JsIntegerDoubleMap.IterationCallBack;
import com.google.gwt.graphics.client.Color;
import com.google.gwt.topspin.ui.client.ClickEvent;
import com.google.gwt.topspin.ui.client.ClickListener;
import com.google.gwt.topspin.ui.client.Container;
import com.google.gwt.topspin.ui.client.MouseOverEvent;
import com.google.gwt.topspin.ui.client.MouseOverListener;
import com.google.speedtracer.client.SymbolServerController;
import com.google.speedtracer.client.SymbolServerService;
import com.google.speedtracer.client.model.ButtonDescription;
import com.google.speedtracer.client.model.LogEvent;
import com.google.speedtracer.client.model.UiEvent;
import com.google.speedtracer.client.model.UiEventDispatcher;
import com.google.speedtracer.client.util.TimeStampFormatter;
import com.google.speedtracer.client.util.Url;
import com.google.speedtracer.client.visualizations.model.SluggishnessModel;
import com.google.speedtracer.client.visualizations.model.SluggishnessVisualization;
import com.google.speedtracer.client.visualizations.view.SluggishnessDetailView.EventWaterfallFilter;

/**
 * The waterfall of DOM/UI Events shown on the sluggishness graph.
 */
public class EventWaterfall extends FilteringScrollTable {
  /**
   * Externalized Resources.
   */
  public interface Resources extends FilteringScrollTable.Resources,
      EventWaterfallRow.Resources, SluggishnessEventFilterPanel.Resources {
  }

  /**
   * Handles events for a row.
   */
  private interface RowListener extends MouseOverListener, ClickListener {
  }

  /**
   * TODO(knorton): This class was simply extracted from EventTraceBreakdown but
   * there are obvious refactorings and cleanups to be made in another pass. I
   * plan to simplify this class considerably.
   * 
   * NOTES:
   * <p>
   * An event/sub-event gets marked with a dominant type if it's important, but
   * small. Importance is determined by whether the node has children who are
   * part of the set of types that exceed a threshold. Or, if it's a log
   * message. :-) The dominant type is computed by descending a sub-tree and
   * computing a type-duration map for every node.
   * </p>
   */
  private static class Presenter implements LazyEventTree.Presenter {
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

    private static final int SIGNIFICANCE_IN_PIXELS = 1;
    private static final int AGGREGATE_SIGNIFICANCE_IN_PIXELS = 5;

    private static native Color getDominantColor(UiEvent event) /*-{
      return event.dominantColor;
    }-*/;

    private static native void setDominantColor(UiEvent event, Color color) /*-{
      event.dominantColor = color;
    }-*/;

    public Color getColor(UiEvent event) {
      return EventRecordColors.getColorForType(event.getType());
    }

    public Color getDominantTypeColor(UiEvent event) {
      return getDominantColor(event);
    }

    public double getInsignificanceThreshold(double msPerPixel) {
      return SIGNIFICANCE_IN_PIXELS / msPerPixel;
    }

    public String getLabel(UiEvent event) {
      return UiEvent.typeToDetailedTypeString(event);
    }

    public boolean hasDominantType(UiEvent event, UiEvent rootEvent,
        double msPerPixel) {
      // We now attempt to associate a dominant type (color) with this tiny
      // node. We do not want to have exponential search behavior. So we
      // computeDominantColorForSubtree will memoize its results.
      computeDominantColorForSubtree(event, msPerPixel, rootEvent);
      return getDominantColor(event) != null;
    }

    /**
     * Post order traversal. At each visit, we know that the typeMap for all our
     * children should be up to date for that subtree. We simply update our own
     * typemap with the information contained in the children's typeMap, and
     * then set our own dominant color if it matters.
     */
    private void computeDominantColorForSubtree(UiEvent node,
        double msPerPixel, UiEvent rootEvent) {
      if (isMarked(node)) {
        return;
      }

      JsIntegerDoubleMap typeMap = JsIntegerDoubleMap.create();
      JSOArray<UiEvent> children = node.getChildren();
      // Leaf node check
      if (children.isEmpty()) {
        markNode(node);
        final int nodeType = node.getType();
        typeMap.put(nodeType, node.getSelfTime());
        // Set the typemap for parent nodes to use in their calculations.
        node.setTypeDurations(typeMap);
        setDominantColorIfImportant(node, nodeType, msPerPixel, rootEvent);
        return;
      }

      // Recursive call
      for (int i = 0, n = children.size(); i < n; i++) {
        computeDominantColorForSubtree(children.get(i), msPerPixel, rootEvent);
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
      setDominantColorIfImportant(node, aggregator.typeOfMax, msPerPixel,
          rootEvent);
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

    private void setDominantColorIfImportant(UiEvent node, int dominantType,
        double msPerPixel, UiEvent rootEvent) {
      final double aggregateThreshold = AGGREGATE_SIGNIFICANCE_IN_PIXELS
          / msPerPixel;
      // We check to see if this insignificant thing is part of something
      // significant.
      JsIntegerDoubleMap aggregateTimes = rootEvent.getTypeDurations();
      // Visitors should already have run
      assert aggregateTimes != null : "aggregateTimes is null";

      // Find the dominant color for this node, and if it belongs to an
      // important color, then set the dominant color on the UiEvent.
      if (aggregateTimes.hasKey(dominantType)
          && (aggregateTimes.get(dominantType) >= aggregateThreshold)
          || (node.getType() == LogEvent.TYPE)) {
        setDominantColor(node, EventRecordColors.getColorForType(dominantType));
      }
    }
  }

  // The index of the first event in the table model.
  private int beginIndex = 0;

  // The index of the last event in the table model.
  private int endIndex = 0;

  private SluggishnessEventFilterPanel eventFilterPanel;

  // A map of all rows in the table keyed by the record sequence number
  private JsIntegerMap<EventWaterfallRow> recordMap = JsIntegerMap.createObject().cast();

  private final EventWaterfallRow.Resources resources;

  private RowListener rowListener;

  private final UiEventDispatcher sourceDispatcher;

  private final SluggishnessVisualization visualization;

  private final Presenter presenter = new Presenter();

  public EventWaterfall(Container container, EventWaterfallFilter filter,
      final SluggishnessVisualization visualization,
      UiEventDispatcher sourceDispatcher, EventWaterfall.Resources resources) {
    super(container, filter, resources);

    this.resources = resources;
    this.visualization = visualization;
    this.sourceDispatcher = sourceDispatcher;

    rowListener = new RowListener() {
      public void onClick(ClickEvent event) {
        TableRow row = (TableRow) event.getSource();
        row.toggleDetails();
        EventWaterfallRowDetails details = (EventWaterfallRowDetails) row.getDetails();
        if (details.isJavaScriptProfileInProgress()) {
          details.updateProfile();
        }
      }

      public void onMouseOver(MouseOverEvent event) {
        UiEvent e = (UiEvent) event.getSource();
        assert e != null;
        String description = UiEvent.typeToDetailedTypeString(e) + " @"
            + TimeStampFormatter.format(e.getTime());
        EventWaterfall.this.getVisualization().getCurrentEventMarkerModel().update(
            e.getTime(), e.getDuration(), description, 0);
      }
    };

    // Create the EventFilterPanel UI.
    eventFilterPanel = new SluggishnessEventFilterPanel(
        getFilterPanelContainer(), this, filter.eventFilter, resources,
        visualization.getModel());

    // Add a ButtonDescription entry for the control that will open/close the
    // EventFilterPanel.
    visualization.addButton(new ButtonDescription("Open/Close Filter Panel",
        resources.sluggishnessFiletPanelCss().filterPanelButton(),
        new ClickListener() {
          public void onClick(ClickEvent event) {
            eventFilterPanel.refresh(visualization.getModel());
            toggleFilterPanelVisible();
          }
        }));
  }

  /**
   * This method adds a row to the table for a given UiEvent.
   * 
   * @param e the event
   * @param append to append or not to append
   */
  public TableRow addRowForUiEvent(UiEvent e, boolean append) {
    EventWaterfallRow row = append ? appendRow(e) : prependRow(e);
    row.createDetails();

    recordMap.put(e.getSequence(), row);

    // Add mouse over listener
    row.addMouseOverListener(e, rowListener);
    row.addClickListener(row, rowListener);

    return row;
  }

  public EventWaterfallRow appendRow(UiEvent uiEvent) {
    EventWaterfallRow row = new EventWaterfallRow(this, uiEvent, resources);
    insertRow(row, true);
    return row;
  }

  public UiEventDispatcher getSourceDispatcher() {
    return sourceDispatcher;
  }

  public SluggishnessVisualization getVisualization() {
    return visualization;
  }

  public EventWaterfallRow prependRow(UiEvent uiEvent) {
    EventWaterfallRow row = new EventWaterfallRow(this, uiEvent, resources);
    insertRow(row, false);
    return row;
  }

  public void refreshRecord(UiEvent uiEvent) {
    EventWaterfallRow row = this.recordMap.get(uiEvent.getSequence());
    if (row == null) {
      return;
    }
    row.getTitleCell().refresh();
    ((EventWaterfallRowDetails) row.getDetails()).refresh();
  }

  /**
   * Wipes the table and re-adds events in the current page.
   */
  @Override
  public void renderTable() {
    // Cancel any pending resymbolization requests.
    // Add resymbolized frame if it is available.
    SymbolServerController ssController = getCurrentSymbolServerController();
    if (ssController != null) {
      ssController.cancelPendingRequests();
    }

    // Clear out the rows.
    clearTable();

    // Clear the map of record number to table rows
    recordMap = JsIntegerMap.createObject().cast();

    for (int i = beginIndex; i < endIndex; i++) {
      addRowForUiEvent(getVisualization().getModel().getEventList().get(i),
          true);
    }

    // Actually add the rows to the dom.
    super.renderTable();
  }

  public void setEndIndex(int endIndex) {
    this.endIndex = endIndex;
  }

  /**
   * Sets the total range of events viewable by the table. Resets the view to
   * the first page.
   * 
   * @param beginIndex
   * @param endIndex
   */
  public void updateTotalTableRange(int beginIndex, int endIndex) {
    this.beginIndex = beginIndex;
    this.setEndIndex(endIndex);
    renderTable();
  }

  LazyEventTree.Presenter getPresenter() {
    return presenter;
  }

  private SymbolServerController getCurrentSymbolServerController() {
    SluggishnessModel sModel = (SluggishnessModel) getVisualization().getModel();
    String resourceUrl = sModel.getDataDispatcher().getTabDescription().getUrl();
    return SymbolServerService.getSymbolServerController(new Url(resourceUrl));
  }
}
