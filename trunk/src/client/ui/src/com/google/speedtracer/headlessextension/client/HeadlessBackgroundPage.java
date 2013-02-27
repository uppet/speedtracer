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
package com.google.speedtracer.headlessextension.client;

import java.util.HashMap;

import com.google.gwt.chrome.crx.client.Chrome;
import com.google.gwt.chrome.crx.client.Console;
import com.google.gwt.chrome.crx.client.Extension;
import com.google.gwt.chrome.crx.client.Port;
import com.google.gwt.chrome.crx.client.events.ConnectEvent;
import com.google.gwt.chrome.crx.client.events.MessageEvent;
import com.google.gwt.chrome.crx.client.events.MessageEvent.Message;
import com.google.gwt.core.client.Duration;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptException;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.coreext.client.JSON;
import com.google.gwt.xhr.client.XMLHttpRequest;
import com.google.speedtracer.client.ClientConfig;
import com.google.speedtracer.client.messages.HeadlessClearDataMessage;
import com.google.speedtracer.client.messages.HeadlessDumpDataAckMessage;
import com.google.speedtracer.client.messages.HeadlessDumpDataMessage;
import com.google.speedtracer.client.messages.HeadlessMonitoringOffAckMessage;
import com.google.speedtracer.client.messages.HeadlessMonitoringOffMessage;
import com.google.speedtracer.client.messages.HeadlessMonitoringOnAckMessage;
import com.google.speedtracer.client.messages.HeadlessMonitoringOnMessage;
import com.google.speedtracer.client.messages.HeadlessSendDataAckMessage;
import com.google.speedtracer.client.messages.HeadlessSendDataMessage;
import com.google.speedtracer.client.model.ChromeDebuggerDataInstance;
import com.google.speedtracer.client.model.DataInstance;
import com.google.speedtracer.client.model.EventRecord;
import com.google.speedtracer.client.util.Xhr;

/**
 * A Chrome extension background page script for running a headless version of
 * SpeedTracer intended to support benchmarking and unit testing.
 * 
 * NOTE: the public key is included because currently the Headless Extension
 * relies on statically referring to the chrome extension ID. While the private
 * key is required to generate a signed extension with the proper ID, including
 * the public key in the manifest allows an unpacked, unsigned extension to load
 * with the proper ID.
 * 
 * TODO(conroy): remove this once the reliance on the chrome extension ID is
 * resolved.
 * 
 * TODO(zundel): Before this can move into production, we need some kind of
 * security precautions against the dump feature being used by untrustworthy
 * websites. The current idea is to use a whitelist to configure sites that can
 * access the headless API and sites that can be a target of the XHR when run
 * from the query string. See {@link HeadlessOptionsPage}
 */
@Extension.ManifestInfo(name = "Speed Tracer - headless (by Google)", description = "Get insight into the performance of your web applications.", version = ClientConfig.VERSION, permissions = {
    "tabs", "http://*/*", "https://*/*", "debugger"}, icons = {
    "resources/icon16.png", "resources/icon32.png", "resources/icon48.png",
    "resources/icon128.png"}, publicKey = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDLwpQwF5uAQ8ufE3XErrzZBim2rDzUpKFOD+/jStzSBczBXkZIdUhOpdrfhSbDjDUsPeWkHg1bdsjSGg/4hfGeJCFCOwwPqOJHFKVRPan1hMWu7nIDKWbP6d/eCBw8MWq1o+FObwbB0AIgNFsvoQgN1iwrRZB6rxkQmEdYQqiIOQIDAQAB")
public class HeadlessBackgroundPage extends Extension implements
    ConnectEvent.Listener {

  /**
   * Fires when messages are returned from the content script.
   */
  public class MessageHandler implements MessageEvent.Listener {
    private class HeadlessDataModel implements DataInstance.DataListener {
      private int sequence = 0;

      public void onEventRecord(EventRecord event) {
        event.setSequence(sequence++);
        // Send this message over to the content script
        String dataString = JSON.stringify(event);
        eventRecordData.push(dataString);
      }
      
      public void onEventStreamStarted() {
      }
    }

    private final Port port;

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
      DataInstance dataInstance = getDataInstance();
      dataInstance.setBaseTime(Duration.currentTimeMillis());
      eventRecordData.setLength(0);
    }

    private void doHeadlessDumpData(HeadlessDumpDataMessage message) {
      // Pack up the data we've been saving in our list and send it back
      // to the API.
      HeadlessDumpDataAckMessage dumpMessage = HeadlessDumpDataAckMessage.create(eventRecordData.join("\n"));
      sendToContentScript(port, dumpMessage);
    }

    private void doHeadlessMonitoringOff(HeadlessMonitoringOffMessage message) {
      // The API is telling us to turn off monitoring.
      DataInstance dataInstance = getDataInstance();
      dataInstance.stopMonitoring();
      HeadlessMonitoringOffAckMessage ackMessage = HeadlessMonitoringOffAckMessage.create();
      sendToContentScript(port, ackMessage);
    }

    private void doHeadlessMonitoringOn(HeadlessMonitoringOnMessage message) {
      // The API is telling us to turn on monitoring.
      DataInstance dataInstance = getDataInstance();
      dataInstance.resumeMonitoring();
      HeadlessMonitoringOnMessage.Options options = message.getOptions();
      HeadlessMonitoringOnAckMessage ackMessage = HeadlessMonitoringOnAckMessage.create();
      if (options != null) {
        if (options.clearData()) {
          doHeadlessClearData(null);
        }
        ackMessage.setOptions(options);
      }
      sendToContentScript(port, ackMessage);
    }

    private void doHeadlessSendData(HeadlessSendDataMessage message) {
      DataInstance dataInstance = getDataInstance();
      // Pack up the data we've been saving in our list and send it out
      // via XHR.
      JsArray<JavaScriptObject> data = getEventRecordData();
      String payload = createXhrPayload(dataInstance.getBaseTime(), message,
          data);

      if (ClientConfig.isDebugMode()) {
        console.log("Sending payload of " + payload.length() + " bytes ("
            + data.length() + " trace records) to " + message.getUrl());
      }
      try {
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
        console.log("XHR failed: " + ex);
        HeadlessSendDataAckMessage sendMessage = HeadlessSendDataAckMessage.create(false);
        sendToContentScript(port, sendMessage);
        throw ex;
      }
    }

    /**
     * Find the currently active DataInstance associated with this tab, or
     * create a new one.
     */
    private DataInstance getDataInstance() {
      int id = port.getSender().getTab().getId();
      DataInstance dataInstance = dataInstances.get(id);
      if (dataInstance == null) {
        dataInstance = ChromeDebuggerDataInstance.create(id);
        dataInstances.put(id, dataInstance);
        HeadlessDataModel dataModel = new HeadlessDataModel();
        dataInstance.load(dataModel);
      }
      return dataInstance;
    }

    private JsArray<JavaScriptObject> getEventRecordData() {
      JsArray<JavaScriptObject> result = JsArray.createArray().cast();
      for (int i = 0, length = eventRecordData.length(); i < length; ++i) {
        JavaScriptObject row = JSON.parse(eventRecordData.get(i));
        result.push(row);
      }
      return result;
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

  private Console console;

  private HashMap<Integer, DataInstance> dataInstances = new HashMap<Integer, DataInstance>();

  private final JsArrayString eventRecordData = JsArrayString.createArray().cast();

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
    console = Chrome.getExtension().getBackgroundPage().getConsole();
  }
}