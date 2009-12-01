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

import com.google.speedtracer.client.model.DataModel.EventCallbackProxy;
import com.google.speedtracer.client.model.DataModel.EventCallbackProxyProvider;
import com.google.speedtracer.client.util.JsIntegerMap;

import java.util.ArrayList;
import java.util.List;

/**
 * Native model which sources Network Events for the UI. Hooks into underlying
 * DataInstance.
 */
public class NetworkResourceModel implements EventCallbackProxyProvider {

  /**
   * Listener Interface for a NetworkResourceModel.
   */
  public interface Listener {
    void onNetworkResourceRequestStarted(NetworkResourceStart resourceStart);

    void onNetworkResourceResponseFailed(NetworkResourceError resourceError);

    void onNetworkResourceResponseFinished(
        NetworkResourceFinished resourceFinish);

    void onNetworkResourceResponseStarted(
        NetworkResourceResponse resourceResponse);
  }

  /**
   * Sets up mapping of NetworkResourceRecord types to their respective
   * handlers.
   * 
   * @param proxy the {@link NetworkResourceModel}
   * @param typeMap the {@link FastStringMap}
   */
  private static void setNetworkEventCallbacks(
      final NetworkResourceModel proxy,
      JsIntegerMap<EventCallbackProxy> typeMap) {

    typeMap.put(EventRecordType.NETWORK_RESOURCE_START, new EventCallbackProxy() {
      public void onEventRecord(EventRecord data) {
        proxy.onNetworkResourceStarted(data.<NetworkResourceStart>cast());
      }
    });

    typeMap.put(EventRecordType.NETWORK_RESOURCE_RESPONSE, new EventCallbackProxy() {
      public void onEventRecord(EventRecord data) {
        proxy.onNetworkResourceResponse(data.<NetworkResourceResponse>cast());
      }
    });

    typeMap.put(EventRecordType.NETWORK_RESOURCE_FINISH, new EventCallbackProxy() {
      public void onEventRecord(EventRecord data) {
        proxy.onNetworkResourceFinished(data.<NetworkResourceFinished>cast());
      }
    });

    typeMap.put(EventRecordType.NETWORK_RESOURCE_ERROR, new EventCallbackProxy() {
      public void onEventRecord(EventRecord data) {
        proxy.onNetworkResourceFailed(data.<NetworkResourceError>cast());
      }
    });
  };

  private final List<Listener> listeners = new ArrayList<Listener>();
  
  private final JsIntegerMap<EventCallbackProxy> typeMap = JsIntegerMap.create();

  NetworkResourceModel() {
    setNetworkEventCallbacks(this, typeMap);
  }
  
  public void addListener(Listener listener) {
    listeners.add(listener);
  }

  public EventCallbackProxy getEventCallback(EventRecord data) {
    return typeMap.get(data.getType());
  }

  public void onNetworkResourceFailed(NetworkResourceError resourceFail) {
    for (int i = 0, n = listeners.size(); i < n; i++) {
      Listener listener = listeners.get(i);
      listener.onNetworkResourceResponseFailed(resourceFail);
    }
  }

  public void onNetworkResourceFinished(NetworkResourceFinished resourceFinish) {
    for (int i = 0, n = listeners.size(); i < n; i++) {
      Listener listener = listeners.get(i);
      listener.onNetworkResourceResponseFinished(resourceFinish);
    }
  }

  public void onNetworkResourceResponse(NetworkResourceResponse resourceResponse) {
    for (int i = 0, n = listeners.size(); i < n; i++) {
      Listener listener = listeners.get(i);
      listener.onNetworkResourceResponseStarted(resourceResponse);
    }
  }

  public void onNetworkResourceStarted(NetworkResourceStart resourceStart) {
    for (int i = 0, n = listeners.size(); i < n; i++) {
      Listener listener = listeners.get(i);
      listener.onNetworkResourceRequestStarted(resourceStart);
    }
  }

  public void removeListener(Listener listener) {
    listeners.remove(listener);
  }
}
