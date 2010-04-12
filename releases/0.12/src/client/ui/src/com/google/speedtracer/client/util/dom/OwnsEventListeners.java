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

/**
 * Implementers are considered the owner or collector of
 * {@link com.google.gwt.events.client.EventListenerRemover}s. Not only are they
 * able to manage event listeners ( {@link ManagesEventListeners}) but they also
 * have the ability to remove all
 * {@link com.google.gwt.events.client.EventListenerRemover}s.
 * 
 * Best practice is for the root of a UI hierarchy to create an
 * {@link EventListenerOwner}, pass a reference to all sub-components as
 * {@link ManagesEventListeners} and call {@link #removeAllEventListeners()}
 * manually as the UI hierarchy is destoyed.
 * 
 */
public interface OwnsEventListeners extends ManagesEventListeners {
  void removeAllEventListeners();
}
