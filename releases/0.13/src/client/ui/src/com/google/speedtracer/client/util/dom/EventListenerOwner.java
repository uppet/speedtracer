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

package com.google.speedtracer.client.util.dom;

import com.google.gwt.events.client.EventListenerRemover;

import java.util.ArrayList;
import java.util.List;

/**
 * A class that allows for sane management and cleanup of
 * {@link EventListenerRemover}s. A UI component responsible for cleaning up
 * {@link EventListenerRemover}s will instantiate one of these classes and may
 * pass it to sub-components as a {@link ManagesEventListeners} to allow those
 * sub-components to delegate the cleanup of any {@link EventListenerRemover}s.
 * 
 * This class also implements {@link EventListenerRemover} directly making it
 * possible to have another {@link EventListenerOwner} instance share ownership.
 * This is useful in cases where you want to be able to
 * {@link #removeAllEventListeners()} as you tear down and rebuild a component
 * but defer the responsibility of final cleanup to a higher-level component.
 */
public class EventListenerOwner implements OwnsEventListeners,
    EventListenerRemover {
  private final List<EventListenerRemover> removers = new ArrayList<EventListenerRemover>();

  public void manageEventListener(EventListenerRemover remover) {
    removers.add(remover);
  }

  public void remove() {
    removeAllEventListeners();
  }

  public void removeAllEventListeners() {
    for (int i = 0, n = removers.size(); i < n; ++i) {
      removers.get(0).remove();
    }
    removers.clear();
  }
}
