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
 * Utility class for providing a simple mechanism for cleaning up EventHandlers.
 * 
 * TODO (jaimeyap): Port ALL the event cleanup code in the UI to use this. It is
 * much much better than willy nilly cleanup we are currently doing.
 */
public class EventCleanup {
  /**
   * Extending this class imbues a class with the ability to track and cleanup
   * hooked up event handlers.
   */
  public abstract static class EventCleanupTrait implements HasRemovers {
    private final EventCleanup eventCleanup;

    protected EventCleanupTrait() {
      this.eventCleanup = new EventCleanup();
    }

    public void cleanupRemovers() {
      this.eventCleanup.cleanupRemovers();
    }

    public EventListenerRemover getRemover() {
      return this.eventCleanup.getRemover();
    }

    public void trackRemover(EventListenerRemover remover) {
      this.eventCleanup.trackRemover(remover);
    }
  }

  /**
   * Classes that already extend another class that wish to conform to the
   * EventCleanup contract should implement this interface and delegate to an
   * EventCleanup instance.
   * 
   * In the absence of traits, the next best thing is for implementors just to
   * delegate to their EventCleanup instance.
   */
  public interface HasRemovers {
    void cleanupRemovers();

    EventListenerRemover getRemover();

    void trackRemover(EventListenerRemover remover);
  }

  protected final List<EventListenerRemover> removerHandles = new ArrayList<EventListenerRemover>();

  private final EventListenerRemover removeAll;

  public EventCleanup() {
    this.removeAll = new EventListenerRemover() {
      public void remove() {
        while (!removerHandles.isEmpty()) {
          removerHandles.remove(0).remove();
        }
      }
    };
  }

  public void cleanupRemovers() {
    getRemover().remove();
  }

  /**
   * For users that are part of a hierarchy of things that have removers, they
   * can simply pass this up to their parent EventCleanup.
   * 
   * @return a {@link EventListenerRemover} that cleans up all tracked remover
   *         handles in this EventCleanup.
   */
  public EventListenerRemover getRemover() {
    return removeAll;
  }

  public void trackRemover(EventListenerRemover remover) {
    removerHandles.add(remover);
  }
}
