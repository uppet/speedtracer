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

import com.google.gwt.topspin.ui.client.ClickEvent;
import com.google.gwt.topspin.ui.client.ClickListener;
import com.google.gwt.topspin.ui.client.Container;
import com.google.gwt.topspin.ui.client.MouseOverEvent;
import com.google.gwt.topspin.ui.client.MouseOverListener;
import com.google.speedtracer.client.SymbolServerController;
import com.google.speedtracer.client.SymbolServerService;
import com.google.speedtracer.client.model.ButtonDescription;
import com.google.speedtracer.client.model.EventRecordType;
import com.google.speedtracer.client.model.UiEvent;
import com.google.speedtracer.client.model.UiEventModel;
import com.google.speedtracer.client.util.JsIntegerMap;
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

  // The index of the first event in the table model.
  private int beginIndex = 0;

  // The index of the last event in the table model.
  private int endIndex = 0;

  private SluggishnessEventFilterPanel eventFilterPanel;

  // A map of all rows in the table keyed by the record sequence number
  private JsIntegerMap<EventWaterfallRow> recordMap = JsIntegerMap.createObject().cast();

  private final EventWaterfallRow.Resources resources;

  private RowListener rowListener;

  private final UiEventModel sourceModel;

  private final SluggishnessVisualization visualization;

  public EventWaterfall(Container container, EventWaterfallFilter filter,
      final SluggishnessVisualization visualization, UiEventModel sourceModel,
      EventWaterfall.Resources resources) {
    super(container, filter, resources);

    this.resources = resources;
    this.visualization = visualization;
    this.sourceModel = sourceModel;

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
        String description = EventRecordType.typeToDetailedTypeString(e) + " @"
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

  public UiEventModel getSourceModel() {
    return sourceModel;
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

  private SymbolServerController getCurrentSymbolServerController() {
    SluggishnessModel sModel = (SluggishnessModel) getVisualization().getModel();
    String resourceUrl = sModel.getDataModel().getTabDescription().getUrl();
    return SymbolServerService.getSymbolServerController(new Url(resourceUrl));
  }
}
