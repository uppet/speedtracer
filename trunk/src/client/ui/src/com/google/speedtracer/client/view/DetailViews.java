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

import com.google.gwt.dom.client.Element;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.topspin.ui.client.Container;
import com.google.gwt.topspin.ui.client.DefaultContainerImpl;
import com.google.gwt.topspin.ui.client.Div;
import com.google.speedtracer.client.model.Visualization;
import com.google.speedtracer.client.util.IterableFastStringMap;

/**
 * Central panel in MainTimeLine. Contains views for each type of visualization.
 * 
 * TODO (jaimeyap): Rename this to VisualizationViewsPanel.
 */
public class DetailViews extends Div {

  /**
   * CSS.
   */
  public interface Css extends CssResource {
    String detailViews();
    
    int offsetTop();
  }
  
  /**
   * Externalized interface.
   */
  public interface Resources extends ClientBundle {
    @Source("resources/DetailViews.css")
    DetailViews.Css detailViewsCss();
  }
  
  private final DefaultContainerImpl container;
  private DetailView currentView = null;
  private final IterableFastStringMap<DetailView> views = new IterableFastStringMap<DetailView>();

  public DetailViews(Container container, DetailViews.Resources resources) {
    super(container);
    Element elem = getElement();
    elem.setClassName(resources.detailViewsCss().detailViews());

    this.container = new DefaultContainerImpl(elem);

    getElement();
    elem.getStyle().setPropertyPx("top", resources.detailViewsCss().offsetTop());
  }

  public void addViewForVisualization(Visualization<?, ?> visualization) {
    views.put(visualization.getTitle(), visualization.getDetailsView());
  }

  public Container getContainer() {
    return container;
  }

  public void removeViewForVisualization(Visualization<?, ?> visualization) {
    views.remove(visualization.getTitle());
  }

  public void setCurrentView(Visualization<?, ?> visualization) {
    if (currentView != null) {
      currentView.hide();
    }
    currentView = views.get(visualization.getTitle());
    currentView.show();
  }

  public void updateCurrentView(double left, double right) {
    if (currentView != null) {
      currentView.updateView(left, right);
    }
  }
}
