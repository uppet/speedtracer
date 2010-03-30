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
package com.google.speedtracer.client.visualizations.view;

import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.TableCellElement;
import com.google.gwt.dom.client.TableRowElement;
import com.google.gwt.topspin.ui.client.Container;
import com.google.gwt.topspin.ui.client.DefaultContainerImpl;
import com.google.gwt.topspin.ui.client.Table;
import com.google.gwt.topspin.ui.client.Widget;
import com.google.speedtracer.client.model.HintRecord;
import com.google.speedtracer.client.util.TimeStampFormatter;
import com.google.speedtracer.client.util.dom.DocumentExt;
import com.google.speedtracer.client.view.SortableTableHeader;
import com.google.speedtracer.client.view.SortableTableHeaderGroup;
import com.google.speedtracer.client.view.SortableTableHeader.SortToggleListener;
import com.google.speedtracer.client.visualizations.model.HintletReportModel;

import java.util.ArrayList;
import java.util.List;

/**
 * Base class for trees that can be displayed in this report.
 * 
 */
public abstract class HintletReportTree extends Widget {

  /**
   * Represents a table used as a detail item for a ReportRow.
   */
  protected abstract static class ReportDetails extends Tree.Item {
    private class DescriptionSortToggle implements SortToggleListener {
      public void onSortToggle(boolean isAscending) {
        reportModel.sortByDescription(hintletRecords, isAscending);
        clearRows();
        populateSubTable();
      }
    }

    private class RuleSortToggle implements SortToggleListener {
      public void onSortToggle(boolean isAscending) {
        reportModel.sortByRuleName(hintletRecords, isAscending);
        clearRows();
        populateSubTable();
      }
    }

    private class SeveritySortToggle implements SortToggleListener {
      public void onSortToggle(boolean isAscending) {
        reportModel.sortBySeverity(hintletRecords, isAscending);
        clearRows();
        populateSubTable();
      }
    }

    private class TimeSortToggle implements SortToggleListener {
      public void onSortToggle(boolean isAscending) {
        reportModel.sortByTime(hintletRecords, isAscending);
        clearRows();
        populateSubTable();
      }
    }

    protected final List<Integer> columns = new ArrayList<Integer>();
    protected final HintletReport.Css css;
    protected final List<HintRecord> hintletRecords;
    protected final HintletReportModel reportModel;
    protected final HintletReport.Resources resources;
    protected final TableRowElement headerRowElem;
    protected final Table subTable;
    private List<SortableTableHeader> columnHeaders = new ArrayList<SortableTableHeader>();
    private final SortableTableHeaderGroup headerGroup = new SortableTableHeaderGroup();

    protected ReportDetails(Tree.Item parent,
        List<HintRecord> hintletRecords, HintletReport.Resources resources,
        HintletReportModel reportModel) {
      super(parent, resources);
      this.css = resources.hintletReportCss();
      this.hintletRecords = hintletRecords;
      this.resources = resources;
      this.reportModel = reportModel;
      this.setExpandIconVisible(false);
      subTable = new Table(new DefaultContainerImpl(this.getItemLabelElement()));
      subTable.setStyleName(css.reportRowDetailTable());
      headerRowElem = subTable.appendRow();
      String className = this.getContentElement().getClassName();
      this.getContentElement().setClassName(
          css.reportRowDetailContentElem() + " " + className);
    }

    /**
     * Adds a column to the sub-table.
     * 
     * This implementation assumes that only one column of any type will be
     * added to the table. The behavior of the table is undefined if the same
     * type of column is added more than once.
     * 
     * @param columnType Pass one of the HintletRowTree.COL_XXX constants.
     */
    public void addColumn(int columnType) {
      int index = columns.size();
      columns.add(columnType);

      TableCellElement cell = headerRowElem.insertCell(index);
      Container cellContainer = new DefaultContainerImpl(cell);
      SortableTableHeader header;
      switch (columnType) {
        case COL_SEVERITY:
          cell.setClassName(css.reportRowDetailSeverityCell());
          header = new SortableTableHeader(cellContainer, "", resources);
          header.addSortToggleListener(new SeveritySortToggle());
          break;
        case COL_TIME:
          cell.setClassName(css.reportRowDetailTimeCell());
          header = new SortableTableHeader(cellContainer, "Time", resources);
          header.addSortToggleListener(new TimeSortToggle());
          break;
        case COL_RULE_NAME:
          cell.setClassName(css.reportRowDetailCell());
          header = new SortableTableHeader(cellContainer, "RuleName", resources);
          header.addSortToggleListener(new RuleSortToggle());
          break;
        case COL_DESCRIPTION:
        default:
          cell.setClassName(css.reportRowDetailCell());
          header = new SortableTableHeader(cellContainer, "Description",
              resources);
          header.addSortToggleListener(new DescriptionSortToggle());
          break;
      }
      columnHeaders.add(header);
      headerGroup.add(header);
    }

    public final void detachEventListeners() {
      for (int i = 0, j = columnHeaders.size(); i < j; ++i) {
        SortableTableHeader header = columnHeaders.get(i);
        header.detachEventListeners();
      }
      columnHeaders.clear();
    }

    // Clear everything but the headers from the table.
    protected final void clearRows() {
      int rowCount = subTable.getRowCount();
      while (--rowCount > 0) {
        subTable.deleteRow(1);
      }
    }

    /**
     * Sets the sort criteria for the table.
     * 
     * @param columnType Pass one of the HintletRowTree.COL_XXX constants.
     */
    protected void setSortColumn(int columnType, boolean isAscending) {
      int i;
      int j = columns.size();
      for (i = 0; i < j; ++i) {
        if (columnType == columns.get(i)) {
          break;
        }
      }
      if (i == columns.size()) {
        // not found
        return;
      }

      SortableTableHeader header = columnHeaders.get(i);
      if (isAscending) {
        header.setAscending();
      } else {
        header.setDescending();
      }
    }

    /**
     * Adds the rows to the table. Assumes there are no data rows in the table.
     */
    private void populateSubTable() {
      for (int i = 0, j = hintletRecords.size(); i < j; ++i) {
        HintRecord record = hintletRecords.get(i);
        TableRowElement rowElem = subTable.appendRow();
        if (i % 2 == 0) {
          rowElem.setClassName(css.reportTableEvenRow());
        }

        int length = columns.size();
        for (int cellIndex = 0; cellIndex < length; ++cellIndex) {
          TableCellElement cell = rowElem.insertCell(cellIndex);
          switch (columns.get(cellIndex)) {
            case COL_TIME:
              cell.setClassName(css.reportRowDetailTimeCell());
              cell.setInnerText(TimeStampFormatter.format(record.getTimestamp()));
              break;
            case COL_SEVERITY:
              cell.setClassName(css.reportRowDetailSeverityCell());
              // A colored square to indicate the severity
              DivElement severitySquare = DocumentExt.get().createDivWithClassName(
                  css.reportSeverityDot());
              cell.appendChild(severitySquare);
              severitySquare.getStyle().setProperty("backgroundColor",
                  HintletIndicator.getSeverityColor(record.getSeverity()));
              break;
            case COL_DESCRIPTION:
              cell.setClassName(css.reportRowDetailCell());
              cell.setInnerText(record.getDescription());
              break;
            case COL_RULE_NAME:
              cell.setClassName(css.reportRowDetailCell());
              cell.setInnerText(record.getHintletRule());
              break;
            default:
              // not a valid row type
              assert (false);
          }
        }
      }
    }
  }

  /**
   * Represents a major heading in the report that is collapsable.
   */
  protected abstract static class ReportRow extends Tree.Item {
    protected ReportRow(HintletReport.Resources resources, Tree tree) {
      super(resources, tree);
    }

    public abstract void detachEventListeners();
  }

  static final int COL_DESCRIPTION = 4;
  static final int COL_RULE_NAME = 3;
  static final int COL_SEVERITY = 1;
  static final int COL_TIME = 2;

  protected final HintletReport.Css css;
  protected HintletReportModel reportModel;
  protected final HintletReport.Resources resources;
  protected final List<ReportRow> rows = new ArrayList<ReportRow>();
  protected final Tree tree;

  protected HintletReportTree(Container container,
      HintletReport.Resources resources, HintletReportModel reportModel) {
    super(DocumentExt.get().createDivElement(), container);
    tree = new Tree(new DefaultContainerImpl(getElement()), resources);
    tree.disableSelection(true);
    this.resources = resources;
    this.css = resources.hintletReportCss();
    this.reportModel = reportModel;
    String treeClass = tree.getElement().getClassName() + " "
        + css.reportTree();
    tree.getElement().setClassName(treeClass);
    populateTable();
  }

  public void detachEventListeners() {
    for (int i = 0, j = rows.size(); i < j; ++i) {
      rows.get(i).detachEventListeners();
    }
  }

  public void refresh() {
    // Re-fill the table with data
    populateTable();
  }

  public void setReportModel(HintletReportModel reportModel) {
    this.reportModel = reportModel;
    populateTable();
  }

  protected void clearReportTree() {
    detachEventListeners();
    tree.getElement().setInnerHTML("");
    rows.clear();
  }

  protected abstract void populateTable();
}
