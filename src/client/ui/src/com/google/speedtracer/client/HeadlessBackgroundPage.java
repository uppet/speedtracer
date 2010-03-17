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

import com.google.gwt.chrome.crx.client.Chrome;
import com.google.gwt.chrome.crx.client.Extension;
import com.google.gwt.chrome.crx.client.Port;
import com.google.gwt.chrome.crx.client.events.ConnectEvent;
import com.google.gwt.chrome.crx.client.events.MessageEvent;
import com.google.gwt.chrome.crx.client.events.MessageEvent.Message;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptException;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.xhr.client.XMLHttpRequest;
import com.google.speedtracer.client.messages.HeadlessClearDataMessage;
import com.google.speedtracer.client.messages.HeadlessDumpDataAckMessage;
import com.google.speedtracer.client.messages.HeadlessDumpDataMessage;
import com.google.speedtracer.client.messages.HeadlessMonitoringOffMessage;
import com.google.speedtracer.client.messages.HeadlessMonitoringOnMessage;
import com.google.speedtracer.client.messages.HeadlessSendDataAckMessage;
import com.google.speedtracer.client.messages.HeadlessSendDataMessage;
import com.google.speedtracer.client.model.DataInstance;
import com.google.speedtracer.client.model.DataModel;
import com.google.speedtracer.client.model.DevToolsDataInstance;
import com.google.speedtracer.client.model.EventRecord;
import com.google.speedtracer.client.model.TabDescription;
import com.google.speedtracer.client.util.JSOArray;
import com.google.speedtracer.client.util.JSON;
import com.google.speedtracer.client.util.Xhr;

import java.util.ArrayList;
import java.util.List;

/**
 * A Chrome extension background page script for running a headless version of
 * SpeedTracer intended to support benchmarking and unit testing.
 */
@Extension.ManifestInfo(name = "Speed Tracer - headless (by Google)", description = "Get insight into the performance of your web applications.", version = ClientConfig.VERSION, permissions = {
    "tabs", "http://*/*", "https://*/*"}, icons = {
    "resources/icon16.png", "resources/icon32.png", "resources/icon48.png",
    "resources/icon128.png"})
public class HeadlessBackgroundPage extends Extension implements
    ConnectEvent.Listener {

  /**
   * Fires when messages are returned from the content script.
   */
  public class MessageHandler implements MessageEvent.Listener {
    private class HeadlessDataModel extends DataModel {

      @Override
      public void fireOnEventRecord(EventRecord data) {
        // Send this message over to the content script
        String dataString = JSON.stringify(data);
        eventRecordData.add(dataString);
      }

      @Override
      public void resumeMonitoring(int tabId) {
      }

      @Override
      public void saveRecords(JSOArray<String> visitedUrls, String version) {
      }

      @Override
      public void stopMonitoring() {
      }

      @Override
      protected void bind(TabDescription tabDescription,
          DataInstance dataInstance) {
      }
    }

    private final Port port;
    private DataInstance dataInstance;

    public MessageHandler(Port port) {
      this.port = port;
    }

    /**
     * Handle an incoming message from the Content Script delivered over a
     * Chrome Extensions Port.
     */
    public void onMessage(Message msg) {
      Port.Message message = msg.cast();
      switch (message.getType()) {
        case HeadlessClearDataMessage.TYPE:
          doHeadlessClearData(message.<HeadlessClearDataMessage> cast());
          break;
        case HeadlessMonitoringOnMessage.TYPE:
          doHeadlessMonitoringOn(message.<HeadlessMonitoringOnMessage> cast());
          break;
        case HeadlessMonitoringOffMessage.TYPE:
          doHeadlessMonitoringOff(message.<HeadlessMonitoringOffMessage> cast());
          break;
        case HeadlessDumpDataMessage.TYPE:
          doHeadlessDumpData(message.<HeadlessDumpDataMessage> cast());
          break;
        case HeadlessSendDataMessage.TYPE:
          doHeadlessSendData(message.<HeadlessSendDataMessage> cast());
          break;
        default:
      }
    }

    private void doHeadlessClearData(HeadlessClearDataMessage message) {
      eventRecordData.clear();
    }

    private void doHeadlessDumpData(HeadlessDumpDataMessage message) {
      // Pack up the data we've been saving in our list and send it back
      // to the API.
      HeadlessDumpDataAckMessage dumpMessage = HeadlessDumpDataAckMessage.create(getEventRecordDataAsString());
      sendToContentScript(port, dumpMessage);
    }

    private void doHeadlessMonitoringOff(HeadlessMonitoringOffMessage message) {
      // The API is telling us to turn off monitoring.
      initDataInstance();
      dataInstance.stopMonitoring();
    }

    private void doHeadlessMonitoringOn(HeadlessMonitoringOnMessage message) {
      // The API is telling us to turn on monitoring.
      initDataInstance();
      dataInstance.resumeMonitoring();
      HeadlessMonitoringOnMessage.Options options = message.getOptions();
      if (options != null) {
        dataInstance.setProfilingOptions(options.isStackTraceOn(),
            options.isProfilingOn());
      }
    }

    private void doHeadlessSendData(HeadlessSendDataMessage message) {
      // Pack up the data we've been saving in our list and send it out
      // via XHR.
      JsArray<JavaScriptObject> data = getEventRecordData();
      try {
        String payload = createXhrPayload(dataInstance.getBaseTime(), message,
            data);
        Xhr.post(message.getUrl(), payload, "application/json",
            new Xhr.XhrCallback() {
              public void onFail(XMLHttpRequest xhr) {
                HeadlessSendDataAckMessage sendMessage = HeadlessSendDataAckMessage.create(false);
                sendToContentScript(port, sendMessage);
              }

              public void onSuccess(XMLHttpRequest xhr) {
                HeadlessSendDataAckMessage sendMessage = HeadlessSendDataAckMessage.create(true);
                sendToContentScript(port, sendMessage);
              }
            });
      } catch (JavaScriptException ex) {
        HeadlessSendDataAckMessage sendMessage = HeadlessSendDataAckMessage.create(false);
        sendToContentScript(port, sendMessage);
      }
    }

    private JsArray<JavaScriptObject> getEventRecordData() {
      JsArray<JavaScriptObject> result = JsArray.createArray().cast();

      for (int i = 0, length = eventRecordData.size(); i < length; ++i) {
        JavaScriptObject row = JSON.parse(eventRecordData.get(i));
        result.push(row);
      }
      return result;
    }

    private String getEventRecordDataAsString() {
      StringBuilder builder = new StringBuilder();
      for (int i = 0, length = eventRecordData.size(); i < length; ++i) {
        builder.append(eventRecordData.get(i));
        builder.append("\n");
      }
      return builder.toString();
    }

    private void initDataInstance() {
      if (dataInstance != null) {
        return;
      }

      dataInstance = DevToolsDataInstance.create(port.getTab().getId());
      DataModel dataModel = new HeadlessDataModel();
      dataInstance.load(dataModel);
    }
  }

  private static native String createXhrPayload(double baseTime,
      HeadlessSendDataMessage message, JsArray<JavaScriptObject> data) /*-{
    var header = message.header ? message.header : {} ;
    header.timeStamp = baseTime;
    var resultObject = {'header':header, 'data':data};
    return JSON.stringify(resultObject);
  }-*/;

  /**
   * This method provides a convenient place to annotate sending messages with
   * debugging.
   */
  private static void sendToContentScript(Port port, Port.Message message) {
    port.postMessage(message);
  }

  private final List<String> eventRecordData = new ArrayList<String>();

  @Override
  public String getVersion() {
    return null;
  }

  /**
   * Our entry point function. All things start here.
   */
  @Override
  public void onBackgroundPageLoad() {
    GWT.create(HeadlessContentScript.class);
    initialize();
  }

  /**
   * Fires when a new connection from a content script is received.
   */
  public void onConnect(Port port) {
    port.getOnMessageEvent().addListener(new MessageHandler(port));
  }

  private void initialize() {
    // Listen for messages from the content script
    Chrome.getExtension().getOnConnectEvent().addListener(this);
  }
}