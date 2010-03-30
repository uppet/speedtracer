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

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.topspin.ui.client.KeyDownEvent;
import com.google.gwt.topspin.ui.client.KeyUpEvent;
import com.google.speedtracer.client.HotKey.Handler;

/**
 * Simple abstract base class for creating panels that are bound to hot keys.
 */
public abstract class HotKeyPanel implements Handler {
  private Element element;

  protected HotKeyPanel() {
  }

  public void hide() {
    element.removeFromParent();
    element = null;
  }

  public void onKeyDown(KeyDownEvent event) {
    if (element == null) {
      show(event.getNativeEvent().getTarget().getOwnerDocument());
    } else {
      hide();
    }
  }

  public void onKeyUp(KeyUpEvent event) {
  }

  protected abstract Element createContentElement(Document document);

  protected abstract void populateContent(Element contentElement);

  private void show(Document document) {
    Element elem = createContentElement(document);
    populateContent(elem);
    element = document.getBody().appendChild(elem);
  }
}
