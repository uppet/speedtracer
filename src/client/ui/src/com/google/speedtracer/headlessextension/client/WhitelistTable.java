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
package com.google.speedtracer.headlessextension.client;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.speedtracer.client.model.WhitelistEntry;

import java.util.ArrayList;
import java.util.List;

/**
 * Widget that implements the table for retrieving and saving the whitelist to
 * allow XHRs of Speed Traces.
 */
public class WhitelistTable extends Composite {

  /**
   * Css.
   */
  public interface Css extends CssResource {
    String whitelistTable();

    String whitelistTableDataEntry();

    String whitelistTableHeader();
  }

  /**
   * Client bundle definitions.
   */
  public interface Resources extends ClientBundle {
    @Source("resources/WhitelistTable.css")
    Css whitelistTableCss();
  }

  /**
   * Callback to be invoked whenever the data needs to be saved out of the
   * table.
   */
  public interface SaveListener {
    void onSave();
  }

  public static final int BUTTON_COLUMN = 2;
  public static final int HOST_COLUMN = 0;
  public static final int PORT_COLUMN = 1;

  private final Css css;
  private final List<WhitelistEntry> rows = new ArrayList<WhitelistEntry>();
  private SaveListener saveListener;
  private final FlexTable table = new FlexTable();

  public WhitelistTable(Resources resources) {
    css = resources.whitelistTableCss();
    ScrollPanel scrollPanel = new ScrollPanel();
    scrollPanel.add(table);
    table.setStyleName(css.whitelistTable());
    initTable();
    initWidget(scrollPanel);
  }

  public void addSaveListener(SaveListener listener) {
    this.saveListener = listener;
  }

  /**
   * Adds a new row to the table.
   * 
   * @param entry represents the data for the row to add.
   */
  public void addWhitelistEntry(final WhitelistEntry entry) {
    int numRows = table.getRowCount();
    table.insertRow(numRows);
    table.addCell(numRows);
    table.addCell(numRows);
    table.addCell(numRows);
    table.setText(numRows, HOST_COLUMN, entry.getHost());
    table.getCellFormatter().setStyleName(numRows, HOST_COLUMN,
        css.whitelistTableDataEntry());
    table.setText(numRows, PORT_COLUMN, entry.getPort());
    table.getCellFormatter().setStyleName(numRows, PORT_COLUMN,
        css.whitelistTableDataEntry());
    HorizontalPanel buttonPanel = new HorizontalPanel();
    Button removeButton = new Button("Remove");
    removeButton.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        for (int i = 0; i < rows.size(); i++) {
          if (entry.isEqual(rows.get(i))) {
            rows.remove(i);
            break;
          }
        }
        refresh();
        if (saveListener != null) {
          saveListener.onSave();
        }
      }
    });
    buttonPanel.add(removeButton);

    // TODO(zundel): add an Edit feature

    table.setWidget(numRows, BUTTON_COLUMN, buttonPanel);
    table.getCellFormatter().setStyleName(numRows, BUTTON_COLUMN,
        css.whitelistTableDataEntry());
    rows.add(entry);
  }

  /**
   * Retrieves all entries.
   */
  public JsArray<WhitelistEntry> getEntries() {
    JsArray<WhitelistEntry> results = JavaScriptObject.createArray().cast();
    for (WhitelistEntry entry : rows) {
      results.push(entry);
    }
    return results;
  }

  /**
   * Clears the table and adds in a header.
   */
  public void initTable() {
    table.removeAllRows();
    table.clear();
    rows.clear();
    table.insertRow(0);
    table.addCell(0);
    table.addCell(0);
    table.addCell(0);
    table.setHTML(0, HOST_COLUMN, "<b>Host</b>");
    table.getCellFormatter().setStyleName(0, HOST_COLUMN,
        css.whitelistTableHeader());
    table.setHTML(0, PORT_COLUMN, "<b>Port</b>");
    table.getCellFormatter().setStyleName(0, PORT_COLUMN,
        css.whitelistTableHeader());
    table.setHTML(0, BUTTON_COLUMN, "");
    table.getCellFormatter().setStyleName(0, BUTTON_COLUMN,
        css.whitelistTableHeader());
  }

  public void refresh() {
    List<WhitelistEntry> copy = new ArrayList<WhitelistEntry>();
    for (WhitelistEntry entry : rows) {
      copy.add(entry);
    }
    initTable();
    for (WhitelistEntry entry : copy) {
      this.addWhitelistEntry(entry);
    }
  }
}
