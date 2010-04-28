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
import com.google.speedtracer.client.util.Url;

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

    public String getType() {
      return getStringProperty("type");
    }

    private void setApplicationUrl(String url) {
      setProperty("applicationUrl", url);
    }

    private void setEndPointUrl(String url) {
      setProperty("endPointUrl", url);
    }

    private native void setProperty(String name, String value) /*-{
      this[name] = value;
    }-*/;
  }

  /**
   * Spring Insight has a notion of a Frame which is simliar to our UiEvent.
   * This is a simple representation of that structure.
   * 
   */
  private static final class Frame extends DataBag {
    @SuppressWarnings("all")
    protected Frame() {
    }

    JSOArray<Frame> getChildren() {
      return getJSObjectProperty("children");
    }

    Operation getOperation() {
      return getJSObjectProperty("operation");
    }

    Range getRange() {
      return getJSObjectProperty("range");
    }
  }

  /**
   * A Spring Insight JSON object. An Operation is contained within a
   * {@link Frame}.
   */
  private static final class Operation extends DataBag {
    @SuppressWarnings("all")
    protected Operation() {
    }

    String getLabel() {
      return getStringProperty("label");
    }

    String getType() {
      return getStringProperty("type");
    }
  }

  private static final class Range extends DataBag {
    @SuppressWarnings("all")
    protected Range() {
    }

    int getDuration() {
      return getIntProperty("duration");
    }

    double getStart() {
      return getDoubleProperty("start");
    }
  }

  private static final class Resources extends DataBag {
    @SuppressWarnings("all")
    protected Resources() {
    }

    public String getApplicationUrl() {
      return getStringProperty("Application");
    }

    public String getEndPointUrl() {
      return getStringProperty("Application.EndPoint");
    }
  }

  /**
   * The top-level object in a Spring Insight JSON object.
   */
  private static final class Trace extends DataBag {
    static Trace getTrace(JavaScriptObject root) {
      return DataBag.getJSObjectProperty(root, "trace");
    }

    @SuppressWarnings("all")
    protected Trace() {
    }

    Frame getFrameStack() {
      return getJSObjectProperty("frameStack");
    }

    Range getRange() {
      return getJSObjectProperty("range");
    }

    Resources getResources() {
      return getJSObjectProperty("resources");
    }
  }

  public static final int TYPE = EventRecordType.SERVER_EVENT;

  private static void addDataToTopLevelEvent(Data data,
      NetworkResource resource, Trace trace) {
    final Resources resources = trace.getResources();
    if (resources != null) {
      final String origin = new Url(resource.getUrl()).getOrigin();

      final String appUrl = resources.getApplicationUrl();
      if (appUrl != null) {
        data.setApplicationUrl(origin + appUrl);
      }

      final String endPointUrl = resources.getEndPointUrl();
      if (endPointUrl != null) {
        data.setEndPointUrl(origin + endPointUrl);
      }
    }
  }

  /**
   * Transforms a Spring Insight JSON object to a {@link ServerEvent}.
   * 
   * @param resource the network resource associated with the JSON object
   * @param object the Spring Insight JSON object
   * @return a valid server event
   */
  public static ServerEvent fromSpringInsightTrace(NetworkResource resource,
      JavaScriptObject object) {
    final Trace trace = Trace.getTrace(object);
    assert trace != null;
    final ServerEvent event = toServerEvent(resource,
        trace.getRange().getStart(), trace.getFrameStack());
    addDataToTopLevelEvent(event.getServerEventData(), resource, trace);
    AggregateTimeVisitor.apply(event);
    return event;
  }

  private static native ServerEvent create(int type, double time,
      double duration, JavaScriptObject data, JSOArray<UiEvent> children) /*-{
    return {
      "type" : type,
      "time" : time,
      "data" : data,
      "duration" : duration,
      "children" : children
    };
  }-*/;

  private static native JavaScriptObject createDataBag(String label, String type) /*-{
    return {
      "type" : type,
      "label" : label
    };
  }-*/;

  private static ServerEvent toServerEvent(NetworkResource resource,
      double traceStartTime, Frame frame) {
    final JSOArray<Frame> frames = frame.getChildren();
    final JSOArray<UiEvent> events = JSOArray.create();
    for (int i = 0, n = frames.size(); i < n; ++i) {
      events.push(toServerEvent(resource, traceStartTime, frames.get(i)));
    }
    final double startTime = resource.getStartTime();
    final Range range = frame.getRange();
    final Operation operation = frame.getOperation();
    return create(EventRecordType.SERVER_EVENT, startTime
        + (range.getStart() - traceStartTime), range.getDuration(),
        createDataBag(operation.getLabel(), operation.getType()), events);
  }

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
