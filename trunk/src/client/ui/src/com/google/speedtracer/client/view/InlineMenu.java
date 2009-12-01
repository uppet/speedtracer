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
package com.google.speedtracer.client.view;

import com.google.gwt.dom.client.Element;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.resources.client.CssResource.Strict;
import com.google.gwt.topspin.ui.client.ClickEvent;
import com.google.gwt.topspin.ui.client.ClickListener;
import com.google.gwt.topspin.ui.client.Container;
import com.google.speedtracer.client.Monitor;
import com.google.speedtracer.client.util.dom.DocumentExt;

/**
 * Inline radial menu popup around mouse.
 */
public class InlineMenu extends AutoHideDiv {

  /**
   * CSS.
   */
  public interface Css extends CssResource {
    String content();

    String help();

    String inlineMenu();

    String menuItem();
  }

  /**
   * A Menu Item.
   */
  public static class InlineMenuItem {
    private Element elem;
    private final ClickListener listener;
    private final String name;

    public InlineMenuItem(String name, ClickListener listener) {
      this.name = name;
      this.listener = listener;
    }

    private Element getElement(String cssClassName) {
      if (elem == null) {
        elem = DocumentExt.get().createDivWithClassName(cssClassName);
        elem.setInnerText(name);
        ClickEvent.addClickListener(this, elem, listener);
      }
      return elem;
    }
  }

  /**
   * Externalized Interface.
   */
  public interface Resources extends ClientBundle {
    @Source("resources/inline-bg.png")
    ImageResource inlineMenuBg();

    @Source("resources/InlineMenu.css")
    @Strict
    InlineMenu.Css inlineMenuCss();

    @Source("resources/inline-menu-item.png")
    ImageResource inlineMenuItem();

    @Source("resources/inline-menu-item-hover.png")
    ImageResource inlineMenuItemHover();

    @Source("resources/question_mark.png")
    ImageResource questionMark();
  }

  private static InlineMenu INSTANCE;

  // The content of this menu
  private final Element contentDiv;

  private final InlineMenu.Css css;

  // The help Icon
  private final Element helpIcon;

  private int startX = 0;

  private int startY = 0;

  public InlineMenu(Container container, InlineMenu.Resources resources) {
    // TODO(jaimeyap): get rid of magic constants!!!
    super(container, 800);
    if (INSTANCE == null) {
      INSTANCE = this;
    }
    css = resources.inlineMenuCss();

    Element elem = getElement();
    elem.setClassName(css.inlineMenu());

    contentDiv = DocumentExt.get().createDivWithClassName(css.content());
    helpIcon = DocumentExt.get().createDivWithClassName(css.help());

    elem.appendChild(contentDiv);
    elem.appendChild(helpIcon);

    ClickEvent.addClickListener(helpIcon, helpIcon, new ClickListener() {
      public void onClick(ClickEvent event) {
        Monitor.getPopup().show(startX, startY);
      }
    });
  }

  public void loadMenu(MenuSource source, OnHideCallBack cb) {
    if (callBack != null && !callBack.equals(cb)) {
      callBack.onHide();
    }
    callBack = cb;
    InlineMenuItem[] items = source.getMenuItems();
    Monitor.getPopup().setContentProvider(source.getPopupContent());

    contentDiv.setInnerHTML("");
    int flip = 1;
    for (int i = 0; items != null && i < items.length; i++) {
      Element elem = items[i].getElement(css.menuItem());
      // These seem like magic values now, but they are basically offsets I
      // figured out on
      // paper based on the dimensions of the background images I used.
      elem.getStyle().setProperty("webkitTransformOrigin", "61px 17px 0");
      elem.getStyle().setProperty("webkitTransform",
          "translate(-47px,11px) rotate(" + (flip * 35) + "deg)");
      contentDiv.appendChild(elem);
      flip = flip * -1;
    }
  }

  public void show(int mouseX, int mouseY) {
    startX = mouseX;
    startY = mouseY;
    Element elem = getElement();
    elem.getStyle().setPropertyPx("left", mouseX - 13);
    elem.getStyle().setPropertyPx("top", mouseY - 24);
    elem.getStyle().setProperty("display", "block");
    isVisible = true;
  }
}
