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
package com.google.speedtracer.client.model;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.speedtracer.client.model.DevToolsDataInstance.DevToolsDataProxy;
import com.google.speedtracer.client.model.EventVisitor.PreOrderVisitor;

/**
 * This class needs to be the first visitor that runs over the context trees. It
 * is responsible for eagerly normalizing all our times and transforming Raw
 * event records into proper {@link UiEvent}s.
 */
public class TimeNormalizerVisitor implements PreOrderVisitor {
  /**
   * An EventRecord that has not yet been time normalized.
   */
  public static class RawEvent extends JavaScriptObject {
    protected RawEvent() {
    }

    public final native double getStartTime() /*-{
      return this.startTime;
    }-*/;

    private native EventRecord convertToEventRecord(double baseTime) /*-{
      if (this.hasOwnProperty("endTime")) {
        this.duration = this.endTime - this.startTime;  
      }
      
      this.time = this.startTime - baseTime;

      delete this.startTime;
      delete this.endTime;

      return this;
    }-*/;
  }

  private final DevToolsDataProxy proxy;

  TimeNormalizerVisitor(DevToolsDataProxy proxy) {
    this.proxy = proxy;
  }

  public void postProcess() {
  }

  public void visitUiEvent(UiEvent e) {
    convertRawEvent(e.<RawEvent> cast());
  }

  private void convertRawEvent(RawEvent rawEvent) {
    if (proxy.getBaseTime() < 0) {
      proxy.setBaseTime(rawEvent.getStartTime());
    }

    rawEvent.convertToEventRecord(proxy.getBaseTime());
  }
}
