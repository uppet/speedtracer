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
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.topspin.ui.client.ClickEvent;
import com.google.gwt.topspin.ui.client.ClickListener;
import com.google.speedtracer.client.SourceViewer.SourcePresenter;
import com.google.speedtracer.client.SymbolServerController.Resymbolizeable;
import com.google.speedtracer.client.model.JsSymbol;
import com.google.speedtracer.client.util.dom.ManagesEventListeners;
import com.google.speedtracer.client.visualizations.model.JsStackTrace.JsStackFrame;

/**
 * Simple class with utility methods used to create DOM structure to render
 * {@link com.google.speedtracer.client.visualizations.model.JsStackTrace}
 * instances.
 * 
 * Supports showing both an obfuscated and a re-symbolized stack trace.
 */
public class StackFrameRenderer implements Resymbolizeable {

  /**
   * Styles.
   */
  public interface Css extends CssResource {
    String resymbolizedSymbol();

    String stackFrame();
  }

  /**
   * Externalized Resource interface.
   */
  public interface Resources extends ClientBundle {
    @Source("resources/StackFrameRenderer.css")
    Css stackFrameRendererCss();
  }

  private final Css css;

  private final Element myElem;

  private final JsStackFrame stackFrame;

  private final ManagesEventListeners listenerManager;

  public StackFrameRenderer(Element parent, JsStackFrame stackFrame,
      Resources resources, ManagesEventListeners listenerManager) {
    this.myElem = parent.getOwnerDocument().createDivElement();
    this.stackFrame = stackFrame;
    this.css = resources.stackFrameRendererCss();
    this.listenerManager = listenerManager;
    this.myElem.setClassName(css.stackFrame());
    parent.appendChild(myElem);
  }

  /**
   * Renders the specified stack frame to the parent element.
   * 
   * @param symbolClickHandler The {@link ClickListener} that will handle the
   *          click on the line/col number for the symbol
   */
  public void renderFrame(ClickListener symbolClickHandler) {
    Document document = myElem.getOwnerDocument();
    myElem.setInnerHTML("");

    String resourceName = stackFrame.getResourceName().equals("")
        ? stackFrame.getResourceBase() : stackFrame.getResourceName();

    // If we still don't have anything, replace with [unknown]
    String symbolName = (stackFrame.getSymbolName().equals("")) ? "[unknown] "
        : stackFrame.getSymbolName() + "() ";

    myElem.appendChild(document.createTextNode(resourceName + "::"));
    myElem.appendChild(document.createTextNode(symbolName));
    // We make a link out of the line number which should pop open
    // the Source Viewer when clicked.
    AnchorElement lineLink = document.createAnchorElement();
    lineLink.getStyle().setProperty("whiteSpace", "nowrap");
    lineLink.setInnerText("Line " + stackFrame.getLineNumber() + " Col "
        + stackFrame.getColNumber());
    lineLink.setHref("javascript:;");
    myElem.appendChild(lineLink);
    myElem.appendChild(document.createBRElement());
    listenerManager.manageEventListener(ClickEvent.addClickListener(lineLink,
        lineLink, symbolClickHandler));
  }

  /**
   * Renders the specified stack frame to the parent element, along with a
   * re-symbolized stack frame.
   * 
   * @param sourceServer The source server URL that is needed to display a
   *          relative path for the source file
   * @param sourceSymbol The symbol mapping in the original source for the
   *          function symbol in our stack frame.
   * @param sourcePresenter The {@link SourcePresenter} that will handle
   *          displaying the source of the resymbolized symbol.
   */
  public void reSymbolize(final String sourceServer,
      final JsSymbol sourceSymbol, final SourcePresenter sourcePresenter) {
    Document document = myElem.getOwnerDocument();
    AnchorElement symbolLink = document.createAnchorElement();

    symbolLink.setInnerText(sourceSymbol.getSymbolName());
    symbolLink.setHref("javascript:;");
    symbolLink.setClassName(css.resymbolizedSymbol());
    myElem.appendChild(symbolLink);
    myElem.appendChild(document.createBRElement());
    listenerManager.manageEventListener(ClickEvent.addClickListener(symbolLink,
        symbolLink, new ClickListener() {
          public void onClick(ClickEvent event) {
            sourcePresenter.showSource(sourceServer
                + sourceSymbol.getResourceBase()
                + sourceSymbol.getResourceName(), sourceSymbol.getLineNumber(),
                0);
          }
        }));
  }
}
