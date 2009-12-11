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

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.StyleInjector;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.CssResource.Strict;
import com.google.gwt.topspin.ui.client.ClickEvent;
import com.google.gwt.topspin.ui.client.ClickListener;
import com.google.gwt.topspin.ui.client.Container;
import com.google.gwt.topspin.ui.client.DefaultContainerImpl;
import com.google.gwt.topspin.ui.client.Div;
import com.google.gwt.topspin.ui.client.Root;
import com.google.speedtracer.client.MonitorResources;
import com.google.speedtracer.client.MonitorResources.CommonCss;

/**
 * Implementation of a simple Logger widget.
 */
public class ZippyLogger extends Div {

  /**
   * CSS.
   */
  public interface Css extends CssResource {
    String entries();

    String label();

    String zippyLogger();
  }

  /**
   * Externalized Interface.
   */
  public interface Resources extends ClientBundle {
    @Source("resources/ZippyLogger.css")
    @Strict
    ZippyLogger.Css zippyLoggerCss();
  }

  // TODO (knorton): Refactor to topspin ScrollPanel.
  private class Entries extends Div {
    private int counter = 0;

    public Entries() {
      super(getContainer());
      setStyleName(css.entries());
      setVisible(false);
    }

    Element createEntry() {
      final String className = (((counter++) & 1) == 1) ? commonCss.even()
          : commonCss.odd();
      final Element elem = Document.get().createElement("div");
      elem.setClassName(className);
      elem.getStyle().setPropertyPx("padding", 4);
      Element thisElem = getElement();
      thisElem.insertBefore(elem, thisElem.getFirstChild());
      return elem;
    }
  }

  // TODO (knorton): refactor to topspin Label.
  private class Label extends Div implements ClickListener {

    public Label() {
      super(getContainer());
      setStyleName(css.label());
      ClickEvent.addClickListener(this, getElement(), this);
      getElement().setInnerHTML("&#x2318; DEBUG LOG");
    }

    public void onClick(ClickEvent event) {
      setExpanded(!isExpanded());
    }
  }

  private static ZippyLogger INSTANCE;

  public static ZippyLogger get() {
    if (INSTANCE == null) {
      ZippyLogger.Resources resources = GWT.create(ZippyLogger.Resources.class);
      StyleInjector.injectStylesheet(resources.zippyLoggerCss().getText());
      INSTANCE = new ZippyLogger(resources.zippyLoggerCss(),
          MonitorResources.getResources().commonCss());
    }
    return INSTANCE;
  }

  private final CommonCss commonCss;
  private final ZippyLogger.Css css;

  private final Entries entries;

  private boolean expanded = false;

  private ZippyLogger(ZippyLogger.Css css, CommonCss commonCss) {
    super(Root.getContainer());
    this.css = css;
    this.commonCss = commonCss;
    new Label();
    entries = new Entries();
    setStyleName(css.zippyLogger());
    setExpanded(expanded);

    this.setVisible(true);
  }

  public void logHtml(String html) {
    entries.createEntry().setInnerHTML(html);
  }

  public void logText(String text) {
    entries.createEntry().setInnerText(text);
  }

  private Container getContainer() {
    return new DefaultContainerImpl(getElement());
  }

  private boolean isExpanded() {
    return expanded;
  }

  private void setExpanded(boolean expanded) {
    this.expanded = expanded;
    entries.setVisible(expanded);
  }

}
