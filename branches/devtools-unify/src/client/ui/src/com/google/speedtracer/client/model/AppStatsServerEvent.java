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

/**
 * Transforms JSON from server-side traces into a valid {@link ServerEvent} when
 * the JSON is from an AppStats backend.
 * 
 * @see ServerEvent#fromServerJson(NetworkResource, JavaScriptObject)
 */
class AppStatsServerEvent {
  /**
   * An AppStats event representing an AppEngine api call.
   */
  private static final class ApiEvent extends DataBag {
    @SuppressWarnings("all")
    protected ApiEvent() {
    }

    int getDuration() {
      return getIntProperty("duration");
    }

    String getName() {
      return getStringProperty("name");
    }

    int getStartTimeOffset() {
      return getIntProperty("start");
    }

    boolean wasSuccessful() {
      return getBooleanProperty("success");
    }
  }

  /**
   * The root object from an AppStats trace object.
   */
  private static final class RequestEvent extends DataBag {
    static RequestEvent get(JavaScriptObject json) {
      return "appstats".equals(DataBag.getStringProperty(json, "format"))
          ? json.<RequestEvent> cast() : null;
    }

    @SuppressWarnings("all")
    protected RequestEvent() {
    }

    JSOArray<ApiEvent> getApiEvents() {
      return getJSObjectProperty("children");
    }

    int getDuration() {
      return getIntProperty("duration");
    }

    String getHttpMethod() {
      return getStringProperty("http_method");
    }

    String getHttpPath() {
      return getStringProperty("http_path");
    }

    String getHttpQuery() {
      return getStringProperty("http_query");
    }

    // TODO(knorton): This data will be included eventually.
    @SuppressWarnings("unused")
    int getHttpStatus() {
      return getIntProperty("http_status");
    }
  }

  /**
   * Transforms the given object into a valid {@link ServerEvent}.
   * 
   * @see ServerEvent#fromServerJson(NetworkResource, JavaScriptObject)
   * 
   * @param resource
   * @param object
   * @return a server event, null if the object is not AppStats format.
   */
  static ServerEvent fromServerJson(NetworkResource resource,
      JavaScriptObject object) {
    final RequestEvent stats = RequestEvent.get(object);

    if (stats == null) {
      return null;
    }

    final double startTime = resource.getStartTime();
    final ServerEvent event = ServerEvent.create(ServerEvent.TYPE, startTime,
        stats.getDuration(), ServerEvent.createDataBag(createLabel(stats),
            "HTTP"), toServerEvents(stats.getApiEvents(), startTime));
    AggregateTimeVisitor.apply(event);
    return event;
  }

  private static String createLabel(ApiEvent event) {
    return event.wasSuccessful() ? event.getName() : event.getName()
        + " (FAILED)";
  }

  private static String createLabel(RequestEvent request) {
    final String label = request.getHttpMethod() + " " + request.getHttpPath();
    final String query = request.getHttpQuery();
    return (query == null || query.length() == 0) ? label : label + "?" + query;
  }

  private static JSOArray<UiEvent> toServerEvents(JSOArray<ApiEvent> apiEvents,
      double startTime) {
    final JSOArray<UiEvent> uiEvents = JSOArray.create();
    for (int i = 0, n = apiEvents.size(); i < n; ++i) {
      final ApiEvent apiEvent = apiEvents.get(i);
      uiEvents.push(ServerEvent.create(ServerEvent.TYPE, startTime
          + apiEvent.getStartTimeOffset(), apiEvent.getDuration(),
          ServerEvent.createDataBag(createLabel(apiEvent), "API"),
          JSOArray.<UiEvent> create()));
    }
    return uiEvents;
  }

  private AppStatsServerEvent() {
  }
}
