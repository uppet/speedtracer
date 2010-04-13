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

import com.google.gwt.core.client.JsArrayNumber;
import com.google.speedtracer.client.util.JsIntegerDoubleMap;

/**
 * Computes self time and type durations for a {@link UiEvent}.
 */
public class AggregateTimeVisitor {
  private static class Visitor implements UiEvent.LeafFirstTraversalNumber {
    private static void updateTypeDuration(JsIntegerDoubleMap typeDurations,
        int type, double duration) {
      typeDurations.put(type, typeDurations.hasKey(type)
          ? typeDurations.get(type) + duration : duration);
    }

    private final JsIntegerDoubleMap typeDurations = JsIntegerDoubleMap.create();

    public double visit(UiEvent event, JsArrayNumber values) {
      double childTime = 0;
      for (int i = 0, n = values.length(); i < n; ++i) {
        childTime += values.get(i);
      }
      final double duration = event.getDuration();
      final double selfTime = duration - childTime;
      event.setSelfTime(selfTime);
      updateTypeDuration(typeDurations, event.getType(), selfTime);
      return duration;
    }
  }

  /**
   * Applies this visitor to the {@link UiEvent}. This will return immediately
   * if this visitor was previously run on this event.
   * 
   * @param event
   */
  public static void apply(UiEvent event) {
    if (event.getTypeDurations() != null) {
      return;
    }
    final Visitor visitor = new Visitor();
    event.apply(visitor);
    event.setTypeDurations(visitor.typeDurations);
  }
}
