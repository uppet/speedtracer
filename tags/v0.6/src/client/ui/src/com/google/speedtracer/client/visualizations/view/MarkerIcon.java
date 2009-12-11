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

import com.google.gwt.dom.client.Element;
import com.google.gwt.events.client.EventListenerRemover;
import com.google.gwt.topspin.ui.client.ClickEvent;
import com.google.gwt.topspin.ui.client.ClickListener;
import com.google.gwt.topspin.ui.client.Container;
import com.google.gwt.topspin.ui.client.Div;
import com.google.speedtracer.client.Monitor;
import com.google.speedtracer.client.view.HoveringPopup;
import com.google.speedtracer.client.view.InlineMenu;
import com.google.speedtracer.client.view.MenuSource;
import com.google.speedtracer.client.view.HoveringPopup.PopupContentProvider;
import com.google.speedtracer.client.view.InlineMenu.InlineMenuItem;

/**
 * This is a div that allows the user to interact with the marker drawn on the
 * canvas. May optionally draw an image to draw attention to the marker.
 */
public class MarkerIcon extends Div {

  private final MenuSource menuSource;
  private EventListenerRemover remover;

  /**
   * 
   * @param container The container to create this div in.
   * @param menuSource Object that contains the menu items and/or the popup
   *          contents for help.
   * @param markerIconType controls the CSS stylename for the icon to display
   *          over the marker that is sensitive for menus and help.
   */
  public MarkerIcon(Container container, MenuSource menuSource,
      String markerIconType) {
    super(container);
    Element elem = getElement();
    elem.setClassName(markerIconType);
    this.menuSource = menuSource;
  }

  /**
   * Create a MarkerIcon that does not host a menu.
   * 
   * @param container The container to create this div in.
   * @param markerIconType controls the CSS stylename for the icon to display
   *          over the marker that is sensitive for menus and help. Pass
   *          MarkerIcon.DEFAULT for a transparent area that is clickable.
   */
  public MarkerIcon(Container container, String markerIconType) {
    this(container, null, markerIconType);
  }

  public void moveTo(int x) {
    getElement().getStyle().setPropertyPx("left", x - 5);
  }

  public void setVisible(boolean visible) {
    if (visible) {
      show();
    } else {
      hide();
    }
  }

  private void hide() {
    if (remover != null) {
      remover.remove();
      remover = null;
    }

    getElement().getStyle().setProperty("display", "none");
  }

  /**
   * TODO(zundel): Since we have basically bent this base class into doing stuff
   * slightly different from how we originally used it. Maybe this method should
   * be abstract since a default implementation no longer makes as much sense.
   */
  private void show() {
    if (remover == null) {

      remover = ClickEvent.addClickListener(this, getElement(),
          new ClickListener() {

            public void onClick(ClickEvent event) {
              if (menuSource == null) {
                return;
              }

              int mX = ((ClickEvent) event).getNativeEvent().getClientX();
              int mY = ((ClickEvent) event).getNativeEvent().getClientY();

              InlineMenuItem[] items = menuSource.getMenuItems();
              if (items != null && items.length > 0) {
                // load the inline menu
                Monitor.getInlineMenu().loadMenu(menuSource, null);
                InlineMenu inlineMenu = Monitor.getInlineMenu();
                if (!inlineMenu.isVisible()) {
                  inlineMenu.show(mX, mY);
                }
              } else {
                // go straight to the popup help
                HoveringPopup popup = Monitor.getPopup();
                PopupContentProvider content = menuSource.getPopupContent();
                if (content != null) {
                  popup.setContentProvider(content);
                  popup.show(mX, mY);
                }
              }
            }

          });

      getElement().getStyle().setProperty("display", "block");
    }
  }
};
