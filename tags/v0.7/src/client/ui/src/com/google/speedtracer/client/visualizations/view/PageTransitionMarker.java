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

import com.google.gwt.graphics.client.Canvas;
import com.google.gwt.graphics.client.Color;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.CssResource.Strict;
import com.google.gwt.topspin.ui.client.ClickEvent;
import com.google.gwt.topspin.ui.client.ClickListener;
import com.google.gwt.topspin.ui.client.Container;
import com.google.gwt.topspin.ui.client.DefaultContainerImpl;
import com.google.gwt.topspin.ui.client.Div;
import com.google.speedtracer.client.Monitor;
import com.google.speedtracer.client.util.TimeStampFormatter;
import com.google.speedtracer.client.view.FastTooltip;
import com.google.speedtracer.client.view.HoveringPopup;
import com.google.speedtracer.client.view.HoveringPopup.PopupContentProvider;
import com.google.speedtracer.client.view.InlineMenu.InlineMenuItem;
import com.google.speedtracer.client.visualizations.model.StaticGraphMarkerModel;

/**
 * Marker for page transitions to show up on the graph.
 */
public class PageTransitionMarker extends StaticGraphMarker {

  /**
   * CSS.
   */
  public interface Css extends CssResource {
    String icon();
  }

  /**
   * Externalized interface.
   */
  public interface Resources extends FastTooltip.Resources {
    @Source("resources/PageTransitionMarker.css")
    @Strict
    PageTransitionMarker.Css pageTransitionMarkerCss();
  }

  /**
   * The model for the PageTransition.
   */
  public static class PageTransitionModel extends StaticGraphMarkerModel {
    final String title;
    final String url;

    public PageTransitionModel(double startTime, String title, String url) {
      super(startTime, 0);
      this.title = title;
      this.url = url;
    }
  }

  private final Resources resources;
  private boolean tooltipAdded;

  public PageTransitionMarker(PageTransitionModel markerItem,
      PageTransitionMarker.Resources resources) {
    super(markerItem, resources.pageTransitionMarkerCss().icon());
    this.resources = resources;
  }

  @Override
  protected InlineMenuItem[] createMenuItems() {
    // no menu.
    return null;
  }

  // TODO(zundel): Should StaticGraphMarker extend MenuSource? Should MenuSource
  // be an interface instead of an abstract class?
  @Override
  protected PopupContentProvider createPopupContent() {
    return null;
  };

  @Override
  protected void protectedPaint(Canvas c, double startX, double startY,
      MarkerIcon interactiveComp, double graphHeightCoords, int startXPx) {
    if (!tooltipAdded && interactiveComponent != null) {
      // Add tooltip lazily, as interactiveComponent is created lazily in
      // super.paint().
      Container container = new DefaultContainerImpl(
          interactiveComponent.getElement());
      String tooltipText = ((PageTransitionModel) markerModel).title;

      // A blank tooltip looks really odd. Try to find something to fill in
      // the text with.
      if (tooltipText == null || tooltipText.equals("")) {
        String url = ((PageTransitionModel) markerModel).url;
        tooltipText = (url == null || url.equals("")) ? "<blank page>" : url;
      }
      FastTooltip tip = new FastTooltip(container, tooltipText, resources);
      tip.getElement().getStyle().setProperty("cursor", "pointer");
      ClickEvent.addClickListener(this, tip.getElement(), new ClickListener() {

        public void onClick(ClickEvent event) {
          HoveringPopup popup = Monitor.getPopup();
          popup.setContentProvider(createTooltipPoupupContent());
          Div interactiveComponent = PageTransitionMarker.this.interactiveComponent;
          popup.show(interactiveComponent.getAbsoluteLeft(),
              interactiveComponent.getAbsoluteTop());
        }
      });

      interactiveComponent.getElement().appendChild(tip.getElement());
      tooltipAdded = true;
      interactiveComp.setVisible(true);
    }

    // TODO(zundel): alpha?
    Color strokeColor = new Color("#415086");
    c.setStrokeStyle(strokeColor);
    c.setLineWidth(1);
    c.beginPath();
    c.moveTo((int) startX, graphHeightCoords);
    c.lineTo((int) startX, 0);
    c.stroke();
    interactiveComp.moveTo(startXPx);
  }

  private PopupContentProvider createTooltipPoupupContent() {
    return new PopupContentProvider() {

      public String getPopupContent() {
        PageTransitionModel model = (PageTransitionModel) markerModel;
        return model.title + "<br>New Url: <b>" + model.url + "</b><br>" + "@"
            + TimeStampFormatter.format(model.startTime);
      }

      public String getPopupTitle() {
        return "Page Transition";
      }
    };
  }
}
