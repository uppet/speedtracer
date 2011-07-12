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
package com.google.speedtracer.client;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.GWT.UncaughtExceptionHandler;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.StyleInjector;
import com.google.gwt.topspin.ui.client.Root;
import com.google.gwt.user.client.Window.Location;
import com.google.speedtracer.client.MonitorResources.Resources;
import com.google.speedtracer.client.WindowChannel.Client;
import com.google.speedtracer.client.WindowChannel.Message;
import com.google.speedtracer.client.WindowChannel.Request;
import com.google.speedtracer.client.WindowChannel.Server;
import com.google.speedtracer.client.WindowChannel.ServerListener;
import com.google.speedtracer.client.messages.InitializeMonitorMessage;
import com.google.speedtracer.client.messages.RecordingDataMessage;
import com.google.speedtracer.client.messages.RequestInitializationMessage;
import com.google.speedtracer.client.messages.ResendProfilingOptions;
import com.google.speedtracer.client.messages.ResetBaseTimeMessage;
import com.google.speedtracer.client.model.ApplicationState;
import com.google.speedtracer.client.model.DataDispatcher;
import com.google.speedtracer.client.model.DataInstance;
import com.google.speedtracer.client.model.EventRecord;
import com.google.speedtracer.client.model.InspectorWillSendRequest;
import com.google.speedtracer.client.model.ResourceWillSendEvent;
import com.google.speedtracer.client.model.TabChangeDispatcher;
import com.google.speedtracer.client.model.TabChangeEvent;
import com.google.speedtracer.client.model.TabDescription;
import com.google.speedtracer.client.timeline.Constants;
import com.google.speedtracer.client.util.Command;
import com.google.speedtracer.client.util.Url;
import com.google.speedtracer.client.util.dom.LocalStorage;
import com.google.speedtracer.client.util.dom.WindowExt;
import com.google.speedtracer.client.view.Controller;
import com.google.speedtracer.client.view.HotKeyPanel;
import com.google.speedtracer.client.view.HoveringPopup;
import com.google.speedtracer.client.view.InlineMenu;
import com.google.speedtracer.client.visualizations.view.MergeProfilesPanel;
import com.google.speedtracer.shared.EventRecordType;

import java.util.ArrayList;
import java.util.List;

/**
 * EntryPoint class for Speed Tracer's monitor.
 */
public class Monitor implements EntryPoint, WindowChannel.Listener,
    TabChangeDispatcher.Listener, DataDispatcher.EventStreamStatusListener {

  /**
   * Panel that displays the build info when pressing "V".
   */
  private static class BuildInfoView extends HotKeyPanel {
    private final String version;

    BuildInfoView(String version) {
      this.version = version;
    }

    @Override
    protected Element createContentElement(Document document) {
      return document.createDivElement();
    }

    @Override
    protected void populateContent(Element contentElement) {
      final BuildInfo info = GWT.create(BuildInfo.class);
      contentElement.setInnerText("Version: " + version + ", Revision: r"
          + info.getBuildRevision() + ", Date: " + info.getBuildTime());
      contentElement.setClassName(MonitorResources.getResources().monitorCss().buildInfoView());
    }
  }

  /**
   * Utilities to allow debugging the Monitor in dev mode. This will create a
   * stub background page capable of responding to the Monitor's initialization
   * request.
   */
  private static class MockUtils {
    private static final int MOCK_TABID = 0;

    private static void createMockBackgroundPage() {
      Server.listen(WindowExt.getHostWindow(), CHANNEL_NAME,
          new ServerListener() {
            public void onClientChannelRequested(Request request) {
              request.accept(new WindowChannel.Listener() {
                // We make mock stubs of the DataInstance and the
                // TabDescription.
                final DataInstance dataInstance = createMockDataInstance();
                final TabDescription tabDescription = createTabDescription(
                    MOCK_TABID, "http://mock.com/", "Mock web site");

                public void onChannelClosed(Client channel) {
                }

                public void onChannelConnected(Client channel) {
                }

                public void onMessage(Client channel, int type, Message data) {
                  if (type == RequestInitializationMessage.TYPE) {
                    channel.sendMessage(InitializeMonitorMessage.TYPE,
                        InitializeMonitorMessage.create(tabDescription,
                            dataInstance, "0.0"));
                  }
                }
              });
            }
          });
    }

    private static native DataInstance createMockDataInstance() /*-{
      return  { 
        Load: function(callback) {       
        },

        Resume: function(port) {        
        },

        Stop: function() {
        },

        Unload: function() {
        },

        SetBaseTime: function(baseTime) {
        },

        SetOptions: function(enableStackTraces, enableCpuProfiling) {
        }
      };
    }-*/;

    private static native TabDescription createTabDescription(int id,
        String url, String title) /*-{
      return {id: id, url: url, title: title};
    }-*/;
  }

  /**
   * Use this value for an invalid browser or tab ID.
   */
  public static final int DEFAULT_ID = -1;

  static final String CHANNEL_NAME = "launcher";

  private static final int START_EVENT_STREAM_TIMEOUT = 2000;

  private static InlineMenu inlineMenu;

  private static HoveringPopup popup;

  public static InlineMenu getInlineMenu() {
    return inlineMenu;
  }

  public static HoveringPopup getPopup() {
    return popup;
  }

  private static int getIdParameter(String param) {
    String id = Location.getParameter(param);
    if (id != null) {
      return Integer.parseInt(id);
    } else {
      return DEFAULT_ID;
    }
  }

  private int browserId = DEFAULT_ID;

  private WindowChannel.Client channel;

  private Controller controller;

  private DataDispatcher dataDispatcher;

  private MonitorVisualizationsPanel monitorVisualizationsPanel;
  
  private boolean eventStreamHasStarted;

  private NotificationSlideout notificationSlideout;

  /**
   * List of ApplicationStates. One for each page we have monitored.
   */
  private List<ApplicationState> pageStates;

  private int tabId = DEFAULT_ID;

  private String version;

  /**
   * Adds an newly created applicationState to our page states list, and also
   * adds an entry to the select box of all page page transitions.
   * 
   * @param pageUrl the url for the new page
   * @param state the new ApplicationState
   * @return the index for the page state
   */
  public int addPageState(String pageUrl, ApplicationState state) {
    if (pageStates != null && controller != null) {
      pageStates.add(state);
      controller.addPage(pageUrl);
      return pageStates.size() - 1;
    } else {
      return -1;
    }
  }

  /**
   * Each connected browser gets a unique identifier.
   */
  public int getBrowserId() {
    return browserId;
  }

  /**
   * Returns the number of pages we have seen so far.
   * 
   * @return the number of pages viewed.
   */
  public int getNumberOfPagesViewed() {
    if (pageStates != null) {
      return pageStates.size();
    } else {
      return 0;
    }
  }

  /**
   * Each monitored tab gets a unique identifier.
   */
  public int getTabId() {
    return tabId;
  }

  public String getVersion() {
    return version;
  }

  public void onChannelClosed(Client channel) {
    pageStates.clear();
    dataDispatcher.clear();
  }

  public void onChannelConnected(Client channel) {
  }

  public void onEventStreamStarted() {
    eventStreamHasStarted = true;
    
    // This is a special case where we receive notification that the event
    // stream has started after we start displaying the notification. This does
    // not seem to happen in practice.
    if (notificationSlideout != null) {
      notificationSlideout.hideThenDestroy();
    }
    
    notificationSlideout = null;
  }
  
  public void onMessage(Client channel, int type, WindowChannel.Message data) {
    switch (type) {
      case InitializeMonitorMessage.TYPE:
        final InitializeMonitorMessage initMessage = data.cast();
        initialize(initMessage.getTabDescription(), initMessage.getHandle(),
            initMessage.getVersion());
        break;
      case RecordingDataMessage.TYPE:
        final RecordingDataMessage recordingDataMessage = data.cast();
        recordingDataReceived(recordingDataMessage.isRecording());
        break;
      case ResendProfilingOptions.TYPE:
        controller.sendProfilingOptions();
        break;
      default:
        assert false : "Unhandled Message";
    }
  }
  
  /**
   * Log uncaught exceptions in Debug Mode.
   */
  private class DebugUeh implements UncaughtExceptionHandler {
    public void onUncaughtException(Throwable e) {
      Logging.getLogger().logTextError(e.toString());      
    }
  }

  public void onModuleLoad() {
    if (ClientConfig.isMockMode()) {
      MockUtils.createMockBackgroundPage();
    }
    
    if (ClientConfig.isDebugMode() && GWT.isScript()) {
      GWT.setUncaughtExceptionHandler(new DebugUeh());
    }

    browserId = getIdParameter("browserId");
    tabId = getIdParameter("tabId");

    MonitorResources.init();
    final Resources resources = MonitorResources.getResources();
    // Inject styles. Compiler should concat all these into a big style String.
    StyleInjector.inject(resources.overViewGraphCss().getText()
        + resources.mainTimeLineCss().getText()
        + resources.transientGraphSelectionCss().getText()
        + resources.commonCss().getText()
        + resources.overViewTimeLineCss().getText()
        + resources.domainRegionSelectionCss().getText()
        + resources.hoveringPopupCss().getText()
        + resources.controllerCss().getText()
        + resources.monitorVisualizationsPanelCss().getText()
        + resources.fastTooltipCss().getText()
        + resources.timeScaleCss().getText()
        + resources.currentSelectionMarkerCss().getText()
        + resources.pageTransitionMarkerCss().getText()
        + resources.detailViewsCss().getText()
        + resources.networkTimeLineDetailViewCss().getText()
        + resources.resourceRowCss().getText()
        + resources.networkPillBoxCss().getText()
        + resources.requestDetailsCss().getText()
        + resources.sluggishnessDetailViewCss().getText()
        + resources.hintletIndicatorCss().getText()
        + resources.filteringScrollTableCss().getText()
        + resources.pieChartCss().getText()
        + resources.hintletReportCss().getText()
        + resources.hintletReportDialogCss().getText()
        + resources.sortableTableHeaderCss().getText()
        + resources.scopeBarCss().getText()
        + resources.colorListCss().getText() + resources.treeCss().getText()
        + resources.eventTraceBreakdownCss().getText()
        + resources.mainGraphCss().getText()
        + resources.overViewGraphCss().getText()
        + resources.monitorCss().getText()
        + resources.sourceViewerCss().getText()
        + resources.stackFrameRendererCss().getText()
        + resources.javaScriptProfileRendererCss().getText()
        + resources.eventWaterfallRowCss().getText()
        + resources.eventWaterfallRowDetailsCss().getText()
        + resources.sluggishnessFiletPanelCss().getText()
        + resources.timelineMarksCss().getText(), true);

    final WindowExt window = getBackgroundView();
    channel = Client.connect(window, CHANNEL_NAME, this);
    requestInitialization();
  }

  /**
   * Create a new blank application state for this URL. Pull related
   * NetworkResources from the previous NetworkModel, add them to the new
   * Application State. The Swap it in.
   */
  public void onPageTransition(TabChangeEvent nav) {
    // We should never get a page transition before initialize!!
    assert (pageStates != null && pageStates.size() > 0) : "We should never get a page transition before initialize";

    // update tab description.
    dataDispatcher.getTabDescription().updateUrl(
        Url.getUrlWithoutHash(nav.getUrl()));

    // Create the ApplicationState with some initial guesses for bounds
    ApplicationState newState = new ApplicationState(dataDispatcher);

    ApplicationState oldState = pageStates.get(pageStates.size() - 1);
    // The current ApplicationState should now be neutered and no longer
    // receive updates. It should also transfer relevant old state to the new
    // ApplicationState.
    oldState.detachModelsFromDispatchers();

    newState.setFirstDomainValue(nav.getTime());
    newState.setLastDomainValue(nav.getTime()
        + Constants.DEFAULT_GRAPH_WINDOW_SIZE);

    // Add this new ApplicationState to our collection of states for each page
    int pageIndex = addPageState(nav.getUrl(), newState);

    // Now swap in the page state
    setStateForPageAtIndex(pageIndex);
    controller.setSelectedPage(pageIndex);

    maybeInitializeSymbolServerController(dataDispatcher.getTabDescription());
    
    // copy over associated network resource from before the tab change was processed
    // copy all back to the main resource
    DataDispatcher dispatcher = oldState.getDataDispatcher();
    copyRecordsFromBeforePageTransition(dispatcher, newState, nav.getUrl());
  }
  
  /**
   * Look backwards for the main associated with the page transition
   * #TODO (sarahgsmith) do some further investigation to see what happens
   *    with mouse events, etc.
   */
  private void copyRecordsFromBeforePageTransition(DataDispatcher oldDispatcher, ApplicationState newState, String newUrl) {
    List<EventRecord> eventRecords = oldDispatcher.getEventRecords();
    
    assert (eventRecords.size() > 0) : "No EventRecords when onPageTransition was called";
    
    int index = eventRecords.size() - 1;
    int startCopyIndex = -1;
    // the ResourceWillSendEvent URL we are looking for which will terminate the search
    String searchUrl = newUrl;
    
    // search backwards through the resources until
    // we find the main ResourceSendRequest
    while (startCopyIndex == -1 && index >= 0) {
      EventRecord aRecord = eventRecords.get(index);
      
      if (aRecord.getType() == EventRecordType.INSPECTOR_WILL_SEND_REQUEST) {
        // see if there's a redirect
        InspectorWillSendRequest request = aRecord.cast();
        String redirectUrl = request.getRedirectUrl();
        if(redirectUrl != null) {
          // if so, keep searching for the original URL request
          searchUrl = redirectUrl;
        }
      }
      
      if (aRecord.getType() == EventRecordType.RESOURCE_SEND_REQUEST) {
        // check if we've found a matching URL
        ResourceWillSendEvent event = aRecord.cast();
        String thisUrl = event.getUrl();
        if(thisUrl.equals(searchUrl)) {
          startCopyIndex = index;
        }
      }
      
      index--;
    }
    
    // did not find the matching URL
    // should this be an assert or are there cases where it will happen?
    if(startCopyIndex < 0) {
      return;
    }
    
    for (int k = startCopyIndex; k < eventRecords.size(); k++) {
      EventRecord copyRecord = eventRecords.get(k);
      // don't re-dispatch the tab change event
      if (copyRecord.getType() == EventRecordType.TAB_CHANGED) {
        continue;
      }
      newState.getDataDispatcher().getNetworkEventDispatcher().onEventRecord(copyRecord);
    }
  }

  public void onRefresh(TabChangeEvent change) {
    // Only care about page transitions here.
  }

  /**
   * Removes all current application states and starts a new one at the last
   * page we were monitoring.
   */
  public void resetApplicationStates() {
    ApplicationState newState = new ApplicationState(dataDispatcher);
    newState.setFirstDomainValue(0);
    newState.setLastDomainValue(Constants.DEFAULT_GRAPH_WINDOW_SIZE);

    detachApplicationStates();
    pageStates.clear();
    dataDispatcher.clear();
    monitorVisualizationsPanel.clearTimelineMarks();

    // Tell the controller to clean up its drop down menu.
    controller.resetPageStates();

    addPageState(dataDispatcher.getTabDescription().getUrl(), newState);

    // Now swap in the page state
    setStateForPageAtIndex(0);
    controller.setSelectedPage(0);

    if (channel != null) {
      channel.sendMessage(ResetBaseTimeMessage.TYPE,
          ResetBaseTimeMessage.create(getTabId(), getBrowserId()));
    }
  }

  /**
   * Notify the background page that the UI requested to start/stop recording
   * data.
   * 
   * @param isRecording <code>true</code> to start recording data.
   */
  public void setIsRecording(boolean isRecording) {
    if (channel != null) {
      channel.sendMessage(RecordingDataMessage.TYPE,
          RecordingDataMessage.create(getTabId(), getBrowserId(), isRecording));
    }
  }

  /**
   * Sets the current application state for the ApplicationState at the given
   * index.
   * 
   * @param pageIndex the index of the ApplicationState we want to set as the
   *          current state
   */
  public void setStateForPageAtIndex(int pageIndex) {
    if (pageStates != null) {
      assert (pageIndex >= 0 && pageIndex < pageStates.size());
      ApplicationState state = pageStates.get(pageIndex);
      setApplicationState(state);
    }
  }

  private void detachApplicationStates() {
    for (int i = 0, length = pageStates.size(); i < length; ++i) {
      pageStates.get(i).detachModelsFromDispatchers();
    }
  }

  /**
   * TODO(jaimeyap): We don't want to inject a dependency on the CRX library in
   * the monitor since it won't work in hosted mode. We need to fix the CRX
   * library for hosted mode.
   * 
   * @return handle to the background window
   */
  private native WindowExt getBackgroundView() /*-{
    // Not worth a deferred binding. Simple capability detect to see if we are
    // in an crx supported environment.
    if ($wnd.chrome && $wnd.chrome.extension) {
      return $wnd.chrome.extension.getBackgroundPage();
    }
    return $wnd;
  }-*/;

  private void initialize(TabDescription tabDescription,
      final DataInstance handle, String version) {
    assert (pageStates == null);

    this.version = version;
    final Resources resources = MonitorResources.getResources();
    // Create our collection for ApplicationStates.
    pageStates = new ArrayList<ApplicationState>();
    // Create our backing DataDispatcher which provides our event stream.
    dataDispatcher = DataDispatcher.create(tabDescription, handle, this);

    // setup debug logger.
    Logging.getLogger().listenTo(dataDispatcher);

    dataDispatcher.getTabChangeDispatcher().addListener(this);
    // The top Controller bar for our top level actions.
    controller = new Controller(Root.getContainer(), dataDispatcher, this,
        resources);

    // Create the initial ApplicationState.
    addPageState(tabDescription.getUrl(), new ApplicationState(dataDispatcher));

    // The Panel that contains the Visualizations.
    monitorVisualizationsPanel = new MonitorVisualizationsPanel(
        Root.getContainer(), controller, pageStates.get(0), resources);

    // Create the reusable hovering popup once (invisible until we need it)
    popup = new HoveringPopup(Root.getContainer(), resources);

    HotKey.register('B', new BuildInfoView(version),
        "Show revision information.");

    HotKey.register('M', new SymbolServerEntryPanel(
        dataDispatcher.getTabDescription()),
        "UI for configuring the SymbolMap manifest location.");

    HotKey.register('1', new MergeProfilesPanel(dataDispatcher, resources),
        "Search for JavaScript profiles with the same Log entry and merge");

    // Start fetching the symbol manifest if it is available.
    maybeInitializeSymbolServerController(dataDispatcher.getTabDescription());

    // Attach the notification widget.
    MonitorVisualizationsPanel.Css css = MonitorResources.getResources().monitorVisualizationsPanelCss();
    notificationSlideout = NotificationSlideout.create(monitorVisualizationsPanel.getElement());
    notificationSlideout.setTopOffset(css.timelineHeight() + css.borderWidth());
    showNotificationIfEventStreamDoesNotStart();
  }

  private void maybeInitializeSymbolServerController(
      TabDescription tabDescription) {
    LocalStorage storage = WindowExt.getHostWindow().getLocalStorage();
    Url resourceUrl = new Url(tabDescription.getUrl());

    // We are not permitted to do file:// XHRs.
    if (Url.SCHEME_FILE.equals(resourceUrl.getScheme())) {
      return;
    }

    String symbolManifestUrl = storage.getStringItem(resourceUrl.getApplicationUrl());

    if (symbolManifestUrl == null || symbolManifestUrl.equals("")) {
      // Attempt to fetch one at a predetermined location.
      symbolManifestUrl = resourceUrl.getResourceBase() + "symbolmanifest.json";
    }

    SymbolServerService.registerSymbolServerController(resourceUrl, new Url(
        symbolManifestUrl));
  }

  /**
   * The background page is forwarding a message to the UI regarding whether
   * data is being recorded or not.
   * 
   * @param isRecording <code>true</code> if data is currently being recorded.
   */
  private void recordingDataReceived(boolean isRecording) {
    controller.setIsRecording(isRecording);
  }

  private void requestInitialization() {
    if (channel != null) {
      channel.sendMessage(RequestInitializationMessage.TYPE,
          RequestInitializationMessage.create(getTabId(), getBrowserId(),
              WindowExt.getHostWindow()));
    }
  }

  private void setApplicationState(ApplicationState state) {
    monitorVisualizationsPanel.setApplicationState(state);
  }

  private void showNotificationIfEventStreamDoesNotStart() {
    if (eventStreamHasStarted || ClientConfig.isDebugMode()) {
      return;
    }

    Command.defer(new Command.Method() {
      public void execute() {
        if (eventStreamHasStarted) {
          return;
        }

        String content =
            "<strong>Pfffttt, Speed Tracer is not working.</strong><br/><br/>"
                + "Please double check a couple of things:<br/>"
                + "<ol>"
                + "<li>You must start Chrome with the flag: --enable-extension-timeline-api</li>"
                + "<li>You must be running the <a target=\"_blank\" href=\"http://dev.chromium.org/getting-involved/dev-channel#TOC-Subscribing-to-a-channel\">Chrome Dev channel</a>.</li>"
                + "</ol>" + "For more details, see our <a target='_blank' href='"
                + Constants.HELP_URL + "'>getting started</a> docs.";
        notificationSlideout.setContentHtml(content);
        notificationSlideout.show();
      }
    }, START_EVENT_STREAM_TIMEOUT);
  }
}
