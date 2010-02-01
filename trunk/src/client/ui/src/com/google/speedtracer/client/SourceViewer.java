/*
 * Copyright 2009 Google Inc.
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
package com.google.speedtracer.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.AnchorElement;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.HeadElement;
import com.google.gwt.dom.client.IFrameElement;
import com.google.gwt.dom.client.NodeList;
import com.google.gwt.dom.client.SpanElement;
import com.google.gwt.dom.client.StyleElement;
import com.google.gwt.dom.client.TableCellElement;
import com.google.gwt.dom.client.TableElement;
import com.google.gwt.dom.client.TableRowElement;
import com.google.gwt.dom.client.Style.Display;
import com.google.gwt.events.client.Event;
import com.google.gwt.events.client.EventListener;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.resources.client.CssResource.Strict;
import com.google.gwt.topspin.ui.client.ClickEvent;
import com.google.gwt.topspin.ui.client.ClickListener;
import com.google.speedtracer.client.util.Command;

/**
 * Class that exposes API for mapping a resource URL and line number to actual
 * source. This wraps an iFrame which contains the source contents formatted as
 * an HTML table. View and Model are coupled in that the presentation of the
 * source (rendered as a styled table) also encodes the line number and line
 * contents per row.
 * 
 * WebKit's inspector allows for iFrames to take on the semantics of a
 * view-source:// URL if you attach the appropriate attribute
 * <code>viewSource="true"</code> to the iFrame element. Since we are not
 * allowed to violate same origin, we rely on Cross Domain XHR functionality
 * provided by Chromium Extensions. We use a simply proxy script embedded in a
 * local "ResourceFetcher.html" file that fetches the resource, and formats it
 * appropriately in the table structure that view-source:// Urls expect.
 */
public class SourceViewer {
  /**
   * Styles that get applied to the internals of the iFrame for highlighting
   * line numbers and other code styling.
   */
  public interface CodeCss extends CssResource {
    String columnMarker();

    String highlightedLine();
  }

  /**
   * Styles for styling the outside of this Widget.
   */
  public interface Css extends CssResource {
    String base();

    String closeLink();

    String frameWrapper();

    String header();

    String titleText();
  }

  /**
   * Externalized ClientBundle Resource interface.
   */
  public interface Resources extends ClientBundle {
    @Source("resources/column-marker.png")
    ImageResource columnMarker();

    @Source("resources/SourceViewerCode.css")
    @Strict()
    CodeCss sourceViewerCodeCss();

    @Source("resources/SourceViewer.css")
    @Strict()
    Css sourceViewerCss();
  }

  /**
   * Callback invoked when the iFrame is loaded. This is used externally to
   * obtain an initialized instance of the SourceViewer.
   */
  public interface SourceViewerLoadedCallback {
    void onSourceViewerLoaded(SourceViewer viewer);
  }

  /**
   * Callback used internally to know when the SourceFetcher.html page has
   * fetched the source resource.
   */
  private interface SourceFetcherCallback {
    void onContentReady();
  }

  private static final String LINE_CONTENT = "webkit-line-content";

  private static final String SOURCE_FETCHER_URL = GWT.getModuleBaseURL()
      + "SourceFetcher.html";

  /**
   * Creates an instance of the SourceViewer and invokes the passed in callback
   * when the iFrame is loaded with the target source.
   * 
   * We first load the proxy html page. This proxy page uses cross site XHR
   * enabled by Chrome extensions to fetch and format the target source. Once
   * the target source is loaded, we consider the viewer initialized.
   * 
   * @param parent the parent container element we will attach the SourceViewer
   *          to.
   * @param targetSource the URL of the source we wish to view.
   * @param resources the ClientBundle instance for this class.
   * @param callback the {@link SourceViewerLoadedCallback} that we pass the
   *          loaded SourceViewer to.
   */
  public static void create(Element parent, final String targetSource,
      final Resources resources, final SourceViewerLoadedCallback callback) {
    Document document = parent.getOwnerDocument();
    // Create the iframe within which we will load the source.
    final IFrameElement sourceFrame = document.createIFrameElement();
    Element frameWrapper = document.createDivElement();
    frameWrapper.setClassName(resources.sourceViewerCss().frameWrapper());
    frameWrapper.appendChild(sourceFrame);

    final Element baseElement = document.createDivElement();
    final Element headerElem = document.createDivElement();
    headerElem.setClassName(resources.sourceViewerCss().header());
    baseElement.appendChild(headerElem);

    // IFrame must be attached to fire onload.
    baseElement.appendChild(frameWrapper);
    parent.appendChild(baseElement);
    Event.addEventListener("load", sourceFrame, new EventListener() {
      public void handleEvent(Event event) {
        // The source fetcher should be loaded. Lets now point it at the source
        // we want to load.
        SourceViewer sourceViewer = new SourceViewer(baseElement, headerElem,
            sourceFrame, resources);
        sourceViewer.loadResource(targetSource, callback);
      }
    });
    sourceFrame.setSrc(SOURCE_FETCHER_URL);
  }

  /**
   * Calls into the iframe's window context to call a method defined by
   * SourceFetcher.html. This method does a Cross Domain XHR to fetch the source
   * resource we want to view.
   */
  private static native void fetchSource(IFrameElement sourceFrame,
      String targetSource, SourceFetcherCallback callback) /*-{
    var frameWindow = sourceFrame.contentWindow;
    if (frameWindow && frameWindow._doFetchUrl) {
      frameWindow._doFetchUrl(targetSource, function() {
        callback.@com.google.speedtracer.client.SourceViewer.SourceFetcherCallback::onContentReady()();
      });
    }
  }-*/;

  private static void injectStyles(IFrameElement sourceFrame, String styleText) {
    Document iframeDocument = sourceFrame.getContentDocument();
    HeadElement head = iframeDocument.getElementsByTagName("head").getItem(0).cast();
    StyleElement styleTag = iframeDocument.createStyleElement();
    styleTag.setInnerText(styleText);
    head.appendChild(styleTag);
  }

  private final SpanElement columnMarker;

  private String currentResourceUrl;

  // The base Element container for the SourceViewer.
  private final Element element;

  private TableRowElement highlightedRow;

  private final IFrameElement sourceFrame;

  private final CodeCss styles;

  // The element where we display the URL of the currently loaded source.
  private final Element titleElement;

  protected SourceViewer(Element myElement, Element headerElem,
      IFrameElement sourceFrame, Resources resources) {
    this.element = myElement;
    this.sourceFrame = sourceFrame;
    this.styles = resources.sourceViewerCodeCss();
    this.element.setClassName(resources.sourceViewerCss().base());

    // Create the title element and the close link.
    Document document = myElement.getOwnerDocument();
    this.titleElement = document.createDivElement();
    titleElement.setClassName(resources.sourceViewerCss().titleText());
    AnchorElement closeLink = document.createAnchorElement();
    closeLink.setClassName(resources.sourceViewerCss().closeLink());
    closeLink.setHref("javascript:;");
    closeLink.setInnerText("Close");
    headerElem.appendChild(titleElement);
    headerElem.appendChild(closeLink);

    this.columnMarker = document.createSpanElement();

    // TODO(jaimeyap): I guess this listener is going to leak.
    ClickEvent.addClickListener(closeLink, closeLink, new ClickListener() {
      public void onClick(ClickEvent event) {
        hide();
      }
    });

    injectStyles(sourceFrame, this.styles.getText());
  }

  public String getCurrentResourceUrl() {
    return currentResourceUrl;
  }

  public Element getElement() {
    return element;
  }

  /**
   * Gets the source line contents for a specified line number.
   * 
   * @param lineNumber the 1 based line index.
   * @return the source line contents as a String.
   */
  public String getLineContents(int lineNumber) {
    TableRowElement row = getTableRowElement(lineNumber);
    TableCellElement contents = getRowContentCell(row);
    if (contents != null) {
      return contents.getInnerText();
    }

    return null;
  }

  public void hide() {
    getElement().getStyle().setDisplay(Display.NONE);
  }

  /**
   * Highlights a specified line of source. Only one line of source will be
   * highlighted at a time. Subsequent calls to this method will remove
   * highlighting of other lines before highlighting the new line.
   * 
   * @param lineNumber the 1 based line index this method will highlight.
   */
  public void highlightLine(int lineNumber) {
    if (highlightedRow != null) {
      highlightedRow.removeClassName(styles.highlightedLine());
    }
    highlightedRow = getTableRowElement(lineNumber);
    highlightedRow.addClassName(styles.highlightedLine());
  }

  /**
   * Points the SourceViewer at a particular resource URL and calls back once it
   * has loaded. This method is asynchronous and will call you back some time
   * later.
   * 
   * @param resource the resource URL you wish to load.
   * @param callback the {@link SourceViewerLoadedCallback} that gets invoked
   *          once the resource has been fetched.
   */
  public void loadResource(final String resource,
      final SourceViewerLoadedCallback callback) {
    // Early out if this frame already points at the requested resource.
    if (resource.equals(currentResourceUrl)) {
      // This method is expected to be asynchronous.
      Command.defer(new Command() {
        @Override
        public void execute() {
          callback.onSourceViewerLoaded(SourceViewer.this);
        }
      });
      return;
    }

    fetchSource(sourceFrame, resource, new SourceFetcherCallback() {
      public void onContentReady() {
        // This target source resource should now be fetched and the table
        // constructed.
        sourceFrame.setAttribute("viewSource", "true");
        currentResourceUrl = resource;

        // Display the name of the resource URL and a link to close it.
        titleElement.setInnerText("Viewing: " + currentResourceUrl);

        callback.onSourceViewerLoaded(SourceViewer.this);
      }
    });
  }

  /**
   * Places a marker in the source for the specified line number at the
   * specified character offset.
   * 
   * @param lineNumber the line which we will use for marking the column.
   * @param columnNumber the offset from the start of the line to mark.
   */
  public void markColumn(int lineNumber, int columnNumber) {
    if (columnNumber <= 0) {
      return;
    }

    TableCellElement contentCell = getRowContentCell(getTableRowElement(lineNumber));
    columnMarker.removeFromParent();

    int zeroIndexCol = columnNumber - 1;
    String textBeforeMark = contentCell.getInnerText().substring(0,
        zeroIndexCol);
    String textAfterMark = contentCell.getInnerText().substring(zeroIndexCol);

    Document document = contentCell.getOwnerDocument();
    contentCell.setInnerText("");
    contentCell.appendChild(document.createTextNode(textBeforeMark));
    contentCell.appendChild(columnMarker);
    contentCell.appendChild(document.createTextNode(textAfterMark));
    columnMarker.setClassName(styles.columnMarker());
  }

  /**
   * We scroll to the highlighted node to the top of the source viewer frame.
   * 
   * @param parentScroll optional additional scroll in case the caller is within
   *          an element that is also scrolled.
   */
  public void scrollColumnMarkerIntoView(int parentScroll) {
    sourceFrame.getContentDocument().setScrollTop(
        columnMarker.getOffsetTop() - parentScroll);
  }

  /**
   * We scroll to the highlighted line to the top of the source viewer frame.
   * 
   * @param parentScroll optional additional scroll in case the caller is within
   *          an element that is also scrolled.
   */
  public void scrollHighlightedLineIntoView(int parentScroll) {
    sourceFrame.getContentDocument().setScrollTop(
        highlightedRow.getOffsetTop() - parentScroll);
  }

  public void show() {
    getElement().getStyle().setDisplay(Display.BLOCK);
  }

  /**
   * Getter for the <code>tr</code> element wrapping the line of code.
   * 
   * @param lineNumber the 1 based index for the row.
   * @return the {@link TableRowElement} wrapping the line of code.
   */
  TableRowElement getTableRowElement(int lineNumber) {
    NodeList<TableElement> tables = sourceFrame.getContentDocument().getElementsByTagName(
        "table").cast();
    TableElement sourceTable = tables.getItem(0);

    assert (lineNumber > 0);
    assert (sourceTable != null) : "No table loaded in source frame.";

    return sourceTable.getRows().getItem(lineNumber - 1);
  }

  /**
   * Returns the cell that contains the line contents for a row.
   */
  private TableCellElement getRowContentCell(TableRowElement row) {
    NodeList<TableCellElement> cells = row.getElementsByTagName("td").cast();

    for (int i = 0, n = cells.getLength(); i < n; i++) {
      TableCellElement cell = cells.getItem(i);
      if (cell.getClassName().indexOf(LINE_CONTENT) >= 0) {
        return cell;
      }
    }

    return null;
  }
}
