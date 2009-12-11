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
package com.google.speedtracer.client.view;

import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Element;
import com.google.gwt.events.client.EventListenerRemover;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.resources.client.CssResource.Strict;
import com.google.gwt.topspin.ui.client.ClickEvent;
import com.google.gwt.topspin.ui.client.ClickListener;
import com.google.gwt.topspin.ui.client.Container;
import com.google.gwt.topspin.ui.client.Widget;
import com.google.speedtracer.client.util.dom.DocumentExt;

import java.util.ArrayList;
import java.util.List;

/**
 * For the header of a table that needs to provide an UI to allow sorting.
 */
public class SortableTableHeader extends Widget {

  /**
   * CSS.
   */
  public interface Css extends CssResource {
    String arrow();

    String arrowAscending();

    String arrowDescending();

    String headerOuterDiv();

    String text();
  }

  /**
   * Externalized interface.
   */
  public interface Resources extends ClientBundle {

    @Source("resources/arrow-ascending.png")
    ImageResource arrowAscending();

    @Source("resources/arrow-descending.png")
    ImageResource arrowDescending();

    @Source("resources/SortableTableHeader.css")
    @Strict
    Css sortableTableHeaderCss();
  }

  /**
   * Fires when the state of the header changes from ascending to descending or
   * vice-versa.
   */
  public static interface SortToggleListener {
    void onSortToggle(boolean isAscending);
  }

  private final Css css;
  private final List<EventListenerRemover> removers = new ArrayList<EventListenerRemover>();
  private List<SortToggleListener> sortToggleListeners = new ArrayList<SortToggleListener>();
  private final DivElement sortIndicator;

  public SortableTableHeader(Container parentContainer, String text,
      SortableTableHeader.Resources resources) {
    super(DocumentExt.get().createDivElement(), parentContainer);
    css = resources.sortableTableHeaderCss();
    getElement().setClassName(css.headerOuterDiv());

    // First div is to create the text field
    Element textElement = DocumentExt.get().createDivWithClassName(css.text());
    textElement.setInnerText(text);
    getElement().appendChild(textElement);

    // Second div is an image of an arrow for sort on/off ascending/descending
    sortIndicator = DocumentExt.get().createDivWithClassName(css.arrow());
    getElement().appendChild(sortIndicator);

    // Add a click handler to grab sorting for this column and to
    // toggle the ascending/descending header
    removers.add(ClickEvent.addClickListener(this, getElement(),
        new ClickListener() {
          public void onClick(ClickEvent event) {
            toggleSortability();
          }
        }));
  }

  public void addSortToggleListener(SortToggleListener listener) {
    sortToggleListeners.add(listener);
  }

  /**
   * Removes event handlers from this object. Needed to avoid memory leaks after
   * you are finished using this header.
   */
  public void detachEventListeners() {
    for (int i = 0, n = removers.size(); i < n; i++) {
      removers.get(i).remove();
    }
    removers.clear();
  }

  /**
   * Returns <code>true</code> if the header state is set to ascending.
   */
  public boolean isAscending() {
    return sortIndicator.getClassName().contains(css.arrowAscending());
  }

  /**
   * Returns <code>true</code> if the header state is set to descending.
   */
  public boolean isDescending() {
    return sortIndicator.getClassName().contains(css.arrowDescending());
  }

  public void removeSortableHeaderListener(SortToggleListener listener) {
    sortToggleListeners.remove(listener);
  }

  public void setAscending() {
    sortIndicator.setClassName(css.arrow() + " " + css.arrowAscending());
    fireSortableHeaderListeners(true);
  }

  public void setDescending() {
    sortIndicator.setClassName(css.arrow() + " " + css.arrowDescending());
    fireSortableHeaderListeners(false);
  }

  public void setNotSorting() {
    sortIndicator.setClassName(css.arrow());
    // No need to fire anything.
  }

  private void fireSortableHeaderListeners(boolean isAscending) {
    int length = sortToggleListeners.size();
    for (int i = 0; i < length; ++i) {
      sortToggleListeners.get(i).onSortToggle(isAscending);
    }
  }

  /**
   * Toggles the state of the header and fires off an event.
   */
  private void toggleSortability() {
    if (isDescending()) {
      setAscending();
    } else {
      setDescending();
    }
  }
}
