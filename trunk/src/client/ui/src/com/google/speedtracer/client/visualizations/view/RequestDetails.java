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

    String nameCell();

    String nameValueTable();

    String rowEven();

    String sectionHeader();

    String valueCell();
  }

  /**
   * Externalized interface.
   */
  public interface Resources extends HintletRecordsTree.Resources {
    @Source("resources/RequestDetails.css")
    RequestDetails.Css requestDetailsCss();
  }

  private static class OddEvenIterator {
    private boolean even = true;

    public boolean next() {
      return even = !even;
    }
  }

  /**
   * Appends a TableRowElement and populates it with two cells.
   * 
   * @param summaryTable
   * @param title
   * @param value
   */
  private static void addRowPair(Table dataTable, Css css, boolean isEven,
      String title, String value) {
    TableRowElement row = dataTable.appendRow();
    if (isEven) {
      row.setClassName(css.rowEven());
    }

    final TableCellElement nameCell = row.insertCell(-1);
    nameCell.setClassName(css.nameCell());
    nameCell.setInnerText(title);

    final TableCellElement valueCell = row.insertCell(-1);
    valueCell.setClassName(css.valueCell());
    valueCell.setInnerText(value);
  }

  private static void addSectionHeader(Css css, Document document,
      Element parent, String text) {
    final DivElement header = document.createDivElement();
    header.setClassName(css.sectionHeader());
    header.setInnerHTML(text);
    parent.appendChild(header);
  }

  private static Table createTable(Container container, String classname) {
    final Table table = new Table(container);
    table.getElement().setClassName(classname);
    return table;
  }

  private static void fillHeaderTable(final Table headerTable, final Css css,
      HeaderMap headers) {
    if (headers != null) {
      headers.iterate(new IterationCallBack() {
        final OddEvenIterator iter = new OddEvenIterator();

        public void onIteration(String key, String value) {
          addRowPair(headerTable, css, iter.next(), key, value);
        }
      });
    }
  }

  /**
   * Produces a commonly used string in this part of the UI "@23ms for 112ms".
   * 
   * @param startTime
   * @param endTime
   * @return
   */
  private static String formatTimeSpan(double startTime, double endTime) {
    return "@" + TimeStampFormatter.formatMilliseconds(startTime) + " for "
        + TimeStampFormatter.formatMilliseconds(endTime - startTime);
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
    }

    if (contentElem == null) {
      // Lazily initialize contentElem
      contentElem = Document.get().createDivElement();
      // Fill the contentElem
      populateContent();
      getElement().appendChild(contentElem);
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

  /**
   * Fills in the Summary Data.
   * 
   * @param summaryTable
   * @param info
   */
  private void fillSummaryTable(Table summaryTable, Css css,
      NetworkResource info) {
    final OddEvenIterator iter = new OddEvenIterator();

    addRowPair(summaryTable, css, iter.next(), "URL", info.getUrl());
    addRowPair(summaryTable, css, iter.next(), "From Cache", info.isCached()
        + "");
    addRowPair(summaryTable, css, iter.next(), "Method", info.getHttpMethod());
    addRowPair(summaryTable, css, iter.next(), "Http Status",
        info.getStatusCode() + "");
    addRowPair(summaryTable, css, iter.next(), "Mime-type", info.getMimeType());

    String requestTiming, responseTiming, totalTiming;

    if (info.didFail()) {
      requestTiming = formatTimeSpan(info.getStartTime(), info.getEndTime());
      responseTiming = "No response";
      totalTiming = formatTimeSpan(info.getStartTime(), info.getEndTime())
          + " with an error";
    } else {
      requestTiming = formatTimeSpan(info.getStartTime(),
          info.getResponseReceivedTime());
      responseTiming = formatTimeSpan(info.getResponseReceivedTime(),
          info.getEndTime());
      totalTiming = formatTimeSpan(info.getStartTime(), info.getEndTime());
    }

    addRowPair(summaryTable, css, iter.next(), "Total Bytes",
        getContentLengthString(info));
    addRowPair(summaryTable, css, iter.next(), "Request Timing", requestTiming);
    addRowPair(summaryTable, css, iter.next(), "Response Timing",
        responseTiming);
    addRowPair(summaryTable, css, iter.next(), "Total Timing", totalTiming);
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
    final Document document = contentElem.getOwnerDocument();

    ContainerImpl container = new DefaultContainerImpl(contentElem);
    Css css = resources.requestDetailsCss();
    hintletTreeWrapper = DocumentExt.get().createDivWithClassName(
        css.hintletTreeWrapper());
    contentElem.appendChild(hintletTreeWrapper);
    if (info.getHintRecords() != null) {
      createHintletTree();
    }

    // Summary Table.
    addSectionHeader(css, document, contentElem, "Summary");
    fillSummaryTable(createTable(container, css.nameValueTable()), css, info);

    // Request Headers.
    addSectionHeader(css, document, contentElem, "Request Headers");
    fillHeaderTable(createTable(container, css.nameValueTable()), css,
        info.getRequestHeaders());

    // Response Headers.
    addSectionHeader(css, document, contentElem, "Response Headers");
    fillHeaderTable(createTable(container, css.nameValueTable()), css,
        info.getResponseHeaders());
  }
}
