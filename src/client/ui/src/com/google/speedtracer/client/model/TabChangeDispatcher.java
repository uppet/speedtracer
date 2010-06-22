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

import com.google.speedtracer.client.model.DataDispatcher.DataDispatcherDelegate;
import com.google.speedtracer.client.util.Url;

import java.util.ArrayList;
import java.util.List;

/**
 * Provides a data model for tab navigation events. Logically these are separate
 * from UI and network events although they have a very similar format.
 */
public class TabChangeDispatcher implements DataDispatcherDelegate {
  /**
   * Listener interface for handling page transitions.
   */
  public interface Listener {
    void onPageTransition(TabChangeEvent change);

    void onRefresh(TabChangeEvent change);
  }

  private String currentUrl = "";

  private final List<Listener> listeners = new ArrayList<Listener>();

  public void addListener(Listener listener) {
    listeners.add(listener);
  }

  public void clearData() {
    currentUrl = "";
  }

  public void onEventRecord(EventRecord data) {
    if (TabChangeEvent.TYPE == data.getType()) {
      // These should only be fired when there is a main resource request
      // (redirects should already be accounted for).
      TabChangeEvent e = data.cast();
      String newUrl = Url.getUrlWithoutHash(e.getUrl());

      for (int i = 0, n = listeners.size(); i < n; i++) {
        Listener listener = listeners.get(i);
        if (!currentUrl.equals(newUrl)) {
          listener.onPageTransition(e);
        } else {
          listener.onRefresh(e);
        }
      }

      currentUrl = newUrl;
    }
  }

  public void removePageTransitionListener(Listener listener) {
    listeners.remove(listener);
  }
}
