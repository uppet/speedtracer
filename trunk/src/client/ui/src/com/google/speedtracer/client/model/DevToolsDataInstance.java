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
import com.google.gwt.coreext.client.DataBag;
import com.google.gwt.coreext.client.JSOArray;
import com.google.speedtracer.client.model.UiEvent.LeafFirstTraversalVoid;
import com.google.speedtracer.shared.EventRecordType;

/**
 * This class is used in Chrome when we are getting data from the devtools API.
 * Its job is to receive data from the devtools API, ensure that the data in
 * properly transformed into a consumable form, and to invoke callbacks passed
 * in from the UI. We use this overlay type as the object we pass to the Monitor
 * UI.
 */
public class DevToolsDataInstance extends DataInstance {

  private final static class TypeTranslationMap extends JavaScriptObject {
    protected TypeTranslationMap() {
    }

    static TypeTranslationMap create() {
      final TypeTranslationMap map = JavaScriptObject.createObject().cast();
      // The weird end of line comments below keep the auto-formatter from
      // from reformatting these lines.
      return map //
          .add("EventDispatch", EventRecordType.DOM_EVENT) //
          .add("Layout", EventRecordType.LAYOUT_EVENT) //
          .add("RecalculateStyles", EventRecordType.RECALC_STYLE_EVENT) //
          .add("Paint", EventRecordType.PAINT_EVENT) //
          .add("ParseHTML", EventRecordType.PARSE_HTML_EVENT) //
          .add("TimerInstall", EventRecordType.TIMER_INSTALLED) //
          .add("TimerRemove", EventRecordType.TIMER_CLEARED) //
          .add("TimerFire", EventRecordType.TIMER_FIRED) //
          .add("XHRReadyStateChange", EventRecordType.XHR_READY_STATE_CHANGE) //
          .add("XHRLoad", EventRecordType.XHR_LOAD) //
          .add("EvaluateScript", EventRecordType.EVAL_SCRIPT_EVENT) //
          // MarkTimeline has been deprecated for TimeStamp, this can be
          // removed soon.
          .add("MarkTimeline", EventRecordType.LOG_MESSAGE_EVENT) //
          .add("TimeStamp", EventRecordType.LOG_MESSAGE_EVENT) //
          .add("ScheduleResourceRequest", EventRecordType.SCHEDULE_RESOURCE_REQUEST) //          
          .add("ResourceSendRequest", EventRecordType.RESOURCE_SEND_REQUEST) //
          .add("ResourceReceiveResponse", EventRecordType.RESOURCE_RECEIVE_RESPONSE) //
          .add("ResourceReceivedData", EventRecordType.RESOURCE_DATA_RECEIVED) //
          .add("ResourceFinish", EventRecordType.RESOURCE_FINISH) //
          .add("FunctionCall", EventRecordType.JAVASCRIPT_EXECUTION) //
          .add("GCEvent", EventRecordType.GC_EVENT) //
          .add("MarkDOMContent", EventRecordType.DOM_CONTENT_LOADED) //
          .add("MarkLoad", EventRecordType.LOAD_EVENT);
    }

    native TypeTranslationMap add(String name, int id) /*-{
      this[name] = id;
      return this;
    }-*/;

    native int get(String typeName) /*-{
      var type = this[typeName];
      return type === undefined ? @com.google.speedtracer.client.model.EventRecord::INVALID_TYPE : type;
    }-*/;
  }

  /**
   * Proxy class that normalizes data coming in from the devtools API into a
   * digestable form, and then forwards it on to the DevToolsDataInstance.
   */
  public static class Proxy implements DataProxy {
    private class TimeNormalizingVisitor implements LeafFirstTraversalVoid {
      public void visit(UiEvent event) {
        assert getBaseTime() >= 0 : "baseTime should already be set.";
        event.<UnNormalizedEventRecord> cast().convertToEventRecord(getBaseTime());
      }
    }

    private static class TypeTranslationVisitor implements LeafFirstTraversalVoid {
      private final TypeTranslationMap map = TypeTranslationMap.create();

      private native static void updateType(UiEvent event, int type) /*-{
        event.type = type;
      }-*/;

      public void visit(UiEvent event) {
        updateType(event, map.get(DataBag.getStringProperty(event, "type")));
      }
    }

    private double baseTime;

    private DevToolsDataInstance dataInstance;

    private final Dispatcher dispatcher;

    private ListenerHandle listenerHandle;

    private JSOArray<UnNormalizedEventRecord> pendingRecords = JSOArray.create();

    private final int tabId;

    private final TimeNormalizingVisitor timeNormalizingVisitor = new TimeNormalizingVisitor();

    private final TypeTranslationVisitor typeTranslationVistior = new TypeTranslationVisitor();

    private double lastStartTime;

    public Proxy(int tabId) {
      this.baseTime = -1;
      this.tabId = tabId;
      this.dispatcher = Dispatcher.create(this);
      lastStartTime = -1;
    }

    public final void dispatchPageEvent(PageEvent event) {
      if (event.isTimelineStartedEvent()) {
        this.onTimelineProfilerStarted();
        return;
      }

      dispatcher.invoke(event.getMethod(), event.getBody());
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

    public void setProfilingOptions(boolean enableStackTraces, boolean enableCpuProfiling) {
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
        this.listenerHandle =
            DevTools.getTabEvents(tabId).getPageEvent().addListener(new Listener() {
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
    private double normalizeNetworkTime(double seconds) {
      assert getBaseTime() >= 0 : "NormalizeTime called before a base time was established.";

      double millis = seconds * 1000;
      return millis - getBaseTime();
    }

    @SuppressWarnings("unused")
    private void onNetworkResponseReceived(NetworkResponseReceivedEvent.Data data) {
      // Normalize the detailed timing request time.
      DetailedResponseTiming timing = data.getResponse().getDetailedTiming();
      if (timing != null) {
        timing.setRequestTime(normalizeNetworkTime(timing.getRequestTime()));
      }

      onNetworkResourceMessage(EventRecordType.NETWORK_RESPONSE_RECEIVED, data);
    }

    @SuppressWarnings("unused")
    private void onPageFrameNavigated(JavaScriptObject body) {
      FrameNavigation navigation = body.cast();
      normalizeAndDispatchEventRecord(TabChangeEvent.createUnNormalized(lastStartTime, navigation.getUrl()));
    }

    private void onNetworkResourceMessage(int messageType, NetworkEvent.Data data) {
      if (getBaseTime() < 0) {
        // We only allow proper timeline agent records to set base time.
        return;
      }
      forwardToDataInstance(NetworkEvent.create(messageType,
          normalizeNetworkTime(data.getTimeStamp()), data));
    }

    @SuppressWarnings("unused")
    private void onNetworkDataReceived(NetworkDataReceivedEvent.Data data) {
      onNetworkResourceMessage(EventRecordType.NETWORK_DATA_RECEIVED, data);
    }

    private void onTimelineProfilerStarted() {
      dataInstance.onTimelineProfilerStarted();
    }

    @SuppressWarnings("unused")
    private void onTimelineRecord(UnNormalizedEventRecord record) {
      assert (dataInstance != null) : "Someone called invoke that wasn't our connect call!";
      
      // When this visitor is applied, the appropriate speed tracer type
      // is set. An issue occurs if a record comes through and is pushed
      // onto pending and then sent back to onTimelineRecord. Therefore,
      // any saved records should be sent directly to sendRecord()
      record.<UiEvent> cast().apply(typeTranslationVistior);
      
      if(record.getType() == ResourceWillSendEvent.TYPE) {
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
      }

      sendRecord(record);
    }
    
    /**
     * Processes event records. 
     * Page transition events are processesd
     * @param record
     */
    private void sendRecord(UnNormalizedEventRecord record) {
      lastStartTime = record.getStartTime();
      
      // Normalize and send to the dataInstance.
      normalizeAndDispatchEventRecord(record);
    }

    @SuppressWarnings("unused")
    private void onNetworkRequestWillBeSent(NetworkRequestWillBeSentEvent.Data data) {
      onNetworkResourceMessage(EventRecordType.NETWORK_REQUEST_WILL_BE_SENT, data);
    }

    /**
     * Clears the record buffer and establishes a baseTime.
     * 
     * @param triggerRecord the first record that is not a Resource Start.
     */
    private void sendPendingRecordsAndSetBaseTime(UnNormalizedEventRecord triggerRecord) {
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
        sendRecord(pendingRecords.get(i));
      }

      // Nuke the pending records list.
      pendingRecords = JSOArray.create();
    }
  }
  
  /**
   * Represents a Page.frameNavigation event
   * #TODO (sarahgsmith) consider sending this event with an isTabChange 
   * flag instead of using TabChangeEvent
   */
  private final static class FrameNavigation extends JavaScriptObject {
    
    protected FrameNavigation() {}
    
    public native String getUrl() /*-{
      return this.url;
    }-*/;
    
    public native String getId() /*-{
      return this.id;
    }-*/;
    
  }

  /**
   * Overlay type for our dispatcher used by {@link Proxy}.
   */
  private static final class Dispatcher extends JavaScriptObject {

    /**
     * Simple routing dispatcher used by the DevToolsDataProxy to quickly route.
     */
    static native Dispatcher create(Proxy delegate) /*-{
      var dispatcher = {};
      dispatcher['Page.frameNavigated'] = function(body) {
        delegate.
          @com.google.speedtracer.client.model.DevToolsDataInstance.Proxy::onPageFrameNavigated(Lcom/google/gwt/core/client/JavaScriptObject;)
          (body.frame);
      };
      // Events generated by the Timeline profiler.
      dispatcher['Timeline.eventRecorded'] = function(body) {
        delegate.
          @com.google.speedtracer.client.model.DevToolsDataInstance.Proxy::onTimelineRecord(Lcom/google/speedtracer/client/model/UnNormalizedEventRecord;)
          (body.record);
      };
      // Network resource events.
      dispatcher['Network.requestWillBeSent'] = function(body) {
        delegate.
          @com.google.speedtracer.client.model.DevToolsDataInstance.Proxy::onNetworkRequestWillBeSent(Lcom/google/speedtracer/client/model/NetworkRequestWillBeSentEvent$Data;)
          (body);
      };
      dispatcher['Network.responseReceived'] = function(body) {
        delegate.
          @com.google.speedtracer.client.model.DevToolsDataInstance.Proxy::onNetworkResponseReceived(Lcom/google/speedtracer/client/model/NetworkResponseReceivedEvent$Data;)
          (body);
      };
      dispatcher['Network.dataReceived'] = function(body) {
        delegate.
          @com.google.speedtracer.client.model.DevToolsDataInstance.Proxy::onNetworkDataReceived(Lcom/google/speedtracer/client/model/NetworkDataReceivedEvent$Data;)
          (body);
      };
      return dispatcher;
    }-*/;

    protected Dispatcher() {
    }

    native void invoke(String method, JavaScriptObject payload) /*-{
      if (this[method]) {
        this[method](payload);
      }
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
