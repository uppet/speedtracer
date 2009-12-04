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
package com.google.speedtracer.client;

import com.google.gwt.chrome.crx.client.Chrome;
import com.google.gwt.chrome.crx.client.Extension;
import com.google.gwt.chrome.crx.client.Icon;
import com.google.gwt.chrome.crx.client.Port;
import com.google.gwt.chrome.crx.client.Tabs;
import com.google.gwt.chrome.crx.client.Windows;
import com.google.gwt.chrome.crx.client.Tabs.OnTabCallback;
import com.google.gwt.chrome.crx.client.Tabs.Tab;
import com.google.gwt.chrome.crx.client.Windows.OnWindowCallback;
import com.google.gwt.chrome.crx.client.Windows.Window;
import com.google.gwt.chrome.crx.client.events.BrowserActionEvent;
import com.google.gwt.chrome.crx.client.events.ConnectEvent;
import com.google.gwt.chrome.crx.client.events.MessageEvent;
import com.google.gwt.core.client.GWT;
import com.google.gwt.events.client.Event;
import com.google.gwt.events.client.EventListener;
import com.google.speedtracer.client.DataLoader.EventRecordMessageEvent;
import com.google.speedtracer.client.WindowChannel.Client;
import com.google.speedtracer.client.WindowChannel.Request;
import com.google.speedtracer.client.WindowChannel.Server;
import com.google.speedtracer.client.WindowChannel.ServerListener;
import com.google.speedtracer.client.messages.InitializeMonitorMessage;
import com.google.speedtracer.client.messages.RecordingDataMessage;
import com.google.speedtracer.client.messages.RequestInitializationMessage;
import com.google.speedtracer.client.model.DevToolsDataInstance;
import com.google.speedtracer.client.model.LoadFileDataInstance;
import com.google.speedtracer.client.model.Model;
import com.google.speedtracer.client.model.TabDescription;
import com.google.speedtracer.client.model.DataModel.DataInstance;
import com.google.speedtracer.client.util.dom.WindowExt;

import java.util.HashMap;

/**
 * The Chrome extension background page script.
 */
@Extension.ManifestInfo(name = "Speed Tracer",
    description = "Get insight into the performance of your web applications.",
    version = "0.5",
    permissions = {"tabs"},
    updateUrl = "http://dl.google.com/gwt/speedtracer/updates.xml",
    icons = {"resources/icon16.png",
             "resources/icon32.png",
             "resources/icon48.png",
             "resources/icon128.png"})
public abstract class BackgroundPage extends Extension {
  /**
   * Listener that receives callbacks from the ControlInstance. This class is
   * responsible for tracking connections to the various browsers in a browser
   * connection map.
   */
  class ModelListener implements Model.Listener {

    public void onBrowserConnected(int browserId, String name, String version) {
      browserConnectionMap.put(browserId, new BrowserConnectionState());
    }

    public void onBrowserDisconnected(int browserId) {
      browserConnectionMap.remove(browserId);
    }

    public void onMonitoredTabChanged(int browserId, TabDescription tab) {
    }

    public void onTabMonitorStarted(int browserId, TabDescription tab,
        DataInstance dataInstance) {
      BrowserConnectionState browserConnection = browserConnectionMap.get(browserId);
      assert (browserConnection != null);

      TabModel tabModel = getOrCreateTabModel(browserConnection, tab.getId());
      tabModel.dataInstance = dataInstance;
      tabModel.tabDescription = tab;
      openMonitor(browserId, tab.getId(), tabModel);
    }

    public void onTabMonitorStopped(int browserId, int tabID) {
    }
  }

  /**
   * Simple data structure class to maintain information about the connection
   * states for a connected browser and its tabs.
   */
  private class BrowserConnectionState {
    private final HashMap<Integer, TabModel> tabMap = new HashMap<Integer, TabModel>();

    BrowserConnectionState() {
    }
  }

  /**
   * Listener that is responsible for handling clicks on the
   * MonitorTabPageAction in the chrome omnibox and will open the monitor UI if
   * it isn't already open.
   */
  private class MonitorTabClickListener implements BrowserActionEvent.Listener {
    public void onClicked(Tab tab) {
      BrowserConnectionState browserConnection = browserConnectionMap.get(CHROME_BROWSER_ID);

      int tabId = tab.getId();
      String tabUrl = tab.getUrl();

      // Verify that it is not a click on the browser action button in a
      // Monitor window. If it is, early out.
      String urlNoParams = tabUrl.split("\\?")[0];
      if (urlNoParams.equals(Chrome.getExtension().getUrl(MONITOR_RESOURCE_PATH))) {
        return;
      }

      TabModel tabModel = getOrCreateTabModel(browserConnection, tabId);

      // Update the URL if we have a tabDescription already.
      if (tabModel.tabDescription != null) {
        tabModel.tabDescription.updateUrl(tabUrl);
      } else {
        tabModel.tabDescription = TabDescription.create(tabId, tab.getTitle(),
            tabUrl);
      }

      // We want to either open the monitor or resume monitoring.
      if (tabModel.currentIcon == browserAction.mtIcon()) {
        if (tabModel.dataInstance == null) {
          tabModel.dataInstance = DevToolsDataInstance.create(tabId);
        }

        if (tabModel.monitorClosed) {
          // Open the Monitor UI.
          openMonitor(CHROME_BROWSER_ID, tabId, tabModel);
        } else {
          // If this is the case then restart monitoring instead of starting
          // over.
          tabModel.dataInstance.<DataInstance> cast().resumeMonitoring(tabId);
          setBrowserActionIcon(tabId, browserAction.mtIconActive(), tabModel);
          tabModel.channel.sendMessage(RecordingDataMessage.TYPE,
              RecordingDataMessage.create(true));
        }

        return;
      }

      // If the icon is the record button, then we should already have an open
      // monitor, and we should start monitoring.
      if (tabModel.currentIcon == browserAction.mtIconActive()) {
        tabModel.dataInstance.<DataInstance> cast().stopMonitoring();
        setBrowserActionIcon(tabId, browserAction.mtIcon(), tabModel);
        tabModel.channel.sendMessage(RecordingDataMessage.TYPE,
            RecordingDataMessage.create(false));
      }
    }
  }

  /**
   * Simple wrapper that hold on to a reference for the DataInstance and most
   * recent TabDescription object for a tab.
   */
  private static class TabModel {
    WindowChannel.Client channel = null;
    DataInstance dataInstance;
    boolean monitorClosed = true;
    TabDescription tabDescription = null;
    Icon currentIcon;

    TabModel(Icon icon) {
      this.currentIcon = icon;
    }
  }

  private static final int CHROME_BROWSER_ID = 0;

  private static final int FILE_BROWSER_ID = 0x7FFFFFFF;

  private static final String MONITOR_RESOURCE_PATH = "monitor.html";

  private final HashMap<Integer, BrowserConnectionState> browserConnectionMap = new HashMap<Integer, BrowserConnectionState>();

  private final MonitorTabBrowserAction browserAction = GWT.create(MonitorTabBrowserAction.class);

  /**
   * Our entry point function. All things start here.
   */
  @Override
  public void onBackgroundPageLoad() {
    // Chrome is "connected". Insert an entry for it.
    browserConnectionMap.put(CHROME_BROWSER_ID, new BrowserConnectionState());

    GWT.create(PageActionInstaller.class);
    GWT.create(DataLoader.class);

    // The content script connect to us.
    Chrome.getExtension().getOnConnectEvent().addListener(
        new ConnectEvent.Listener() {
          public void onConnect(final Port port) {
            if (port.getName().equals(PageActionInstaller.NAME)) {
              int tabId = port.getTab().getId();
              // We want the icon to remain what it was before the page
              // transition.
              TabModel tabModel = browserConnectionMap.get(CHROME_BROWSER_ID).tabMap.get(tabId);
              setBrowserActionIcon(tabId, tabModel == null
                  ? browserAction.mtIcon() : tabModel.currentIcon, tabModel);
            } else if (port.getName().equals(DataLoader.NAME)) {
              // We are loading a file.
              BrowserConnectionState browserConn = browserConnectionMap.get(FILE_BROWSER_ID);
              if (browserConn == null) {
                browserConn = new BrowserConnectionState();
                browserConnectionMap.put(FILE_BROWSER_ID, browserConn);
              }

              // In situation where we open a file in a tab that was previously
              // used to open a file... we dont care. Overwrite it.
              final TabModel tabModel = new TabModel(browserAction.mtIcon());
              tabModel.dataInstance = LoadFileDataInstance.create();
              int tabId = port.getTab().getId();
              browserConn.tabMap.put(tabId, tabModel);

              // Connect the datainstance to receive data from the data_loader
              port.getOnMessageEvent().addListener(new MessageEvent.Listener() {
                public void onMessage(MessageEvent.Message message) {
                  EventRecordMessageEvent evtRecordMessage = message.cast();
                  tabModel.dataInstance.<LoadFileDataInstance> cast().onEventRecord(
                      evtRecordMessage.getRecordString());
                }
              });

              tabModel.tabDescription = TabDescription.create(tabId,
                  port.getTab().getTitle(), port.getTab().getUrl());
              openMonitor(FILE_BROWSER_ID, tabId, tabModel);
            }
          }
        });

    // Register page action and browser action listeners.
    browserAction.addListener(new MonitorTabClickListener());

    initialize();
  }

  /**
   * Returns a TabModel for a specified tab in a BrowserConnectionState object,
   * or creates one if one is not found. It also initialized the TabModel with
   * the specified DataInstance.
   */
  private TabModel getOrCreateTabModel(
      BrowserConnectionState browserConnection, int tabId) {
    TabModel tabModel = browserConnection.tabMap.get(tabId);
    if (tabModel == null) {
      tabModel = new TabModel(browserAction.mtIcon());
      browserConnection.tabMap.put(tabId, tabModel);
    }
    return tabModel;
  }

  /**
   * Temporary method until we figure out what updates to give to topspin to
   * make it document aware.
   * 
   * TODO(jaimeyap): Make Topspin document aware.
   */
  private native WindowExt getWindow() /*-{
    return window;
  }-*/;

  /**
   * Injects the plugin and calls Load(). Also starts our
   * {@link WindowChannel.Server} for communicating and initializing instances
   * of our Monitor UI.
   */
  private void initialize() {
    // Inject the plugin and call Load() on it.
    // TODO(jaimeyap): Uncomment this when we have a building plugin.
    // final Model model = new PluginModel();
    // model.load(new ModelListener());

    Server.listen(getWindow(), Monitor.CHANNEL_NAME, new ServerListener() {
      public void onClientChannelRequested(Request request) {
        request.accept(new WindowChannel.Listener() {
          public void onChannelClosed(Client channel) {
          }

          public void onChannelConnected(Client channel) {
          }

          public void onMessage(final Client channel, int type,
              WindowChannel.Message data) {
            switch (type) {
              case RequestInitializationMessage.TYPE:
                doRequestInitialization(channel, data);
                break;
              case RecordingDataMessage.TYPE:
                doRecordingData(data);
                break;
              default:
                assert false : "Unhandled Message" + type;
            }
          }

          private void doRecordingData(WindowChannel.Message data) {
            final RecordingDataMessage recordingDataMessage = data.cast();
            int tabId = recordingDataMessage.getTabId();
            int browserId = recordingDataMessage.getBrowserId();
            TabModel tabModel = browserConnectionMap.get(browserId).tabMap.get(tabId);
            Icon pageActionIcon;
            if (recordingDataMessage.isRecording()) {
              tabModel.dataInstance.<DataInstance> cast().resumeMonitoring(
                  tabId);
              pageActionIcon = browserAction.mtIconActive();
            } else {
              tabModel.dataInstance.<DataInstance> cast().stopMonitoring();
              pageActionIcon = browserAction.mtIcon();
            }
            if (browserId == CHROME_BROWSER_ID) {
              // Update the page action icon.
              setBrowserActionIcon(tabId, pageActionIcon, tabModel);
            }
          }

          private void doRequestInitialization(final Client channel,
              WindowChannel.Message data) {
            final RequestInitializationMessage request = data.cast();
            final int tabId = request.getTabId();
            final int browserId = request.getBrowserId();
            final BrowserConnectionState browserConnection = browserConnectionMap.get(browserId);
            assert (browserConnection != null);

            // Extract the relevant DataInstance and TabDescription
            // that we have stashed.
            final TabModel tabModel = browserConnection.tabMap.get(tabId);
            // Store a reference to the channel in case we want to
            // send messages later.
            tabModel.channel = channel;

            // We are talking to another browser. Go ahead and initialize
            // the window since the interaction was started externally.
            assert (tabModel.tabDescription != null);
            assert (tabModel.dataInstance != null);

            final InitializeMonitorMessage initializeMessage = InitializeMonitorMessage.create(
                tabModel.tabDescription, tabModel.dataInstance, getVersion());
            channel.sendMessage(InitializeMonitorMessage.TYPE,
                initializeMessage);

            // If we are talking to chrome, update the pageAction Icon and
            // wait for a second click to initialize the monitor.
            if (browserId == CHROME_BROWSER_ID) {
              // We now change the page action icon. This signals that the
              // next time it is clicked, we should initialize.
              setBrowserActionIcon(tabId, browserAction.mtIconActive(),
                  tabModel);
            }

            // Hook unload so we can close down and keep track of monitor
            // state.
            request.getMonitorWindow().addUnloadListener(new EventListener() {
              public void handleEvent(Event event) {
                assert (browserConnection != null);
                TabModel tabModel = browserConnection.tabMap.get(tabId);
                channel.close();
                tabModel.channel = null;
                tabModel.monitorClosed = true;
                tabModel.dataInstance.<DataInstance> cast().unload();
                tabModel.dataInstance = null;
                setBrowserActionIcon(tabId, browserAction.mtIcon(), tabModel);
                browserConnection.tabMap.remove(tabModel);
              }
            });
          }
        });
      }

    });
  }
  
  /**
   * Opens the monitor UI for a given tab, iff it is not already open.
   */
  private void openMonitor(final int browserId, final int tabId,
      final TabModel tabModel) {
    assert (tabModel != null);

    Windows.create(MONITOR_RESOURCE_PATH + "?tabId=" + tabId + "&browserId="
        + browserId, 0, 0, 820, 700, new OnWindowCallback() {
      public void onWindow(Window window) {
        tabModel.monitorClosed = false;
        // The Tab containing the Monitor UI should not have a valid browser
        // action button.
        Tabs.getSelected(window.getId(), new OnTabCallback() {
          public void onTab(Tab tab) {
            setBrowserActionIcon(tab.getId(), browserAction.mtIconDisabled(),
                null);
          }
        });
      }
    });
  }

  private void setBrowserActionIcon(int tabId, Icon icon, TabModel tabModel) {
    String title = "";
    if (icon == browserAction.mtIcon()) {
      title = "Monitor Tab";
    }
    if (icon == browserAction.mtIconActive()) {
      title = "Stop Monitoring";
    }

    browserAction.setIcon(tabId, icon);
    browserAction.setTitle(tabId, title);
    if (tabModel != null) {
      tabModel.currentIcon = icon;
    }
  }
}
