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
package com.google.speedtracer.client;

import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Element;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.topspin.ui.client.ClickEvent;
import com.google.gwt.topspin.ui.client.ClickListener;
import com.google.gwt.topspin.ui.client.Container;
import com.google.gwt.topspin.ui.client.DefaultContainerImpl;
import com.google.gwt.topspin.ui.client.Div;
import com.google.gwt.topspin.ui.client.InsertingContainerImpl;
import com.google.gwt.topspin.ui.client.KeyDownEvent;
import com.google.gwt.topspin.ui.client.KeyUpEvent;
import com.google.gwt.topspin.ui.client.MouseDownEvent;
import com.google.gwt.topspin.ui.client.MouseDownListener;
import com.google.gwt.topspin.ui.client.ResizeEvent;
import com.google.gwt.topspin.ui.client.ResizeListener;
import com.google.gwt.topspin.ui.client.Window;
import com.google.gwt.user.client.Timer;
import com.google.speedtracer.client.model.ApplicationState;
import com.google.speedtracer.client.model.HintRecord;
import com.google.speedtracer.client.model.HintletEngineHost;
import com.google.speedtracer.client.model.Visualization;
import com.google.speedtracer.client.timeline.Constants;
import com.google.speedtracer.client.timeline.GraphModel;
import com.google.speedtracer.client.timeline.TimeLineModel;
import com.google.speedtracer.client.timeline.fx.Zoom;
import com.google.speedtracer.client.timeline.fx.Zoom.CallBack;
import com.google.speedtracer.client.util.dom.DocumentExt;
import com.google.speedtracer.client.view.Controller;
import com.google.speedtracer.client.view.DetailViews;
import com.google.speedtracer.client.view.MainTimeLine;
import com.google.speedtracer.client.view.OverViewTimeLine;
import com.google.speedtracer.client.view.TimeScale;
import com.google.speedtracer.client.view.OverViewTimeLine.OverViewTimeLineModel;
import com.google.speedtracer.client.visualizations.model.NetworkTimeLineModel;
import com.google.speedtracer.client.visualizations.model.NetworkVisualization;
import com.google.speedtracer.client.visualizations.model.SluggishnessModel;
import com.google.speedtracer.client.visualizations.model.SluggishnessVisualization;
import com.google.speedtracer.client.visualizations.model.TransientMarkerModel;
import com.google.speedtracer.client.visualizations.model.VisualizationModel;
import com.google.speedtracer.client.visualizations.view.HintletReportDialog;

import java.util.ArrayList;
import java.util.List;

/**
 * Panel that contains the main UI components of the Monitor. All the TimeLine
 * and other visualizations get attached here.
 */
public class MonitorVisualizationsPanel extends Div {

  /**
   * CSS.
   */
  public interface Css extends CssResource {
    int borderWidth();

    String graphContainer();

    String tabList();

    String tabListEntry();

    String tabListEntrySelected();

    String timelineContainer();

    int timelineHeight();

    String visualizationPanel();
  }

  /**
   * Externalized interface.
   */
  public interface Resources extends TimeScale.Resources,
      OverViewTimeLine.Resources, MainTimeLine.Resources,
      HintletReportDialog.Resources {
    @Source("resources/MonitorVisualizationsPanel.css")
    MonitorVisualizationsPanel.Css monitorVisualizationsPanelCss();
  }

  /**
   * Single object to handle keyboard events and window resizes.
   */
  private class EventListener implements ResizeListener, HotKey.Handler {
    // Guard against re-entrant onresize event dispatches.
    private boolean inOnResize = false;

    public void onKeyDown(KeyDownEvent event) {
      int keyCode = event.getKeyCode();
      if (keyCode == HotKey.LEFT_ARROW || keyCode == HotKey.RIGHT_ARROW) {
        jog(keyCode);
      }
    }

    public void onKeyUp(KeyUpEvent event) {
      int keyCode = event.getKeyCode();
      if (keyCode == HotKey.LEFT_ARROW || keyCode == HotKey.RIGHT_ARROW) {
        detailsViewPanel.updateCurrentView(mainTimeLineModel.getLeftBound(),
            mainTimeLineModel.getRightBound(), false);
      }
    }

    /**
     * We need to guard against re-entrant resize events.
     * http://code.google.com/p/chromium/issues/detail?id=27653
     */
    public void onResize(ResizeEvent event) {
      if (inOnResize) {
        return;
      }
      inOnResize = true;
      mainTimeLine.recomputeGraphDimensions();
      mainTimeLineModel.refresh();
      detailsViewPanel.updateCurrentView(mainTimeLineModel.getLeftBound(),
          mainTimeLineModel.getRightBound(), false);
      inOnResize = false;
    }

    private void jog(int direction) {
      double left = mainTimeLineModel.getLeftBound();
      double right = mainTimeLineModel.getRightBound();
      double delta = (right - left) / 200;
      if (direction == HotKey.RIGHT_ARROW) {
        mainTimeLineModel.updateBounds(
            mainTimeLineModel.getLeftBound() + delta,
            mainTimeLineModel.getRightBound() + delta);
      }
      if (direction == HotKey.LEFT_ARROW) {
        mainTimeLineModel.updateBounds(left - delta, right - delta);
      }
      fixYAxisLabel();
    }
  }

  /**
   * Simple overloading subclass that fascilitates short circuiting the updating
   * of the details view.
   */
  private class ShortCircuitTimeLineModel extends TimeLineModel {
    public ShortCircuitTimeLineModel() {
      super(false, false);
    }

    @Override
    public void onDomainChange(double newValue) {
      loadedState.setLastDomainValue(newValue);
      super.onDomainChange(newValue);
    }

    @Override
    public void onModelDataRefreshTick(double now) {
      double left = getLeftBound();
      double right = getRightBound();
      if (now < right) {
        detailsViewPanel.updateCurrentView(left, right, false);
        fixYAxisLabel();
      }

      super.onModelDataRefreshTick(now);
    }

    @Override
    public void updateBounds(double leftBound, double rightBound) {
      // update the scale
      scale.updateScaleLabels(mainTimeLine.getCurrentGraphWidth(), leftBound,
          rightBound);
      super.updateBounds(leftBound, rightBound);
    }
  }

  /**
   * The list of Tabs on the left.
   */
  private class TabList extends Div {
    private Element previouslySelected;

    public TabList(Container container) {
      super(container);
      getElement().setClassName(
          resources.monitorVisualizationsPanelCss().tabList());
      getElement().getStyle().setPropertyPx("width",
          Constants.GRAPH_PIXEL_OFFSET);

      createTabs();

      // Make the first entry be the selected one.
      previouslySelected = getElement().getFirstChildElement();
      previouslySelected.setClassName(resources.monitorVisualizationsPanelCss().tabListEntry()
          + " "
          + resources.monitorVisualizationsPanelCss().tabListEntrySelected());
      selectVisualization(visualizations.get(0));
    }

    /**
     * Creates the tablist entries.
     */
    private void createTabs() {
      for (int i = 0, n = visualizations.size(); i < n; i++) {
        final Visualization<?, ?> viz = visualizations.get(i);
        String tabTitle = viz.getTitle() + " (" + viz.getSubtitle() + ")";
        DocumentExt doc = getElement().getOwnerDocument().cast();
        final DivElement entry = doc.createDivWithClassName(resources.monitorVisualizationsPanelCss().tabListEntry());
        entry.setInnerText(tabTitle);

        attachTransientMarkers(viz);

        ClickEvent.addClickListener(entry, entry, new ClickListener() {
          public void onClick(ClickEvent event) {
            highlightEntry(entry, viz);
          }
        });

        getElement().appendChild(entry);
      }
    }

    private void highlightEntry(Element entry, Visualization<?, ?> viz) {
      previouslySelected.setClassName(resources.monitorVisualizationsPanelCss().tabListEntry());
      previouslySelected = entry;
      previouslySelected.setClassName(resources.monitorVisualizationsPanelCss().tabListEntry()
          + " "
          + resources.monitorVisualizationsPanelCss().tabListEntrySelected());
      selectVisualization(viz);
    }
  }

  private static final int HINTLET_REFRESH_DELAY_MS = 1000;

  private final DetailViews detailsViewPanel;

  private ApplicationState loadedState;

  private final MainTimeLine mainTimeLine;

  private final TimeLineModel mainTimeLineModel;

  private final OverViewTimeLine overViewTimeLine;

  private final Resources resources;

  private final TimeScale scale;

  private Visualization<?, ?> selectedVisualization;

  private final List<Visualization<?, ?>> visualizations = new ArrayList<Visualization<?, ?>>();

  public MonitorVisualizationsPanel(Container parentContainer,
      Controller controller, ApplicationState initialState,
      MonitorVisualizationsPanel.Resources resources) {
    super(parentContainer);
    this.loadedState = initialState;
    this.resources = resources;
    // TODO(jaimeyap): UiBinder would cut this init code in half. Post release
    // bug to port all our stuff to use UiBinder. For rizzles.
    final Css css = resources.monitorVisualizationsPanelCss();
    setStyleName(css.visualizationPanel());
    Container container = new DefaultContainerImpl(getElement());

    DivElement timeLineContainerElem = getElement().getOwnerDocument().createDivElement();
    timeLineContainerElem.setClassName(css.timelineContainer());
    getElement().appendChild(timeLineContainerElem);

    // Create a little wrapper div to wrap the main and overview timelines.
    DivElement graphContainerElem = getElement().getOwnerDocument().createDivElement();
    graphContainerElem.setClassName(css.graphContainer());
    graphContainerElem.getStyle().setPropertyPx("left",
        Constants.GRAPH_PIXEL_OFFSET);
    timeLineContainerElem.appendChild(graphContainerElem);
    Container graphContainer = new DefaultContainerImpl(graphContainerElem);

    // Add the scale
    this.scale = new TimeScale(graphContainer, resources);

    // callback to update the details panel when transition changes.
    Zoom.CallBack transitionCallback = new CallBack() {
      public void onAnimationComplete() {
        fixYAxisLabel();
        detailsViewPanel.updateCurrentView(mainTimeLineModel.getLeftBound(),
            mainTimeLineModel.getRightBound(), true);
      }
    };

    // Add the MainTimeLine.
    this.mainTimeLineModel = new ShortCircuitTimeLineModel();
    this.mainTimeLine = new MainTimeLine(graphContainer, visualizations,
        mainTimeLineModel, transitionCallback, resources);

    // Graph overviews. Automatically monitors the MainTimeline.
    this.overViewTimeLine = new OverViewTimeLine(graphContainer, mainTimeLine,
        new OverViewTimeLineModel(), visualizations, resources);

    this.detailsViewPanel = new DetailViews(container, resources);

    controller.observe(mainTimeLine, overViewTimeLine);

    // Now load and populate the Visualization list.
    createVisualizations(initialState, resources);

    // Create the TabList. Stick it in before the timeline stuff.
    TabList tabList = new TabList(new InsertingContainerImpl(
        timeLineContainerElem, graphContainerElem));

    refresh();
    sinkEvents();

    preventNativeSelection(tabList.getElement(), graphContainerElem);
  }

  public MainTimeLine getMainTimeLine() {
    return mainTimeLine;
  }

  /**
   * Sets the <code>loadedState</code> to the specified {@link ApplicationState}
   * . It swaps in the new {@link VisualizationModel}s and reloads all the
   * Visualizations and graph properties.
   * 
   * @param state the {@link ApplicationState} we want to load
   */
  public void setApplicationState(ApplicationState state) {
    if (state.getLastDomainValue() > mainTimeLine.getModel().getMostRecentDomainValue()) {
      mainTimeLineModel.onDomainChange(state.getLastDomainValue());
    }
    // Update all visualizations that have been loaded
    for (int i = 0, n = visualizations.size(); i < n; i++) {
      Visualization<?, ?> viz = visualizations.get(i);
      viz.getModel().getGraphModel().removeDomainObserver(mainTimeLineModel);

      VisualizationModel vizModel = state.getVisualizationModel(viz.getTitle());
      viz.setModel(vizModel);
      vizModel.getGraphModel().addDomainObserver(mainTimeLineModel);
    }
    loadedState = state;
    mainTimeLineModel.updateBounds(state.getFirstDomainValue(),
        state.getLastDomainValue());
    overViewTimeLine.getModel().updateBounds(state.getFirstDomainValue(),
        state.getLastDomainValue());
    // Redraw a second frame so that it can rescale correctly
    refresh();
  }

  /**
   * Hook up the transient marker models associated with each visualization's
   * graph UI.
   */
  private void attachTransientMarkers(Visualization<?, ?> viz) {
    // load up any new transient markers in the new visualization.
    TransientMarkerModel model = viz.getCurrentEventMarkerModel();
    if (model != null) {
      model.addModelChangeListener(model.createTransientMarkerInstance(
          mainTimeLine.getGraphContainerElement(), mainTimeLine, resources));
    }
  }

  private void createVisualizations(ApplicationState initialState,
      MainTimeLine.Resources resources) {

    // Sluggishness
    SluggishnessModel sluggishnessModel = (SluggishnessModel) initialState.getVisualizationModel(SluggishnessVisualization.TITLE);
    SluggishnessVisualization sluggishnessVisualization = new SluggishnessVisualization(
        mainTimeLine, sluggishnessModel,
        initialState.getDataModel().getUiEventModel(),
        detailsViewPanel.getContainer(), resources);

    // Network Visualization
    NetworkTimeLineModel networkModel = (NetworkTimeLineModel) initialState.getVisualizationModel(NetworkVisualization.TITLE);
    NetworkVisualization networkVisualization = new NetworkVisualization(
        mainTimeLine, networkModel, detailsViewPanel.getContainer(), resources);

    // Load the visualization that we just added.
    loadVisualization(sluggishnessVisualization);
    loadVisualization(networkVisualization);

    // Setup the graphs to refresh when hintlet data arrives. Buffer the data
    // so that the screen doesn't jump from rapid hintlet data coming in.
    initialState.getDataModel().getHintletEngineHost().addHintListener(
        new HintletEngineHost.HintListener() {
          boolean queued = false;

          public void onHint(HintRecord hintlet) {
            if (queued) {
              return;
            }
            double hintletTime = hintlet.getTimestamp();
            if (hintletTime < mainTimeLineModel.getLeftBound()
                || hintletTime > mainTimeLineModel.getRightBound()) {
              // out of bounds - no need to refresh
              return;
            }

            Timer t = new Timer() {

              @Override
              public void run() {
                mainTimeLineModel.refresh();
                queued = false;
              }

            };
            t.schedule(HINTLET_REFRESH_DELAY_MS);
            queued = true;
          }

        });
  }

  /**
   * Updates the Y Axis label to be the max scale value for the currently
   * selected visualization.
   */
  private void fixYAxisLabel() {
    mainTimeLine.updateYAxisLabel(
        selectedVisualization.getGraphUiProps().getActiveMaxYAxisValue(),
        selectedVisualization.getModel().getGraphModel().getYAxisUnit());
  }

  /**
   * Adds a {@link Visualization} and its associated
   * {@link com.google.speedtracer.client.view.DetailView}. It also hooks up the
   * {@link MainTimeLine} to the underlying {@link GraphModel} associated with
   * each Visualization.
   * 
   * @param visualization
   */
  private void loadVisualization(Visualization<?, ?> visualization) {
    selectedVisualization = visualization;

    visualizations.add(visualization);
    detailsViewPanel.addViewForVisualization(visualization);

    GraphModel graphModel = visualization.getModel().getGraphModel();
    graphModel.addDomainObserver(mainTimeLineModel);

    mainTimeLineModel.refresh();
    fixYAxisLabel();
  }

  /**
   * Simply stops ugly native selection on components surrounding our main
   * graph. Accidental drags dirty the UI.
   */
  private void preventNativeSelection(Element tabList,
      DivElement graphContainerElem) {
    MouseDownListener listener = new MouseDownListener() {
      public void onMouseDown(MouseDownEvent event) {
        event.preventDefault();
      }
    };
    MouseDownEvent.addMouseDownListener(tabList, tabList, listener);
    MouseDownEvent.addMouseDownListener(graphContainerElem, graphContainerElem,
        listener);
  }

  private void refresh() {
    mainTimeLineModel.refresh();
    overViewTimeLine.refresh();
    detailsViewPanel.updateCurrentView(mainTimeLineModel.getLeftBound(),
        mainTimeLineModel.getRightBound(), true);
  }

  /**
   * Simple mechanism for setting the correct draw order for the visualizations
   * in the list.
   * 
   * @param moveToTop the visualization to move to the end.
   */
  private void reOrderVisualizations(Visualization<?, ?> moveToTop) {
    visualizations.remove(moveToTop);
    visualizations.add(moveToTop);
  }

  private void selectVisualization(Visualization<?, ?> viz) {
    reOrderVisualizations(viz);
    detailsViewPanel.setCurrentView(viz);
    refresh();
    selectedVisualization = viz;
    fixYAxisLabel();
  }

  private void sinkEvents() {
    EventListener listener = new EventListener();
    // WindowLevelEvents
    ResizeEvent.addResizeListener(Window.get(), Window.get(), listener);
    // TODO (jaimeyap): Re-implement jogging without using HotKey class.
  }
}
