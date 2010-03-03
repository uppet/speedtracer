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
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.TableCellElement;
import com.google.gwt.dom.client.TableRowElement;
import com.google.gwt.events.client.EventListenerRemover;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.topspin.ui.client.Container;
import com.google.gwt.topspin.ui.client.ContainerImpl;
import com.google.gwt.topspin.ui.client.DefaultContainerImpl;
import com.google.gwt.topspin.ui.client.Div;
import com.google.gwt.topspin.ui.client.Table;
import com.google.speedtracer.client.model.NetworkResource;
import com.google.speedtracer.client.model.NetworkResource.HeaderMap;
import com.google.speedtracer.client.model.NetworkResource.HeaderMap.IterationCallBack;
import com.google.speedtracer.client.util.TimeStampFormatter;
import com.google.speedtracer.client.util.dom.DocumentExt;
import com.google.speedtracer.client.view.fx.CssTransitionPx;
import com.google.speedtracer.client.view.fx.CssTransitionPx.CallBack;
import com.google.speedtracer.client.visualizations.view.Tree.ExpansionChangeListener;
import com.google.speedtracer.client.visualizations.view.Tree.Item;

import java.util.List;

/**
 * The panel of details that displays the details of the network request and
 * conditionally, the hintlet records associated with this resource.
 */
public class RequestDetails extends Div {
  /**
   * CSS.
   */
  public interface Css extends CssResource {
    String details();

    int detailsTableWidth();

    int hintletTreeMargin();

    String hintletTreeWrapper();
  }

  /**
   * Externalized interface.
   */
  public interface Resources extends HintletRecordsTree.Resources {
    @Source("resources/RequestDetails.css")
    RequestDetails.Css requestDetailsCss();
  }

  private final CallBack closeCallBack;

  private Element contentElem;

  private HintletRecordsTree hintletTree;

  private DivElement hintletTreeWrapper;

  private NetworkResource info;

  private boolean isVisible = false;

  /**
   * The parent manages cleaning up this list, we just tack some things on to
   * it.
   */
  // TODO(zundel): topspin should provide better affordances for event cleanup
  private final List<EventListenerRemover> parentRemovers;

  /**
   * We need a reference to the styles for our parent Widget because we have
   * some toggles in this class that mutate the styles of the
   * {@link NetworkPillBox}.
   */
  private final NetworkPillBox.Css pbCss;

  private final Element pillBoxContainer;
  private final Resources resources;
  private int targetHeight = 0;

  /**
   * ctor.
   * 
   * @param container what we attach to
   * @param pb the container element for the pillBox
   * @param info the information about this network request/response
   * @param resources the Resources for our parent widget which magically is
   *          also the resources for us
   */
  public RequestDetails(Container container, Element pb, NetworkResource info,
      NetworkPillBox.Resources resources,
      List<EventListenerRemover> parentRemovers) {
    super(container);
    this.parentRemovers = parentRemovers;
    this.resources = resources;
    this.pbCss = resources.networkPillBoxCss();
    pillBoxContainer = pb;
    setStyleName(resources.requestDetailsCss().details());
    this.info = info;

    // CallBack invoked after collapsing RequestDetails
    closeCallBack = new CallBack() {
      public void onTransitionEnd() {
        getElement().getStyle().setProperty("display", "none");
        pillBoxContainer.setClassName(RequestDetails.this.pbCss.pillBoxWrapper());
      }
    };
  }

  public void refresh() {
    // The hintlet information might have changed since the details panel
    // was first created.
    if (hintletTreeWrapper != null && info.getHintRecords() != null) {
      if (hintletTree == null) {
        createHintletTree();
      } else {
        hintletTree.refresh(info.getHintRecords());
      }
    }
  }

  /**
   * Toggles the Visibility of the RequestDetails.
   * 
   * TODO (jaimeyap): put some logic in here to detect when the user is
   * highlighting text (ie. mouseDown + mouseMove before mouseUp) so it doesn't
   * close.
   * 
   * @return whether or not we closed or opened the details
   */
  public boolean toggleVisibility() {
    if (isVisible) {
      CssTransitionPx.get().setCallBack(closeCallBack);
      CssTransitionPx.get().transition(getElement(), "height", targetHeight, 0,
          200);
      isVisible = false;
      return false;
    } else {
      if (contentElem == null) {
        // Lazily initialize contentElem
        contentElem = Document.get().createElement("div");
        // Takes up 0 space on parent
        contentElem.getStyle().setProperty("position", "absolute");
        contentElem.getStyle().setProperty("width", "100%");
        getElement().appendChild(contentElem);

        // Fill the contentElem
        populateContent();
      }

      // Make the element renderable
      getElement().getStyle().setProperty("display", "block");

      // Sniff the height of the contentElem
      targetHeight = contentElem.getOffsetHeight();

      // Animate the expansion
      CssTransitionPx.get().transition(getElement(), "height", 0, targetHeight,
          200);

      pillBoxContainer.setClassName(pbCss.pillBoxWrapperSelected());
      isVisible = true;
      return true;
    }
  }

  /**
   * Updates the Details of the Network Request.
   * 
   * @param info NetworkResource JSO containing the information about the
   *          request/response for the resource
   */
  public void updateInfo(NetworkResource info) {
    this.info = info;
    populateContent();
  }

  /**
   * Appends a TableRowElement and populates it with two cells.
   * 
   * @param summaryTable
   * @param title
   * @param value
   */
  private void addRowPair(Table dataTable, String title, String value) {
    TableRowElement row = dataTable.appendRow();
    TableCellElement cell = row.insertCell(-1);
    cell.getStyle().setProperty("backgroundColor", "#eef");
    cell.getStyle().setPropertyPx("width", 90);
    cell.setInnerText(title);
    cell = row.insertCell(-1);
    cell.getStyle().setProperty("wordWrap", "break-word");
    cell.setInnerText(value);
  }

  private void createHintletTree() {
    hintletTree = new HintletRecordsTree(new DefaultContainerImpl(
        hintletTreeWrapper), info.getHintRecords(), resources);

    // Hook listener to tree list to monitor expansion changes.
    hintletTree.addExpansionChangeListener(new ExpansionChangeListener() {
      public void onExpansionChange(Item changedItem) {
        fixHeightOfParentRow();
      }
    });

    // We make sure to have the tree cleaned up when we clean up ourselves.
    parentRemovers.add(hintletTree.getRemover());
  }

  private void fillHeaderTable(final Table headerTable, HeaderMap headers) {
    if (headers != null) {
      headers.iterate(new IterationCallBack() {
        public void onIteration(String key, String value) {
          addRowPair(headerTable, key, value);
        }
      });
    }
  }

  /**
   * Fills in the Summary Data.
   * 
   * @param summaryTable
   * @param info
   */
  private void fillSummaryTable(Table summaryTable, NetworkResource info) {
    addRowPair(summaryTable, "URL", info.getUrl());
    addRowPair(summaryTable, "From Cache", info.isCached() + "");
    addRowPair(summaryTable, "Method", info.getHttpMethod());
    addRowPair(summaryTable, "Http Status", info.getStatusCode() + "");
    addRowPair(summaryTable, "Mime-type", info.getMimeType());

    String requestTiming, responseTiming, totalTiming;

    if (!info.didFail()) {
      requestTiming = "@"
          + TimeStampFormatter.formatMilliseconds(info.getStartTime())
          + " for "
          + TimeStampFormatter.formatMilliseconds(info.getResponseReceivedTime()
              - info.getStartTime());

      responseTiming = "@"
          + TimeStampFormatter.formatMilliseconds(info.getResponseReceivedTime())
          + " for "
          + TimeStampFormatter.formatMilliseconds(info.getEndTime()
              - info.getResponseReceivedTime());
      totalTiming = "@"
          + TimeStampFormatter.formatMilliseconds(info.getStartTime())
          + " for "
          + TimeStampFormatter.formatMilliseconds(info.getEndTime()
              - info.getStartTime());
    } else {
      // We errored out
      requestTiming = "@"
          + TimeStampFormatter.formatMilliseconds(info.getStartTime())
          + " for "
          + TimeStampFormatter.formatMilliseconds(info.getEndTime()
              - info.getStartTime());

      responseTiming = "No response";
      totalTiming = "@"
          + TimeStampFormatter.formatMilliseconds(info.getStartTime())
          + " for "
          + TimeStampFormatter.formatMilliseconds(info.getEndTime()
              - info.getStartTime()) + " with an error.";
    }

    addRowPair(summaryTable, "Total Bytes", getContentLengthString(info));
    addRowPair(summaryTable, "Request Timing", requestTiming);
    addRowPair(summaryTable, "Response Timing", responseTiming);
    addRowPair(summaryTable, "Total Timing", totalTiming);
  }

  /**
   * When the tree expands, the bottom of the details view slides under the next
   * row. This will adjust the height of the details view to accommodate growth
   * or shrinking.
   */
  private void fixHeightOfParentRow() {
    targetHeight = contentElem.getOffsetHeight();
    getElement().getStyle().setPropertyPx("height", targetHeight);
  }

  /**
   * Looks up the content length in bytes and returns the stringified version
   * for display purposes.
   * 
   * @param info The {@link NetworkResource} that contains the info for the
   *          request.
   * @return returns the content length as a String.
   */
  private String getContentLengthString(NetworkResource info) {
    int contentLength = info.getContentLength();
    return ((contentLength < 0) ? "" : contentLength + " bytes");
  }

  /**
   * Populates the contentElement with the info.
   * 
   * Should only be invoked after contentElem is initialized.
   */
  private void populateContent() {
    // We may be doing an update. So we blow away the existing content.
    contentElem.setInnerHTML("");
    ContainerImpl container = new DefaultContainerImpl(contentElem);
    Css css = resources.requestDetailsCss();
    hintletTreeWrapper = DocumentExt.get().createDivWithClassName(
        css.hintletTreeWrapper());
    contentElem.appendChild(hintletTreeWrapper);
    if (info.getHintRecords() != null) {
      createHintletTree();
    }

    Element summaryTitle = Document.get().createElement("h2");
    summaryTitle.setInnerText("Summary");
    contentElem.appendChild(summaryTitle);

    Table summaryTable = new Table(container);
    fillSummaryTable(summaryTable, info);
    summaryTable.getElement().setAttribute("cellspacing", "0");
    summaryTable.getElement().setAttribute("cellpadding", "2");

    Element requestHeadersTitle = Document.get().createElement("h2");
    requestHeadersTitle.setInnerText("Request Headers");
    contentElem.appendChild(requestHeadersTitle);

    Table requestHeaders = new Table(container);
    requestHeaders.getElement().setAttribute("cellspacing", "0");
    requestHeaders.getElement().setAttribute("cellpadding", "2");

    fillHeaderTable(requestHeaders, info.getRequestHeaders());

    Element responseHeadersTitle = Document.get().createElement("h2");
    responseHeadersTitle.setInnerText("Response Headers");
    contentElem.appendChild(responseHeadersTitle);

    Table responseHeaders = new Table(container);
    fillHeaderTable(responseHeaders, info.getResponseHeaders());
    responseHeaders.getElement().setAttribute("cellspacing", "0");
    responseHeaders.getElement().setAttribute("cellpadding", "2");
  }
}
