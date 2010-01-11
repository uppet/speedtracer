/*
 * Copyright 2009 Google Inc.
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

import com.google.gwt.chrome.crx.client.DevTools;
import com.google.gwt.chrome.crx.client.events.DevToolsPageEvent.Listener;
import com.google.gwt.chrome.crx.client.events.DevToolsPageEvent.PageEvent;
import com.google.gwt.chrome.crx.client.events.Event.ListenerHandle;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.speedtracer.client.model.DataModel.DataInstance;
import com.google.speedtracer.client.model.EventVisitor.PostOrderVisitor;
import com.google.speedtracer.client.model.EventVisitor.PreOrderVisitor;
import com.google.speedtracer.client.model.InspectorResourceConverter.InspectorResourceConverterImpl;

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
  public static class DevToolsDataProxy {
    /**
     * Simple routing dispatcher used by the DevToolsDataProxy to quickly route.
     */
    static native Dispatcher createDispatcher(DevToolsDataProxy delegate) /*-{
      return {
        addRecordToTimeline: function(record) {
          delegate.@com.google.speedtracer.client.model.DevToolsDataInstance.DevToolsDataProxy::onTimeLineRecord(Lcom/google/speedtracer/client/model/EventRecord;)(record[1]);
        },
        // This becomes a dead code path as of webkit r52154, but we need it for
        // older versions.
        addResource: function(resource) {
          delegate.@com.google.speedtracer.client.model.DevToolsDataInstance.DevToolsDataProxy::onLegacyAddResource(ILcom/google/gwt/core/client/JavaScriptObject;)(resource[1], resource[2]);
        },
        updateResource: function(update) {
          delegate.@com.google.speedtracer.client.model.DevToolsDataInstance.DevToolsDataProxy::onUpdateResource(ILcom/google/gwt/core/client/JavaScriptObject;)(update[1], update[2]);
        }
      };
    }-*/;

    private double baseTime = -1;

    private DevToolsDataInstance dataInstance;

    private final Dispatcher dispatcher;

    private ListenerHandle listenerHandle;

    private final PostOrderVisitor[] postOrderVisitors = {};

    private final PreOrderVisitor[] preOrderVisitors = {new TimeNormalizerVisitor(
        this)};

    private InspectorResourceConverter resourceConverter;

    // TODO (jaimeyap): Get rid of this once WebKit r52154 is pushed to Dev
    // Channel.
    private boolean usingLegacyResourceConverter = false;

    public DevToolsDataProxy() {
      dispatcher = createDispatcher(this);
    }

    public final void dispatchPageEvent(PageEvent event) {
      dispatcher.invoke(event.getMethod(), event);
    }

    protected void connectToDataSource(int tabId) {
      this.listenerHandle = DevTools.getTabEvents(tabId).getPageEvent().addListener(
          new Listener() {
            public void onPageEvent(PageEvent event) {
              dispatchPageEvent(event);
            }
          });
    }

    void connectToDevTools(int tabId, final DevToolsDataInstance dataInstance) {
      // Connect to the devtools API as the data source.
      if (this.dataInstance == null) {
        this.dataInstance = dataInstance;
        this.resourceConverter = new InspectorResourceConverterImpl(this);
      }
      connectToDataSource(tabId);
    }

    void disconnect() {
      if (listenerHandle != null) {
        listenerHandle.removeListener();
      }

      listenerHandle = null;
    }

    double getBaseTime() {
      return baseTime;
    }
 
    void onEventRecord(EventRecord record) {
      dataInstance.onEventRecord(record);
    }

    void setBaseTime(double baseTime) {
      this.baseTime = baseTime;
    }

    /**
     * TODO (jaimeyap): Get rid of this once WebKit r52154 is pushed to Dev
     * Channel.
     */
    @SuppressWarnings("unused")
    private void onLegacyAddResource(int resourceId, JavaScriptObject resource) {
      if (!usingLegacyResourceConverter) {
        resourceConverter = new LegacyInspectorResourceConverter(this);
        usingLegacyResourceConverter = true;
      }
      ((LegacyInspectorResourceConverter) resourceConverter).onAddResource(
          resourceId, resource);
    }

    // Called from JavaScript.
    @SuppressWarnings("unused")
    private void onTimeLineRecord(EventRecord record) {
      assert (dataInstance != null) : "Someone called invoke that wasn't our connect call!";

      // We currently want to drop the webkit style network resource
      // timeline records since we synthesize our own.
      // TODO(jaimeyap): figure out a good way to consume these guys.
      int type = record.getType();
      switch (type) {
        case EventRecordType.RESOURCE_SEND_REQUEST:
        case EventRecordType.RESOURCE_RECEIVE_RESPONSE:
        case EventRecordType.RESOURCE_FINISH:
          break;
        default:
          // We run visitors to normalize the times for this tree and to do any
          // other transformations we want.
          EventVisitorTraverser.traverse(preOrderVisitors,
              record.<UiEvent> cast(), postOrderVisitors);
          // Forward to the dataInstance.
          onEventRecord(record);
      }
    }

    @SuppressWarnings("unused")
    private void onUpdateResource(int resourceId, JavaScriptObject resource) {
      resourceConverter.onUpdateResource(resourceId, resource);
    }
  }

  /**
   * Overlay type for our dispatcher used by {@link DevToolsDataProxy}.
   */
  private static class Dispatcher extends JavaScriptObject {
    @SuppressWarnings("unused")
    protected Dispatcher() {
    }

    final native void invoke(String method, JavaScriptObject payload) /*-{
      this[method](payload);
    }-*/;
  }

  /**
   * Constructs and returns a {@link DevToolsDataIstance} after wiring it up to
   * receive events over the extensions-devtools API.
   * 
   * @param tabId the tab that we want to connec to.
   * @return a newly wired up {@link DevToolsDataInstance}.
   */
  public static DevToolsDataInstance create(int tabId) {
    DevToolsDataProxy proxy = new DevToolsDataProxy();
    DevToolsDataInstance dataInstance = createImpl(tabId, proxy);
    return dataInstance;
  }

  /**
   * Constructs and returns a {@link DevToolsDataIstance} after wiring it up to
   * receive events over the extensions-devtools API.
   * 
   * @param tabId the tab that we want to connec to.
   * @param proxy an externally supplied proxy to act as the record
   *          transformation layer
   * @return a newly wired up {@link DevToolsDataInstance}.
   */
  public static DevToolsDataInstance create(int tabId, DevToolsDataProxy proxy) {
    DevToolsDataInstance dataInstance = createImpl(tabId, proxy);
    return dataInstance;
  }

  private static native DevToolsDataInstance createImpl(int tabId,
      DevToolsDataProxy proxy) /*-{
    var dataInstance = {
      Load: function(callback) {
        this._callback = callback;
        // Connect to devtools
        this.Resume(tabId);
      },

      Resume: function(tabId) {
        proxy.@com.google.speedtracer.client.model.DevToolsDataInstance.DevToolsDataProxy::connectToDevTools(ILcom/google/speedtracer/client/model/DevToolsDataInstance;)(tabId, dataInstance);
      },

      Stop: function() {
        proxy.@com.google.speedtracer.client.model.DevToolsDataInstance.DevToolsDataProxy::disconnect()();
      },

      Unload: function() {
        proxy.@com.google.speedtracer.client.model.DevToolsDataInstance.DevToolsDataProxy::baseTime = -1;
        proxy.@com.google.speedtracer.client.model.DevToolsDataInstance.DevToolsDataProxy::disconnect()();
        // Remove the connection back to the monitor's model in order to prevent
        // a memory leak.
        this._callback = null;
      }
    };
    // Initialize the sequence number count to 0
    dataInstance.seqCount = 0;
    return dataInstance;
  }-*/;

  protected DevToolsDataInstance() {
  }

  /**
   * Forwards an EventRecord to the callback. Records that come in before we
   * have assigned a callback get buffered.
   * 
   * @param record
   */
  final native void onEventRecord(EventRecord record) /*-{
    record.sequence = this.seqCount;
    this._callback.onEventRecord(record);
    this.seqCount = this.seqCount + 1;
  }-*/;
}
