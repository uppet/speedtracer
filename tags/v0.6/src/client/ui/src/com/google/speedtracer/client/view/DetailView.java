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
package com.google.speedtracer.client.view;

import com.google.gwt.topspin.ui.client.Container;
import com.google.gwt.topspin.ui.client.Div;
import com.google.speedtracer.client.model.Visualization;

/**
 * Abstract class for panels that get added to the Central DetailsViewPanel.
 * 
 * TODO (jaimeyap): Rename this to VisualizationDetailView.
 */
public abstract class DetailView extends Div {
  // TODO (jaimeyap): figure out the generics foo to get rid of these wildcards.
  private Visualization<?, ?> visualization;

  public DetailView(Container container, Visualization<?, ?> visualization) {
    super(container);
    this.visualization = visualization;
  }

  public void hide() {
    getElement().getStyle().setProperty("display", "none");
  }

  public void show() {
    getElement().getStyle().setProperty("display", "block");
  }

  /**
   * Updates the view to show model state within left and right bounds.
   * 
   * @param left
   * @param right
   * @param doTransitions
   */
  public abstract void updateView(double left, double right,
      boolean doTransitions);

  protected Visualization<?, ?> getVisualization() {
    return visualization;
  }
}
