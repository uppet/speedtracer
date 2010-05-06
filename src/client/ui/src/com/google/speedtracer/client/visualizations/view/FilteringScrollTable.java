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

import com.google.gwt.dom.client.AnchorElement;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.SpanElement;
import com.google.gwt.dom.client.Text;
import com.google.gwt.events.client.EventListenerRemover;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.resources.client.ImageResource.ImageOptions;
import com.google.gwt.resources.client.ImageResource.RepeatStyle;
import com.google.gwt.topspin.ui.client.ClickEvent;
import com.google.gwt.topspin.ui.client.ClickListener;
import com.google.gwt.topspin.ui.client.Container;
import com.google.gwt.topspin.ui.client.CssTransitionEvent;
import com.google.gwt.topspin.ui.client.CssTransitionListener;
import com.google.gwt.topspin.ui.client.DefaultContainerImpl;
import com.google.gwt.topspin.ui.client.Div;
import com.google.gwt.topspin.ui.client.ResizeEvent;
import com.google.gwt.topspin.ui.client.ResizeListener;
import com.google.speedtracer.client.MonitorResources.CommonCss;
import com.google.speedtracer.client.MonitorResources.CommonResources;
import com.google.speedtracer.client.util.Command;
import com.google.speedtracer.client.util.TimeStampFormatter;
import com.google.speedtracer.client.util.dom.DocumentExt;
import com.google.speedtracer.client.util.dom.EventListenerOwner;
import com.google.speedtracer.client.util.dom.LazilyCreateableElement;
import com.google.speedtracer.client.util.dom.ManagesEventListeners;
import com.google.speedtracer.client.util.dom.WindowExt;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Scrollable Table with pluggable filter to collapse and coalesce
 * "uninteresing" rows. Also has an expandable placeholder for each row to
 * "expand" a row when you click on it.
 */
public abstract class FilteringScrollTable extends Div implements
    ManagesEventListeners, ResizeListener {
  /**
   * Cell in the table.
   */
  public class Cell extends LazilyCreateableElement {
    public final String contents;
    public String toolTipText;
    public final int width;

    public Cell(String contents, int width) {
      this(contents, width, css.cell());
    }

    protected Cell(String contents, int width, String cssClassName) {
      super(FilteringScrollTable.this, cssClassName);
      this.contents = contents;
      this.width = width;
    }

    /**
     * Attaches a {@link Cell} to a Dom {@link Element} with absolute
     * positioning.
     * 
     * @param elem the {@link Element} to add to
     * @param leftOffset the x offset into the parent element
     * @return returns the new leftOffset that should be used when adding to the
     *         same element that this Cell was added to.
     */
    public int addToElement(Element elem, int leftOffset) {
      Element cellElem = getElement();
      if (width > 0) {
        // We simulate the border-box box model.
        cellElem.getStyle().setPropertyPx("width", width - css.rowCellPadding());
      } else {
        cellElem.getStyle().setPropertyPx("right", 0);
      }
      cellElem.getStyle().setPropertyPx("left", leftOffset);
      elem.appendChild(cellElem);
      return leftOffset + width;
    }

    @Override
    protected Element createElement() {
      Element elem = Document.get().createElement("div");
      if (toolTipText != null) {
        elem.setAttribute("title", toolTipText);
      }
      elem.setInnerText(contents);
      return elem;
    }

    /**
     * Set or reset the tooltip text value.
     * 
     * @param tooltipText text to use for the tooltip when mouse hovers.
     */
    protected void setTooltipText(String tooltipText) {
      this.toolTipText = tooltipText;
      if (isCreated()) {
        getElement().setAttribute("title", toolTipText);
      }
    }
  }

  /**
   * A row in the table that groups a bunch of uninteresting {@link TableRow}
   * instances together.
   */
  public class CoalescedRow extends Row {

    private static final int pageSize = 10;
    private double aggregateTime = 0;
    private Element filterBar;
    private boolean isAttached = false;
    private Text labelText;
    private Element rowsAbove;
    private Element rowsBelow;
    private AnchorElement showAbove;
    private AnchorElement showBelow;
    private final LinkedList<TableRow> tableRows = new LinkedList<TableRow>();
    private int visibleRowsAbove = 0;
    private int visibleRowsBelow = 0;

    public CoalescedRow() {
      super(true, css.coalescedRow());
    }

    /**
     * Sticks a row into our CoalescedRow which will be later lazily created and
     * attached to the DOM.
     * 
     * @param append whether or not we should append the row to the end of the
     *          table
     * @param row the TableRow to add.
     */
    public void coalesceRow(boolean append, TableRow row) {
      if (append) {
        tableRows.addLast(row);
      } else {
        tableRows.addFirst(row);
      }
      aggregateTime += row.filterValue;
      // We lazily create everything.
      if (labelText != null) {
        updateLabel();
      }
    }

    public boolean isAttached() {
      return isAttached;
    }

    public void setAttached(boolean b) {
      this.isAttached = b;
    }

    @Override
    protected Element createElement() {
      // Create all the Top level elements
      filterBar = DocumentExt.get().createDivWithClassName(css.filterBar());

      final SpanElement labelElem = filterBar.appendChild(DocumentExt.get().createSpanElement());
      showAbove = labelElem.appendChild(DocumentExt.get().createAnchorElement());
      labelText = labelElem.appendChild(DocumentExt.get().createTextNode(""));
      showBelow = labelElem.appendChild(DocumentExt.get().createAnchorElement());

      labelElem.setClassName(css.filterLabel());
      showAbove.setHref("javascript:void(0);");
      showBelow.setHref("javascript:void(0);");

      rowsAbove = DocumentExt.get().createDivElement();
      rowsBelow = DocumentExt.get().createDivElement();

      final Element elem = DocumentExt.get().createDivElement();
      elem.appendChild(rowsAbove);
      elem.appendChild(filterBar);
      elem.appendChild(rowsBelow);
      updateLabel();

      // hook a click listener to open 10 rows above
      manageEventListener(ClickEvent.addClickListener(this, showAbove,
          new ClickListener() {
            public void onClick(ClickEvent event) {
              expandAbove();
            }
          }));

      // hook a click listener to open 10 rows below
      manageEventListener(ClickEvent.addClickListener(this, showBelow,
          new ClickListener() {
            public void onClick(ClickEvent event) {
              expandBelow();
            }
          }));

      return elem;
    }

    private void doRowInsertion(Element parent, TableRow row,
        String cssClassName, boolean append) {
      row.addClassName(cssClassName);
      // decrement aggregate time.
      aggregateTime -= row.filterValue;
      Element rowElem = row.getElement();
      if (append) {
        parent.appendChild(rowElem);
      } else {
        parent.insertBefore(rowElem, parent.getFirstChild());
      }
    }

    private void expandAbove() {
      // expose pageSize rows at a time from top.
      for (int i = 0, n = tableRows.size(); i < n && i < pageSize; i++) {
        String evenOdd = ((visibleRowsAbove++ & 1) == 1) ? commonCss.even()
            : commonCss.odd();
        TableRow row = tableRows.removeFirst();
        doRowInsertion(rowsAbove, row, evenOdd, true);
      }
      finalizeExpand();
      getElement().getStyle().setPropertyPx("paddingTop", 5);
    }

    private void expandBelow() {
      // expose pageSize rows at a time from bottom.
      for (int i = tableRows.size(), c = 0; i > 0 && c < pageSize; i--) {
        String evenOdd = ((visibleRowsBelow++ & 1) == 1) ? commonCss.even()
            : commonCss.odd();
        TableRow row = tableRows.removeLast();
        doRowInsertion(rowsBelow, row, evenOdd, false);
        c++;
      }
      finalizeExpand();
      getElement().getStyle().setPropertyPx("paddingBottom", 5);
    }

    private void finalizeExpand() {
      int remaining = tableRows.size();
      if (remaining == 0) {
        filterBar.getStyle().setProperty("display", "none");
        getElement().getStyle().setPropertyPx("paddingTop", 5);
        getElement().getStyle().setPropertyPx("paddingBottom", 5);
      } else {
        updateLabel();
      }
    }

    private void updateLabel() {
      int remaining = tableRows.size();
      labelText.setData("Hiding " + remaining + " events ("
          + TimeStampFormatter.format(aggregateTime) + ")");
      showAbove.setInnerText("show " + Math.min(pageSize, remaining) + " above");
      showBelow.setInnerText("show " + Math.min(pageSize, remaining) + " below");
    }
  }

  /**
   * Css.
   */
  public interface Css extends CssResource {
    String cell();

    String coalescedRow();

    String details();

    String filterBar();

    String filteringScrollTable();

    String filterLabel();

    String filterPanel();

    String filterPanelClose();

    int filterPanelHeight();

    String row();

    int rowCellPadding();

    int rowHeight();

    String tableContent();
  }

  /**
   * To be implemented by a concrete subclass to specify a Filter to be applied
   * to all Rows.
   */
  public interface Filter {
    boolean shouldFilter(TableRow row);
  }

  /**
   * Externalized interface.
   */
  public interface Resources extends CommonResources {
    @Source("resources/light_grey_gradient.png")
    @ImageOptions(repeatStyle = RepeatStyle.Horizontal)
    ImageResource detailsBg();

    @Source("resources/filter-bar-background.png")
    @ImageOptions(repeatStyle = RepeatStyle.Both)
    ImageResource filterBarBg();

    @Source("resources/FilteringScrollTable.css")
    FilteringScrollTable.Css filteringScrollTableCss();

    @Source("resources/close-x-10px.png")
    ImageResource filterPanelClose();
  }

  /**
   * Expandable placeholder for details about a row.
   */
  public class RowDetails extends LazilyCreateableElement {
    private final TableRow parent;

    protected RowDetails(TableRow parent) {
      super(FilteringScrollTable.this, css.details());
      this.parent = parent;
      parent.setDetails(this);
    }

    @Override
    protected Element createElement() {
      Element elem = Document.get().createDivElement();
      // Make sure we reflow to height of contents
      elem.getStyle().setProperty("height", "auto");
      parent.getElement().appendChild(elem);

      // Make sure clicking around the detail view doesn't bubble up
      manageEventListener(ClickEvent.addClickListener(this, elem,
          new ClickListener() {

            public void onClick(ClickEvent event) {
              event.getNativeEvent().cancelBubble(true);
            }

          }));

      return elem;
    }

    protected TableRow getParentRow() {
      return parent;
    }
  }

  /**
   * A row in the table.
   */
  public class TableRow extends Row {
    private static final String selectedColor = "#e6e6ff";
    public final double filterValue;
    private final List<Cell> cells = new ArrayList<Cell>();
    private RowDetails details;
    private int internalOffset = 0;
    private boolean isExpanded = false;

    public TableRow(double filterValue) {
      super(false, css.row());
      this.filterValue = filterValue;
    }

    public void addCell(Cell cell) {
      cells.add(cell);
    }

    public List<Cell> getCells() {
      return cells;
    }

    public RowDetails getDetails() {
      return details;
    }

    public boolean isExpanded() {
      return isExpanded;
    }

    public void setDetails(RowDetails details) {
      this.details = details;
    }

    public void toggleDetails() {
      if (details != null) {
        // We want to allow the details table to schedule the expansion change
        // listener in front of us on the event loop.
        Command.defer(new Command.Method() {
          public void execute() {
            // Query the height. This should also create the details panel if it
            // hasn't already been create.
            int targetHeight = details.getElement().getOffsetHeight()
                + css.rowHeight();

            if (isExpanded()) {
              getElement().getStyle().setProperty("backgroundColor", "");
              getElement().getStyle().setProperty("borderBottom", "");
              getElement().getStyle().setPropertyPx("height", css.rowHeight());
              isExpanded = false;
            } else {
              getElement().getStyle().setProperty("backgroundColor",
                  selectedColor);
              getElement().getStyle().setProperty("borderBottom",
                  "1px solid #888");
              getElement().getStyle().setPropertyPx("height", targetHeight);
              isExpanded = true;
            }
          }
        }, 50);
      }
    }

    @Override
    protected Element createElement() {
      DivElement elem = Document.get().createDivElement();
      DivElement cellWrapper = Document.get().createDivElement();
      for (int i = 0, n = cells.size(); i < n; i++) {
        internalOffset = cells.get(i).addToElement(cellWrapper, internalOffset);
      }

      elem.appendChild(cellWrapper);
      return elem;
    }
  }

  /**
   * This panel can be popped out to allow setting of the filter criteria used
   * for the table.
   */
  protected class FilterPanel extends Div {
    protected final Container panelContainer;

    public FilterPanel(Container parent) {
      super(parent);
      setStyleName(css.filterPanel());
      panelContainer = new DefaultContainerImpl(getElement());

      Div closeButton = new Div(panelContainer);
      closeButton.setStyleName(css.filterPanelClose());
      closeButton.addClickListener(new ClickListener() {

        public void onClick(ClickEvent event) {
          toggleFilterPanelVisible();
        }

      });
    }
  }

  /**
   * Rows are things that can be added to this table. The corresponding DOM
   * elements are lazily constructible.
   */
  private abstract class Row extends LazilyCreateableElement {
    private final boolean isCoalesceable;

    public Row(boolean isCoalesceable, String cssClassName) {
      super(listenerOwner, cssClassName);
      this.isCoalesceable = isCoalesceable;
    }

    public boolean isCoalesceable() {
      return isCoalesceable;
    }

    /**
     * Empty default impl. Subclasses may choose to override to handle this.
     */
    public void onResize() {
    }
  }

  private final CommonCss commonCss;

  private final FilteringScrollTable.Css css;

  private final EventListenerOwner listenerOwner = new EventListenerOwner();

  private final Filter filter;

  private final FilterPanel filterPanel;

  private final Container myContainer;

  private final ArrayList<Row> rowList = new ArrayList<Row>();

  private final Element tableContents;

  public FilteringScrollTable(Container container, Filter filter,
      FilteringScrollTable.Resources resources) {
    super(container);
    Element elem = getElement();
    this.filter = filter;
    this.css = resources.filteringScrollTableCss();
    this.commonCss = resources.commonCss();
    myContainer = new DefaultContainerImpl(elem);
    filterPanel = new FilterPanel(getContainer());
    tableContents = DocumentExt.get().createDivWithClassName(css.tableContent());
    elem.appendChild(tableContents);
    CssTransitionEvent.addTransitionListener(tableContents, tableContents,
        new CssTransitionListener() {
          public void onTransitionEnd(CssTransitionEvent event) {
            int tableContentTop = tableContents.getOffsetTop();
            if (tableContentTop == 0) {
              filterPanel.setVisible(false);
            }
          }
        });

    WindowExt window = WindowExt.getHostWindow();
    ResizeEvent.addResizeListener(window, window, this);
  }

  public void clearTable() {
    // Remove the existing event handlers for the row.
    listenerOwner.removeAllEventListeners();
    rowList.clear();
    getTableContents().setInnerHTML("");
  }

  public Container getContainer() {
    return myContainer;
  }

  /**
   * A panel between the header and content that can be used to display values
   * for the filter.
   */
  public Container getFilterPanelContainer() {
    return filterPanel.panelContainer;
  }

  /**
   * Returns the last row added to the Table.
   * 
   * @return
   */
  public Row getLastRow() {
    return rowList.get(rowList.size() - 1);
  }

  public Element getTableContents() {
    return tableContents;
  }

  /**
   * Inserts a row at the end of the table if append is true, or at the
   * beginning if it is false. The row may or may not be displayed depending on
   * the filter setting.
   * 
   * @param row
   * @param append
   */
  public Row insertRow(TableRow row, boolean append) {
    Row toAdd = row;

    if (append) {
      // We want to append the row
      if (filter.shouldFilter(row)) {
        // We want to filer this row
        Row targetRow;
        if (rowList.size() > 0
            && (targetRow = rowList.get(rowList.size() - 1)).isCoalesceable()) {
          // The we add it to this group of TableRows that have been coalesced.
          ((CoalescedRow) targetRow).coalesceRow(true, row);
          return targetRow;
        } else {
          // We should create a new coalesceable group.
          CoalescedRow newRow = new CoalescedRow();
          newRow.coalesceRow(true, row);
          toAdd = newRow;
        }
      }

      // Add to bookkeeping
      rowList.add(toAdd);
    } else {
      // We want to stick the row on the front
      if (filter.shouldFilter(row) && rowList.size() > 0) {
        // We want to filter this row
        Row targetRow;
        if (rowList.size() > 0 && (targetRow = rowList.get(0)).isCoalesceable()) {
          // The we add it to this group of TableRows that have been coalesced.
          ((CoalescedRow) targetRow).coalesceRow(false, row);
          return targetRow;
        } else {
          // We should create a new coalesceable group.
          CoalescedRow newRow = new CoalescedRow();
          newRow.coalesceRow(true, row);
          toAdd = newRow;
        }
      }

      // Add to bookkeeping
      rowList.add(0, toAdd);
    }

    return toAdd;
  }

  public void manageEventListener(EventListenerRemover remover) {
    listenerOwner.manageEventListener(remover);
  }

  /**
   * Resize handler that gives all rows in the table a chance to resize
   * themselves.
   */
  public void onResize(ResizeEvent event) {
    for (int i = 0, n = rowList.size(); i < n; i++) {
      rowList.get(i).onResize();
    }
  }

  /**
   * Immediately adds a Row to the table.
   * 
   * @param row
   */
  public void renderRow(Row row) {
    if (row.isCoalesceable()) {
      CoalescedRow cRow = (CoalescedRow) row;
      if (cRow.isAttached()) {
        // Early out if this row is already attached.
        return;
      }
      ((CoalescedRow) row).setAttached(true);
    } else {
      // We want to have all main rows to be white with hover since they
      // mostly will be separated by coalesced rows which are grey
      row.addClassName(commonCss.odd());
    }
    tableContents.appendChild(row.getElement());
  }

  /**
   * Builds the DOM structure for the entire table.
   */
  public void renderTable() {
    for (int i = 0, n = rowList.size(); i < n; i++) {
      renderRow(rowList.get(i));
    }
  }

  public void toggleFilterPanelVisible() {
    int tableContentTop = tableContents.getOffsetTop();
    if (tableContentTop == 0) {
      filterPanel.setVisible(true);
      filterPanel.getElement().getStyle().setProperty("display", "inline-block");
      tableContents.getStyle().setPropertyPx("top", css.filterPanelHeight() + 3);
    } else {
      tableContents.getStyle().setPropertyPx("top", 0);
    }
  }
}
