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
package com.google.speedtracer.client.visualizations.model;

import com.google.speedtracer.client.model.ApplicationState;
import com.google.speedtracer.client.timeline.GraphModel;
import com.google.speedtracer.client.timeline.HighlightModel;

/**
 * Interface type to be implemented by all Models for each Visualization.
 */
public interface VisualizationModel {
  void clearData();

  /**
   * Implementors should remove themselves from any Models they happen to be
   * listening to. This gets called right before doing an ApplicationState swap.
   */
  void detachFromSourceModel();

  GraphModel getGraphModel();

  HighlightModel getHighlightModel();

  /**
   * Implementors may want to transfer state when swapping between Application
   * states. This is your chance to do it. This gets called right before doing
   * an ApplicationState swap.
   * 
   * @param oldState the old {@link ApplicationState} that is being swapped out
   * @param newState the new {@link ApplicationState} that is being swapped in
   * @param newUrl the URL of the new page transition
   */
  void transferEndingState(ApplicationState oldState,
      ApplicationState newState, String newUrl);
}
