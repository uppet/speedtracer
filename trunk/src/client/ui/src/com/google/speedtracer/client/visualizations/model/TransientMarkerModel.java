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
package com.google.speedtracer.client.visualizations.model;

import com.google.gwt.dom.client.Element;
import com.google.gwt.events.client.EventListenerRemover;
import com.google.speedtracer.client.view.MainTimeLine;
import com.google.speedtracer.client.visualizations.view.CurrentSelectionMarker;
import com.google.speedtracer.client.visualizations.view.TransientMarker;

import java.util.ArrayList;

/**
 * Event class for synthetic overlay events that need to move around on the
 * graph, but don't need to live on the graph permanently.
 */
public abstract class TransientMarkerModel {

  /**
   * A listener that fires when an event model changes its values.
   */
  public interface TransientMarkerModelChangeListener {
    void onModelChange(TransientMarkerModel m);
  }

  protected String description = "";
  protected double duration = 0.;
  protected double startTime = 0.;
  protected double value = 0.;

  private ArrayList<TransientMarkerModelChangeListener> listeners;

  private boolean noSelection = true;

  /**
   * Base class for a visualization overlays.
   */
  public TransientMarkerModel() {
  }

  /**
   * Base class for a visualization overlays.
   * 
   * @param startTime Timestamp that represents the start of the overlay event
   *          (in milliseconds). This timestamp must be unnormalized.
   * @param duration duration of the event represented by the overlay (in
   *          milliseconds)
   */
  public TransientMarkerModel(double startTime, double duration,
      String description, double value) {
    // Normalize here
    this.startTime = startTime;
    this.duration = duration;
    this.description = description;
    this.noSelection = false;
  }

  public EventListenerRemover addModelChangeListener(
      final TransientMarkerModelChangeListener listener) {
    if (listeners == null) {
      listeners = new ArrayList<TransientMarkerModelChangeListener>();
    }
    listeners.add(listener);
    return new EventListenerRemover() {
      public void remove() {
        listeners.remove(listener);
      }
    };
  }

  public abstract TransientMarker createTransientMarkerInstance(
      Element element, MainTimeLine mainTimeLine,
      CurrentSelectionMarker.Resources resources);

  public String getDescription() {
    return description;
  }

  public double getDuration() {
    return duration;
  }

  public double getStartTime() {
    return startTime;
  }

  public double getValue() {
    return value;
  }

  public boolean isNoSelection() {
    return noSelection;
  }

  public void setDescription(String description) {
    this.description = description;
    this.noSelection = false;
    fireChangeListeners();
  }

  public void setDuration(double duration) {
    this.duration = duration;
    this.noSelection = false;
    fireChangeListeners();
  }

  public void setNoSelection() {
    this.noSelection = true;
    fireChangeListeners();
  }

  public void setStartTime(double startTime) {
    this.startTime = startTime;
    this.noSelection = false;
    fireChangeListeners();
  }

  public void setValue(double value) {
    this.value = value;
    this.noSelection = false;
    fireChangeListeners();
  }

  public void update(double startTime, double duration, String description,
      double value) {
    this.startTime = startTime;
    this.duration = duration;
    this.description = description;
    this.value = value;
    this.noSelection = false;
    fireChangeListeners();
  }

  /**
   * Notify any listeners that the data in the model has changed.
   */
  protected void fireChangeListeners() {
    if (listeners == null) {
      return;
    }
    for (int i = 0; i < listeners.size(); ++i) {
      listeners.get(i).onModelChange(this);
    }
  }
}
