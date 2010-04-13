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

import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Element;
import com.google.gwt.events.client.EventListenerRemover;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.resources.client.ImageResource.ImageOptions;
import com.google.gwt.resources.client.ImageResource.RepeatStyle;
import com.google.gwt.topspin.ui.client.Container;
import com.google.gwt.topspin.ui.client.DefaultContainerImpl;
import com.google.gwt.topspin.ui.client.Div;
import com.google.gwt.topspin.ui.client.MouseOutListener;
import com.google.gwt.topspin.ui.client.MouseOverListener;
import com.google.speedtracer.client.model.HintRecord;
import com.google.speedtracer.client.model.NetworkResource;
import com.google.speedtracer.client.timeline.Constants;
import com.google.speedtracer.client.util.IterableFastStringMap;
import com.google.speedtracer.client.util.JSOArray;
import com.google.speedtracer.client.util.dom.DocumentExt;
import com.google.speedtracer.client.util.dom.ImageResourceElementCreator;

import java.util.ArrayList;
import java.util.List;

/**
 * A row in the ResourcePanel.
 */
public class ResourceRow extends Div {

  /**
   * CSS.
   */
  public interface Css extends CssResource {
    String header();

    String headerText();

    int headerTextRightPad();

    int hintletIndicatorTopPad();

    String resourceHintIndicator();

    String rowEven();

    String rowOdd();

    String url();
  }

  /**
   * Externalized interface for Images and CssResources.
   */
  public interface Resources extends NetworkPillBox.Resources,
      HintletIndicator.Resources {
    @Source("resources/resourceCSSIcon.png")
    ImageResource cssIcon();

    @Source("resources/resourcePlainIcon.png")
    ImageResource defaultIcon();

    @Source("resources/resourceDocumentIcon.png")
    ImageResource htmlDocumentIcon();

    @Source("resources/resourceJSIcon.png")
    ImageResource javascriptIcon();

    @Source("resources/ResourceRow.css")
    ResourceRow.Css resourceRowCss();

    @Source("resources/scale_line.png")
    @ImageOptions(repeatStyle = RepeatStyle.Both)
    ImageResource scaleLine();
  }

  private HintletIndicator hintletIndicator;

  private final DefaultContainerImpl hintletIndicatorContainer;

  private int idCounter = 0;

  private final IterableFastStringMap<ImageResource> mimeTypeMap = new IterableFastStringMap<ImageResource>();

  private final NetworkPillBox pillBox;

  private final List<EventListenerRemover> removers = new ArrayList<EventListenerRemover>();

  private final NetworkResource resource;

  private final ResourceRow.Resources resources;

  private final Element textElem;

  public ResourceRow(Container container, NetworkResource resource,
      String fileType, double windowDomainLeft, double windowDomainRight,
      NetworkTimeLineDetailView networkDetailView,
      ResourceRow.Resources resources) {
    super(container);
    initMimeTypes(resources);
    ResourceRow.Css css = resources.resourceRowCss();
    Element elem = getElement();
    this.resource = resource;
    this.resources = resources;

    if (((networkDetailView.getRowCount()) & 1) == 1) {
      elem.setClassName(css.rowOdd());
    } else {
      elem.setClassName(css.rowEven());
    }

    Element headerElem = DocumentExt.get().createDivWithClassName(css.header());
    headerElem.getStyle().setPropertyPx("width", Constants.GRAPH_HEADER_WIDTH);
    ImageResource icon = getIconFromFileType(fileType);

    textElem = DocumentExt.get().createDivWithClassName(css.headerText());

    DivElement path = DocumentExt.get().createDivElement();
    // Strip query params from lastPathComponent.
    String[] lastPathComponent = resource.getLastPathComponent().split("\\?");

    path.setInnerText(lastPathComponent[0]);
    DivElement url = DocumentExt.get().createDivWithClassName(css.url());
    url.setInnerText(resource.getUrl());

    textElem.appendChild(path);
    textElem.appendChild(url);

    headerElem.appendChild(ImageResourceElementCreator.createElementFrom(icon));
    headerElem.appendChild(textElem);

    headerElem.setAttribute("title", resource.getUrl());
    elem.appendChild(headerElem);

    hintletIndicatorContainer = new DefaultContainerImpl(headerElem);

    // Adds a hintlet indicator if the record has associated hintlets.
    addHintletIndicator(resource.getHintRecords());

    pillBox = new NetworkPillBox(elem, resource, windowDomainLeft,
        windowDomainRight, resources);
  }

  @Override
  public EventListenerRemover addMouseOutListener(MouseOutListener listener) {
    EventListenerRemover remover = super.addMouseOutListener(listener);
    removers.add(remover);
    return remover;
  }

  @Override
  public EventListenerRemover addMouseOverListener(MouseOverListener listener) {
    EventListenerRemover remover = super.addMouseOverListener(listener);
    removers.add(remover);
    return remover;
  }

  public void cleanUp() {
    for (int i = 0, n = removers.size(); i < n; i++) {
      removers.get(i).remove();
    }
    removers.clear();
    pillBox.removeAllEventListeners();
  }

  public NetworkPillBox getPillBox() {
    return pillBox;
  }

  public NetworkResource getResource() {
    return resource;
  }

  public void onResize(int panelWidth) {
    pillBox.onResize(panelWidth);
  }

  public void refresh() {
    if (hintletIndicator != null) {
      hintletIndicatorContainer.remove(hintletIndicator);
    }
    addHintletIndicator(resource.getHintRecords());

    pillBox.refresh();
  }

  private void addHintletIndicator(JSOArray<HintRecord> hintRecords) {
    if (hintRecords == null) {
      return;
    }

    hintletIndicator = new HintletIndicator(hintletIndicatorContainer,
        hintRecords, resources);
    Element indicatorElem = hintletIndicator.getElement();
    indicatorElem.setId(idCounter++ + "");
    ResourceRow.Css css = resources.resourceRowCss();
    indicatorElem.addClassName(css.resourceHintIndicator());

    // Make room for the HintletIndicator
    textElem.getStyle().setPropertyPx("right",
        indicatorElem.getClientWidth() + css.headerTextRightPad());
  }

  /**
   * Takes in a string corresponding to the mime type for a network resource and
   * returns an AbstractImagePrototype corresponding to the image icon.
   * 
   * @param extension the mime type we are querying
   * @return the image icon for the mime type
   */
  private ImageResource getIconFromFileType(String extension) {
    ImageResource icon = mimeTypeMap.get(extension);
    return (icon != null) ? icon : mimeTypeMap.get("default");
  }

  private void initMimeTypes(ResourceRow.Resources images) {
    mimeTypeMap.put(".css", images.cssIcon());
    mimeTypeMap.put(".html", images.htmlDocumentIcon());
    mimeTypeMap.put(".xhtml", images.htmlDocumentIcon());
    mimeTypeMap.put(".js", images.javascriptIcon());
    mimeTypeMap.put("default", images.defaultIcon());
  }
}
