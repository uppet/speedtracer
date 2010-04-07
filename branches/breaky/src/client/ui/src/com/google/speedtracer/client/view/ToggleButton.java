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

import com.google.gwt.resources.client.CssResource;
import com.google.gwt.topspin.ui.client.Button;
import com.google.gwt.topspin.ui.client.ClickEvent;
import com.google.gwt.topspin.ui.client.ClickListener;
import com.google.gwt.topspin.ui.client.Container;

/**
 * A button widget that provides toggling behavior.
 */
public class ToggleButton extends Button {

  /**
   * Css stylename declarations for {@link ToggleButton}.
   */
  public interface Css extends CssResource {
    String toggleButtonDown();
  }

  private boolean down;
  private final ToggleButton.Css css;
  
  public ToggleButton(Container container, Css css) {
    super(container);
    this.css = css;
    addClickListener(new ClickListener() {
      public void onClick(ClickEvent event) {
        toggle();
      }
    });
  }

  public boolean isDown() {
    return down;
  }
  
  public void toggle() {
    down = !down;
    String downStyleName = css.toggleButtonDown();
    if (down) {
      addStyleName(downStyleName);
    } else {
      removeStyleName(downStyleName);
    }
  }
}
