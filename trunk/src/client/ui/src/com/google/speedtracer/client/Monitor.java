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

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
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
import com.google.speedtracer.client.model.DataInstance;
import com.google.speedtracer.client.model.DataModel;
import com.google.speedtracer.client.model.NoDataNotifier;
import com.google.speedtracer.client.model.TabChange;
import com.google.speedtracer.client.model.TabChangeModel;
import com.google.speedtracer.client.model.TabDescription;
import com.google.speedtracer.client.timeline.Constants;
import com.google.speedtracer.client.util.Url;
import com.google.speedtracer.client.util.dom.LocalStorage;
import com.google.speedtracer.client.util.dom.WindowExt;
import com.google.speedtracer.client.view.Controller;
import com.google.speedtracer.client.view.HotKeyPanel;
import com.google.speedtracer.client.view.HoveringPopup;
import com.google.speedtracer.client.view.InlineMenu;
import com.google.speedtracer.client.visualizations.model.HintletReportModel;
import com.google.speedtracer.client.visualizations.view.HintletReport;
import com.google.speedtracer.client.visualizations.view.HintletReportDialog;
import com.google.speedtracer.client.visualizations.view.MergeProfilesPanel;

import java.util.ArrayList;
import java.util.List;

/**
 * EntryPoint class for Speed Tracer's monitor.
 */
public class Monitor implements EntryPoint, WindowChannel.Listener,
    TabChangeModel.Listener {

  /**
   * Utilities to allow debugging the Monitor in dev mode. This will create a
   * stub background page capable of responding to the Monitor's initialization
   * request.
   */
  private static class MockUtils {
    private static final int MOCK_TABID = 0;

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

    private static void createMockBackgroundPage() {
      Server.listen(WindowExt.get(), CHANNEL_NAME, new ServerListener() {
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
  }

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
   * Use this value for an invalid browser or tab ID.
   */
  public static final int DEFAULT_ID = -1;

  static final String CHANNEL_NAME = "launcher";

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

  private HintletReportDialog hintletReportDialog;

  private DataModel model;

  private MonitorVisualizationsPanel monitorVisualizationsPanel;

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
    model.clear();
  }

  public void onChannelConnected(Client channel) {
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

  public void onModuleLoad() {
    if (ClientConfig.isMockMode()) {
      MockUtils.createMockBackgroundPage();
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
        + resources.sluggishnessFiletPanelCss().getText(), true);

    final WindowExt window = getBackgroundView();
    channel = Client.connect(window, CHANNEL_NAME, this);
    requestInitialization();
  }

  /**
   * Create a new blank application state for this URL. Pull related
   * NetworkResources from the previous NetworkModel, add them to the new
   * Application State. The Swap it in.
   */
  public void onTabChanged(TabChange nav) {
    // We should never get a page transition before initialize!!
    assert (pageStates != null && pageStates.size() > 0) : "We should never get a page transition before initialize";

    String oldUrl = getUrlWithoutHash(controller.getPageUrlForIndex(pageStates.size() - 1));
    String newUrl = getUrlWithoutHash(nav.getUrl());
    if (!oldUrl.equals(newUrl)) {
      // update tab description.
      model.getTabDescription().updateUrl(newUrl);

      // Create the ApplicationState with some initial guesses for bounds
      ApplicationState newState = new ApplicationState(model);

      ApplicationState oldState = pageStates.get(pageStates.size() - 1);
      // The current ApplicationState should now be neutered and no longer
      // receive updates. It should also transfer relevant old state to the new
      // ApplicationState.
      oldState.detachFromSourceModels();

      newState.setFirstDomainValue(nav.getTime());
      newState.setLastDomainValue(nav.getTime()
          + Constants.DEFAULT_GRAPH_WINDOW_SIZE);

      // Add this new ApplicationState to our collection of states for each page
      int pageIndex = addPageState(nav.getUrl(), newState);

      // Now swap in the page state
      setStateForPageAtIndex(pageIndex);
      controller.setSelectedPage(pageIndex);
      
      maybeInitializeSymbolServerController(model.getTabDescription());
    }
  }

  /**
   * Removes all current application states and starts a new one at the last
   * page we were monitoring.
   */
  public void resetApplicationStates() {
    ApplicationState newState = new ApplicationState(model);
    newState.setFirstDomainValue(0);
    newState.setLastDomainValue(Constants.DEFAULT_GRAPH_WINDOW_SIZE);
    String lastUrl = getUrlWithoutHash(controller.getPageUrlForIndex(pageStates.size() - 1));

    detachApplicationStatesFromSourceModels();
    pageStates.clear();
    model.clear();

    // Tell the controller to clean up its drop down menu.
    controller.resetPageStates();

    addPageState(lastUrl, newState);

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

  public void showHintletReport() {
    hintletReportDialog.setVisible(true);
  }

  private void detachApplicationStatesFromSourceModels() {
    for (int i = 0, length = pageStates.size(); i < length; ++i) {
      pageStates.get(i).detachFromSourceModels();
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

  private String getUrlWithoutHash(String url) {
    return url.split("#")[0];
  }

  private void initialize(TabDescription tabDescription,
      final DataInstance handle, String version) {
    assert (pageStates == null);

    this.version = version;
    final Resources resources = MonitorResources.getResources();
    // Create our collection for ApplicationStates.
    pageStates = new ArrayList<ApplicationState>();
    // Create our backing Model which provides our event stream.
    model = DataModel.Provider.createModel(tabDescription, handle);
    
    // setup debug logger as soon as we create the model
    Logging.getLogger().listenTo(model);
    
    model.getTabNavigationModel().addListener(this);
    // The top Controller bar for our top level actions.
    controller = new Controller(Root.getContainer(), model, this, resources);

    // Create the initial ApplicationState.
    addPageState(tabDescription.getUrl(), new ApplicationState(model));

    // The Panel that contains the Visualizations.
    monitorVisualizationsPanel = new MonitorVisualizationsPanel(
        Root.getContainer(), controller, pageStates.get(0), resources);

    // Create the reusable hovering popup once (invisible until we need it)
    popup = new HoveringPopup(Root.getContainer(), resources);

    hintletReportDialog = new HintletReportDialog(
        (HintletReportModel) pageStates.get(0).getVisualizationModel(
            HintletReport.TITLE), resources);

    HotKey.register('B', new BuildInfoView(version),
        "Show revision information.");

    HotKey.register('M', new SymbolServerEntryPanel(model.getTabDescription()),
        "UI for configuring the SymbolMap manifest location.");

    HotKey.register('1', new MergeProfilesPanel(model, resources),
        "Search for JavaScript profiles with the same Log entry and merge");

    // Start fetching the symbol manifest if it is available.
    maybeInitializeSymbolServerController(model.getTabDescription());

    // Attach the notification widget.
    MonitorVisualizationsPanel.Css css = MonitorResources.getResources().monitorVisualizationsPanelCss();
    NotificationSlideout slideout = NotificationSlideout.create(monitorVisualizationsPanel.getElement());
    slideout.setTopOffset(css.timelineHeight() + css.borderWidth());

    // Wrap this in an observer that looks for the first bit of data that comes
    // in.
    new NoDataNotifier(monitorVisualizationsPanel.getMainTimeLine().getModel(),
        slideout);
  }

  private void maybeInitializeSymbolServerController(
      TabDescription tabDescription) {
    LocalStorage storage = WindowExt.get().getLocalStorage();
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
              WindowExt.get()));
    }
  }

  private void setApplicationState(ApplicationState state) {
    monitorVisualizationsPanel.setApplicationState(state);
    hintletReportDialog.setHintletReportModel((HintletReportModel) state.getVisualizationModel(HintletReport.TITLE));
  }
}
