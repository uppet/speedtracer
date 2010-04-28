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

package com.google.speedtracer.server;

import com.google.json.serialization.JsonArray;
import com.google.json.serialization.JsonException;
import com.google.json.serialization.JsonObject;

import java.io.IOException;
import java.io.InputStreamReader;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Used for debugging purposes only. This servlet accepts URLs of the form
 * /duration and it returns a JSON value of a Spring Insight trace scaled to
 * duration.
 */
@SuppressWarnings("serial")
public class MockServerSideTracesServlet extends HttpServlet {

  private static JsonObject TEMPLATE_DATA = loadTemplateData("mock-server-side-trace.json");

  private static long getRangeDuration(JsonObject json) {
    return json.get("duration").asNumber().getInteger();
  }

  private static long getRangeStartTime(JsonObject json) {
    return json.get("start").asNumber().getInteger();
  }

  private static JsonObject loadTemplateData(String name) {
    final InputStreamReader reader = new InputStreamReader(
        MockServerSideTracesServlet.class.getResourceAsStream(name));
    try {
      return JsonObject.parse(reader);
    } catch (JsonException e) {
      throw new RuntimeException(e);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private static void scaleRange(JsonObject json, long rootStartTime,
      double scaleFactor) {
    assert json.get("duration").asNumber() != null;
    assert json.get("start").asNumber() != null;

    final long oldStartTime = getRangeStartTime(json);
    final long oldDuration = getRangeDuration(json);

    final double newStartTime = rootStartTime + (oldStartTime - rootStartTime)
        * scaleFactor;
    final double newDuration = oldDuration * scaleFactor;

    json.put("start", (long) newStartTime);
    json.put("duration", (long) newDuration);
    json.put("end", (long) (newStartTime + newDuration));
  }

  private static JsonObject scaleTrace(JsonObject data, int duration) {
    final JsonObject trace = data.get("trace").asObject();
    // Get the top-level range.
    final JsonObject range = trace.get("range").asObject();

    final long startTime = getRangeStartTime(range);

    // Compute the scaling factor.
    final double scaleFactor = (double) duration
        / (double) getRangeDuration(range);

    // Update the top-level range.
    scaleRange(range, startTime, scaleFactor);

    // Update ranges on the frame stack.
    updateRanges(trace.get("frameStack").asObject(), getRangeStartTime(range),
        scaleFactor);

    return data;
  }

  private static boolean shouldReportTrace(int duration) {
    return duration > 100;
  }

  private static void updateRanges(JsonObject frame, long startTime,
      double scaleFactor) {
    scaleRange(frame.get("range").asObject(), startTime, scaleFactor);
    final JsonArray children = frame.get("children").asArray();
    for (int i = 0, n = children.getLength(); i < n; ++i) {
      updateRanges(children.get(i).asObject(), startTime, scaleFactor);
    }
  }

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse res)
      throws ServletException, IOException {
    // Last part of the path is the duration. (i.e. /32)
    final String durationAsString = req.getPathInfo().substring(1);
    final int duration = Integer.parseInt(durationAsString);
    if (shouldReportTrace(duration)) {
      res.setContentType("application/json");
      scaleTrace(TEMPLATE_DATA.copyDeeply(), duration).write(res.getWriter());
    } else {
      res.sendError(404);
    }
  }

  @Override
  protected void doHead(HttpServletRequest req, HttpServletResponse res)
      throws ServletException, IOException {
    final String durationAsString = req.getPathInfo().substring(1);
    final int duration = Integer.parseInt(durationAsString);
    if (shouldReportTrace(duration)) {
      res.setStatus(200);
    } else {
      res.sendError(404);
    }
  }
}