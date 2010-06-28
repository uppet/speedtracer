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

import com.google.gwt.dom.client.Element;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.resources.client.ImageResource.ImageOptions;
import com.google.gwt.resources.client.ImageResource.RepeatStyle;
import com.google.gwt.topspin.ui.client.Container;
import com.google.gwt.topspin.ui.client.DefaultContainerImpl;
import com.google.gwt.topspin.ui.client.ResizeEvent;
import com.google.gwt.topspin.ui.client.ResizeListener;
import com.google.gwt.topspin.ui.client.Window;
import com.google.speedtracer.client.ServerEventController;
import com.google.speedtracer.client.model.NetworkResource;
import com.google.speedtracer.client.timeline.Constants;
import com.google.speedtracer.client.util.dom.DocumentExt;
import com.google.speedtracer.client.view.DetailView;
import com.google.speedtracer.client.view.fx.CssTransitionFloat;
import com.google.speedtracer.client.view.fx.CssTransitionFloat.CallBack;
import com.google.speedtracer.client.visualizations.model.NetworkVisualization;
import com.google.speedtracer.client.visualizations.model.NetworkVisualizationModel;

import java.util.ArrayList;
import java.util.List;

/**
 * Shows each requested resource.
 */
public class NetworkTimeLineDetailView extends DetailView implements
    ResizeListener, ServerEventController.AuthenticationDelegate {

  /**
   * CSS.
   */
  public interface Css extends CssResource {
    String content();

    String contentWrapper();

    String heightFiller();

    String resourcePanel();
  }

  /**
   * Externalized interface.
   */
  public interface Resources extends ResourceRow.Resources {
    @Source("resources/NetworkTimeLineDetailView.css")
    NetworkTimeLineDetailView.Css networkTimeLineDetailViewCss();

    @Source("resources/scale_line.png")
    @ImageOptions(repeatStyle = RepeatStyle.Both)
    ImageResource scaleLine();
  }

  private final DefaultContainerImpl contentContainer;

  private final List<ResourceRow> displayed;

  private double oldLeft = 0;

  private double oldRight = 0;

  private final NetworkTimeLineDetailView.Resources resources;

  private final ServerEventController serverEventController = new ServerEventController(
      this);

  private boolean shouldFlash = false;

  public NetworkTimeLineDetailView(Container parent, NetworkVisualization viz,
      NetworkTimeLineDetailView.Resources resources) {
    super(parent, viz);
    this.resources = resources;
    NetworkTimeLineDetailView.Css css = resources.networkTimeLineDetailViewCss();
    Element elem = getElement();
    elem.setClassName(css.resourcePanel());
    elem.getStyle().setProperty("backgroundPosition",
        Constants.GRAPH_PIXEL_OFFSET + "px 0");
    displayed = new ArrayList<ResourceRow>();
    DocumentExt document = elem.getOwnerDocument().cast();
    Element contentWrapper = document.createDivWithClassName(css.contentWrapper());
    Element contentElement = document.createDivWithClassName(css.content());
    contentWrapper.appendChild(contentElement);
    contentContainer = new DefaultContainerImpl(contentElement);

    // nice border going the height of the element
    Element filler = document.createDivWithClassName(css.heightFiller());
    filler.getStyle().setPropertyPx("width", Constants.GRAPH_PIXEL_OFFSET);

    elem.appendChild(filler);
    elem.appendChild(contentWrapper);

    ResizeEvent.addResizeListener(this, Window.get(), this);
  }

  public void flash() {
    final Element elem = getElement();
    elem.getStyle().setProperty("opacity", "0");
    CssTransitionFloat.get().setCallBack(new CallBack() {
      public void onTransitionEnd() {
        elem.getStyle().setProperty("opacity", "1.0");
      }
    });
    shouldFlash = true;
  }

  public Container getContentContainer() {
    return contentContainer;
  }

  public int getRowCount() {
    return displayed.size();
  }

  public void onAuthenticationRequired(String url) {
    // TODO(knorton): Add UI to help a user log into the server providing
    // traces.
  }

  public void onResize(ResizeEvent event) {
    for (int i = 0, n = displayed.size(); i < n; i++) {
      displayed.get(i).onResize(
          Window.getInnerWidth() - Constants.GRAPH_HEADER_WIDTH);
    }
  }

  public void refreshResource(NetworkResource resource) {
    // Short circuit if it's not in the window
    if (!isResourceInWindow(resource, oldLeft, oldRight)) {
      return;
    }
    
    // Always display if we haven't displayed anything yet.
    if (displayed == null) {
      displayResource(oldLeft, oldRight, resource);
      return;
    }

    boolean found = false;
    for (int i = 0, l = displayed.size(); i < l; ++i) {
      ResourceRow row = displayed.get(i);
      if (row.getResource().equals(resource)) {
        row.refresh();
        found = true;
        break;
      }
    }
    
    // It's in the window but not yet displayed, so create it
    if (!found) {
      displayResource(oldLeft, oldRight, resource);
    }
  }

  public void updateView(double left, double right) {
    flash();
    // When we are not flashing, we want to force a redisplay
    displayResourcesInWindow(left, right);
  }

  /**
   * Display a resource in the window.
   * 
   * @param left the left boundary
   * @param right the right boundary
   * @param resource the resource to display
   */
  protected void displayResource(double left, double right,
      NetworkResource resource) {
    String lastPathComponent = resource.getLastPathComponent();
    lastPathComponent = (lastPathComponent == null) ? ""
        : lastPathComponent;
    int dotIndex = lastPathComponent.lastIndexOf(".");
    String fileExtension = (dotIndex < 0) ? ".html"
        : lastPathComponent.substring(dotIndex);

    final ResourceRow row = new ResourceRow(getContentContainer(),
        resource, fileExtension, left, right, this, resources,
        serverEventController);

    displayed.add(row);
  }

  /**
   * Display all resources that fall in the given window.
   * @param left the left boundary
   * @param right the right boundary
   */
  protected void displayResourcesInWindow(double left, double right) {
    NetworkVisualizationModel model = getModel();
    // We dont need to update if we
    // have not shifted bounds.
    if ((displayed.size() > 0) && (left == oldLeft) && (right == oldRight)) {
      return;
    } else {
      oldLeft = left;
      oldRight = right;
    }

    // clean up Event Hookups
    for (int i = 0; i < displayed.size(); i++) {
      ResourceRow row = displayed.get(i);
      row.cleanUp();
    }

    // blank the resource Panel
    displayed.clear();
    getContentElement().setInnerHTML("");

    // We do a naive linear search until the kinks can be ironed
    // out of more sophisticated search.
    List<NetworkResource> networkResources = model.getSortedResources();
    for (int i = 0; i < networkResources.size(); i++) {
      NetworkResource resource = networkResources.get(i);
      if (isResourceInWindow(resource, left, right)) {
        displayResource(left, right, resource);
      }
      
      // Bail once we've hit the right edge
      if (resource.getStartTime() >= right) {
        break;
      }
    }

    if (shouldFlash) {
      CssTransitionFloat.get().transition(getElement(), "opacity", 0, 1, 200);
      shouldFlash = false;
    }
  }

  /**
   * Check if a resource request/response overlaps with the display window.
   * If no end time for the resource exists, it is treated as Double.MAX_VALUE
   * 
   * @param resource the resource to check
   * @param left the left boundary of the window
   * @param right the right boundary of the window
   * @return true if some portion of the request falls in the window
   */
  protected boolean isResourceInWindow(NetworkResource resource, double left,
      double right) {
    double startTime = resource.getStartTime();
    double endTime = resource.getEndTime();

    // A version we can use to compare to our left bound
    double comparableEndTime = endTime;

    if (Double.isNaN(endTime)) {
      // We assume the request simply has not terminated, so we set the
      // comparable version to something big.
      comparableEndTime = Double.MAX_VALUE;
    }

    return (startTime < right && comparableEndTime > left);
  }

  private Element getContentElement() {
    return contentContainer.getElement();
  }

  private NetworkVisualizationModel getModel() {
    return ((NetworkVisualization) getVisualization()).getModel();
  }
}
