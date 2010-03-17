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

import com.google.speedtracer.client.model.DataModel.EventRecordHandler;
import com.google.speedtracer.client.util.JsIntegerMap;

import java.util.ArrayList;
import java.util.List;

/**
 * Native model which sources Network Events for the UI. Hooks into underlying
 * DataInstance.
 */
public class NetworkResourceModel implements EventRecordHandler {

  /**
   * Listener Interface for a NetworkResourceModel.
   */
  public interface Listener {
    void onNetworkResourceRequestStarted(ResourceWillSendEvent resourceStart);

    void onNetworkResourceResponseFinished(ResourceFinishEvent resourceFinish);

    void onNetworkResourceResponseStarted(ResourceResponseEvent resourceResponse);

    void onNetworkResourceUpdated(ResourceUpdateEvent resourceUpdate);
  }

  /**
   * Sets up mapping of NetworkResourceRecord types to their respective
   * handlers.
   * 
   * @param proxy the {@link NetworkResourceModel}
   * @param typeMap the {@link FastStringMap}
   */
  private static void setNetworkEventCallbacks(final NetworkResourceModel proxy, JsIntegerMap<EventRecordHandler> typeMap) {

    typeMap.put(EventRecordType.RESOURCE_SEND_REQUEST, new EventRecordHandler() {
      public void onEventRecord(EventRecord data) {
        proxy.onNetworkResourceStarted(data.<ResourceWillSendEvent> cast());
      }
    });

    typeMap.put(EventRecordType.RESOURCE_RECEIVE_RESPONSE, new EventRecordHandler() {
      public void onEventRecord(EventRecord data) {
        proxy.onNetworkResourceResponse(data.<ResourceResponseEvent> cast());
      }
    });

    typeMap.put(EventRecordType.RESOURCE_FINISH, new EventRecordHandler() {
      public void onEventRecord(EventRecord data) {
        ResourceFinishEvent finish = data.cast();
        proxy.onNetworkResourceFinished(finish);
      }
    });

    typeMap.put(EventRecordType.RESOURCE_UPDATED, new EventRecordHandler() {
      public void onEventRecord(EventRecord data) {
        proxy.onNetworkResourceUpdated(data.<ResourceUpdateEvent> cast());
      }
    });
  };

  private final List<Listener> listeners = new ArrayList<Listener>();

  private final JsIntegerMap<EventRecordHandler> typeMap = JsIntegerMap.create();

  NetworkResourceModel() {
    setNetworkEventCallbacks(this, typeMap);
  }

  public void addListener(Listener listener) {
    listeners.add(listener);
  }

  public void onEventRecord(EventRecord data) {
    final EventRecordHandler handler = typeMap.get(data.getType());
    if (handler != null) {
      handler.onEventRecord(data);
    }
  }

  public void onNetworkResourceFinished(ResourceFinishEvent resourceFinish) {
    for (int i = 0, n = listeners.size(); i < n; i++) {
      Listener listener = listeners.get(i);
      listener.onNetworkResourceResponseFinished(resourceFinish);
    }
  }

  public void onNetworkResourceResponse(ResourceResponseEvent resourceResponse) {
    for (int i = 0, n = listeners.size(); i < n; i++) {
      Listener listener = listeners.get(i);
      listener.onNetworkResourceResponseStarted(resourceResponse);
    }
  }

  public void onNetworkResourceStarted(ResourceWillSendEvent resourceStart) {
    for (int i = 0, n = listeners.size(); i < n; i++) {
      Listener listener = listeners.get(i);
      listener.onNetworkResourceRequestStarted(resourceStart);
    }
  }

  public void onNetworkResourceUpdated(ResourceUpdateEvent update) {
    for (int i = 0, n = listeners.size(); i < n; i++) {
      Listener listener = listeners.get(i);
      listener.onNetworkResourceUpdated(update);
    }
  }

  public void removeListener(Listener listener) {
    listeners.remove(listener);
  }
}
