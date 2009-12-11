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
import com.google.gwt.graphics.client.Canvas;
import com.google.gwt.topspin.ui.client.DefaultContainerImpl;
import com.google.speedtracer.client.view.MenuSource;
import com.google.speedtracer.client.view.HoveringPopup.PopupContentProvider;
import com.google.speedtracer.client.view.InlineMenu.InlineMenuItem;
import com.google.speedtracer.client.visualizations.model.StaticGraphMarkerModel;

/**
 * Marks an unchanging static marker on the Overview Graph. These are to be used
 * in situations where we want to call out something on the graph that is
 * unchanging. "Set it and forget it". For things like hintlets.
 */
public abstract class StaticGraphMarker extends MenuSource {

  protected String markerIconType;

  protected MarkerIcon interactiveComponent;

  protected final StaticGraphMarkerModel markerModel;

  // Lazily initialized
  protected InlineMenuItem[] menuItems;

  protected PopupContentProvider popupContent;

  protected StaticGraphMarker(StaticGraphMarkerModel markerItem,
      String markerIconType) {
    this.markerModel = markerItem;
    this.markerIconType = markerIconType;
  }

  public StaticGraphMarkerModel getMarkerModel() {
    return markerModel;
  }

  /**
   * This is the method invoked by external components.
   * 
   * @param c the canvas object to paint to
   * @param startX the x coordinate to start painting to
   * @param startY the y coordinate to paint to (the bottom of the graph).
   * @param graphHeightCoords coordinates representing the entire height of the
   *          graph
   * @param startXPx pixel coordinates of the X position on the graph.
   */
  public void paint(Element iconContainer, Canvas c, double startX,
      double startY, double graphHeightCoords, int startXPx) {
    if (interactiveComponent == null) {
      interactiveComponent = new MarkerIcon(new DefaultContainerImpl(
          iconContainer), this, markerIconType);
    }
    protectedPaint(c, startX, startY, interactiveComponent, graphHeightCoords,
        startXPx);
  }

  /**
   * Call this immediately in the subclass constructor before the marker is
   * painted for the first time to select a different type of hint icon.
   * 
   * @param hintIconType The type of icon to display.
   */
  public void setHintIconType(String hintIconType) {
    assert interactiveComponent == null;
    this.markerIconType = hintIconType;
  }

  public void setIconVisible(boolean visible) {
    if (interactiveComponent != null) {
      interactiveComponent.setVisible(visible);
    }
  }

  protected abstract InlineMenuItem[] createMenuItems();

  protected abstract PopupContentProvider createPopupContent();

  /**
   * This is overridden per instance of StaticGraphMarker.
   * 
   * @param c the canvas object to paint to
   * @param startX the x coordinate to start painting to
   * @param startY the y coordinate to paint to (the bottom of the graph).
   * @param interactiveComp the hint icon to paint.
   * @param graphHeightCoords coordinates representing the entire height of the
   *          graph
   * @param startXPx pixel coordinates of the X position on the graph.
   */
  protected abstract void protectedPaint(Canvas c, double startX,
      double startY, MarkerIcon interactiveComp, double graphHeightCoords,
      int startXPx);
}
