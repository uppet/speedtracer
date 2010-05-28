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
package com.google.speedtracer.client.model;

import com.google.gwt.events.client.EventListenerRemover;

import java.util.ArrayList;

/**
 * Event class for synthetic overlay events that need to move around on the
 * graph, but don't need to live on the graph permanently.
 */
public class GraphCalloutModel {

  /**
   * A listener that fires when an event model changes its values.
   */
  public interface ChangeListener {
    void onModelChange(GraphCalloutModel m);
  }

  protected String description;

  protected double duration;

  protected double startTime;

  protected double value;

  private boolean isSelected;

  private ArrayList<ChangeListener> listeners;

  /**
   * Default constructor that sets some empty initial values.
   */
  public GraphCalloutModel() {
    this(0, 0, "", 0);
  }

  /**
   * Model for callouts on the timeline graph.
   * 
   * @param startTime Time that represents the start of the overlay event (in
   *          milliseconds). This timestamp must be unnormalized.
   * @param duration Time in milliseconds that this overlay will highlight.
   * @param description Textual description of what this overlay represents.
   * @param value An arbitrary value to associate with this overlay.
   */
  public GraphCalloutModel(double startTime, double duration,
      String description, double value) {
    // Normalize here
    this.startTime = startTime;
    this.duration = duration;
    this.description = description;
    this.isSelected = false;
  }

  public EventListenerRemover addModelChangeListener(
      final ChangeListener listener) {
    if (listeners == null) {
      listeners = new ArrayList<ChangeListener>();
    }
    listeners.add(listener);
    return new EventListenerRemover() {
      public void remove() {
        listeners.remove(listener);
      }
    };
  }

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

  public boolean isSelected() {
    return isSelected;
  }

  public void setSelected(boolean isSelected) {
    this.isSelected = isSelected;
    fireChangeListeners();
  }

  public void update(double startTime, double duration, String description,
      double value, boolean isSelected) {
    this.startTime = startTime;
    this.duration = duration;
    this.description = description;
    this.value = value;
    this.isSelected = isSelected;
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
