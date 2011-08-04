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
package com.google.speedtracer.client.visualizations.model;

import com.google.gwt.coreext.client.JSOArray;
import com.google.gwt.coreext.client.JsIntegerDoubleMap;
import com.google.gwt.coreext.client.JsIntegerDoubleMap.IterationCallBack;
import com.google.gwt.coreext.client.JsStringMap;
import com.google.speedtracer.client.model.AggregateTimeVisitor;
import com.google.speedtracer.client.model.DataDispatcher;
import com.google.speedtracer.client.model.EventRecord;
import com.google.speedtracer.client.model.HintRecord;
import com.google.speedtracer.client.model.NetworkResource;
import com.google.speedtracer.client.model.ResourceRecord;
import com.google.speedtracer.client.model.UiEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Class responsible for collecting and aggregating information within a given
 * window selection.
 */
public class ReportDataCollector {
  /**
   * The data collected during a run of the data collector.
   */
  public class ReportData {
    private final JsIntegerDoubleMap aggregatedTypeDurations;
    private final List<HintRecord> hints;

    public ReportData(JsIntegerDoubleMap aggregatedTypeDurations,
        List<HintRecord> hints) {
      this.aggregatedTypeDurations = (aggregatedTypeDurations == null)
          ? JsIntegerDoubleMap.create() : aggregatedTypeDurations;
      this.hints = (hints == null) ? new ArrayList<HintRecord>() : hints;
    }

    public ReportData combineWith(ReportData data) {
      doAggregation(data.getAggregatedTypeDurations(), aggregatedTypeDurations);
      hints.addAll(data.getHints());
      return this;
    }

    public JsIntegerDoubleMap getAggregatedTypeDurations() {
      return aggregatedTypeDurations;
    }

    public List<HintRecord> getHints() {
      return hints;
    }
  }

  /**
   * UiEvent and Network events have different hint collection schemes. This is
   * a simple utility base class for implementing these different schemes.
   */
  private abstract class Collector {
    private double totalAvailableTime = 0;

    public double getTotalAvailableTime() {
      assert (this.totalAvailableTime >= 0) : "Cannot have negative available time!";

      return this.totalAvailableTime;
    }

    public void setTotalAvailableTime(double totalAvailableTime) {
      this.totalAvailableTime = totalAvailableTime;
    }

    abstract void examineRecord(EventRecord record, JsIntegerDoubleMap out);

    abstract void finishCollection(JsIntegerDoubleMap aggregateDurationsOut,
        List<HintRecord> hintsOut);
  }

  static native UiEvent createUiEvent(int type, double time, double duration,
      JSOArray<HintRecord> hints) /*-{
    return {
      time: time,
      type: type,
      duration: duration,
      hints: hints
    };
  }-*/;

  /**
   * Deep clone of an event trace tree that also does a split of the tree based
   * on some time boundary.
   * 
   * If lessThan is true, the node's start time MUST be less than the boundary
   * time. If lessThan is false, the nodes end time MUST be greater than the
   * boundary time.
   * 
   * @param node The trace tree that we want to split and deep clone.
   * @param time The time boundary.
   * @param lessThan Whether we want the portion of the tree less than the
   *          boundary, or greater than the boundary.
   * @return cloned tree split on a time boundary.
   */
  static UiEvent splitEventTreeOnBoundary(UiEvent node, double time,
      boolean lessThan) {
    assert (node != null) : "This function should never be called with a null node!";

    UiEvent shallowClone = boundedClone(node, time, lessThan);
    // If this node falls outside the splittable bound, it will be null.
    if (shallowClone == null) {
      return null;
    }

    JSOArray<UiEvent> children = node.getChildren();
    for (int i = 0, n = children.size(); i < n; i++) {
      UiEvent child = splitEventTreeOnBoundary(children.get(i), time, lessThan);
      if (child != null) {
        // Ignore children that have been split.
        shallowClone.addChild(child);
      }
    }

    return shallowClone;
  }

  /**
   * Shallow clone of a {@link UiEvent} based on a time bound. It only clones
   * the time, duration, type, and associated hints for the UiEvent.
   */
  private static UiEvent boundedClone(UiEvent node, double time,
      boolean lessThan) {
    double endTime = node.getTime() + node.getDuration();
    if (lessThan) {
      if (node.getTime() >= time) {
        // This node is completely to the right of the bound. We can therefore
        // return null to be used as the terminating condition.
        return null;
      }
      double endBound = Math.min(endTime, time);
      return createUiEvent(node.getType(), node.getTime(), endBound
          - node.getTime(), node.getHintRecords());
    } else {
      if (endTime < time) {
        // This node is completely to the left of the bound.
        return null;
      }
      double startBound = Math.max(node.getTime(), time);
      return createUiEvent(node.getType(), startBound, endTime - startBound,
          node.getHintRecords());
    }
  }

  private static void doAggregation(JsIntegerDoubleMap in,
      final JsIntegerDoubleMap out) {
    in.iterate(new IterationCallBack() {
      public void onIteration(int key, double val) {
        double duration = out.hasKey(key) ? out.get(key) + val : val;
        out.put(key, duration);
      }
    });
  }

  private final DataDispatcher dataDispatcher;

  public ReportDataCollector(DataDispatcher dataDispatcher) {
    this.dataDispatcher = dataDispatcher;
  }

  public ReportData gatherDataWithinWindow(double leftBound, double rightBound) {
    Collector uiEventCollector = new Collector() {
      private List<HintRecord> hints = new ArrayList<HintRecord>();

      @Override
      void examineRecord(EventRecord record, JsIntegerDoubleMap out) {
        UiEvent castedRecord = record.cast();
        AggregateTimeVisitor.apply(castedRecord);
        // Aggregate the type durations. Only UiEvents will have type
        // durations set. We simply ignore events without a duration map.
        JsIntegerDoubleMap typeDurations = castedRecord.getTypeDurations();
        if (typeDurations != null) {
          doAggregation(typeDurations, out);
        }
        setTotalAvailableTime(getTotalAvailableTime()
            - castedRecord.getDuration());
        addHintsFromJSOArray(record.getHintRecords(), hints);
      }

      @Override
      void finishCollection(JsIntegerDoubleMap aggregateDurationsOut,
          List<HintRecord> hintsOut) {
        // Add a wedge for the available time between top level records.
        aggregateDurationsOut.put(-1, getTotalAvailableTime());
        hintsOut.addAll(hints);
      }
    };

    Collector networkEventCollector = new Collector() {
      JsStringMap<JSOArray<HintRecord>> resourceHints = JsStringMap.create();

      @Override
      void examineRecord(EventRecord record, JsIntegerDoubleMap out) {
        // We dont aggregate time, we simply track network resources.
        if (ResourceRecord.isResourceRecord(record)) {
          ResourceRecord resourceRecord = record.cast();
          NetworkResource resource = dataDispatcher.getNetworkEventDispatcher().getResource(
              resourceRecord.getIdentifier());
          if (resource != null) {
            resourceHints.put(resourceRecord.getIdentifier(),
                resource.getHintRecords());
          }
        }
      }

      @Override
      void finishCollection(JsIntegerDoubleMap aggregateDurationsOut,
          final List<HintRecord> hintsOut) {
        // Collect resource related hints that we placed inside the map.
        resourceHints.iterate(new JsStringMap.IterationCallBack<JSOArray<HintRecord>>() {
          public void onIteration(String key, JSOArray<HintRecord> resourceHints) {
            addHintsFromJSOArray(resourceHints, hintsOut);
          }
        });
      }
    };

    // Gather report for UiEvents.
    ReportData uiEventReport = gatherDataWithinWindowImpl(leftBound,
        rightBound, dataDispatcher.getUiEventDispatcher().getEventList(),
        uiEventCollector);

    // Gather report for Network Events.
    ReportData networkEventReport = gatherDataWithinWindowImpl(leftBound,
        rightBound,
        dataDispatcher.getNetworkEventDispatcher().getNetworkEvents(),
        networkEventCollector);

    return uiEventReport.combineWith(networkEventReport);
  }

  private void addHintsFromJSOArray(JSOArray<HintRecord> hintArray,
      List<HintRecord> output) {
    if (hintArray != null) {
      for (int i = 0, n = hintArray.size(); i < n; i++) {
        output.add(hintArray.get(i));
      }
    }
  }

  private ReportData gatherDataWithinWindowImpl(double leftBound,
      double rightBound, List<? extends EventRecord> eventList,
      Collector collector) {
    // Find the index of the last record that falls within the right edge of the
    // selected window bound.
    int numRecords = eventList.size();
    int index = EventRecord.getIndexOfRecord(eventList, rightBound);

    // If we have a bogus insertion index, do nothing.
    if (index <= 0 || index > numRecords) {
      return new ReportData(null, null);
    }

    // The insertion index is always to the right of the record we want to start
    // with. Unless we hit it directly. But if we hit the start time directly
    // with the right edge of the window, we can ignore anyways. So it is always
    // correct to decrement index by 1.
    index = index - 1;

    // Not all records will be UiEvents. But we want access to fields like
    // duration and the type maps. These will return appropriate 0 and null
    // values for non-UiEvent EventRecords.
    UiEvent record = eventList.get(index).cast();
    final List<HintRecord> hints = new ArrayList<HintRecord>();
    final JsIntegerDoubleMap aggregateTypeDurations = JsIntegerDoubleMap.create();

    // We want to add a wedge in the pie chart for the time the browser's UI
    // thread was available.
    collector.setTotalAvailableTime(rightBound - leftBound);

    // We are starting at the right edge of the window, which may chop an event.
    if (UiEvent.isUiEvent(record)) {
      record = splitEventTreeOnBoundary(record, rightBound, true);
      // Guard against having the record split an event outside the window.
      if (record == null) {
        return new ReportData(null, null);
      }
    }

    // We will walk backward to find the left window boundary. Because we can
    // have ResourceUpdate records that have nonsensical time stamps, we place
    // the terminating condition within the loop.
    while (index >= 0) {
      double endTime = record.getTime() + record.getDuration();
      // Our real terminating condition is if the record passes the left edge.
      if (endTime < leftBound) {
        break;
      }
      // Chop records that fall within the window, but start to the left of
      // the leftBound.
      if (UiEvent.isUiEvent(record) && record.getTime() < leftBound
          && endTime >= leftBound) {
        record = splitEventTreeOnBoundary(record, leftBound, false);
        assert (record != null) : "Splitting a node should yield a valid non-null clone here!";
      }

      collector.examineRecord(record, aggregateTypeDurations);

      // Walk backward.
      --index;
      if (index >= 0) {
        record = eventList.get(index).cast();
      }
    }

    collector.finishCollection(aggregateTypeDurations, hints);

    return new ReportData(aggregateTypeDurations, hints);
  }
}
