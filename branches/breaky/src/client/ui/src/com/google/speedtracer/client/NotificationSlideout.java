/*
 * Copyright 2009 Google Inc.
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
import com.google.gwt.dom.client.Element;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;

/**
 * Slide out notification widget to be used for application notifications.
 */
public class NotificationSlideout {
  interface MyUiBinder extends UiBinder<DivElement, NotificationSlideout> {
  }

  private static MyUiBinder uiBinder = GWT.create(MyUiBinder.class);

  /**
   * Static factory method. Use this method to get an instance of
   * NotificationSlideout. The parent element must be specified at construction
   * time.
   * 
   * @param parent the parent {@link Element} that will contain this
   *          NotificationSlideout
   * @return freshly created NotificationSlideout.
   */
  public static NotificationSlideout create(Element parent) {
    NotificationSlideout slideout = new NotificationSlideout();
    parent.appendChild(slideout.myElement);

    return slideout;
  }

  @UiField
  DivElement contentElem;

  @UiField
  DivElement borderElem;

  private final DivElement myElement;

  private NotificationSlideout() {
    myElement = uiBinder.createAndBindUi(this);
  }

  public void hide() {
    borderElem.getStyle().setPropertyPx("height", 0);
  }

  /**
   * Sets the content HTML to be the supplied html string.
   * 
   * TODO(jaimeyap): HTML escape the supplied String. Not so important right now
   * since no user input is passed into this function currently. But this could
   * change in the future.
   * 
   * @param html the content to be displayed.
   */
  public void setContentHtml(String html) {
    contentElem.setInnerHTML(html);
  }

  public void setTopOffset(int offset) {
    myElement.getStyle().setPropertyPx("top", offset);
  }

  public void show() {
    borderElem.getStyle().setPropertyPx("height",
        contentElem.getOffsetHeight());
  }
}
