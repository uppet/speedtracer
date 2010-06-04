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
package com.google.speedtracer.client.visualizations.view;

import com.google.gwt.dom.client.Element;
import com.google.gwt.events.client.EventListenerRemover;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.topspin.ui.client.ClickEvent;
import com.google.gwt.topspin.ui.client.ClickListener;
import com.google.gwt.topspin.ui.client.Container;
import com.google.gwt.topspin.ui.client.Widget;
import com.google.speedtracer.client.util.dom.DocumentExt;

import java.util.ArrayList;
import java.util.List;

/**
 * An imitation of some of the features of Apple's Scope Bar.
 * 
 * http://developer.apple.com/documentation/UserExperience/Conceptual/AppleHIGuidelines/XHIGWindows/XHIGWindows.html#//apple_ref/doc/uid/20000961-SW3
 * 
 */
public class ScopeBar extends Widget {

  /**
   * Css.
   */
  public interface Css extends CssResource {
    String scopeBar();

    String scopeBarButton();

    String scopeBarButtonSelected();
  }

  /**
   * Externalized interface.
   */
  public interface Resources extends ClientBundle {

    @Source("resources/ScopeBar.css")
    ScopeBar.Css scopeBarCss();
  }

  // TODO(zundel): maybe there is a way to make the widget w/o keeping this list
  private final List<Element> buttons = new ArrayList<Element>();
  private final List<ClickListener> callbacks = new ArrayList<ClickListener>();
  private final Css css;
  private EventListenerRemover remover;

  public ScopeBar(Container parent, ScopeBar.Resources resources) {
    super(DocumentExt.get().createDivElement(), parent);
    css = resources.scopeBarCss();
    getElement().setClassName(css.scopeBar());

    remover = ClickEvent.addClickListener(this, getElement(),
        new ClickListener() {
          public void onClick(ClickEvent event) {
            Element elem = event.getNativeEvent().getTarget();
            for (int i = 0, j = buttons.size(); i < j; ++i) {
              if (buttons.get(i).equals(elem)) {
                setSelected(elem, false);
                callbacks.get(i).onClick(event);
                return;
              }
            }
          }
        });
  }

  /**
   * Adds a button to the ScopeBar.
   * 
   * @param name the string to display in the button
   * @param listener callback to invoke when the button is pressed.
   */
  public Element add(String name, ClickListener listener) {
    Element button = DocumentExt.get().createDivElement();
    button.setClassName(css.scopeBarButton());
    button.setInnerText(name);
    getElement().appendChild(button);
    buttons.add(button);
    callbacks.add(listener);
    return button;
  }

  /**
   * Call this to free resources associated with the widget.
   */
  public void destroy() {
    remover.remove();
  }

  /**
   * Set the currently selected element.
   * 
   * @param elem value returned from {@link #add(String, ClickListener)}
   * @param runCallback if true, the callback is fired (with a null event
   *          argument)
   */
  public void setSelected(Element elem, boolean runCallback) {
    for (int i = 0, j = buttons.size(); i < j; ++i) {
      Element button = buttons.get(i);
      if (elem.equals(button)) {
        button.setClassName(css.scopeBarButton() + " "
            + css.scopeBarButtonSelected());
        if (runCallback) {
          callbacks.get(i).onClick(null);
        }
      } else {
        button.setClassName(css.scopeBarButton());
      }
    }
  }

  /**
   * Set the currently selected element by name.
   * 
   * @param name the name of the scope bar button to fire.
   * @param runCallback if true, the callback is fired (with a null event
   *          argument)
   */
  public void setSelected(String name, boolean runCallback) {
    for (int i = 0, j = buttons.size(); i < j; ++i) {
      Element button = buttons.get(i);
      if (name.equals(button.getInnerText())) {
        button.setClassName(css.scopeBarButton() + " "
            + css.scopeBarButtonSelected());
        if (runCallback) {
          callbacks.get(i).onClick(null);
        }
      } else {
        button.setClassName(css.scopeBarButton());
      }
    }
  }
}
