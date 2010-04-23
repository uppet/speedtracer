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
import com.google.speedtracer.client.model.DataModel.EventRecordHandler;
import com.google.speedtracer.client.model.UiEvent.LeafFirstTraversalVoid;
import com.google.speedtracer.shared.EventRecordType;

import java.util.ArrayList;
import java.util.List;

/**
 * Model that dispatches UiEvents to the UI.
 */
public class UiEventModel implements EventRecordHandler {
  /**
   * Listener interface for handling UiEventModel events.
   */
  public interface Listener {
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
   * @param model
   * @param typeMap
   */
  private static void setSpecialCasedEventCallbacks(final UiEventModel model,
      JsIntegerMap<EventRecordHandler> typeMap) {

    typeMap.put(EventRecordType.TIMER_CLEARED, new EventRecordHandler() {
      public void onEventRecord(EventRecord data) {
        model.onTimerCleared(data.<TimerCleared> cast());
      }
    });

    typeMap.put(EventRecordType.TIMER_INSTALLED, new EventRecordHandler() {
      public void onEventRecord(EventRecord data) {
        model.onTimerInstalled(data.<TimerInstalled> cast());
      }
    });

    // TODO(jaimeyap): We should eventually make InspectorResourceConverter use
    // these types instead of our own record types. For now we simply ignore
    // these webkit style network resource checkpoints.
    typeMap.put(EventRecordType.RESOURCE_SEND_REQUEST,
        new EventRecordHandler() {
          public void onEventRecord(EventRecord data) {
            // Special cased to do nothing for now.
          }
        });

    typeMap.put(EventRecordType.RESOURCE_RECEIVE_RESPONSE,
        new EventRecordHandler() {
          public void onEventRecord(EventRecord data) {
            // Special cased to do nothing for now.
          }
        });

    typeMap.put(EventRecordType.RESOURCE_FINISH, new EventRecordHandler() {
      public void onEventRecord(EventRecord data) {
        // Special cased to do nothing for now.
      }
    });

    typeMap.put(EventRecordType.RESOURCE_UPDATED, new EventRecordHandler() {
      public void onEventRecord(EventRecord data) {
        // Special cased to do nothing for now.
      }
    });
  }

  private final List<Listener> listeners = new ArrayList<Listener>();

  private final JsIntegerMap<TimerInstalled> timerMetaData = JsIntegerMap.create();

  private final JsIntegerMap<EventRecordHandler> specialCasedTypeMap = JsIntegerMap.create();

  private final TimerInstallationVisitor timerInstallationVisitor = new TimerInstallationVisitor();

  UiEventModel() {
    setSpecialCasedEventCallbacks(this, specialCasedTypeMap);
  }

  public void addListener(Listener listener) {
    listeners.add(listener);
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
    final EventRecordHandler specialHandler = specialCasedTypeMap.get(data.getType());
    if (specialHandler != null) {
      specialHandler.onEventRecord(data);
    } else if (UiEvent.isUiEvent(data)) {
      onUiEventFinished(data.<UiEvent> cast());
    }
  }

  public void removeListener(Listener listener) {
    listeners.remove(listener);
  }

  private void onTimerCleared(TimerCleared event) {
    // TODO (jaimeyap): handle this.
  }

  // This does not dispatch to the model listener. All we need to do here
  // is bookkeep the Timer Data.
  private void onTimerInstalled(TimerInstalled timerData) {
    assert (!Double.isNaN(timerData.getTime()));
    timerMetaData.put(timerData.getTimerId(), timerData);
  }

  private void onUiEventFinished(UiEvent event) {
    assert (!Double.isNaN(event.getTime()));

    // Run some visitors here to extract Timer Installations.
    event.apply(timerInstallationVisitor);

    for (int i = 0, n = listeners.size(); i < n; i++) {
      Listener listener = listeners.get(i);
      listener.onUiEventFinished(event);
    }
  }
}
