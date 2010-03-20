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
package com.google.speedtracer.client.model;

import com.google.gwt.topspin.ui.client.Button;
import com.google.gwt.topspin.ui.client.ClickListener;
import com.google.gwt.topspin.ui.client.Container;
import com.google.speedtracer.client.util.dom.EventCleanup;

/**
 * Class used to encapsulate the look and the behavior of a
 * {@link com.google.gwt.topspin.ui.client.Button}.
 */
public class ButtonDescription {
  private final ClickListener behavior;
  private final String cssClassSelector;
  private final String toolTip;

  public ButtonDescription(String toolTip, String cssClassSelector,
      ClickListener behavior) {
    this.toolTip = toolTip;
    this.cssClassSelector = cssClassSelector;
    this.behavior = behavior;
  }

  /**
   * Converts this ButtonDescription to an instance of {@link Button}.
   * 
   * @param parent the parent {@link Container} that we will attach this
   *          {@link Button} to.
   * @param cleanup the {@link EventCleanup} object that owns the remover object
   *          returned when hooking up the {@link ClickListener}.
   * @return the newly created {@link Button}.
   */
  public Button createButton(Container parent, EventCleanup cleanup) {
    Button button = new Button(parent);
    button.getElement().setClassName(cssClassSelector);
    button.getElement().setAttribute("title", toolTip);
    cleanup.trackRemover(button.addClickListener(behavior));
    return button;
  }

  public ClickListener getBehavior() {
    return behavior;
  }

  public String getCssSelector() {
    return cssClassSelector;
  }

  public String getToolTip() {
    return toolTip;
  }
}
