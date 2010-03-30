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
package com.google.speedtracer.client.util.dom;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Element;
import com.google.gwt.events.client.EventListenerRemover;
import com.google.gwt.topspin.ui.client.ClickEvent;
import com.google.gwt.topspin.ui.client.ClickListener;
import com.google.gwt.topspin.ui.client.MouseOverEvent;
import com.google.gwt.topspin.ui.client.MouseOverListener;
import com.google.speedtracer.client.util.dom.EventCleanup.HasRemovers;

import java.util.ArrayList;
import java.util.List;

/**
 * Boilerplate class to provide a lazy constructor for the underlying DOM
 * element of our table components. Vital for coalescing.
 * 
 * Need to be able to lazily construct DOM elements and lazily hook event
 * listeners. Currently only supports MouseOver and Click Events.
 */
public abstract class LazilyCreateableElement implements HasRemovers {
  protected final EventCleanup eventCleanup;
  private String cssClassName;
  private Element element;
  private final List<LazyEventListenerAttacher<?>> listenerAttachers = new ArrayList<LazyEventListenerAttacher<?>>();

  protected LazilyCreateableElement(String cssClassName) {
    this.cssClassName = cssClassName;
    this.eventCleanup = new EventCleanup();
  }

  /**
   * Appends a CSS class name selector.
   * 
   * @param cssClassName the CSS class name selector we want to append.
   */
  public void addClassName(String cssClassName) {
    this.cssClassName += " " + cssClassName;
  }

  /**
   * Adds a {@link ClickListener} to be hooked later.
   * 
   * @param eventSource not what we actually attach the listener to. But what
   *          gets set as the event source field on the event that eventually
   *          gets dispatched.
   * @param clickListener the listener that handles the event.
   */
  public void addClickListener(Object eventSource, ClickListener clickListener) {
    listenerAttachers.add(new LazyEventListenerAttacher<ClickListener>(
        eventSource, clickListener) {
      @Override
      public void attach(JavaScriptObject eventTarget) {
        eventCleanup.trackRemover(ClickEvent.addClickListener(getEventSource(),
            eventTarget, getListener()));
      }
    });
  }

  /**
   * Adds a {@link MouseOverListener} to be hooked later.
   * 
   * @param eventSource not what we actually attach the listener to. But what
   *          gets set as the event source field on the event that eventually
   *          gets dispatched.
   * @param mouseOverListener the listener that handles the event.
   */
  public void addMouseOverListener(Object eventSource,
      MouseOverListener mouseOverListener) {
    listenerAttachers.add(new LazyEventListenerAttacher<MouseOverListener>(
        eventSource, mouseOverListener) {
      @Override
      public void attach(JavaScriptObject eventTarget) {
        eventCleanup.trackRemover(MouseOverEvent.addMouseOverListener(
            getEventSource(), eventTarget, getListener()));
      }
    });
  }

  public void cleanupRemovers() {
    eventCleanup.cleanupRemovers();
  }

  public String getClassName() {
    return cssClassName;
  }

  /**
   * Will construct underlying DOM element if it is not already constructed.
   * 
   * @param additionalClassNames
   * @return
   */
  public Element getElement() {
    if (element == null) {
      element = createElement();
      element.setClassName(cssClassName);
      hookListeners();
    }
    return element;
  }

  /**
   * Gets a remover object that goes through and unhooks all the event listeners
   * attached to us, if there are any.
   * 
   * @return the {@link EventListenerRemover} that cleans up our listeners
   */
  public EventListenerRemover getRemover() {
    return eventCleanup.getRemover();
  }

  /**
   * Returns <code>true</code> if the element has already been created.
   * 
   * @return <code>true</code> if the element has already been created.
   */
  public boolean isCreated() {
    return element != null;
  }

  /**
   * Sets the CSS class for the element we will eventually create. Blows away
   * any previously set or added class names.
   * 
   * @param cssClassName the class selector name we want to set.
   */
  public void setClassName(String cssClassName) {
    this.cssClassName = cssClassName;
  }
  
  public void trackRemover(EventListenerRemover remover) {
    eventCleanup.trackRemover(remover);
  }

  /**
   * Constructs the DOM structures associated with this Row.
   * 
   * @return
   */
  protected abstract Element createElement();

  private void hookListeners() {
    for (int i = 0, n = listenerAttachers.size(); i < n; i++) {
      listenerAttachers.get(i).attach(element);
    }
    listenerAttachers.clear();
  }
}
