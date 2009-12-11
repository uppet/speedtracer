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
import com.google.gwt.events.client.EventListenerRemover;
import com.google.gwt.topspin.ui.client.DefaultContainerImpl;
import com.google.speedtracer.client.timeline.TimeLineModel;
import com.google.speedtracer.client.view.MainTimeLine;
import com.google.speedtracer.client.visualizations.model.TransientMarkerModel;
import com.google.speedtracer.client.visualizations.model.TransientMarkerModel.TransientMarkerModelChangeListener;

/**
 * Marks an marker on a Graph that changes position. These are to be used in
 * situations where we want to display a temporary marker, such as when a user
 * moves the mouse over an event in the DOM Event table.
 */
public class TransientMarker implements TransientMarkerModelChangeListener {

  protected MarkerIcon interactiveComponent;

  protected String markerIconType;

  protected final TransientMarkerModel markerModel;

  protected double pixelsPerDomain;

  protected int xPosition;

  // Used for computing offsets for positioning. MainTimeLine has the current
  // window size and domain bounds
  private final MainTimeLine mainTimeLine;

  private final Element parentElement;

  private final EventListenerRemover remover;

  private boolean visible = true;

  public TransientMarker(Element parentElement, MainTimeLine mainTimeLine,
      TransientMarkerModel markerModel, String markerIconType) {
    assert markerModel != null;
    this.parentElement = parentElement;
    this.mainTimeLine = mainTimeLine;
    this.markerModel = markerModel;
    setMarkerIconType(markerIconType);
    remover = markerModel.addModelChangeListener(this);
  }

  /**
   * Call this method when you are finished using the marker to remove elements
   * from the DOM and clean up any potential memory leaks or stray listeners.
   */
  public void destroy() {
    if (interactiveComponent != null) {
      parentElement.removeChild(interactiveComponent.getElement());
    }
    remover.remove();
  }

  public TransientMarkerModel getMarkerModel() {
    return markerModel;
  }

  public Element getParentElement() {
    return parentElement;
  }

  public boolean isVisible() {
    return visible;
  }

  public void onModelChange(TransientMarkerModel m) {
    if (interactiveComponent == null) {
      interactiveComponent = new MarkerIcon(new DefaultContainerImpl(
          parentElement), markerIconType);
    }

    if (m.isNoSelection()) {
      interactiveComponent.setVisible(false);
      return;
    }

    if (!visible) {
      return;
    }

    interactiveComponent.setVisible(true);

    TimeLineModel timeLineModel = mainTimeLine.getModel();
    pixelsPerDomain = mainTimeLine.getCurrentGraphWidth()
        / (timeLineModel.getRightBound() - timeLineModel.getLeftBound());
    xPosition = (int) ((m.getStartTime() - timeLineModel.getLeftBound()) * pixelsPerDomain);
    interactiveComponent.moveTo(xPosition);
  }

  /**
   * Call this immediately in the subclass constructor before the marker is
   * painted for the first time to select a different type of icon.
   * 
   * @param markerIconType The type of icon to display.
   */
  public void setMarkerIconType(String markerIconType) {
    assert interactiveComponent == null;
    this.markerIconType = markerIconType;
  }

  /**
   * Show or hide the marker.
   * 
   * @param visible if <code>true</code>, the marker is shown, if
   *          <code>false</code> the marker is hidden.
   */
  public void setVisible(boolean visible) {
    if (interactiveComponent != null) {
      interactiveComponent.setVisible(visible);
    }
    this.visible = visible;
  }

}
