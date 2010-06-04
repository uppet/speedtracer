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

import com.google.gwt.coreext.client.IterableFastStringMap;
import com.google.gwt.coreext.client.IterableFastStringMap.IterationCallBack;
import com.google.speedtracer.client.timeline.Constants;
import com.google.speedtracer.client.visualizations.model.HintletReportModel;
import com.google.speedtracer.client.visualizations.model.NetworkVisualizationModel;
import com.google.speedtracer.client.visualizations.model.NetworkVisualization;
import com.google.speedtracer.client.visualizations.model.SluggishnessModel;
import com.google.speedtracer.client.visualizations.model.SluggishnessVisualization;
import com.google.speedtracer.client.visualizations.model.VisualizationModel;
import com.google.speedtracer.client.visualizations.view.HintletReport;

/**
 * This class contains the state of all of visualizations.
 */
public class ApplicationState {
  private final DataDispatcher dataDispatcher;

  private double firstDomainValue;

  private double lastDomainValue;

  private final IterableFastStringMap<VisualizationModel> visualizationModelMap;

  public ApplicationState(DataDispatcher dataDispatcher) {
    this.dataDispatcher = dataDispatcher;
    visualizationModelMap = new IterableFastStringMap<VisualizationModel>();
    firstDomainValue = 0;
    // this defaults to Constants.DEFAULT_GRAPH_WINDOW_SIZE in the future
    lastDomainValue = Constants.DEFAULT_GRAPH_WINDOW_SIZE; // endTime;
    populateVisualizationModelMap(dataDispatcher);
  }

  public void detachModelsFromDispatchers() {
    visualizationModelMap.iterate(new IterationCallBack<VisualizationModel>() {

      public void onIteration(String key, VisualizationModel vModel) {
        vModel.detachFromData();
      }

    });
  }

  public DataDispatcher getDataDispatcher() {
    return dataDispatcher;
  }

  public double getFirstDomainValue() {
    return firstDomainValue;
  }

  public double getLastDomainValue() {
    return lastDomainValue;
  }

  /**
   * Returns a {@link VisualizationModel} given a specified key.
   * 
   * @param visualizationTitle the key used to lookup the
   *          {@link VisualizationModel}
   * @return the VisualizationModel found or null if not found
   */
  public VisualizationModel getVisualizationModel(String visualizationTitle) {
    return visualizationModelMap.get(visualizationTitle);
  }

  public void setFirstDomainValue(double firstDomainValue) {
    this.firstDomainValue = firstDomainValue;
  }

  public void setLastDomainValue(double lastDomainValue) {
    this.lastDomainValue = lastDomainValue;
  }

  /**
   * Populates our VisualizationModel map with all known VisualizationModels.
   * 
   * @param dataDispatcher the {@link DataDispatcher} that all the
   *          {@link VisualizationModel}s register to
   */
  private void populateVisualizationModelMap(DataDispatcher dataDispatcher) {
    visualizationModelMap.put(SluggishnessVisualization.TITLE,
        new SluggishnessModel(dataDispatcher));
    visualizationModelMap.put(NetworkVisualization.TITLE,
        new NetworkVisualizationModel(dataDispatcher));
    visualizationModelMap.put(HintletReport.TITLE, new HintletReportModel(
        dataDispatcher.getHintletEngineHost()));
  }
}
