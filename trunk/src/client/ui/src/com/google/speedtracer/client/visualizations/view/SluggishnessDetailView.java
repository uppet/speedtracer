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

import com.google.gwt.dom.client.Element;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.topspin.ui.client.Container;
import com.google.gwt.topspin.ui.client.DefaultContainerImpl;
import com.google.gwt.topspin.ui.client.MouseOutEvent;
import com.google.gwt.topspin.ui.client.MouseOutListener;
import com.google.speedtracer.client.model.UiEvent;
import com.google.speedtracer.client.model.UiEventDispatcher;
import com.google.speedtracer.client.view.DetailView;
import com.google.speedtracer.client.visualizations.model.SluggishnessModel;
import com.google.speedtracer.client.visualizations.model.SluggishnessVisualization;
import com.google.speedtracer.client.visualizations.view.FilteringScrollTable.Filter;
import com.google.speedtracer.client.visualizations.view.FilteringScrollTable.TableRow;

/**
 * DetailsView for sluggishness visualization.
 */
public class SluggishnessDetailView extends DetailView {
  /**
   * CSS.
   */
  public interface Css extends CssResource {
    String sluggishnessPanel();
  }

  /**
   * Filter used on our EventTable.
   */
  public static class EventWaterfallFilter implements Filter {
    public EventFilter eventFilter = new EventFilter();

    public EventWaterfallFilter(double durationThreshold) {
      eventFilter.setMinDuration(durationThreshold);
    }

    public boolean shouldFilter(TableRow row) {
      EventWaterfallRow uiRow = (EventWaterfallRow) row;
      return eventFilter.shouldFilter(uiRow.getEvent());
    }
  }

  /**
   * Externalized interface.
   */
  public interface Resources extends CurrentSelectionMarker.Resources,
      EventWaterfall.Resources {
    @Source("resources/SluggishnessDetailView.css")
    Css sluggishnessDetailViewCss();
  }

  private static final double TOP_LEVEL_FILTER_THRESHOLD = 3;

  private final EventWaterfall contentTable;

  private final SluggishnessDetailView.Css css;

  public SluggishnessDetailView(Container parent,
      SluggishnessVisualization viz, UiEventDispatcher sourceDispatcher,
      final SluggishnessDetailView.Resources resources) {
    super(parent, viz);
    css = resources.sluggishnessDetailViewCss();
    Element elem = getElement();
    elem.setClassName(css.sluggishnessPanel());

    EventWaterfallFilter filter = new EventWaterfallFilter(
        TOP_LEVEL_FILTER_THRESHOLD);
    contentTable = new EventWaterfall(new DefaultContainerImpl(elem), filter,
        (SluggishnessVisualization) getVisualization(), sourceDispatcher,
        resources);

    // Add a mouse out listener to turn off the current event marker when
    // the cursor leaves the detail view.
    MouseOutEvent.addMouseOutListener(this, this.getElement(),
        new MouseOutListener() {
          public void onMouseOut(MouseOutEvent event) {
            getVisualization().getCurrentEventMarkerModel().setNoSelection();
          }
        });

    // Start with the selection hidden.
    getVisualization().getCurrentEventMarkerModel().setNoSelection();
  }

  public void refreshRecord(UiEvent uiEvent) {
    contentTable.refreshRecord(uiEvent);
  }

  /**
   * Immediately adds and renders an event to the table. This is important for
   * the initial window where we may have events streaming in at a high rate.
   * Doing a full binary search to rebuild the table on each event would be bad.
   * We can get away with an append to the table.
   * 
   * @param event the {@link UiEvent} to be added to the table.
   */
  public void shortCircuitAddEvent(UiEvent event) {
    contentTable.addRowForUiEvent(event, true);
    contentTable.setEndIndex(getModel().getEventList().size() - 1);
    // The row will not be rendered if it is already attached.
    contentTable.renderRow(contentTable.getLastRow());
  }

  /**
   * Takes in the left and right boundaries of the window we want to display.
   * Figures out the DOM/UI Events that fall within the window.
   * 
   * The algorithm for performing this search is as follows: Do a binary search
   * to find the last element that falls within the window. Linear walk
   * backwards to the start of the window.
   */
  public void updateView(double left, double right) {
    int[] indices = getModel().getIndexesOfEventsInRange(left, right, false);

    if (indices != null) {
      if (indices.length == 0) {
        // blank the table
        contentTable.updateTotalTableRange(0, 0);
      } else {
        // Now we know that endIndex is the last event in the window,
        // and eventIndex has been walked back past the start of the window.
        // If we get a valid endIndex, then eventIndex is guaranteed to be 1 too
        // far past the window.
        contentTable.updateTotalTableRange(indices[0], indices[1]);
      }
    }
  }

  private SluggishnessModel getModel() {
    return ((SluggishnessVisualization) getVisualization()).getModel();
  }
}
