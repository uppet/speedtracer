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
package com.google.speedtracer.client.view;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.StyleInjector;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.topspin.ui.client.Button;
import com.google.gwt.topspin.ui.client.ClickEvent;
import com.google.gwt.topspin.ui.client.ClickListener;
import com.google.gwt.topspin.ui.client.Container;
import com.google.gwt.topspin.ui.client.DefaultContainerImpl;
import com.google.gwt.topspin.ui.client.Div;
import com.google.speedtracer.client.model.DataDispatcher;
import com.google.speedtracer.client.model.MockModelGenerator;

import java.util.List;

/**
 * Panel containing list of datasets to choose from.
 */
public class MockTestDataPanel {

  /**
   * Resources.
   */
  public interface Resources extends ClientBundle {

    @Source("resources/MockTestDataPanel.css")
    Css testDataPanelCss();

    @Source("resources/test-menu.png")
    ImageResource controllerTestMenuButton();

    @Source("resources/test-menu-d.png")
    ImageResource controllerTestMenuButtonDown();

    @Source("resources/test-menu-h.png")
    ImageResource controllerTestMenuButtonHover();

    @Source("resources/test-menu-p.png")
    ImageResource controllerTestMenuButtonPress();
  }

  /**
   * Css.
   */
  public interface Css extends CssResource {
    String base();

    String menuItem();

    String testMenuButton();
  }

  /**
   * The auto-hide div that holds the drop down menu.
   */
  public static class TopDiv extends AutoHideDiv {
    public TopDiv(Container container) {
      super(container, HIDE_DELAY);
    }
  }

  private static final int HIDE_DELAY = 1000;
  private AutoHideDiv base;
  private DataDispatcher dataDispatcher;
  private Css css;
  private Container controllerContainer;
  private Controller controller;

  private void init() {
    Resources resources = GWT.create(Resources.class);
    this.css = resources.testDataPanelCss();
    StyleInjector.inject(css.getText());
    this.base = new TopDiv(controllerContainer);
    this.base.setStyleName(css.base());
    this.dataDispatcher = controller.getDataDispatcher();

    // Add in the menu items
    List<String> menuItems = MockModelGenerator.getDataSetNames();
    for (int i = 0; i < menuItems.size(); ++i) {
      Div menuItem = new Div(new DefaultContainerImpl(base.getElement()));
      menuItem.setStyleName(css.menuItem());
      menuItem.setText(menuItems.get(i));
      final int dataSetIndex = i;
      menuItem.addClickListener(new ClickListener() {
        public void onClick(ClickEvent event) {
          controller.doReset();
          MockModelGenerator.simulateDataSet(dataDispatcher, dataSetIndex);
        }
      });
    }
  }

  // TODO(knorton): This can be moved into a simple constructor now that
  // ClientConfig exists.
  public void addButtonToController(Controller.Resources controllerResources,
      Controller controller, Container controllerContainer) {
    this.controller = controller;
    this.controllerContainer = controllerContainer;

    init();
    /**
     * A button that allows the mock data sets to be selected at runtime.
     */
    final Button testMenuButton = new Button(controllerContainer);
    testMenuButton.setStyleName(controllerResources.controllerCss().control()
        + " " + css.testMenuButton());
    testMenuButton.getElement().setAttribute("title", "Mock Data");
    testMenuButton.addClickListener(new ClickListener() {
      public void onClick(ClickEvent event) {
        int startX = testMenuButton.getAbsoluteLeft() + 10;
        int startY = testMenuButton.getOffsetHeight();
        base.getElement().getStyle().setLeft(startX, Unit.PX);
        base.getElement().getStyle().setTop(startY, Unit.PX);
        base.show();
      }
    });
  }
}
