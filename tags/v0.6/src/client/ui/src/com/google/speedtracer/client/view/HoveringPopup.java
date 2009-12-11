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

import com.google.gwt.animation.client.Animation;
import com.google.gwt.dom.client.Element;
import com.google.gwt.events.client.Event;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.resources.client.CssResource.Strict;
import com.google.gwt.resources.client.ImageResource.ImageOptions;
import com.google.gwt.resources.client.ImageResource.RepeatStyle;
import com.google.gwt.topspin.client.Command;
import com.google.gwt.topspin.ui.client.ClickEvent;
import com.google.gwt.topspin.ui.client.ClickListener;
import com.google.gwt.topspin.ui.client.Container;
import com.google.gwt.topspin.ui.client.Div;
import com.google.gwt.topspin.ui.client.MouseDownEvent;
import com.google.gwt.topspin.ui.client.MouseDownListener;
import com.google.speedtracer.client.util.dom.DocumentExt;
import com.google.speedtracer.client.util.dom.MouseCaptureListener;
import com.google.speedtracer.client.util.dom.MouseEventCapture;

/**
 * Reusable Hovering Popup with close button.
 */
public class HoveringPopup extends Div implements MouseDownListener {

  /**
   * CSS.
   */
  public interface Css extends CssResource {
    String closeButton();

    String content();

    String hoveringPopup();

    String titleText();
  }

  /**
   * Provides content for the message body of the popup.
   */
  public interface PopupContentProvider {
    String getPopupContent();

    String getPopupTitle();
  }

  /**
   * Externalized interface.
   */
  public interface Resources extends ClientBundle {
    @Source("resources/close_button.png")
    @ImageOptions(repeatStyle = RepeatStyle.None)
    ImageResource closeButton();

    @Source("resources/HoveringPopup.css")
    @Strict
    HoveringPopup.Css hoveringPopupCss();

    @Source("resources/content-box-header.png")
    @ImageOptions(repeatStyle = RepeatStyle.Horizontal)
    ImageResource popupHeader();
  }

  /**
   * The capture mouse listener for events that we want to use capture on.
   */
  private class CaptureMouseListener extends MouseCaptureListener {

    @Override
    public void onMouseMove(Event evt) {
      if (!isDragging) {
        return;
      }
      getElement().getStyle().setPropertyPx("left", evt.getClientX() - offsetX);
      getElement().getStyle().setPropertyPx("top", evt.getClientY() - offsetY);
      evt.preventDefault();
    }

    @Override
    public void onMouseUp(Event evt) {
      isDragging = false;
      super.onMouseUp(evt);
    }
  }

  private class CustomAnimation extends Animation {

    private Element element;
    private double endLeft = 200;
    private double endTop = 100;
    private boolean shouldHide = false;
    private double startLeft = 0;
    private double startTop = 0;

    public CustomAnimation(Element target) {
      element = target;
    }

    public void hide() {
      endTop = element.getAbsoluteTop();
      endLeft = element.getAbsoluteLeft();
      shouldHide = true;
      run(200);
    }

    @Override
    public void onCancel() {
    }

    @Override
    public void onComplete() {
      if (shouldHide) {
        Command.defer(new Command() {
          @Override
          public void execute() {
            element.getStyle().setProperty("display", "none");
          }
        }, 100);
      } else {
        element.getStyle().setPropertyPx("width", (int) width);
        element.getStyle().setPropertyPx("top", (int) endTop);
        element.getStyle().setPropertyPx("left", (int) endLeft);
        contentElem.getStyle().setProperty("display", "block");
        title.getStyle().setProperty("display", "block");
        element.getStyle().setPropertyPx("height",
            contentElem.getOffsetHeight() + title.getOffsetHeight());
      }
    }

    @Override
    public void onStart() {
    }

    @Override
    public void onUpdate(double progress) {
      double deltaY = (endTop - startTop) * progress;
      double deltaX = (endLeft - startLeft) * progress;
      if (shouldHide) {
        element.getStyle().setPropertyPx("width",
            20 + (int) (width - (progress * width)));
        element.getStyle().setPropertyPx("height",
            (int) (height - (progress * height)));
        element.getStyle().setPropertyPx("top", (int) (endTop - deltaY));
        element.getStyle().setPropertyPx("left", (int) (endLeft - deltaX));
      } else {
        element.getStyle().setPropertyPx("width", (int) (progress * width));
        element.getStyle().setPropertyPx("height", (int) (progress * height));
        element.getStyle().setPropertyPx("top", (int) (startTop + deltaY));
        element.getStyle().setPropertyPx("left", (int) (startLeft + deltaX));
      }
    }

    public void show(int startX, int startY) {
      element.getStyle().setPropertyPx("left", startX);
      element.getStyle().setPropertyPx("top", startY);
      startTop = startY;
      startLeft = startX;
      shouldHide = false;
      getElement().getStyle().setProperty("display", "block");
      run(200);
    }
  }

  private final CustomAnimation animation;
  private final Element contentElem;
  private PopupContentProvider contentProvider;
  private double height = 200;
  private boolean isDragging = false;
  private int offsetX, offsetY;
  private final Element title;
  private double width = 300;

  public HoveringPopup(Container myContainer, HoveringPopup.Resources resources) {
    super(myContainer);
    HoveringPopup.Css css = resources.hoveringPopupCss();

    final Element elem = getElement();
    elem.setClassName(css.hoveringPopup());

    title = DocumentExt.get().createDivWithClassName(css.titleText());
    final Element closeButton = DocumentExt.get().createDivWithClassName(
        css.closeButton());
    contentElem = DocumentExt.get().createDivWithClassName(css.content());
    elem.appendChild(title);
    elem.appendChild(closeButton);
    elem.appendChild(contentElem);
    animation = new CustomAnimation(elem);
    // sink events
    MouseDownEvent.addMouseDownListener(this, title, this);
    ClickEvent.addClickListener(closeButton, closeButton, new ClickListener() {

      public void onClick(ClickEvent event) {
        hide();
      }

    });
  }

  public Element getContentElement() {
    return contentElem;
  }

  public void hide() {
    title.getStyle().setProperty("display", "none");
    contentElem.getStyle().setProperty("display", "none");
    animation.hide();
  }

  public void onMouseDown(MouseDownEvent event) {
    Element elem = getElement();
    final Event e = event.getNativeEvent();

    offsetX = e.getClientX() - elem.getAbsoluteLeft();
    offsetY = e.getClientY() - elem.getAbsoluteTop();

    isDragging = true;
    // sink capture events
    MouseEventCapture.capture(new CaptureMouseListener());
    e.preventDefault();
  }

  public void setContentProvider(PopupContentProvider provider) {
    contentProvider = provider;
  }

  public void setTitle(String titleText) {
    title.setInnerText(titleText);
  }

  /**
   * Sets the width of the dialog the next time it is popped up.
   */
  public void setWidth(int width) {
    this.width = width;
  }

  public void show(int startX, int startY) {
    if (contentProvider != null) {
      title.setInnerText(contentProvider.getPopupTitle());
      contentElem.setInnerHTML(contentProvider.getPopupContent());
    }
    animation.show(startX, startY);
  }
  
  public void show(String title, Element content, int startX, int startY) {
    this.title.setInnerText(title);
    contentElem.setInnerHTML("");
    contentElem.appendChild(content);
    animation.show(startX, startY);
  }
  
  public void show(String title, String contentHtml, int startX, int startY) {
    this.title.setInnerText(title);
    contentElem.setInnerHTML(contentHtml);
    animation.show(startX, startY);
  }

}
