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

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.InputElement;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.topspin.ui.client.ClickEvent;
import com.google.gwt.topspin.ui.client.ClickListener;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.speedtracer.client.model.DataModel;

/**
 * Panel containing checkboxes to toggle stack trace grabbing and CPU profiling.
 */
public class ProfilingOptionsPanel {
  interface MyUiBinder extends UiBinder<DivElement, ProfilingOptionsPanel> {
  }

  /**
   * The topspin widget that we are compositing. We use this to provide
   * automatic hiding when the mouse leaves the panel.
   */
  private class BaseDiv extends AutoHideDiv {
    public BaseDiv(DivElement myElement) {
      super(myElement, HIDE_DELAY);
    }
  }

  private static final int HIDE_DELAY = 1000;

  private static MyUiBinder uiBinder = GWT.create(MyUiBinder.class);

  public static ProfilingOptionsPanel create(Element parent, int startX,
      int startY, DataModel model) {
    ProfilingOptionsPanel options = new ProfilingOptionsPanel(parent, model);
    options.base.getElement().getStyle().setLeft(startX, Unit.PX);
    options.base.getElement().getStyle().setTop(startY, Unit.PX);
    return options;
  }

  @UiField
  InputElement cpuProfilingCheckbox;

  @UiField
  InputElement stackTraceCheckbox;

  private final BaseDiv base;

  private final DataModel model;

  protected ProfilingOptionsPanel(Element parent, DataModel model) {
    DivElement baseElement = uiBinder.createAndBindUi(this);
    parent.appendChild(baseElement);
    this.base = new BaseDiv(baseElement);
    this.model = model;
    sinkEvents();
  }

  public void show() {
    base.show();
  }

  private void sinkEvents() {
    base.addClickListener(new ClickListener() {
      public void onClick(ClickEvent event) {
        model.getDataInstance().setProfilingOptions(
            stackTraceCheckbox.isChecked(), cpuProfilingCheckbox.isChecked());
      }
    });
  }
}
