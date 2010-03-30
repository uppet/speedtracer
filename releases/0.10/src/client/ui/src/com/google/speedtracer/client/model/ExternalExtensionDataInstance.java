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
package com.google.speedtracer.client.model;

import com.google.gwt.chrome.crx.client.Port;
import com.google.gwt.chrome.crx.client.events.MessageEvent;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.speedtracer.client.messages.EventRecordMessage;

/**
 * {@link DataInstance} implementation for external extensions that wish to
 * supply data to Speed Tracer.
 * 
 * This class exposes utility methods and objects for talking to and receiving
 * Speed Tracer data from external extensions.
 * 
 * The protocol for external extensions works like this:
 * 
 * 1. External extension sends a ConnectRequest to Speed Tracer's background
 * page.
 * 
 * 2. The background page receives the connection request, checks if the
 * extension attempting to connect is blacklisted, and then provisions a
 * listener for a specific port name that is derived from the supplied
 * browser/tab id tuple.
 * 
 * 3. The background page responds to the request with the port name it expects
 * to receive the connection on.
 * 
 * 4. The connecting external extension connects to the background page at that
 * port name.
 * 
 * 5. The background page opens a monitor window with an
 * ExternalExtensionDataInstance to be used to supply data.
 * 
 * 6. The external extension may or may not wait for a ready ACK message to be
 * sent by the speed tracer monitor before blasting data.
 */
public class ExternalExtensionDataInstance extends DataInstance {
  /**
   * Overlay type for the connection request that is sent by an external
   * extension to talk to Speed Tracer to initiate a connection for a new
   * monitored 'tab'.
   */
  public static class ConnectRequest extends JavaScriptObject {
    public static native ConnectRequest create(int browserId, int tabId,
        String title, String url) /*-{
      return {
        browserId: browserId,
        tabId: tabId,
        title: title,
        url: url
      };
    }-*/;

    protected ConnectRequest() {
    }

    public final native int getBrowserId() /*-{
      return this.browserId;
    }-*/;

    public final native int getTabId() /*-{
      return this.tabId;
    }-*/;

    public final native String getTitle() /*-{
      return this.title;
    }-*/;

    public final native String getUrl() /*-{
      return this.url;
    }-*/;
  }

  /**
   * Backing POJO for this DataInstance implementation.
   */
  private static class Proxy implements DataProxy {
    private final Port port;

    Proxy(Port port) {
      this.port = port;
    }

    public void load(final DataInstance dataInstance) {
      // Connect the data coming in the port to the DataInstance.
      port.getOnMessageEvent().addListener(new MessageEvent.Listener() {
        public void onMessage(MessageEvent.Message message) {
          EventRecordMessage evtRecordMessage = message.cast();
          dataInstance.onEventRecord(evtRecordMessage.getEventRecord());
        }
      });
      
      // TODO (jaimeyap): Send message on port to tell external extension to
      // start sending data.
    }

    public void resumeMonitoring() {
      // TODO (jaimeyap): Send message on port to tell external extension to
      // resume sending data if it stopped.
    }

    public void setBaseTime(double baseTime) {
      // TODO (jaimeyap): Send message on port to tell external extension to
      // reset its base time that it uses for normalizing the data.
    }

    public void setProfilingOptions(boolean enableStackTraces,
        boolean enableCpuProfiling) {
      // TODO (jaimeyap): Send message on port to tell external extension to set
      // the profiling options for the external browser.
    }

    public void stopMonitoring() {
      // TODO (jaimeyap): Send message on port to tell external extension to
      // stop monitoring temporarily.
    }

    public void unload() {
      // TODO (jaimeyap): Send message on port to tell external extension to
      // disconnect and stop talking to us.
    }
  }

  public static final String SPEED_TRACER_EXTERNAL_PORT = "speed_tracer_external";

  /**
   * Creates the ExternalExtensionDataInstance JSO interface object that will
   * get passed on to the Monitor.
   * 
   * @param port the port that will be used for two way communication.
   * @return the {@link ExternalExtensionDataInstance} JSO interface object.
   */
  public static ExternalExtensionDataInstance create(Port port) {
    return DataInstance.create(new Proxy(port)).cast();
  }

  /**
   * Response sent to the external extension that initiated a connection
   * request.
   * 
   * @param portName the name of the port that the background page will be
   *          listening on.
   * @return the message payload for the response.
   */
  public static native JavaScriptObject createResponse(String portName) /*-{
    return {
      portName: portName
    };
  }-*/;

  /**
   * If we encounter bad extensions in the wild, we can use this to black list
   * them so they can't talk to Speed Tracer.
   * 
   * @param extensionId the id of extension that is attempting to connect
   * @return whether or not the id is blacklisted.
   */
  public static boolean isBlackListed(String extensionId) {
    return false;
  }

  protected ExternalExtensionDataInstance() {
  }
}
