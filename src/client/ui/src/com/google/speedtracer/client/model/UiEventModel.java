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
import com.google.speedtracer.client.model.EventVisitor.PostOrderVisitor;
import com.google.speedtracer.client.util.JsIntegerMap;

import java.util.ArrayList;
import java.util.List;

/**
 * Model that dispatches UiEvents to the UI.
 */
public class UiEventModel implements EventCallbackProxyProvider {
  /**
   * Visitor that synthesizes top level timer installs if it detects a timer
   * installation in a trace tree.
   */
  private class TimerInstallationVisitor implements PostOrderVisitor {
    public void postProcess() {
    }

    public void visitUiEvent(UiEvent e) {
      if (TimerInstalled.TYPE == e.getType()) {
        onTimerInstalled(e.<TimerInstalled> cast());
      }
    }
  }

  /**
   * Listener interface for handling UiEventModel events.
   */
  public interface Listener {
    void onUiEventFinished(UiEvent event);
  }

  /**
   * This sets up the function routing for EventRecord TYPES corresponding to
   * top level events that we want to special case.
   * 
   * @param proxy
   * @param typeMap
   */
  private static void setSpecialCasedEventCallbacks(final UiEventModel proxy,
      JsIntegerMap<EventCallbackProxy> typeMap) {

    typeMap.put(EventRecordType.TIMER_CLEARED, new EventCallbackProxy() {
      public void onEventRecord(EventRecord data) {
        proxy.onTimerCleared(data.<TimerCleared> cast());
      }
    });

    typeMap.put(EventRecordType.TIMER_INSTALLED, new EventCallbackProxy() {
      public void onEventRecord(EventRecord data) {
        proxy.onTimerInstalled(data.<TimerInstalled> cast());
      }
    });

    // TODO(jaimeyap): We should eventually make InspectorResourceConverter use
    // these types instead of our own record types. For now we simply ignore
    // these webkit style network resource checkpoints.
    typeMap.put(EventRecordType.RESOURCE_SEND_REQUEST,
        new EventCallbackProxy() {
          public void onEventRecord(EventRecord data) {
            // Special cased to do nothing for now.
          }
        });

    typeMap.put(EventRecordType.RESOURCE_RECEIVE_RESPONSE,
        new EventCallbackProxy() {
          public void onEventRecord(EventRecord data) {
            // Special cased to do nothing for now.
          }
        });

    typeMap.put(EventRecordType.RESOURCE_FINISH, new EventCallbackProxy() {
      public void onEventRecord(EventRecord data) {
        // Special cased to do nothing for now.
      }
    });

    typeMap.put(EventRecordType.RESOURCE_UPDATED, new EventCallbackProxy() {
      public void onEventRecord(EventRecord data) {
        // Special cased to do nothing for now.
      }
    });
  }

  private final List<Listener> listeners = new ArrayList<Listener>();

  private final JsIntegerMap<TimerInstalled> timerMetaData = JsIntegerMap.create();

  private final JsIntegerMap<EventCallbackProxy> specialCasedTypeMap = JsIntegerMap.create();

  private final EventCallbackProxy uiEventProxy;

  private final PostOrderVisitor[] postOrderVisitors = {new TimerInstallationVisitor()};

  UiEventModel() {
    setSpecialCasedEventCallbacks(this, specialCasedTypeMap);
    uiEventProxy = new EventCallbackProxy() {
      public void onEventRecord(EventRecord data) {
        onUiEventFinished(data.<UiEvent> cast());
      }
    };
  }

  public void addListener(Listener listener) {
    listeners.add(listener);
  }

  /**
   * If the incoming EventRecord belongs to one of the special cased types, we
   * return a proxy object to handle those types. Otherwise, we use the default
   * UiEvent dispatch proxy.
   */
  public EventCallbackProxy getEventCallback(EventRecord data) {
    EventCallbackProxy ret = specialCasedTypeMap.get(data.getType());
    if (ret == null && UiEvent.isUiEvent(data)) {
      return uiEventProxy;
    }
    return ret;
  }

  public TimerInstalled getTimerMetaData(int timerId) {
    return timerMetaData.get(timerId);
  }

  public void onTimerCleared(TimerCleared event) {
    // TODO (jaimeyap): handle this.
  }

  // This does not dispatch to the model listener. All we need to do here
  // is bookkeep the Timer Data.
  public void onTimerInstalled(TimerInstalled timerData) {
    assert (!Double.isNaN(timerData.getTime()));
    timerMetaData.put(timerData.getTimerId(), timerData);
  }

  public void onUiEventFinished(UiEvent event) {
    assert (!Double.isNaN(event.getTime()));

    // Run some visitors here to extract Timer Installations.
    EventVisitorTraverser.traversePostOrder(event, postOrderVisitors);

    for (int i = 0, n = listeners.size(); i < n; i++) {
      Listener listener = listeners.get(i);
      listener.onUiEventFinished(event);
    }
  }

  public void removeListener(Listener listener) {
    listeners.remove(listener);
  }
}
