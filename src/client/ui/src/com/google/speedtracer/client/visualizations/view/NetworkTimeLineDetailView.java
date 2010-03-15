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
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.resources.client.ImageResource.ImageOptions;
import com.google.gwt.resources.client.ImageResource.RepeatStyle;
import com.google.gwt.topspin.ui.client.Container;
import com.google.gwt.topspin.ui.client.DefaultContainerImpl;
import com.google.gwt.topspin.ui.client.Window;
import com.google.speedtracer.client.model.NetworkResource;
import com.google.speedtracer.client.timeline.Constants;
import com.google.speedtracer.client.util.dom.DocumentExt;
import com.google.speedtracer.client.view.DetailView;
import com.google.speedtracer.client.view.MainTimeLine;
import com.google.speedtracer.client.view.fx.CssTransitionFloat;
import com.google.speedtracer.client.view.fx.CssTransitionFloat.CallBack;
import com.google.speedtracer.client.visualizations.model.NetworkTimeLineModel;
import com.google.speedtracer.client.visualizations.model.NetworkVisualization;

import java.util.ArrayList;
import java.util.List;

/**
 * Shows each requested resource.
 */
public class NetworkTimeLineDetailView extends DetailView {

  /**
   * CSS.
   */
  public interface Css extends CssResource {
    String content();

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

  private final Container container;

  private final Element contentPanel;
  private final List<ResourceRow> displayed;
  private boolean isBlocked = false;
  private boolean isDirty = false;
  private double oldLeft = 0;
  private double oldRight = 0;

  private final NetworkTimeLineDetailView.Resources resources;

  private boolean shouldFlash = false;

  public NetworkTimeLineDetailView(Container parent, NetworkVisualization viz,
      MainTimeLine timeLine, NetworkTimeLineDetailView.Resources resources) {
    super(parent, viz);
    this.resources = resources;
    NetworkTimeLineDetailView.Css css = resources.networkTimeLineDetailViewCss();
    Element elem = getElement();
    elem.setClassName(css.resourcePanel());
    elem.getStyle().setProperty("backgroundPosition",
        Constants.GRAPH_PIXEL_OFFSET + "px 0");
    displayed = new ArrayList<ResourceRow>();
    contentPanel = DocumentExt.get().createDivWithClassName(css.content());
    container = new DefaultContainerImpl(contentPanel);

    // nice border going the height of the element
    Element filler = DocumentExt.get().createDivWithClassName(
        css.heightFiller());
    filler.getStyle().setPropertyPx("width", Constants.GRAPH_HEADER_WIDTH);

    elem.appendChild(filler);
    elem.appendChild(contentPanel);
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

  public Container getContainer() {
    return container;
  }

  public int getRowCount() {
    return displayed.size();
  }

  public boolean isBlocked() {
    return isBlocked;
  }

  public boolean isDirty() {
    return isDirty;
  }

  public void refreshResource(NetworkResource resource) {
    if (displayed == null) {
      return;
    }

    for (int i = 0, l = displayed.size(); i < l; ++i) {
      ResourceRow row = displayed.get(i);
      if (row.getResource().equals(resource)) {
        row.refresh();
        break;
      }
    }
  }

  public void responseStarted(NetworkResource resource) {
  }

  public void setIsBlocked(boolean b) {
    isBlocked = b;
  }

  public void updateView(double left, double right, boolean doSpecialAction) {
    if (doSpecialAction) {
      flash();
    }

    // When we are not flashing, we want to force a redisplay
    displayResourcesInWindow(left, right, doSpecialAction);
  }

  protected void displayResourcesInWindow(double left, double right,
      boolean noForceReDisplay) {
    NetworkTimeLineModel model = getModel();
    // We dont need to update if we
    // have not shifted bounds.
    if (noForceReDisplay && (displayed.size() > 0) && (left == oldLeft)
        && (right == oldRight)) {
      return;
    } else {
      // If we are reading the details of a pillBox, we want to stop smooshing
      // the resource panel with updates.
      if (isBlocked) {
        // We need to repaint ourselves.
        isDirty = true;
        return;
      }

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
    getContentPanel().setInnerHTML("");

    // We do a naive linear search until the kinks can be ironed
    // out of more sophisticated search.
    List<NetworkResource> networkResources = model.getSortedResources();
    int currentPixelWidth = Window.getInnerWidth();
    for (int i = 0; i < networkResources.size(); i++) {
      NetworkResource resource = networkResources.get(i);
      double startTime = resource.getStartTime();
      double endTime = resource.getEndTime();

      // A version we can use to compare to our left bound
      double comparableEndTime = endTime;

      if (Double.isNaN(endTime)) {
        // We assume the request simply has not terminated, so we set the
        // comparable version to something big.
        comparableEndTime = Double.MAX_VALUE;
      }

      if (startTime < right && comparableEndTime > left) {
        String lastPathComponent = resource.getLastPathComponent();
        lastPathComponent = (lastPathComponent == null) ? ""
            : lastPathComponent;
        int dotIndex = lastPathComponent.lastIndexOf(".");
        String fileExtension = (dotIndex < 0) ? ".html"
            : lastPathComponent.substring(dotIndex);

        final ResourceRow row = new ResourceRow(getContainer(), startTime,
            endTime, resource, fileExtension,
            (currentPixelWidth - Constants.GRAPH_PIXEL_OFFSET),
            left, right, this, resources);

        displayed.add(row);
      }
    }

    if (shouldFlash) {
      CssTransitionFloat.get().transition(getElement(), "opacity", 0, 1, 200);
      shouldFlash = false;
    }
    isDirty = false;
  }

  private Element getContentPanel() {
    return contentPanel;
  }

  private NetworkTimeLineModel getModel() {
    return ((NetworkVisualization) getVisualization()).getModel();
  }
}
