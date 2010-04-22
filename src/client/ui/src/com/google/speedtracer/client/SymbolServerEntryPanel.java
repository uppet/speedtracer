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
package com.google.speedtracer.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.InputElement;
import com.google.gwt.topspin.ui.client.ClickEvent;
import com.google.gwt.topspin.ui.client.ClickListener;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.speedtracer.client.model.TabDescription;
import com.google.speedtracer.client.util.Url;
import com.google.speedtracer.client.util.dom.LocalStorage;
import com.google.speedtracer.client.util.dom.WindowExt;
import com.google.speedtracer.client.view.HotKeyPanel;

/**
 * Simple UI brought up via hot key that allows for the entry of.
 */
public class SymbolServerEntryPanel extends HotKeyPanel {
  interface MyUiBinder extends UiBinder<DivElement, SymbolServerEntryPanel> {
  }

  private static MyUiBinder uiBinder = GWT.create(MyUiBinder.class);

  @UiField
  InputElement appUrl;

  @UiField
  InputElement saveButton;

  @UiField
  InputElement symbolManifestUrl;

  private final TabDescription tabDescription;

  private final Element elem;

  public SymbolServerEntryPanel(TabDescription tabDescription) {
    this.tabDescription = tabDescription;
    this.elem = uiBinder.createAndBindUi(this);
  }

  @Override
  protected Element createContentElement(Document document) {
    return elem;
  }

  @Override
  protected void populateContent(Element contentElement) {
    // No-need to build the UI. UI binder does it all.
    // Only need to populate default values.
    final Url resourceUrl = new Url(tabDescription.getUrl());
    final String applicationUrl = resourceUrl.getApplicationUrl();
    appUrl.setValue(applicationUrl);
    final LocalStorage storage = WindowExt.get().getLocalStorage();
    String previousManifestValue = storage.getStringItem(applicationUrl);
    symbolManifestUrl.setValue(previousManifestValue);

    // And wire up the save button.
    ClickEvent.addClickListener(saveButton, saveButton, new ClickListener() {
      public void onClick(ClickEvent event) {
        if (null == symbolManifestUrl.getValue()) {
          return;
        }
        storage.setStringItem(applicationUrl, symbolManifestUrl.getValue());

        // Register the SSController.
        SymbolServerService.registerSymbolServerController(resourceUrl,
            new Url(symbolManifestUrl.getValue()));

        SymbolServerEntryPanel.this.hide();
      }
    });
  }
}
