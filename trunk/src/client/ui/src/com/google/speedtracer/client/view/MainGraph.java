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
package com.google.speedtracer.client.view;

import com.google.gwt.dom.client.Element;
import com.google.gwt.graphics.client.Color;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.resources.client.ImageResource.ImageOptions;
import com.google.gwt.resources.client.ImageResource.RepeatStyle;
import com.google.speedtracer.client.model.Visualization;
import com.google.speedtracer.client.timeline.GraphModel;
import com.google.speedtracer.client.timeline.GraphUiProps;
import com.google.speedtracer.client.timeline.HighlightModel;
import com.google.speedtracer.client.timeline.TimeLineGraph;
import com.google.speedtracer.client.timeline.HighlightModel.HighlightEntry;
import com.google.speedtracer.client.util.dom.DocumentExt;
import com.google.speedtracer.client.visualizations.view.CurrentSelectionMarker;

import java.util.Iterator;
import java.util.List;

/**
 * The main graph that renders the current zoom range for all visualizations.
 */
public class MainGraph extends TimeLineGraph {
  /**
   * Default Style for {@link TimeLineGraph}.
   */
  public interface Css extends CssResource {
    String graphBase();

    String graphCanvas();

    String yAxisLabel();
  }

  /**
   * Resources for {@link TimeLineGraph}.
   */
  public interface Resources extends CurrentSelectionMarker.Resources {
    @Source("resources/scale_line.png")
    @ImageOptions(repeatStyle = RepeatStyle.Both)
    ImageResource graphScaleLines();

    @Source("resources/MainGraph.css")
    Css mainGraphCss();

    @Source("resources/tablist-background.png")
    @ImageOptions(repeatStyle = RepeatStyle.Horizontal)
    ImageResource tabListBackground();
  }

  // List of loaded visualizations.
  // This is a reference to the same collection owned by our parent timeline.
  // Mutations to this collection may have side effects.
  private final List<Visualization<?, ?>> visualizations;

  // Placeholder for the Y Axis label.
  private final Element yAxisScaleLabel;

  public MainGraph(MainTimeLine parent,
      List<Visualization<?, ?>> visualizations, Resources resources) {
    super(parent);
    getElement().setClassName(resources.mainGraphCss().graphBase());
    canvas.getElement().setClassName(resources.mainGraphCss().graphCanvas());
    this.visualizations = visualizations;
    yAxisScaleLabel = DocumentExt.get().createDivWithClassName(
        resources.mainGraphCss().yAxisLabel());
    getElement().appendChild(yAxisScaleLabel);
  }

  @Override
  protected void paint() {
    double coordDelta = getCoordDelta();
    double leftBound = getLeftBound();
    double rightBound = getRightBound();
    double domainDelta = getDomainDelta();
    canvas.clear();
    // We draw all the other ones with global opacity on to de-emphasize them.
    for (int i = 0, n = visualizations.size() - 1; i < n; i++) {
      Visualization<?, ?> viz = visualizations.get(i);
      // Render the background graphs with lower opacity.
      canvas.setGlobalAlpha(0.25);
      paintGraph(coordDelta, leftBound, rightBound, domainDelta, viz.getGraphUiProps(),
          viz.getModel().getGraphModel());
      paintHighlights(coordDelta, leftBound, rightBound, domainDelta,
          viz.getModel().getHighlightModel());
    }

    // Draw the last graph clearly with higher opacity.
    canvas.setGlobalAlpha(0.7);
    Visualization<?, ?> viz = visualizations.get(visualizations.size() - 1);
    paintGraph(coordDelta, leftBound, rightBound, domainDelta,
        viz.getGraphUiProps(), viz.getModel().getGraphModel());
    canvas.setGlobalAlpha(0.9);
    paintHighlights(coordDelta, leftBound, rightBound, domainDelta,
        viz.getModel().getHighlightModel());
  }

  protected void updateScaleLabel(double value, String yAxisUnit) {
    yAxisScaleLabel.setInnerHTML("&nbsp;" + (int) value + yAxisUnit);
  }

  // TODO(zundel): Change to HintletIndictator.getSeverityColor() when it
  // becomes available.
  private Color getHighlightColor(int severity) {
    Color color;
    switch (severity) {
      case 1:
        color = Color.GREEN;
        break;
      case 2:
        color = Color.ORANGE;
        break;
      case 3:
        color = Color.RED;
        break;
      default:
        // This color has no business being here.
        color = Color.CYAN;
    }
    return color;
  }

  private void paintGraph(double coordDelta, double leftBound,
      double rightBound, double domainDelta, GraphUiProps graphUiProps,
      GraphModel model) {
    canvas.setStrokeStyle(graphUiProps.getStrokeColor());
    canvas.setFillStyle(graphUiProps.getGraphColor());

    double maxYValueInWindow = graphUiProps.getYAxisScaleCap();
    double yAdjustment = COORD_Y_HEIGHT / graphUiProps.getActiveMaxYAxisValue();

    canvas.setLineWidth(2);

    // Redraw timeline
    canvas.beginPath();
    canvas.moveTo(0, COORD_Y_HEIGHT);

    for (int x = 0, p = getPlotPrecision(); x <= p; x++) {
      double xVal = x * coordDelta;
      double yVal = model.getRangeValue(leftBound + (domainDelta * x),
          domainDelta);

      // Log the max Y value drawn in this current window.
      // If we ever want a graph to plot negative Y values, we would need to
      // override this paint method anyways, but we should be aware that we
      // would want to log the Abs value here instead.
      maxYValueInWindow = (yVal > maxYValueInWindow) ? yVal : maxYValueInWindow;

      canvas.lineTo(xVal, COORD_Y_HEIGHT - (yVal * yAdjustment));
    }

    canvas.lineTo(COORD_X_WIDTH, COORD_Y_HEIGHT);
    canvas.closePath();

    canvas.fill();

    // Draw Start and End regions
    double now = getTimeLine().getModel().getMostRecentDomainValue();
    double timeToCoordConversion = getCoordsPerTime();

    if (now < rightBound) {
      double end = (now - leftBound) * timeToCoordConversion;
      canvas.setFillStyle(Color.LIGHT_GREY);
      canvas.fillRect(end, 0, COORD_X_WIDTH - end, COORD_Y_HEIGHT);
      if (now > leftBound) {
        canvas.setFillStyle(Color.GREEN);
        canvas.fillRect(end, 0, 2, COORD_Y_HEIGHT);
      }
    }

    if (leftBound < 0) {
      canvas.setFillStyle(Color.RED);

      canvas.fillRect((-leftBound * timeToCoordConversion) - 4, 0, 2,
          COORD_Y_HEIGHT);
    }

    // Remember the active max value for this graph
    graphUiProps.setActiveMaxYAxisValue(maxYValueInWindow);
  }

  /**
   * Highlights indicate special data at these points (e.g. some hintlets
   * present).
   * 
   * @param highlights
   */
  private void paintHighlights(double coordDelta, double leftBound,
      double rightBound, double domainDelta, HighlightModel highlights) {

    // If there is no highlight data, then bail.
    if (highlights == null || canvas == null) {
      return;
    }

    double coordsPerDomain = COORD_X_WIDTH / (rightBound - leftBound);

    Iterator<HighlightEntry> it = highlights.getRangeValues(leftBound,
        rightBound, domainDelta);
    while (it.hasNext()) {
      HighlightEntry highlightEntry = it.next();

      double xVal = (highlightEntry.getKey() - leftBound) * coordsPerDomain;
      int val = highlightEntry.getValue();
      Color color = getHighlightColor(val);

      canvas.beginPath();
      canvas.setStrokeStyle(color);
      canvas.setLineWidth(2);
      canvas.moveTo(xVal, COORD_Y_HEIGHT);
      canvas.lineTo(xVal, COORD_Y_HEIGHT * .8);
      canvas.stroke();
    }
  }
}
