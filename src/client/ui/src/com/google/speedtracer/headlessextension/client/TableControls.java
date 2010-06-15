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

import com.google.gwt.core.client.JsArray;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.speedtracer.client.model.WhitelistEntry;
import com.google.speedtracer.client.model.WhitelistModel;
import com.google.speedtracer.client.util.dom.WindowExt;
import com.google.speedtracer.headlessextension.client.WhitelistTable.SaveListener;

/**
 * These controls include logic and UI to glue together the WhitelistTable and
 * WhitlistModel.
 */
public class TableControls extends Composite {

  /**
   * A popup to allow entering new values for the table.
   */
  public class AddWhitelistEntryPopup extends PopupPanel {
    final TextBox hostField = new TextBox();
    final TextBox portField = new TextBox();

    public AddWhitelistEntryPopup(Resources resources) {
      super();
      Css css = resources.tableControlsCss();
      this.setStyleName(css.popupPanel());
      final Button addButton = new Button("Add");
      VerticalPanel popupPanel = new VerticalPanel();
      add(popupPanel);

      Grid fieldPanel = new Grid(2, 2);
      fieldPanel.setHTML(0, 0, "Host:");
      fieldPanel.setWidget(0, 1, hostField);
      fieldPanel.setHTML(1, 0, "Port:");
      fieldPanel.setWidget(1, 1, portField);
      popupPanel.add(fieldPanel);
      addButton.addClickHandler(new ClickHandler() {
        public void onClick(ClickEvent event) {
          String hostString = hostField.getText();
          String portString = portField.getText();
          // TODO(zundel): some kind of validation
          WhitelistEntry whitelistEntry = WhitelistEntry.create(hostString,
              portString);
          String validationErrors = whitelistEntry.getValidationErrors();
          if (validationErrors != null) {
            WindowExt.alert("Error: " + validationErrors);
          } else {
            whitelistTable.addWhitelistEntry(whitelistEntry);
            whitelistModel.saveEntries(whitelistTable.getEntries());
          }
          hide();
        }
      });
      Button cancelButton = new Button("Cancel");
      cancelButton.addClickHandler(new ClickHandler() {
        public void onClick(ClickEvent event) {
          addPopup.hide();
        }
      });
      Panel buttonPanel = new HorizontalPanel();
      buttonPanel.add(cancelButton);
      buttonPanel.add(addButton);
      popupPanel.add(buttonPanel);
      addButton.setFocus(true);
    }

    public void clear() {
      hostField.setText("");
      portField.setText("");
    }
  }

  /**
   * Css.
   */
  public interface Css extends CssResource {
    String controlPanel();

    String popupPanel();
  }

  /**
   * Resources.
   */
  public interface Resources extends ClientBundle {
    @Source("resources/TableControls.css")
    Css tableControlsCss();
  }

  private final PopupPanel addPopup;
  private final Panel panel;
  private final WhitelistModel whitelistModel;
  private final WhitelistTable whitelistTable;

  public TableControls(Resources resources,
      final WhitelistTable whitelistTable, final WhitelistModel whitelistModel) {
    Css css = resources.tableControlsCss();
    panel = new HorizontalPanel();
    panel.setStyleName(css.controlPanel());
    this.whitelistTable = whitelistTable;
    this.whitelistTable.addSaveListener(new SaveListener() {
      public void onSave() {
        whitelistModel.saveEntries(whitelistTable.getEntries());
      }
    });
    this.whitelistModel = whitelistModel;
    Panel buttonPanel = new VerticalPanel();
    Button addButton = new Button("Add Entry");
    addButton.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        addPopup.setGlassEnabled(true);
        addPopup.setPopupPosition(100, 100);
        addPopup.setVisible(true);
        addPopup.show();
      }
    });
    buttonPanel.add(addButton);
    panel.add(buttonPanel);
    initWidget(panel);
    addPopup = new AddWhitelistEntryPopup(resources);
    addPopup.setSize("500px", "150px");
  }

  public void reloadTable() {
    whitelistTable.initTable();
    JsArray<WhitelistEntry> entries = WhitelistModel.get().getWhitelist();
    for (int i = 0, length = entries.length(); i < length; i++) {
      whitelistTable.addWhitelistEntry(entries.get(i));
    }
  }
}
