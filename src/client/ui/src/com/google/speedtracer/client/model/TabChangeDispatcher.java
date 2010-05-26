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

import com.google.speedtracer.client.model.DataDispatcher.EventRecordDispatcher;

import java.util.ArrayList;
import java.util.List;

/**
 * Provides a data model for tab navigation events. Logically these are separate
 * from UI and network events although they have a very similar format.
 */
public class TabChangeDispatcher implements EventRecordDispatcher {

  /**
   * Listener interface for handling TabNavigationModel events.
   */
  public interface Listener {
    void onPageTransition(PageTransition change);
  }

  private final List<Listener> listeners = new ArrayList<Listener>();

  public void addListener(Listener listener) {
    listeners.add(listener);
  }

  public void onEventRecord(EventRecord data) {
    if (PageTransition.TYPE == data.getType()) {
      for (int i = 0, n = listeners.size(); i < n; i++) {
        listeners.get(i).onPageTransition(data.<PageTransition> cast());
      }
    }
  }

  public void removeListener(Listener listener) {
    listeners.remove(listener);
  }
}
