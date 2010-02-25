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
package com.google.speedtracer.client.model;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Element;
import com.google.speedtracer.client.util.dom.DocumentExt;

/**
 * Model implementation for hooking into the plugin. Handles registration of
 * callbacks to receive connection events from other browsers.
 * 
 * TODO(jaimeyap): This is dead code now. But we will keep it around since it
 * will be the basis for an external plugin based extension.
 */
public class PluginModel implements Model {

  /**
   * Tagged class to add npapi plugin to our extension manifest.
   * 
   * TODO(jaimeyap): Temporarily disable adding the plugin info to the manifest
   * until we have working cross platform plugin.
   */
  // @ManifestInfo(path = "plugins/npspeedtracer.dll", isPublic = false)
  private static class SpeedTracerPlugin { // extends Plugin {
  }

  private static class ControlInstance extends JavaScriptObject {
    protected ControlInstance() {
    }

    public final native void load(JavaScriptObject object) /*-{
      this.Load(object);
    }-*/;

    // Temporary defensive method to make sure that this object is backed by
    // a real plugin.
    private native boolean isLive() /*-{
      return this.hasOwnProperty("Load");
    }-*/;
  }

  private static final String MIME_TYPE = "x-application/speed-tracer";

  private static native JavaScriptObject createListenerObject(
      Model.Listener listener) /*-{
    return {
      onTabMonitorStarted: function(browserId, tab, dataInstance) {
        listener.@com.google.speedtracer.client.model.Model.Listener::onTabMonitorStarted(ILcom/google/speedtracer/client/model/TabDescription;Lcom/google/speedtracer/client/model/DataInstance;)(browserId, tab, dataInstance);
      },
      onMonitoredTabChanged: function(browserId, tab) {
        listener.@com.google.speedtracer.client.model.Model.Listener::onMonitoredTabChanged(ILcom/google/speedtracer/client/model/TabDescription;)(browserId, tab);
      },
      onTabMonitorStopped: function(browserId, tabId) {
        listener.@com.google.speedtracer.client.model.Model.Listener::onTabMonitorStopped(II)(browserId,tabId);
      },
      onBrowserConnected: function(browserId, name, version) {
        listener.@com.google.speedtracer.client.model.Model.Listener::onBrowserConnected(ILjava/lang/String;Ljava/lang/String;)(browserId, name, version);
      },
      onBrowserDisconnected: function(browserId) {
        listener.@com.google.speedtracer.client.model.Model.Listener::onBrowserDisconnected(I)(browserId);
      }
    };
  }-*/;

  private static Element createPluginElement() {
    final Element elem = DocumentExt.getCurrentDocument().createElement("embed");
    elem.setAttribute("type", MIME_TYPE);
    elem.setAttribute("width", "0");
    elem.setAttribute("height", "0");
    return elem;
  }

  private final ControlInstance controlInstance;

  public PluginModel() {
    GWT.create(SpeedTracerPlugin.class);
    final Element elem = createPluginElement();
    DocumentExt.getCurrentDocument().getBody().appendChild(elem);

    controlInstance = elem.cast();
  }

  public void load(Listener listener) {
    // We guard for now since we don't have a plugin built for all platforms.
    if (controlInstance.isLive()) {
      controlInstance.load(createListenerObject(listener));
    }
  }
}
