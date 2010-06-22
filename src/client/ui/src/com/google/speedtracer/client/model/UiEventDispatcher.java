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

import com.google.gwt.coreext.client.JsIntegerMap;
import com.google.speedtracer.client.model.DataDispatcher.DataDispatcherDelegate;
import com.google.speedtracer.client.model.DataDispatcher.EventRecordDispatcher;
import com.google.speedtracer.client.model.UiEvent.LeafFirstTraversalVoid;

import java.util.ArrayList;
import java.util.List;

/**
 * Dispatches Ui/DOM Events.
 */
public class UiEventDispatcher implements DataDispatcherDelegate {
  /**
   * Listener interface for handling load events.
   */
  public interface LoadEventListener {
    void onDomContentLoaded(DomContentLoadedEvent event);

    void onWindowLoad(WindowLoadEvent event);
  }

  /**
   * Listener interface for handling UiEvent events.
   */
  public interface UiEventListener {
    void onUiEventFinished(UiEvent event);
  }

  /**
   * Visitor that synthesizes top level timer installs if it detects a timer
   * installation in a trace tree.
   */
  private class TimerInstallationVisitor implements LeafFirstTraversalVoid {
    public void visit(UiEvent event) {
      if (TimerInstalled.TYPE == event.getType()) {
        onTimerInstalled(event.<TimerInstalled> cast());
      }
    }
  }

  /**
   * This sets up the function routing for EventRecord TYPES corresponding to
   * top level events that we want to special case.
   * 
   * @param dispatcher
   * @param typeMap
   */
  private static void setSpecialCasedEventCallbacks(
      final UiEventDispatcher dispatcher,
      JsIntegerMap<EventRecordDispatcher> typeMap) {

    typeMap.put(TimerCleared.TYPE, new EventRecordDispatcher() {
      public void onEventRecord(EventRecord data) {
        dispatcher.onTimerCleared(data.<TimerCleared> cast());
      }
    });

    typeMap.put(TimerInstalled.TYPE, new EventRecordDispatcher() {
      public void onEventRecord(EventRecord data) {
        dispatcher.onTimerInstalled(data.<TimerInstalled> cast());
      }
    });

    typeMap.put(ResourceResponseEvent.TYPE, new EventRecordDispatcher() {
      public void onEventRecord(EventRecord data) {
        // TODO(jaimeyap): For now we ignore this event. But this event seems to
        // measure things coming out of Application cache, and possibly XHR
        // callbacks coming out of memory cache. We should try to show this
        // properly on the sluggishness view without confusing it with Resource
        // Data Received events.
      }
    });

    typeMap.put(WindowLoadEvent.TYPE, new EventRecordDispatcher() {
      public void onEventRecord(EventRecord data) {
        List<LoadEventListener> listeners = dispatcher.loadEventListeners;
        for (int i = 0, n = listeners.size(); i < n; i++) {
          listeners.get(i).onWindowLoad(data.<WindowLoadEvent> cast());
        }
      }
    });

    typeMap.put(DomContentLoadedEvent.TYPE, new EventRecordDispatcher() {
      public void onEventRecord(EventRecord data) {
        List<LoadEventListener> listeners = dispatcher.loadEventListeners;
        for (int i = 0, n = listeners.size(); i < n; i++) {
          listeners.get(i).onDomContentLoaded(
              data.<DomContentLoadedEvent> cast());
        }
      }
    });
  }

  private final List<UiEvent> eventList = new ArrayList<UiEvent>();

  private final List<LoadEventListener> loadEventListeners = new ArrayList<LoadEventListener>();

  private final JsIntegerMap<EventRecordDispatcher> specialCasedTypeMap = JsIntegerMap.create();

  private final TimerInstallationVisitor timerInstallationVisitor = new TimerInstallationVisitor();

  private final JsIntegerMap<TimerInstalled> timerMetaData = JsIntegerMap.create();

  private final List<UiEventListener> uiEventListeners = new ArrayList<UiEventListener>();

  UiEventDispatcher() {
    setSpecialCasedEventCallbacks(this, specialCasedTypeMap);
  }

  public void addLoadEventListener(LoadEventListener listener) {
    loadEventListeners.add(listener);
  }

  public void addUiEventListener(UiEventListener listener) {
    uiEventListeners.add(listener);
  }

  public void clearData() {
    eventList.clear();
  }

  public List<UiEvent> getEventList() {
    return eventList;
  }

  public TimerInstalled getTimerMetaData(int timerId) {
    return timerMetaData.get(timerId);
  }

  /**
   * If the incoming EventRecord belongs to one of the special cased types, we
   * return a proxy object to handle those types. Otherwise, we use the default
   * UiEvent dispatch proxy.
   */
  public void onEventRecord(EventRecord data) {
    final EventRecordDispatcher specialHandler = specialCasedTypeMap.get(data.getType());
    if (specialHandler != null) {
      specialHandler.onEventRecord(data);
    } else if (UiEvent.isUiEvent(data)) {
      onUiEventFinished(data.<UiEvent> cast());
    }
  }

  public void removeLoadEventListener(LoadEventListener listener) {
    loadEventListeners.remove(listener);
  }
  
  public void removeUiEventListener(UiEventListener listener) {
    uiEventListeners.remove(listener);
  }

  private void onTimerCleared(TimerCleared event) {
    // TODO (jaimeyap): handle this.
  }

  // This does not dispatch to the listener. All we need to do here is book keep
  // the Timer Data.
  private void onTimerInstalled(TimerInstalled timerData) {
    assert (!Double.isNaN(timerData.getTime()));
    timerMetaData.put(timerData.getTimerId(), timerData);
  }

  private void onUiEventFinished(UiEvent event) {
    assert (!Double.isNaN(event.getTime()));

    // Run some visitors here to extract Timer Installations.
    event.apply(timerInstallationVisitor);
    
    // Keep a copy of the event.
    eventList.add(event);

    for (int i = 0, n = uiEventListeners.size(); i < n; i++) {
      UiEventListener listener = uiEventListeners.get(i);
      listener.onUiEventFinished(event);
    }
  }
}
