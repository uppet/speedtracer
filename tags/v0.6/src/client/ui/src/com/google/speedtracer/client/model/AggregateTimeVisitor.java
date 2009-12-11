/*
 * Copyright 2008 Google Inc.
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

import com.google.speedtracer.client.model.EventVisitor.PostOrderVisitor;
import com.google.speedtracer.client.util.JSOArray;
import com.google.speedtracer.client.util.JsIntegerDoubleMap;

/**
 * Visitor for aggregating durations spent in each EventRecord Type in the
 * Context Tree for a UiEvent. This is responsible for computing self time and
 * for computing aggregate times spent in the various components of a node.
 * 
 * This must be run in a post order traversal.
 */
public class AggregateTimeVisitor implements PostOrderVisitor {

  private boolean applied = false;
  private final JsIntegerDoubleMap durations;

  private final UiEvent rootEvent;

  public AggregateTimeVisitor(UiEvent rootEvent) {
    this.rootEvent = rootEvent;

    JsIntegerDoubleMap durationMap = rootEvent.getTypeDurations();
    if (durationMap != null) {
      // We have already computed this.
      durations = durationMap;
      applied = true;
    } else {
      durations = JsIntegerDoubleMap.create();
    }
  }

  public boolean alreadyApplied() {
    return applied;
  }

  /**
   * Returns the time spend in the specified type, provided the durations map
   * has been computed and initialized.
   * 
   * @return the time spent in the type
   */
  public final double getTimeSpentInType(int type) {
    if (durations.hasKey(type)) {
      return durations.get(type);
    } else {
      return 0;
    }
  }

  /**
   * Returns the Durations map, or null if this visitor has not been applied. We
   * maintain an applied boolean instead of lazy initializing
   * <code>durations</code> since we don't want to have to ensure that the
   * durations map has been initialized each time we visit a UiEvent.
   * 
   * @return
   */
  public JsIntegerDoubleMap getTypeDurations() {
    if (applied) {
      return durations;
    } else {
      return null;
    }
  }

  public void postProcess() {
    if (applied) {
      return;
    } else {
      applied = true;
      rootEvent.setTypeDurations(durations);
    }
  }

  public void visitUiEvent(UiEvent e) {
    int currType = e.getType();

    if (currType == EventRecordType.AGGREGATED_EVENTS) {
      // This has to be a leaf node.
      LotsOfLittleEvents lolec = e.cast();
      // We compute the self time for this lolevent.
      JSOArray<TypeCountDurationTuple> tuples = lolec.getTypeCountDurationTuples();
      double lolSelfTime = 0;
      // We also keep track of the dominant tuple.
      TypeCountDurationTuple dominantTuple = tuples.get(0);
      for (int i = 0, n = tuples.size(); i < n; i++) {
        TypeCountDurationTuple tuple = tuples.get(i);
        double tupleDuration = tuple.getDuration();
        updateDuration(tuple.getType(), tupleDuration);
        lolSelfTime += tupleDuration;
        // We later cache the type of the max duration aggregate event.
        if (tupleDuration > dominantTuple.getDuration()) {
          dominantTuple = tuple;
        }
      }
      lolec.setSelfTime(lolSelfTime);
      lolec.setDominantEventTypeTuple(dominantTuple);
    } else {
      // Compute the self time of this node and update the time aggregates.
      double duration = e.getDuration();
      double childTime = computeTimeInChildren(e);
      double selfTime = duration - childTime;
      e.setSelfTime(selfTime);
      updateDuration(currType, selfTime);
    }
  }

  private double computeTimeInChildren(UiEvent node) {
    double duration = 0;
    JSOArray<UiEvent> children = node.getChildren();
    for (int i = 0, n = children.size(); i < n; i++) {
      UiEvent child = children.get(i);
      if (child.getType() == EventRecordType.AGGREGATED_EVENTS) {
        duration += child.getSelfTime();
      } else {
        duration += child.getDuration();
      }
    }
    return duration;
  }

  private void updateDuration(int type, double delta) {
    double d = 0;
    if (durations.hasKey(type)) {
      d = durations.get(type);
    }
    durations.put(type, d + delta);
  }
}
