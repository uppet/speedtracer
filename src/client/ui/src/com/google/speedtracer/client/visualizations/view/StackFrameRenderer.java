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
import com.google.speedtracer.client.SymbolServerController;
import com.google.speedtracer.client.SymbolServerService;
import com.google.speedtracer.client.SourceViewer.SourcePresenter;
import com.google.speedtracer.client.SymbolServerController.Resymbolizeable;
import com.google.speedtracer.client.model.JsSymbol;
import com.google.speedtracer.client.util.Url;
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

  // Gets initialized when first rendered.
  private Element myElem;

  private final JsStackFrame stackFrame;

  private final StackTraceRenderer stackTraceRenderer;

  /**
   * Constructor.
   * 
   * @param stackFrame the {@link JsStackFrame} that this will render.
   * @param stackTraceRenderer the {@link StackTraceRenderer} that is rendering
   *          us.
   */
  public StackFrameRenderer(JsStackFrame stackFrame,
      StackTraceRenderer stackTraceRenderer) {
    this.stackFrame = stackFrame;
    this.stackTraceRenderer = stackTraceRenderer;
  }

  public void render(Element parentElem, boolean attemptResymbolization) {
    assert (myElem == null) : "Render called twice for StackFrameRenderer!";

    myElem = parentElem.getOwnerDocument().createDivElement();
    Document document = myElem.getOwnerDocument();

    String resourceName = stackFrame.getResourceUrl().getLastPathComponent();
    resourceName = ("".equals(resourceName))
        ? stackFrame.getResourceUrl().getPath() : resourceName;

    // If we still don't have anything, replace with [unknown]
    String symbolName = (stackFrame.getSymbolName().equals("")) ? "[unknown] "
        : stackFrame.getSymbolName() + "() ";

    myElem.appendChild(document.createTextNode(resourceName + "::"));
    myElem.appendChild(document.createTextNode(symbolName));
    // We make a link out of the line number which should pop open
    // the Source Viewer when clicked.
    AnchorElement lineLink = document.createAnchorElement();
    lineLink.getStyle().setProperty("whiteSpace", "nowrap");
    String columnStr = (stackFrame.getColNumber() > 0) ? " Col "
        + stackFrame.getColNumber() : "";
    lineLink.setInnerText("Line " + stackFrame.getLineNumber() + columnStr);
    lineLink.setHref("javascript:;");
    myElem.appendChild(lineLink);
    myElem.appendChild(document.createBRElement());
    stackTraceRenderer.getListenerManager().manageEventListener(
        ClickEvent.addClickListener(lineLink, lineLink, new ClickListener() {
          public void onClick(ClickEvent event) {
            stackTraceRenderer.getSourceClickListener().onSymbolClicked(
                stackFrame.getResourceUrl().getUrl(),
                stackFrame.getLineNumber(), stackFrame.getColNumber());
          }
        }));

    myElem.setClassName(stackTraceRenderer.getResources().stackFrameRendererCss().stackFrame());
    parentElem.appendChild(myElem);

    if (attemptResymbolization) {
      // Add resymbolized data to frame/profile if it is available.
      SymbolServerController ssController = SymbolServerService.getSymbolServerController(new Url(
          stackTraceRenderer.getCurrentAppUrl()));
      if (ssController != null) {
        ssController.attemptResymbolization(
            stackFrame.getResourceUrl().getUrl(), stackFrame.getSymbolName(),
            this, stackTraceRenderer.getSourcePresenter());
      }
    }
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
    assert (myElem != null) : "Element is null when attempting resymbolization in StackFrameRenderer";

    Document document = myElem.getOwnerDocument();
    AnchorElement symbolLink = document.createAnchorElement();

    symbolLink.setInnerText(sourceSymbol.getSymbolName());
    symbolLink.setHref("javascript:;");
    symbolLink.setClassName(stackTraceRenderer.getResources().stackFrameRendererCss().resymbolizedSymbol());
    myElem.appendChild(symbolLink);
    myElem.appendChild(document.createBRElement());
    stackTraceRenderer.getListenerManager().manageEventListener(
        ClickEvent.addClickListener(symbolLink, symbolLink,
            new ClickListener() {
              public void onClick(ClickEvent event) {
                sourcePresenter.showSource(sourceServer
                    + sourceSymbol.getResourceUrl().getPath(),
                    sourceSymbol.getLineNumber(), 0);
              }
            }));
  }
}
