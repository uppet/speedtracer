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

import com.google.gwt.dom.client.TableCellElement;
import com.google.speedtracer.client.SourceViewer.SourcePresenter;
import com.google.speedtracer.client.util.dom.ManagesEventListeners;
import com.google.speedtracer.client.visualizations.model.JsStackTrace;
import com.google.speedtracer.client.visualizations.model.JsStackTrace.JsStackFrame;

import java.util.List;

/**
 * Takes in a backtrace String and renders a stack trace to a table cell.
 * 
 * TODO (jaimeyap): When we land stack traces, change this to support
 */
public class StackTraceRenderer implements CellRenderer {
  private final String backTrace;

  private final String currentAppUrl;

  private final ManagesEventListeners listenerManager;

  private final StackFrameRenderer.Resources resources;

  private final SourceSymbolClickListener sourceClickListener;

  private final SourcePresenter sourcePresenter;

  private final boolean attemptResymbolization;

  StackTraceRenderer(String backTrace,
      SourceSymbolClickListener sourceClickListener,
      ManagesEventListeners listenerManager, String currentAppUrl,
      SourcePresenter sourcePresenter, boolean attemptResymbolization,
      StackFrameRenderer.Resources resources) {
    this.backTrace = backTrace;
    this.sourceClickListener = sourceClickListener;
    this.listenerManager = listenerManager;
    this.currentAppUrl = currentAppUrl;
    this.sourcePresenter = sourcePresenter;
    this.attemptResymbolization = attemptResymbolization;
    this.resources = resources;
  }

  public String getBackTrace() {
    return backTrace;
  }

  public String getCurrentAppUrl() {
    return currentAppUrl;
  }

  public ManagesEventListeners getListenerManager() {
    return listenerManager;
  }

  public StackFrameRenderer.Resources getResources() {
    return resources;
  }

  public SourceSymbolClickListener getSourceClickListener() {
    return sourceClickListener;
  }

  public SourcePresenter getSourcePresenter() {
    return sourcePresenter;
  }

  public void render(TableCellElement cell) {
    JsStackTrace stackTrace = JsStackTrace.create(backTrace);
    List<JsStackFrame> frames = stackTrace.getFrames();
    for (int i = 0, n = frames.size(); i < n; i++) {
      final JsStackFrame frame = frames.get(i);
      final StackFrameRenderer frameRenderer = new StackFrameRenderer(frame,
          this);
      frameRenderer.render(cell, attemptResymbolization);
    }
  }
}
