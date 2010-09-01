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

import com.google.gwt.chrome.crx.client.Chrome;
import com.google.gwt.chrome.crx.client.DevTools;
import com.google.gwt.chrome.crx.client.events.DevToolsPageEvent.Listener;
import com.google.gwt.chrome.crx.client.events.DevToolsPageEvent.PageEvent;
import com.google.gwt.chrome.crx.client.events.Event.ListenerHandle;
import com.google.gwt.core.client.JavaScriptException;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.coreext.client.JSOArray;
import com.google.speedtracer.client.model.ResourceUpdateEvent.UpdateResource;
import com.google.speedtracer.client.model.UiEvent.LeafFirstTraversalVoid;

/**
 * This class is used in Chrome when we are getting data from the devtools API.
 * Its job is to receive data from the devtools API, ensure that the data in
 * properly transformed into a consumable form, and to invoke callbacks passed
 * in from the UI. We use this overlay type as the object we pass to the Monitor
 * UI.
 */
public class DevToolsDataInstance extends DataInstance {
  /**
   * Proxy class that normalizes data coming in from the devtools API into a
   * digestable form, and then forwards it on to the DevToolsDataInstance.
   */
  public static class Proxy implements DataProxy {
    private class TimeNormalizingVisitor implements LeafFirstTraversalVoid {
      public void visit(UiEvent event) {
        assert getBaseTime() >= 0 : "baseTime should already be set.";
        event.<UnNormalizedEventRecord> cast().convertToEventRecord(
            getBaseTime());
      }
    }

    private class TypeEnsuringVisitor implements LeafFirstTraversalVoid {
      public void visit(UiEvent event) {
        event.ensureType();
      }
    }

    /**
     * Simple routing dispatcher used by the DevToolsDataProxy to quickly route.
     */
    static native Dispatcher createDispatcher(Proxy delegate) /*-{
      return {
        addRecordToTimeline: function(event) {
          delegate.@com.google.speedtracer.client.model.DevToolsDataInstance.Proxy::dispatchOnAddRecordToTimeline(Lcom/google/gwt/chrome/crx/client/events/DevToolsPageEvent$PageEvent;)(event);
        },
        updateResource: function(event) {
          delegate.@com.google.speedtracer.client.model.DevToolsDataInstance.Proxy::dispatchOnUpdateResource(Lcom/google/gwt/chrome/crx/client/events/DevToolsPageEvent$PageEvent;)(event);
        }
      };
    }-*/;

    private double baseTime;

    private ResourceWillSendEvent currentPage;

    private DevToolsDataInstance dataInstance;

    private final Dispatcher dispatcher;

    private ListenerHandle listenerHandle;

    private JSOArray<UnNormalizedEventRecord> pendingRecords = JSOArray.create();

    private UiEvent previousEvent;

    private final int tabId;

    private final TimeNormalizingVisitor timeNormalizingVisitor = new TimeNormalizingVisitor();

    private final TypeEnsuringVisitor typeEnsuringVisitor = new TypeEnsuringVisitor();

    public Proxy(int tabId) {
      this.baseTime = -1;
      this.tabId = tabId;
      this.dispatcher = createDispatcher(this);
    }

    public final void dispatchPageEvent(PageEvent event) {
      dispatcher.invoke(event.getMethod(), event);
    }

    public double getBaseTime() {
      return baseTime;
    }

    public void load(DataInstance dataInstance) {
      this.dataInstance = dataInstance.cast();
      connectToDataSource();
    }

    public void resumeMonitoring() {
      connectToDataSource();
    }

    public void setBaseTime(double baseTime) {
      this.baseTime = baseTime;
    }

    public void setProfilingOptions(boolean enableStackTraces,
        boolean enableCpuProfiling) {
      DevTools.setProfilingOptions(tabId, enableStackTraces, enableCpuProfiling);
    }

    public void stopMonitoring() {
      disconnect();
    }

    public void unload() {
      // reset the base time.
      this.baseTime = -1;
      disconnect();
    }

    protected void connectToDataSource() {
      if (this.listenerHandle != null) {
        // DevTools doesn't like the event being connected to more than once.
        return;
      }

      try {
        this.listenerHandle = DevTools.getTabEvents(tabId).getPageEvent().addListener(
            new Listener() {
              public void onPageEvent(PageEvent event) {
                dispatchPageEvent(event);
              }
            });
      } catch (JavaScriptException ex) {
        Chrome.getExtension().getBackgroundPage().getConsole().log(
            "Error attaching to DevTools page event: " + ex);
        // ignore
      }
    }

    void connectToDevTools(final DevToolsDataInstance dataInstance) {
      // Connect to the devtools API as the data source.
      if (this.dataInstance == null) {
        this.dataInstance = dataInstance;
      }
      connectToDataSource();
    }

    private void disconnect() {
      if (listenerHandle != null) {
        listenerHandle.removeListener();
      }

      listenerHandle = null;
    }

    private void dispatchOnAddRecordToTimeline(PageEvent event) {
      onTimeLineRecord(event.getRecord().<UnNormalizedEventRecord> cast());
    }

    private void dispatchOnUpdateResource(PageEvent event) {
      onUpdateResource(event.getResourceId(),
          event.getResource().<UpdateResource> cast());
    }

    /**
     * Establishes a base time if it has not been set and dispatches the event
     * to the {@link DataInstance}.
     * 
     * @param record the already normalized record to dispatch
     */
    private void forwardToDataInstance(EventRecord record) {
      assert (!Double.isNaN(record.getTime())) : "Time was not normalized!";

      // TODO(jaimeyap/knorton): Remove this hack.
      // Workaround for http://code.google.com/p/speedtracer/issues/detail?id=29
      // WebKit sometimes delivers timeline records with a negative timestamp.
      // We simply discard these until this issue can be resolved upstream.
      if (record.getTime() < 0) {
        return;
      }

      previousEvent = record.cast();
      dataInstance.onEventRecord(record);
    }

    /**
     * Establishes a base time if it has not been set and dispatches the event
     * to the {@link DataInstance}.
     * 
     * This method will normalizes times for any record passed in.
     * 
     * @param record the record to dispatch
     */
    private void normalizeAndDispatchEventRecord(UnNormalizedEventRecord record) {
      if (getBaseTime() < 0) {
        sendPendingRecordsAndSetBaseTime(record);
      }

      assert (getBaseTime() >= 0) : "Base Time is still not set";

      // Run a visitor to normalize the times for this tree.
      record.<UiEvent> cast().apply(timeNormalizingVisitor);
      forwardToDataInstance(record);
    }

    /**
     * Normalizes the inputed time to be relative to the base time, and converts
     * the units of the inputed time to milliseconds from seconds.
     */
    private double normalizeTime(double seconds) {
      assert getBaseTime() >= 0 : "NormalizeTime called before a base time was established.";

      double millis = seconds * 1000;
      return millis - getBaseTime();
    }

    private void onTimeLineRecord(UnNormalizedEventRecord record) {
      assert (dataInstance != null) : "Someone called invoke that wasn't our connect call!";

      record.<UiEvent> cast().apply(typeEnsuringVisitor);
      int type = record.getType();

      switch (type) {
        case ResourceWillSendEvent.TYPE:
          // We do not want to immediately assume that a resource start is
          // eligible to establish the base time.
          // If the start actually happened as a child of some event trace, then
          // using this to establish base time could lead to negative times for
          // events since all network resource events are short circuited.
          // We buffer it for now and wait for an event that is not a Resource
          // Start to make the decision as to what should be the base time.
          if (getBaseTime() < 0) {
            pendingRecords.push(record);
            return;
          }

          // Maybe synthesize a page transition.
          ResourceWillSendEvent start = record.cast();
          // Dispatch a Page Transition if this is a main resource and we are
          // not part of a redirect.
          if (start.isMainResource()) {
            // For redirects, IDs get recycled. We do not want to double page
            // transition for a single main page redirect.
            if ((currentPage == null)
                || (currentPage.getIdentifier() != start.getIdentifier())) {
              // We synthesize the page transition if currentPage is not set, or
              // the IDs dont match.
              currentPage = start;
              normalizeAndDispatchEventRecord(TabChangeEvent.createUnNormalized(
                  record.getStartTime(), start.getUrl()));
            } else if (currentPage.getIdentifier() == start.getIdentifier()) {
              // IDs get recycled across pages. So remember to null out the
              // current page after concluding that the redirect has completed
              currentPage = null;
            }
          }
          break;
        case ResourceResponseEvent.TYPE:
          // For pages with no redirect, we want to ensure that the next page
          // transition goes off.
          currentPage = null;
          break;
      }
      // Normalize and send to the dataInstance.
      normalizeAndDispatchEventRecord(record);
    }

    private void onUpdateResource(int resourceId, UpdateResource resource) {
      if (getBaseTime() < 0) {
        // We do not allow an updateResource message to set our baseTime. If we
        // get a resource update first, it means that we missed the
        // corresponding start event. That is, the user started monitoring late,
        // and it is OK to ignore this resource.
        // Also, Resource Updates don't have associate timestamp to use as a
        // baseline.
        return;
      }

      UpdateResource update = resource.cast();
      ResourceUpdateEvent updateEvent = ResourceUpdateEvent.create(resourceId,
          update);

      if (update.didTimingChange()) {
        // We need to normalize times for the resource update since they are
        // absolute times given in seconds.
        double domContentEventTime = normalizeTime(update.getDomContentEventTime());
        double loadTime = normalizeTime(update.getLoadEventTime());
        double startTime = normalizeTime(update.getStartTime());
        double responseTime = normalizeTime(update.getResponseReceivedTime());
        double endTime = normalizeTime(update.getEndTime());

        // TODO(knorton): There is an awesome Chrome bug where
        // responseReceivedTime is completely invalid and actually falls before
        // the startTime. We're still investigating how this happens, but it may
        // be fixed on the TOT.
        if (responseTime < startTime) {
          responseTime = startTime;
        }

        if (startTime > 0) {
          update.setStartTime(startTime);

          // ResourceUpdateEvents should have a "time" field to be consistent
          // with the other timeline records and to match our EventRecord
          // schema. We use fall through to give it a time associated with the
          // "last potentially occurring time that it knows about".
          updateEvent.setTime(startTime);
        }

        if (responseTime > 0) {
          update.setResponseReceivedTime(responseTime);
        }

        if (loadTime > 0) {
          update.setLoadEventTime(loadTime);
        }

        if (domContentEventTime > 0) {
          update.setDomContentEventTime(domContentEventTime);
        }

        if (endTime > 0) {
          update.setEndTime(endTime);
        }
      }

      if (update.didResponseChange()) {
        DetailedResponseTiming detailedTiming = update.getDetailedResponseTiming();
        if (detailedTiming != null) {
          double requestTime = normalizeTime(detailedTiming.getRequestTime());
          if (requestTime > 0) {
            detailedTiming.setRequestTime(requestTime);
          }
        }
      }

      if (previousEvent != null) {
        updateEvent.setTime(previousEvent.getTime()
            + previousEvent.getDuration());
        forwardToDataInstance(updateEvent);
      }
    }

    /**
     * Clears the record buffer and establishes a baseTime.
     * 
     * @param triggerRecord the first record that is not a Resource Start.
     */
    private void sendPendingRecordsAndSetBaseTime(
        UnNormalizedEventRecord triggerRecord) {
      assert (getBaseTime() < 0) : "Emptying record buffer after establishing a base time.";

      double baseTimeStamp = triggerRecord.getStartTime();
      if (pendingRecords.size() > 0) {
        // Normalize base time using either the event that triggered the check,
        // or the first event that we buffered.
        UnNormalizedEventRecord firstStart = pendingRecords.get(0).cast();
        if (firstStart.getStartTime() < baseTimeStamp) {
          baseTimeStamp = firstStart.getStartTime();
        }
      }

      setBaseTime(baseTimeStamp);

      // Now that we have set the base time, we can replay the buffered Record
      // Starts since they did come in first, and they in fact still need to
      // go through normalization and through the page transition logic.
      for (int i = 0, n = pendingRecords.size(); i < n; i++) {
        onTimeLineRecord(pendingRecords.get(i));
      }

      // Nuke the pending records list.
      pendingRecords = JSOArray.create();
    }
  }

  /**
   * Overlay type for our dispatcher used by {@link Proxy}.
   */
  private static final class Dispatcher extends JavaScriptObject {
    @SuppressWarnings("all")
    protected Dispatcher() {
    }

    native void invoke(String method, JavaScriptObject payload) /*-{
      this[method](payload)
    }-*/;
  }

  /**
   * Constructs and returns a {@link DevToolsDataInstance} after wiring it up to
   * receive events over the extensions-devtools API.
   * 
   * @param tabId the tab that we want to connect to.
   * @return a newly wired up {@link DevToolsDataInstance}.
   */
  public static DevToolsDataInstance create(int tabId) {
    return DataInstance.create(new Proxy(tabId)).cast();
  }

  /**
   * Constructs and returns a {@link DevToolsDataInstance} after wiring it up to
   * receive events over the extensions-devtools API.
   * 
   * @param proxy an externally supplied proxy to act as the record
   *          transformation layer
   * @return a newly wired up {@link DevToolsDataInstance}.
   */
  public static DevToolsDataInstance create(Proxy proxy) {
    return DataInstance.create(proxy).cast();
  }

  protected DevToolsDataInstance() {
  }
}
