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
import com.google.gwt.coreext.client.JSOArray;
import com.google.speedtracer.shared.EventRecordType;

/**
 * Updates the hasUserLogs flags on a {@link UiEvent}.
 */
public class LogMessageVisitor {

  /**
   * A threshold indicating the maximum number of children with logs that we
   * will tolerate before we actually mark the parent event as not having logs.
   * This is a safe guard against trying to auto-open large trees.
   */
  private static final int CHILDREN_WITH_LOGS_THRESHOLD = 20;

  private static class Visitor implements UiEvent.LeafFirstTraversalNumber {
    public double visit(UiEvent event, JsArrayNumber values) {
      final JSOArray<UiEvent> children = event.getChildren();
      double count = 0;
      for (int i = 0, n = values.length(); i < n; ++i) {
        final double value = values.get(i);
        count += value;

        // If we pass the threshold for the number of children with logs, we
        // stop marking them as having logs.
        if (count > CHILDREN_WITH_LOGS_THRESHOLD) {
          break;
        }

        if (value > 0) {
          children.get(i).setHasUserLogs(true);
        }
      }
      return (event.getType() == EventRecordType.LOG_MESSAGE_EVENT || count > 0)
          ? 1 : 0;
    }
  }

  /**
   * Applies this visitor to the {@link UiEvent}. This will return immediately
   * if this visitor was previously run on this event.
   * 
   * @param event
   */
  public static void apply(UiEvent event) {
    if (event.hasBeenCheckedForLogs()) {
      return;
    }
    double count = event.apply(new Visitor());
    event.setHasUserLogs(count > 0);
  }
}
