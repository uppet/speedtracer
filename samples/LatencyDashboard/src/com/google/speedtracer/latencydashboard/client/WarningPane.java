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
package com.google.speedtracer.latencydashboard.client;

/**
 * Flashes an error at the top of the screen, then transitions itself away if
 * you click the 'x' button.
 */
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.user.client.ui.DockPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootPanel;

public class WarningPane {

  public interface Css extends CssResource {
    public String warningCloseIcon();

    public String warningPaneInner();

    public String warningPaneOuter();

    public String warningPaneText();
  }

  public interface Resources extends ClientBundle {
    @Source("resources/WarningPane.css")
    Css warningPaneCss();
  }

  public static WarningPane singleton;

  public static WarningPane get() {
    return singleton;
  }

  public static void init(Resources resources) {
    if (singleton == null) {
      singleton = new WarningPane(resources);
    }
  }

  private final DockPanel outerElement;
  private final Label textElement;
  private final Label closeIconElement;

  private WarningPane(Resources resources) {
    this.outerElement = new DockPanel();
    this.textElement = new Label();
    this.closeIconElement = new Label();

    createUi(resources);
    RootPanel.get().add(outerElement);
  }

  private void createUi(Resources resources) {
    Css css = resources.warningPaneCss();
    outerElement.addStyleName(css.warningPaneOuter());
    outerElement.setVisible(false);

    DockPanel innerElement = new DockPanel();
    innerElement.addStyleName(css.warningPaneInner());
    outerElement.add(innerElement, DockPanel.NORTH);

    textElement.addStyleName(css.warningPaneText());
    innerElement.add(textElement, DockPanel.CENTER);

    // Create the close icon
    closeIconElement.setStyleName(css.warningCloseIcon());
    closeIconElement.setText("x");
    closeIconElement.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        hide();
      }
    });
    innerElement.add(closeIconElement, DockPanel.EAST);

  }

  public void hide() {
    outerElement.setVisible(false);
  }

  public void show(String message) {
    textElement.setText(message);
    outerElement.setVisible(true);
  }
}