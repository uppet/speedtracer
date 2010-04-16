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

import com.google.speedtracer.shared.EventRecordType;

/**
 * Overlay for UiEvent payload.
 */
public class DomEvent extends UiEvent {
  public static final int TYPE = EventRecordType.DOM_EVENT;
  
  protected DomEvent() {
  }

  public final String asString() {
    return "Start: " + getTime() + ", End: " + getEndTime();
  }

  public final String getDomEventType() {
    return getData().getStringProperty("type");
  }
  
  // TODO (jaimeyap): Re-instrument this stuff.
  /*   
  public final double getBubbleDuration() {
    return getTimeSpentInPhase(DomEventDispatch.BUBBLE_PHASE);
  }

  public final double getCaptureDuration() {
    return getTimeSpentInPhase(DomEventDispatch.CAPTURE_PHASE);
  };

  private double getTimeSpentInPhase(String phase) {    
    JSOArray<UiEvent> children = getChildren();
    for (int i = 0, n = children.size(); i < n; i++) {
      UiEvent child = children.get(i);
      if (child.getType() == EventRecordType.DOM_EVENT_DISPATCH
          && ((DomEventDispatch) child).getPhase().equals(phase)) {
        return child.getDuration();
      }
    }
    return 0;
  }
  */
}