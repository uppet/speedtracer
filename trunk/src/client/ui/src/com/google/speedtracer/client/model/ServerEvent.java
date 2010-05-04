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

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.coreext.client.DataBag;
import com.google.gwt.coreext.client.JSOArray;
import com.google.speedtracer.shared.EventRecordType;

/**
 * A {@link UiEvent} representing a trace that was fetched from a server. This
 * type of event is different from other {@link UiEvent}s in that it only
 * contains other {@link ServerEvent}s in the event tree.
 */
public final class ServerEvent extends UiEvent {

  /**
   * The {@link DataBag} object for a {@link ServerEvent}.
   */
  public static final class Data extends DataBag {
    protected Data() {
    }

    public String getApplicationUrl() {
      return getStringProperty("applicationUrl");
    }

    public String getEndPointUrl() {
      return getStringProperty("endPointUrl");
    }

    public String getLabel() {
      return getStringProperty("label");
    }

    public String getTraceViewUrl() {
      return getStringProperty("traceUrl");
    }

    public String getType() {
      return getStringProperty("type");
    }

    void setApplicationUrl(String url) {
      setProperty("applicationUrl", url);
    }

    void setEndPointUrl(String url) {
      setProperty("endPointUrl", url);
    }

    void setTraceViewUrl(String url) {
      setProperty("traceUrl", url);
    }

    private native void setProperty(String name, String value) /*-{
      this[name] = value;
    }-*/;
  }

  public static final int TYPE = EventRecordType.SERVER_EVENT;

  public static ServerEvent fromServerJson(NetworkResource resource,
      JavaScriptObject object) {
    final ServerEvent appStatsEvent = AppStatsServerEvent.fromServerJson(
        resource, object);
    if (appStatsEvent != null) {
      return appStatsEvent;
    }

    return SpringInsightServerEvent.fromServerJson(resource, object);
  }

  static native ServerEvent create(int type, double time, double duration,
      JavaScriptObject data, JSOArray<UiEvent> children) /*-{
    return {
      "type" : type,
      "time" : time,
      "data" : data,
      "duration" : duration,
      "children" : children
    };
  }-*/;

  static native JavaScriptObject createDataBag(String label, String type) /*-{
    return {
      "type" : type,
      "label" : label
    };
  }-*/;

  protected ServerEvent() {
  }

  /**
   * A type correct accessor for this event's {@link DataBag}.
   * 
   * @return
   */
  public native Data getServerEventData() /*-{
    return this.data;
  }-*/;
}
