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
package com.google.speedtracer.client.view;

import com.google.gwt.graphics.client.Color;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.speedtracer.client.model.Visualization;
import com.google.speedtracer.client.timeline.GraphModel;
import com.google.speedtracer.client.timeline.GraphUiProps;
import com.google.speedtracer.client.timeline.TimeLineGraph;

import java.util.List;

/**
 * The single graph that sits in the OverViewTimeLine at the bottom of the
 * screen.
 */
public class OverViewGraph extends TimeLineGraph {
  /**
   * Css stylenames.
   */
  public interface Css extends CssResource {
    String graphBase();

    String graphCanvas();
  }

  /**
   * Resources for {@link TimeLineGraph}.
   */
  public interface Resources extends ClientBundle {
    @Source("resources/OverViewGraph.css")
    Css overViewGraphCss();
  }

  // List of loaded visualizations.
  // This is a reference to the same collection owned by our parent timeline.
  // Mutations to this collection may have side effects.
  private final List<Visualization<?, ?>> visualizations;

  public OverViewGraph(OverViewTimeLine parent,
      List<Visualization<?, ?>> visualizations,
      OverViewGraph.Resources resources) {
    super(parent);
    getElement().setClassName(resources.overViewGraphCss().graphBase());
    canvas.getElement().setClassName(resources.overViewGraphCss().graphCanvas());
    this.visualizations = visualizations;
  }

  @Override
  protected void paint() {
    canvas.clear();
    for (int i = 0, n = visualizations.size(); i < n; i++) {
      Visualization<?, ?> viz = visualizations.get(i);
      GraphModel model = viz.getModel().getGraphModel();
      GraphUiProps graphUiProps = viz.getGraphUiProps();
      Color graphColor = graphUiProps.getGraphColor();
      Color graphStrokeColor = graphUiProps.getStrokeColor();

      double maxYValue = Math.max(model.getMaxEncounteredValue(),
          graphUiProps.getYAxisScaleCap());

      double yAdjustment = COORD_Y_HEIGHT / maxYValue;

      canvas.setLineWidth(2);
      canvas.setStrokeStyle(graphStrokeColor);
      canvas.setFillStyle(graphColor);
      canvas.setGlobalAlpha(0.7);

      // Redraw timeline
      canvas.beginPath();
      canvas.moveTo(0, COORD_Y_HEIGHT);

      for (int x = 0, p = getPlotPrecision(); x <= p; x++) {
        double xVal = (double) x * getCoordDelta();
        double yVal = model.getRangeValue(getLeftBound()
            + (getDomainDelta() * (double) x), getDomainDelta())
            * yAdjustment;

        canvas.lineTo(xVal, COORD_Y_HEIGHT - yVal);
      }

      canvas.lineTo(COORD_X_WIDTH, COORD_Y_HEIGHT);
      canvas.closePath();

      canvas.stroke();
      canvas.fill();
    }
  }
}
