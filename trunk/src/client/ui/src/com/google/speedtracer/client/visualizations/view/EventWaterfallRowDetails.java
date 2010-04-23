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
package com.google.speedtracer.client.visualizations.view;

import com.google.gwt.coreext.client.IterableFastStringMap;
import com.google.gwt.coreext.client.JSOArray;
import com.google.gwt.coreext.client.JsIntegerDoubleMap;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.HeadingElement;
import com.google.gwt.dom.client.TableCellElement;
import com.google.gwt.dom.client.TableRowElement;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.graphics.client.charts.ColorCodedDataList;
import com.google.gwt.graphics.client.charts.ColorCodedValue;
import com.google.gwt.graphics.client.charts.PieChart;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.topspin.ui.client.ClickEvent;
import com.google.gwt.topspin.ui.client.ClickListener;
import com.google.gwt.topspin.ui.client.Container;
import com.google.gwt.topspin.ui.client.DefaultContainerImpl;
import com.google.gwt.topspin.ui.client.Div;
import com.google.gwt.topspin.ui.client.ResizeEvent;
import com.google.gwt.topspin.ui.client.ResizeListener;
import com.google.gwt.topspin.ui.client.Table;
import com.google.gwt.topspin.ui.client.Widget;
import com.google.speedtracer.client.SourceViewer;
import com.google.speedtracer.client.SourceViewerServer;
import com.google.speedtracer.client.SymbolServerService;
import com.google.speedtracer.client.SourceViewer.SourcePresenter;
import com.google.speedtracer.client.SourceViewer.SourceViewerInitializedCallback;
import com.google.speedtracer.client.SourceViewer.SourceViewerLoadedCallback;
import com.google.speedtracer.client.SourceViewerServer.ActionCompletedCallback;
import com.google.speedtracer.client.model.DataModel;
import com.google.speedtracer.client.model.EvalScript;
import com.google.speedtracer.client.model.HintRecord;
import com.google.speedtracer.client.model.JavaScriptExecutionEvent;
import com.google.speedtracer.client.model.JavaScriptProfile;
import com.google.speedtracer.client.model.LogEvent;
import com.google.speedtracer.client.model.NetworkResource;
import com.google.speedtracer.client.model.NetworkResourceModel;
import com.google.speedtracer.client.model.PaintEvent;
import com.google.speedtracer.client.model.ParseHtmlEvent;
import com.google.speedtracer.client.model.ResourceDataReceivedEvent;
import com.google.speedtracer.client.model.TimerCleared;
import com.google.speedtracer.client.model.TimerFiredEvent;
import com.google.speedtracer.client.model.TimerInstalled;
import com.google.speedtracer.client.model.UiEvent;
import com.google.speedtracer.client.model.XhrLoadEvent;
import com.google.speedtracer.client.model.XhrReadyStateChangeEvent;
import com.google.speedtracer.client.util.Command;
import com.google.speedtracer.client.util.TimeStampFormatter;
import com.google.speedtracer.client.util.Url;
import com.google.speedtracer.client.util.dom.WindowExt;
import com.google.speedtracer.client.visualizations.view.FilteringScrollTable.RowDetails;
import com.google.speedtracer.client.visualizations.view.JavaScriptProfileRenderer.ResizeCallback;
import com.google.speedtracer.client.visualizations.view.Tree.ExpansionChangeListener;
import com.google.speedtracer.client.visualizations.view.Tree.Item;
import com.google.speedtracer.client.visualizations.view.Tree.SelectionChangeListener;
import com.google.speedtracer.shared.EventRecordType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Details component for a UiEvent.
 * 
 * This class contains all the widgetry for the expanded event views. It also
 * exposes a method for getting an element containing aggregate statistics for
 * the UiEvent.
 */
public class EventWaterfallRowDetails extends RowDetails implements
    SourcePresenter {
  /**
   * Styles.
   */
  public interface Css extends CssResource {
    String detailsKeyColumn();

    String detailsLayout();

    String detailsTable();

    String detailsTableKey();

    String eventBreakdownHeader();

    String hintletList();

    String pieChartContainer();

    int uiPadding();
  }

  /**
   * Externalized Resources.
   */
  public interface Resources extends PieChart.Resources,
      HintletRecordsTree.Resources, LazyEventTree.Resources,
      StackFrameRenderer.Resources, SourceViewer.Resources,
      JavaScriptProfileRenderer.Resources, ScopeBar.Resources,
      ColorCodedDataList.Resources, FilteringScrollTable.Resources {
    @Source("resources/EventWaterfallRowDetails.css")
    Css eventWaterfallRowDetailsCss();
  }

  /**
   * Class that handles clicks on the tab bar for the JavaScript Profiles.
   */
  private class ProfileClickListener implements ClickListener {
    private int profileType;

    private ProfileClickListener(int profileType) {
      this.profileType = profileType;
    }

    public void onClick(ClickEvent clickEvent) {
      jsProfileRenderer.show(profileType);
      Command.defer(new Command.Method() {
        public void execute() {
          fixHeightOfParentRow();
        }
      });
    }
  }

  private List<ColorCodedValue> data;

  private Table detailsTable;

  private Container detailsTableContainer;

  private TableCellElement eventTraceContainerCell;

  private final EventWaterfall eventWaterfall;

  private Command.Method heightFixer;

  private HintletRecordsTree hintletTree;

  // Profiles are processed in the background. This variable tells the click
  // handler if the profile needs to be refreshed.
  private boolean javaScriptProfileInProgress = false;

  private JavaScriptProfileRenderer jsProfileRenderer;

  private Div profileDiv;

  private final EventWaterfallRowDetails.Resources resources;

  private SourceViewer sourceViewer;

  private final SourceSymbolClickListener symbolClickListener;

  private DivElement treeDiv;

  protected EventWaterfallRowDetails(EventWaterfall eventWaterfall,
      EventWaterfallRow parent, EventWaterfallRowDetails.Resources resources) {
    eventWaterfall.super(parent);
    parent.setDetails(this);
    this.resources = resources;
    this.eventWaterfall = eventWaterfall;
    this.symbolClickListener = new SourceSymbolClickListener() {
      public void onSymbolClicked(String resourceUrl,
          String sourceViewerServer, int lineNumber, int column,
          String absoluteFilePath) {
        showSource(resourceUrl, sourceViewerServer, lineNumber, 0,
            absoluteFilePath);
      }
    };
  }

  @Override
  public EventWaterfallRow getParentRow() {
    return (EventWaterfallRow) super.getParentRow();
  }

  public boolean isJavaScriptProfileInProgress() {
    return this.javaScriptProfileInProgress;
  }

  /**
   * See if any of the data has changed and refresh the view.
   */
  public void refresh() {
    if (!isCreated()) {
      return;
    }
    if (hintletTree == null) {
      hintletTree = createHintletTree(treeDiv);
    } else {
      hintletTree.refresh(getParentRow().getEvent().getHintRecords());
    }
  }

  public void showSource(final String resourceUrl, String sourceViewerServer,
      final int lineNumber, final int column, String absoluteFilePath) {
    // TODO(jaimeyap): Put up a spinner or something. It may take a while to
    // display the resource.

    // First try to jump to IDE.
    SourceViewerServer.jumpToIde(sourceViewerServer, absoluteFilePath,
        lineNumber, new ActionCompletedCallback() {

          public void onFail() {
            // Just show the source in our source viewer.
            showSourceInSourceViewer(resourceUrl,
                new SourceViewerLoadedCallback() {

                  public void onSourceFetchFail(int statusCode,
                      SourceViewer viewer) {
                    viewer.hide();
                  }

                  public void onSourceViewerLoaded(SourceViewer viewer) {
                    // The viewer should not be loaded at the URL we
                    // care about.
                    viewer.show();
                    viewer.highlightLine(lineNumber);
                    if (column > 0) {
                      viewer.markColumn(lineNumber, column);
                      viewer.scrollColumnMarkerIntoView();
                    } else {
                      viewer.scrollHighlightedLineIntoView();
                    }
                  }
                });
          }

          public void onSuccess() {
            // Do nothing. IDE should have focus.
          }

        });
  }

  @Override
  protected Element createElement() {
    Element elem = super.createElement();
    Container myContainer = new DefaultContainerImpl(elem);
    ensureData();

    // Now we need to layout the rest of the row details
    Table detailsLayout = new Table(myContainer);
    detailsLayout.setFixedLayout(true);
    detailsLayout.getElement().setClassName(getCss().detailsLayout());

    // We have a 1 row, 2 column layout
    TableRowElement row = detailsLayout.insertRow(-1);

    // Create the first column.
    eventTraceContainerCell = row.insertCell(-1);

    // Add the piechart and detailsTable to the second column
    TableCellElement detailsTableCell = row.insertCell(-1);
    detailsTableCell.getStyle().setPropertyPx("paddingRight",
        getCss().uiPadding());

    // Attach the pie chart.
    detailsTableContainer = new DefaultContainerImpl(detailsTableCell);
    PieChart pieChart = createPieChart(detailsTableContainer);
    int pieChartHeight = pieChart.getElement().getOffsetHeight()
        + getCss().uiPadding();

    this.detailsTable = createDetailsTable(detailsTableContainer,
        pieChartHeight, getParentRow().getEvent());

    // Now we populate the first column.
    Container eventTraceContainer = new DefaultContainerImpl(
        eventTraceContainerCell);
    treeDiv = eventTraceContainer.getDocument().createDivElement();
    eventTraceContainerCell.appendChild(treeDiv);

    hintletTree = createHintletTree(treeDiv);
    createEventTrace(eventTraceContainer, pieChartHeight);

    profileDiv = new Div(eventTraceContainer);
    updateProfile();

    // Ensure that window resizes don't mess up our row size due to text
    // reflow. Things may need to grow or shrink.
    manageEventListener(ResizeEvent.addResizeListener(WindowExt.get(),
        WindowExt.get(), new ResizeListener() {
          public void onResize(ResizeEvent event) {
            if (heightFixer == null && getParentRow().isExpanded()) {
              heightFixer = new Command.Method() {
                public void execute() {
                  // We don't want to do this for each resize, but once at
                  // the end.
                  fixHeightOfParentRow();
                  heightFixer = null;
                }
              };

              Command.defer(heightFixer, 200);
            }
          }
        }));
    return elem;
  }

  /**
   * Conditionally puts the profile UI below the trace tree.
   */
  void updateProfile() {
    if (getParentRow().getEvent().hasJavaScriptProfile()) {
      javaScriptProfileInProgress = false;
      buildProfileUi();
    } else if (getParentRow().getEvent().processingJavaScriptProfile()) {
      this.javaScriptProfileInProgress = true;
      profileDiv.setHtml("<h3>Profile</h3><div><i>Processing...</i></div>");
    } else {
      profileDiv.setHtml("");
    }
  }

  private void buildProfileUi() {
    profileDiv.getElement().setInnerHTML("");
    Container container = new DefaultContainerImpl(profileDiv.getElement());
    HeadingElement profileHeading = container.getDocument().createHElement(3);
    profileDiv.getElement().appendChild(profileHeading);
    profileHeading.setInnerText("Profile");
    ScopeBar bar = new ScopeBar(container, resources);
    // TODO(jaimeyap): This will always return the most recent page we are
    // viewing. This is a bug. We should have ApplicationState know the current
    // TabDescription.
    String currentAppUrl = eventWaterfall.getVisualization().getModel().getDataModel().getTabDescription().getUrl();
    jsProfileRenderer = new JavaScriptProfileRenderer(
        container,
        resources,
        this,
        SymbolServerService.getSymbolServerController(new Url(currentAppUrl)),
        this,
        eventWaterfall.getVisualization().getModel().getJavaScriptProfileForEvent(
            getParentRow().getEvent()), new SourceSymbolClickListener() {
          public void onSymbolClicked(String resourceUrl,
              String sourceViewerServer, int lineNumber, int column,
              String absoluteFilePath) {
            showSource(resourceUrl, sourceViewerServer, lineNumber, 0,
                absoluteFilePath);
          }
        }, new ResizeCallback() {
          public void onResize() {
            fixHeightOfParentRow();
          }
        });

    Element flatProfile = bar.add("Flat", new ProfileClickListener(
        JavaScriptProfile.PROFILE_TYPE_FLAT));
    bar.add("Top Down", new ProfileClickListener(
        JavaScriptProfile.PROFILE_TYPE_TOP_DOWN));
    bar.add("Bottom Up", new ProfileClickListener(
        JavaScriptProfile.PROFILE_TYPE_BOTTOM_UP));
    bar.setSelected(flatProfile, true);
  }

  /**
   * Creates the details table information for a single UiEvent selected in the
   * event trace tree.
   * 
   * @param parent The parent {@link Container} that we will be attaching the
   *          table to.
   * @param pieChartHeight The height in pixels of the piechart so that we can
   *          position ourselves accordingly.
   * @param e The {@link UiEvent} that we will be displaying the details of.
   * 
   * @return The {@link Table} that contains the detail information
   */
  private Table createDetailsTable(Container parent, int pieChartHeight,
      final UiEvent e) {
    IterableFastStringMap<CellRenderer> detailsMap = getDetailsMapForEvent(e);
    final Table table = new Table(parent);

    detailsMap.iterate(new IterableFastStringMap.IterationCallBack<CellRenderer>() {
      private boolean hasRow = false;

      public void onIteration(String key, CellRenderer cellRenderer) {
        // If we have at least one piece of data for this table, we add a
        // header
        if (!hasRow) {
          // Establish column widths.
          Element keyCol = Document.get().createElement("th");
          keyCol.setClassName(getCss().detailsKeyColumn());
          Element valCol = Document.get().createElement("th");
          table.getTableHead().appendChild(keyCol);
          table.getTableHead().appendChild(valCol);

          // Now add the title
          Element titleRow = Document.get().createElement("tr");
          Element title = Document.get().createElement("th");
          title.setAttribute("colspan", "2");
          title.setAttribute("align", "left");
          title.setInnerText("Details for "
              + UiEvent.typeToDetailedTypeString(e));
          title.getStyle().setWidth(100, Unit.PX);
          titleRow.appendChild(title);
          table.getTableHead().appendChild(titleRow);
          hasRow = true;
        }

        TableRowElement row = table.appendRow();
        TableCellElement cell = row.insertCell(-1);
        cell.setClassName(getCss().detailsTableKey());
        String rowKey = key.substring(1);
        cell.setInnerText(rowKey);
        cell = row.insertCell(-1);
        cellRenderer.render(cell);
      }
    });

    table.addStyleName(getCss().detailsTable());
    // ensure that the table is positioned below the pieChart
    table.getElement().getStyle().setPropertyPx("marginTop", pieChartHeight);
    return table;
  }

  private Tree createEventTrace(Container parent, final int pieChartHeight) {
    Widget header = new Widget(parent.getDocument().createHElement(2), parent) {
    };
    header.setStyleName(getCss().eventBreakdownHeader());
    header.getElement().setInnerText("Event Trace");

    final LazyEventTree tree = new LazyEventTree(parent,
        eventWaterfall.getPresenter(), getParentRow().getEvent(),
        getParentRow().getEventBreakdown(), resources);

    // Hook listeners to tree list to monitor selection changes and
    // expansion changes.
    tree.addSelectionChangeListener(new SelectionChangeListener() {
      Element offsetParent = getParentRow().getElement();

      public void onSelectionChange(ArrayList<Item> selected) {
        // Wipe the old table
        detailsTable.destroy();

        // Sort the nodes by start time.
        Collections.sort(selected, new Comparator<Item>() {

          public int compare(Item node1, Item node2) {
            UiEvent e1 = (UiEvent) node1.getItemTarget();
            UiEvent e2 = (UiEvent) node2.getItemTarget();

            return Double.compare(e1.getTime(), e2.getTime());
          }
        });

        Item newSelection = selected.get(selected.size() - 1);
        // Find how far to move table down to current selection.
        // We have to recursively walk up to compute the correct offset top.
        // We will encounter the UI padding two extra times along the way
        // crossing the tree boundary and crossing the details div boundary,
        // totally 3 encounters with padding we have to account for.
        int minTableOffset = Math.max(pieChartHeight,
            recursiveGetOffsetTop(newSelection.getElement())
                - (3 * getCss().uiPadding()));

        if (selected.size() == 1) {
          // We have a single selection. Simply display the details for the
          // single node.
          detailsTable = createDetailsTable(detailsTableContainer,
              minTableOffset, (UiEvent) newSelection.getItemTarget());

        } else {
          // Display aggregate information over the range of nodes.
          detailsTable = createMultiNodeDetailsTable(detailsTableContainer,
              minTableOffset, selected);
        }

        fixHeightOfParentRow();
      }

      private int recursiveGetOffsetTop(Element node) {
        if (node == null || node.getOffsetParent() == null
            || node.equals(offsetParent)) {
          return 0;
        } else {
          return node.getOffsetTop()
              + recursiveGetOffsetTop(node.getOffsetParent());
        }
      }

    });

    tree.addExpansionChangeListener(new ExpansionChangeListener() {

      public void onExpansionChange(Item changedItem) {
        fixHeightOfParentRow();
      }

    });

    // We make sure to have the tree cleaned up when we clean up ourselves.
    manageEventListener(tree.getRemover());

    return tree;
  }

  private HintletRecordsTree createHintletTree(DivElement parent) {
    if (!getParentRow().getEvent().hasHintRecords()) {
      return null;
    }

    JSOArray<HintRecord> hintlets = getParentRow().getEvent().getHintRecords();
    parent.setClassName(getCss().hintletList());

    HintletRecordsTree tree = new HintletRecordsTree(new DefaultContainerImpl(
        parent), hintlets, resources);

    // Hook listener to tree list to monitor expansion changes.
    tree.addExpansionChangeListener(new ExpansionChangeListener() {

      public void onExpansionChange(Item changedItem) {
        fixHeightOfParentRow();
      }

    });

    // We make sure to have the tree cleaned up when we clean up ourselves.
    manageEventListener(tree.getRemover());

    return tree;
  }

  private Table createMultiNodeDetailsTable(Container parent,
      int pieChartHeight, ArrayList<Item> selectedNodes) {
    Table table = new Table(parent);
    table.setFixedLayout(true);
    table.addStyleName(getCss().detailsTable());

    // Assume that List is sorted.
    UiEvent earliest = (UiEvent) selectedNodes.get(0).getItemTarget();
    UiEvent latest = (UiEvent) selectedNodes.get(selectedNodes.size() - 1).getItemTarget();
    double delta = latest.getTime() - earliest.getTime();

    TableRowElement row = table.appendRow();
    TableCellElement cell = row.insertCell(-1);
    cell.setClassName(getCss().detailsTableKey());
    cell.setInnerText("Time Delta");
    cell = row.insertCell(-1);
    cell.setInnerText(TimeStampFormatter.formatMilliseconds(delta));

    // ensure that the table is positioned below the pieChart
    table.getElement().getStyle().setPropertyPx("marginTop", pieChartHeight);
    return table;
  }

  private PieChart createPieChart(Container parent) {
    // We put an extra div in there to center our piechart and to apply
    // the rounded corners and backing layer styles underneath the piechart.
    Div centeringDiv = new Div(parent);
    centeringDiv.addStyleName(getCss().pieChartContainer());
    PieChart chart = new PieChart(centeringDiv, data, resources);
    chart.showLegend();

    return chart;
  }

  private void ensureData() {
    if (data == null) {
      data = new ArrayList<ColorCodedValue>();
      UiEvent event = getParentRow().getEvent();

      JsIntegerDoubleMap durations = event.getTypeDurations();
      assert (durations != null);

      durations.iterate(new JsIntegerDoubleMap.IterationCallBack() {

        public void onIteration(int key, double val) {
          if (val > 0) {
            data.add(new ColorCodedValue(EventRecordType.typeToString(key),
                val, EventRecordColors.getColorForType(key)));
          }
        }

      });

      Collections.sort(data);
    }
  }

  private void fixHeightOfParentRow() {
    // Our height should be the size of the details panel + the row height
    int height = getElement().getOffsetHeight()
        + resources.filteringScrollTableCss().rowHeight();
    getParentRow().getElement().getStyle().setPropertyPx("height", height);
  }

  private EventWaterfallRowDetails.Css getCss() {
    return resources.eventWaterfallRowDetailsCss();
  }

  /**
   * Goes to concrete implementation to construct details map for an event.
   * 
   * @param e the {@link UiEvent}
   * @return the details Map for the UiEvent
   */
  private IterableFastStringMap<CellRenderer> getDetailsMapForEvent(UiEvent e) {
    IterableFastStringMap<CellRenderer> details = new IterableFastStringMap<CellRenderer>();

    details.put("Description", new StringCellRenderer(e.getHelpString()));
    details.put("@", new StringCellRenderer(
        TimeStampFormatter.formatMilliseconds(e.getTime())));
    if (e.getDuration() > 0) {
      details.put("Duration", new StringCellRenderer(
          TimeStampFormatter.formatMilliseconds(e.getDuration(), 3)));
    }

    String currentAppUrl = eventWaterfall.getVisualization().getModel().getDataModel().getTabDescription().getUrl();

    // This is the backtrace output by our prototype.
    String backTrace = e.getBackTrace();
    if (backTrace != null) {
      details.put("Stack Trace", new StackTraceRenderer(backTrace,
          symbolClickListener, this, currentAppUrl, this, true, resources));
    }
    // This is the caller location output currently by WebKit.
    if (e.hasCallLocation()) {
      // TODO(jaimeyap): figure out what we want to do with our backTrace vs the
      // stuff landed in WebKit. Can we get symbol names? For now we re-use the
      // same infrastructure to render this.
      backTrace = "ignored," + e.getCallerScriptName() + ","
          + e.getCallerScriptLine() + ",-1,," + e.getCallerFunctionName();
      details.put("Caused by", new StackTraceRenderer(backTrace,
          symbolClickListener, this, currentAppUrl, this, true, resources));
    }

    switch (e.getType()) {
      case TimerFiredEvent.TYPE:
        TimerInstalled timerData = eventWaterfall.getSourceModel().getTimerMetaData(
            e.<TimerInstalled> cast().getTimerId());
        populateDetailsForTimerInstall(timerData, details);
        break;
      case TimerInstalled.TYPE:
        populateDetailsForTimerInstall(e.<TimerInstalled> cast(), details);
        break;
      case TimerCleared.TYPE:
        details.put("Cleared Timer Id", new StringCellRenderer(
            e.<TimerCleared> cast().getTimerId() + ""));
        break;
      case PaintEvent.TYPE:
        PaintEvent paintEvent = e.cast();
        details.put("Origin", new StringCellRenderer(paintEvent.getX() + ", "
            + paintEvent.getY()));
        details.put("Size", new StringCellRenderer(paintEvent.getWidth()
            + " x " + paintEvent.getHeight()));
        break;
      case ParseHtmlEvent.TYPE:
        ParseHtmlEvent parseHtmlEvent = e.cast();
        details.put("Line Number", new StringCellRenderer(
            parseHtmlEvent.getStartLine() + ""));
        details.put("Length", new StringCellRenderer(parseHtmlEvent.getLength()
            + " characters"));
        break;
      case EvalScript.TYPE:
        EvalScript scriptTagEvent = e.cast();
        details.put("Url", new StringCellRenderer(scriptTagEvent.getURL()));
        details.put("Line Number", new StringCellRenderer(
            scriptTagEvent.getLineNumber() + ""));
        break;
      case XhrReadyStateChangeEvent.TYPE:
        XhrReadyStateChangeEvent xhrEvent = e.cast();
        details.put("Ready State", new StringCellRenderer(
            xhrEvent.getReadyState() + ""));
        details.put("Url", new StringCellRenderer(xhrEvent.getUrl()));
        break;
      case XhrLoadEvent.TYPE:
        details.put("Url", new StringCellRenderer(
            e.<XhrLoadEvent> cast().getUrl()));
        break;
      case LogEvent.TYPE:
        LogEvent logEvent = e.cast();
        details.put("Message", new StringCellRenderer(logEvent.getMessage()));
        break;
      case JavaScriptExecutionEvent.TYPE:
        JavaScriptExecutionEvent jsExecEvent = e.cast();
        backTrace = "ignored," + jsExecEvent.getScriptName() + ","
            + jsExecEvent.getScriptLine() + ",-1,,";
        details.put("Function Call", new StackTraceRenderer(backTrace,
            symbolClickListener, this, currentAppUrl, this, true, resources));
        break;
      case ResourceDataReceivedEvent.TYPE:
        ResourceDataReceivedEvent dataRecEvent = e.cast();
        DataModel dataModel = eventWaterfall.getVisualization().getModel().getDataModel();
        NetworkResourceModel networkModel = dataModel.getNetworkResourceModel();
        NetworkResource resource = networkModel.getResource(dataRecEvent.getIdentifier());
        if (resource != null) {
          String resourceUrlStr = resource.getLastPathComponent();
          resourceUrlStr = "".equals(resourceUrlStr) ? resource.getUrl()
              : resourceUrlStr;
          details.put("Processing Resource", new StringCellRenderer(
              resourceUrlStr));
        }
        break;
      default:
        break;
    }

    if (e.getOverhead() > 0) {
      details.put("Overhead", new StringCellRenderer(
          TimeStampFormatter.formatMilliseconds(e.getOverhead(), 2)));
    }

    return details;
  }

  private void populateDetailsForTimerInstall(TimerInstalled timerData,
      IterableFastStringMap<CellRenderer> details) {
    if (timerData != null) {
      details.put("Timer Id", new StringCellRenderer(timerData.getTimerId()
          + ""));
      details.put("Timer Type", new StringCellRenderer(timerData.isSingleShot()
          ? "setTimeout" : "setInterval"));
      details.put("Interval", new StringCellRenderer(timerData.getInterval()
          + "ms"));
    }
  }

  /**
   * Make sure the source viewer exists and is loaded at the specified resource
   * URL.
   */
  private void showSourceInSourceViewer(final String resourceUrl,
      final SourceViewerLoadedCallback callback) {
    if (sourceViewer == null) {
      // Attach the container above the table so that the source viewer is
      // positioned independent of the table scroll and of the currently
      // viewed row.
      SourceViewer.create(eventWaterfall.getTableContents().getParentElement(),
          resources, new SourceViewerInitializedCallback() {
            public void onSourceViewerInitialized(SourceViewer viewer) {
              EventWaterfallRowDetails.this.sourceViewer = viewer;
              // Position the source viewer so that it fills half the
              // details view. Below the table header, and flush with the
              // bottom of the window.
              viewer.getElement().getStyle().setTop(1, Unit.PX);
              // Half the width with a little space for border of the table.
              viewer.getElement().getStyle().setRight(51, Unit.PCT);

              // Now request the resource
              viewer.loadResource(resourceUrl, callback);
            }
          });
    } else {
      sourceViewer.loadResource(resourceUrl, callback);
    }
  }
}
