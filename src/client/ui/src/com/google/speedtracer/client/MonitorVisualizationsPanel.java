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

import com.google.gwt.coreext.client.JSOArray;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.graphics.client.Color;
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
import com.google.speedtracer.client.model.ButtonDescription;
import com.google.speedtracer.client.model.DomContentLoadedEvent;
import com.google.speedtracer.client.model.HintRecord;
import com.google.speedtracer.client.model.HintletInterface;
import com.google.speedtracer.client.model.TabChangeDispatcher;
import com.google.speedtracer.client.model.TabChangeEvent;
import com.google.speedtracer.client.model.UiEventDispatcher;
import com.google.speedtracer.client.model.Visualization;
import com.google.speedtracer.client.model.WindowLoadEvent;
import com.google.speedtracer.client.timeline.Constants;
import com.google.speedtracer.client.timeline.GraphModel;
import com.google.speedtracer.client.timeline.TimeLineModel;
import com.google.speedtracer.client.timeline.fx.Zoom;
import com.google.speedtracer.client.timeline.fx.Zoom.CallBack;
import com.google.speedtracer.client.util.Url;
import com.google.speedtracer.client.util.dom.DocumentExt;
import com.google.speedtracer.client.util.dom.EventListenerOwner;
import com.google.speedtracer.client.view.Controller;
import com.google.speedtracer.client.view.DetailViews;
import com.google.speedtracer.client.view.MainTimeLine;
import com.google.speedtracer.client.view.OverViewTimeLine;
import com.google.speedtracer.client.view.TimeScale;
import com.google.speedtracer.client.view.TimelineMarks;
import com.google.speedtracer.client.view.OverViewTimeLine.OverViewTimeLineModel;
import com.google.speedtracer.client.visualizations.model.NetworkVisualization;
import com.google.speedtracer.client.visualizations.model.NetworkVisualizationModel;
import com.google.speedtracer.client.visualizations.model.SluggishnessModel;
import com.google.speedtracer.client.visualizations.model.SluggishnessVisualization;
import com.google.speedtracer.client.visualizations.model.VisualizationModel;
import com.google.speedtracer.client.visualizations.view.EventRecordColors;
import com.google.speedtracer.client.visualizations.view.HintletReportDialog;
import com.google.speedtracer.shared.EventRecordType;

import java.util.ArrayList;
import java.util.List;

/**
 * Panel that contains the main UI components of the Monitor. All the TimeLine
 * and other visualizations get attached here.
 */
public class MonitorVisualizationsPanel extends Div implements
    TabChangeDispatcher.Listener, UiEventDispatcher.LoadEventListener {

  /**
   * CSS.
   */
  public interface Css extends CssResource {
    int borderWidth();

    String buttonBar();

    String graphContainer();

    String tabList();

    String tabListEntry();

    String tabListEntrySelected();

    String timelineContainer();

    int timelineHeight();

    int topPadding();

    String visualizationPanel();
  }

  /**
   * Externalized interface.
   */
  public interface Resources extends TimeScale.Resources,
      OverViewTimeLine.Resources, MainTimeLine.Resources,
      HintletReportDialog.Resources, TimelineMarks.Resources {
    @Source("resources/MonitorVisualizationsPanel.css")
    MonitorVisualizationsPanel.Css monitorVisualizationsPanelCss();
  }

  /**
   * Single object to handle keyboard events and window resizes.
   */
  private class EventListener implements ResizeListener, HotKey.Handler {
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
            mainTimeLineModel.getRightBound());
      }
    }

    /**
     * Cache the graph dimensions.
     */
    public void onResize(ResizeEvent event) {
      mainTimeLine.recomputeGraphDimensions();
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
        detailsViewPanel.updateCurrentView(left, right);
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
    // Container Element for Visualization specific buttons.
    private final Element buttonBar;

    private final EventListenerOwner listenerOwner = new EventListenerOwner();

    private Element previouslySelected;

    public TabList(Container container) {
      super(container);
      Element elem = getElement();
      Css css = resources.monitorVisualizationsPanelCss();
      elem.setClassName(css.tabList());
      elem.getStyle().setPropertyPx("width", Constants.GRAPH_PIXEL_OFFSET);
      buttonBar = elem.getOwnerDocument().createDivElement();
      buttonBar.setClassName(css.buttonBar());
      elem.appendChild(buttonBar);

      // Initialize previouslySelected to the default tablist entry.
      previouslySelected = createTabs();
      if (previouslySelected != null) {
        previouslySelected.setClassName(css.tabListEntry() + " "
            + css.tabListEntrySelected());
        selectVisualization(visualizations.get(0));
      }
    }

    /**
     * Adds visualization specific
     * {@link com.google.gwt.topspin.ui.client.Button}s to the buttonBar.
     */
    private void addButtonBarButtons(Visualization<?, ?> viz) {
      listenerOwner.removeAllEventListeners();
      buttonBar.setInnerHTML("");

      Container buttonBarContainer = new DefaultContainerImpl(buttonBar);
      JSOArray<ButtonDescription> buttons = viz.getButtons();
      for (int i = 0, n = buttons.size(); i < n; i++) {
        buttons.get(i).createButton(buttonBarContainer, listenerOwner);
      }
    }

    /**
     * Creates the tablist entries.
     * 
     * @retutn returns the TabList entry Element that should be used as the
     *         default selection.
     */
    private Element createTabs() {
      Element defaultSelection = null;
      for (int i = 0, n = visualizations.size(); i < n; i++) {
        final Visualization<?, ?> viz = visualizations.get(i);
        String tabTitle = viz.getTitle() + " (" + viz.getSubtitle() + ")";
        DocumentExt doc = getElement().getOwnerDocument().cast();
        final DivElement entry = doc.createDivWithClassName(resources.monitorVisualizationsPanelCss().tabListEntry());
        entry.setInnerText(tabTitle);

        // The very first one should be flush with the top scale. So we push it
        // down a little. Also, we make the first visualization tablist entry be
        // the default selection.
        if (0 == i) {
          defaultSelection = entry;
          entry.getStyle().setMarginTop(
              resources.monitorVisualizationsPanelCss().topPadding(), Unit.PX);
        }

        ClickEvent.addClickListener(entry, entry, new ClickListener() {
          public void onClick(ClickEvent event) {
            selectTab(entry, viz);
          }
        });

        getElement().appendChild(entry);
      }
      return defaultSelection;
    }

    private void selectTab(Element entry, Visualization<?, ?> viz) {
      previouslySelected.setClassName(resources.monitorVisualizationsPanelCss().tabListEntry());
      previouslySelected = entry;
      previouslySelected.setClassName(resources.monitorVisualizationsPanelCss().tabListEntry()
          + " "
          + resources.monitorVisualizationsPanelCss().tabListEntrySelected());
      selectVisualization(viz);
    }

    private void selectVisualization(Visualization<?, ?> viz) {
      reOrderVisualizations(viz);
      detailsViewPanel.setCurrentView(viz);
      refresh();
      selectedVisualization = viz;
      fixYAxisLabel();
      addButtonBarButtons(viz);
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

  private final TimelineMarks timelineMarks;

  private final List<Visualization<?, ?>> visualizations = new ArrayList<Visualization<?, ?>>();

  public MonitorVisualizationsPanel(Container parentContainer,
      Controller controller, ApplicationState initialState,
      MonitorVisualizationsPanel.Resources resources) {
    super(parentContainer);
    this.loadedState = initialState;
    this.resources = resources;

    // Construct UI.
    final Css css = resources.monitorVisualizationsPanelCss();
    setStyleName(css.visualizationPanel());
    Container container = new DefaultContainerImpl(getElement());

    DivElement timeLineContainerElem = getElement().getOwnerDocument().createDivElement();
    timeLineContainerElem.setClassName(css.timelineContainer());
    getElement().appendChild(timeLineContainerElem);

    // Create a little wrapper div to wrap the main and overview timelines.
    DivElement graphContainerElem = getElement().getOwnerDocument().createDivElement();
    graphContainerElem.setClassName(css.graphContainer());
    // The left header + 1px border.
    graphContainerElem.getStyle().setPropertyPx("left",
        Constants.GRAPH_PIXEL_OFFSET + 1);
    timeLineContainerElem.appendChild(graphContainerElem);
    Container graphContainer = new DefaultContainerImpl(graphContainerElem);

    // Add the scale
    this.scale = new TimeScale(graphContainer, resources);

    // callback to update the details panel when transition changes.
    Zoom.CallBack transitionCallback = new CallBack() {
      public void onAnimationComplete() {
        fixYAxisLabel();
        double left = mainTimeLineModel.getLeftBound();
        double right = mainTimeLineModel.getRightBound();
        detailsViewPanel.updateCurrentView(left, right);
        timelineMarks.drawMarksInBounds(left, right);
      }
    };

    // Add the MainTimeLine.
    this.mainTimeLineModel = new ShortCircuitTimeLineModel();
    this.mainTimeLine = new MainTimeLine(graphContainer, visualizations,
        mainTimeLineModel, transitionCallback, resources);

    // Graph overviews. Automatically monitors the MainTimeline.
    this.overViewTimeLine = new OverViewTimeLine(graphContainer, mainTimeLine,
        new OverViewTimeLineModel(), visualizations, resources);

    // Create the DetailViews panel that will contain each DetailView.
    this.detailsViewPanel = new DetailViews(container, resources);

    // Create TimelineMarks for marking the timeline with vertical lines.
    timelineMarks = new TimelineMarks(detailsViewPanel.getContainer(),
        mainTimeLineModel.getGraphCalloutModel(), mainTimeLine, resources);
    // Subscribe to page refreshes and load events.
    initialState.getDataDispatcher().getTabChangeDispatcher().addListener(this);
    initialState.getDataDispatcher().getUiEventDispatcher().addLoadEventListener(
        this);

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

  public void clearTimelineMarks() {
    timelineMarks.clear();
  }

  public MainTimeLine getMainTimeLine() {
    return mainTimeLine;
  }

  /**
   * Mark line on timeline when the DOM content is loaded.
   */
  public void onDomContentLoaded(DomContentLoadedEvent event) {
    timelineMarks.addMark(event.getTime(),
        EventRecordColors.getColorForType(DomContentLoadedEvent.TYPE),
        EventRecordType.typeToString(DomContentLoadedEvent.TYPE),
        EventRecordType.typeToHelpString(DomContentLoadedEvent.TYPE));
    timelineMarks.drawMarksInBounds(mainTimeLineModel.getLeftBound(),
        mainTimeLineModel.getRightBound());
  }

  /**
   * Page transitions can also be marked so that if we navigate back to a
   * previous application state, we can see the point at which we tried to
   * navigate away.
   */
  public void onPageTransition(TabChangeEvent change) {
    Url refresh = new Url(change.getUrl());
    String resource = refresh.getLastPathComponent();
    String description = "Navigating to "
        + (resource.equals("") ? refresh.getUrl() : resource);
    timelineMarks.addMark(change.getTime(), Color.BLUE, description,
        description);
    timelineMarks.drawMarksInBounds(mainTimeLineModel.getLeftBound(),
        mainTimeLineModel.getRightBound());
  }

  /**
   * Mark line on timeline when we refresh a page.
   */
  public void onRefresh(TabChangeEvent change) {
    Url refresh = new Url(change.getUrl());
    String resource = refresh.getLastPathComponent();
    String description = "Refresh of "
        + (resource.equals("") ? refresh.getUrl() : resource);
    timelineMarks.addMark(change.getTime(), Color.LIGHT_BLUE, description,
        description);
    timelineMarks.drawMarksInBounds(mainTimeLineModel.getLeftBound(),
        mainTimeLineModel.getRightBound());
  }

  public void onWindowLoad(WindowLoadEvent event) {
    timelineMarks.addMark(event.getTime(),
        EventRecordColors.getColorForType(WindowLoadEvent.TYPE),
        EventRecordType.typeToString(WindowLoadEvent.TYPE),
        EventRecordType.typeToHelpString(WindowLoadEvent.TYPE));
    timelineMarks.drawMarksInBounds(mainTimeLineModel.getLeftBound(),
        mainTimeLineModel.getRightBound());
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
    // Maybe redraw timeline marks.
    timelineMarks.drawMarksInBounds(state.getFirstDomainValue(),
        state.getLastDomainValue());
    // Redraw a second frame so that it can rescale correctly
    refresh();
  }

  private void createVisualizations(ApplicationState initialState,
      MainTimeLine.Resources resources) {

    // Sluggishness
    SluggishnessModel sluggishnessModel = (SluggishnessModel) initialState.getVisualizationModel(SluggishnessVisualization.TITLE);
    SluggishnessVisualization sluggishnessVisualization = new SluggishnessVisualization(
        mainTimeLine, sluggishnessModel, detailsViewPanel.getContainer(),
        resources);

    // Network Visualization
    NetworkVisualizationModel networkModel = (NetworkVisualizationModel) initialState.getVisualizationModel(NetworkVisualization.TITLE);
    NetworkVisualization networkVisualization = new NetworkVisualization(
        mainTimeLine, networkModel, detailsViewPanel.getContainer(), resources);

    // Load the visualization that we just added.
    loadVisualization(sluggishnessVisualization);
    loadVisualization(networkVisualization);

    // Setup the graphs to refresh when hintlet data arrives. Buffer the data
    // so that the screen doesn't jump from rapid hintlet data coming in.
    initialState.getDataDispatcher().getHintletEngineHost().addHintListener(
        new HintletInterface.HintListener() {
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
        mainTimeLineModel.getRightBound());
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

  private void sinkEvents() {
    EventListener listener = new EventListener();
    // WindowLevelEvents
    ResizeEvent.addResizeListener(Window.get(), Window.get(), listener);
    // TODO (jaimeyap): Re-implement jogging without using HotKey class.
  }
}
