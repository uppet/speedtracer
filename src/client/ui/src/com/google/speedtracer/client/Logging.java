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

import com.google.gwt.core.client.GWT;
import com.google.speedtracer.client.model.DataModel;
import com.google.speedtracer.client.model.HintletEngineHost;
import com.google.speedtracer.client.model.HintletException;
import com.google.speedtracer.client.model.NetworkResource;
import com.google.speedtracer.client.model.NetworkResourceModel;
import com.google.speedtracer.client.model.UiEvent;
import com.google.speedtracer.client.model.UiEventModel;
import com.google.speedtracer.client.view.ZippyLogger;

/**
 * Debug Logging Infrastructure.
 */
public class Logging {
  /**
   * Listener whose sole purpose is to log events. TODO (knorton): When you add
   * SluggishnessModel and SluggishnessModelListener you should have this
   * implement the listener to get debug output.
   */
  public static class DebugListenerLogger implements ListenerLogger,
      NetworkResourceModel.Listener, UiEventModel.Listener,
      HintletEngineHost.ExceptionListener {

    private final ZippyLogger zippyLogger;

    public DebugListenerLogger() {
      zippyLogger = ZippyLogger.get();
    }

    public void listenTo(DataModel model) {
      model.getNetworkResourceModel().addListener(this);
      model.getUiEventModel().addListener(this);
      model.getHintletEngineHost().addExceptionHandler(this);
    }

    public void logHtml(String html) {
      zippyLogger.logHtml(html);
      GWT.log(html, null);
    }

    public void logText(String text) {
      zippyLogger.logText(text);
      GWT.log(text, null);
    }

    public void onHintletException(HintletException hintletException) {
      String text = "HintletEngine: " + hintletException.getException();
      logText(text);
      GWT.log(text, null);
    }

    public void onNetworkResourceRequestStarted(NetworkResource resource,
        boolean isRedirect) {
    }

    public void onNetworkResourceResponseFinished(NetworkResource resource) {
    }

    public void onNetworkResourceResponseStarted(NetworkResource resource) {
    }

    public void onNetworkResourceUpdated(NetworkResource resource) {
    }

    public void onUiEventFinished(UiEvent event) {
    }
  }

  /**
   * Shared Interface type for deferred binding.
   */
  public interface ListenerLogger {
    void listenTo(DataModel model);

    void logHtml(String html);

    void logText(String text);
  }

  /**
   * This is a stub logging implementation to make stray log messages safe in a
   * release build.
   */
  public static class ReleaseListenerLogger implements ListenerLogger,
      NetworkResourceModel.Listener, UiEventModel.Listener,
      HintletEngineHost.ExceptionListener {

    public ReleaseListenerLogger() {
    }

    public void listenTo(DataModel model) {
    }

    public void logHtml(String html) {
    }

    public void logText(String text) {
    }

    public void onHintletException(HintletException hintletException) {
    }

    public void onNetworkResourceRequestStarted(NetworkResource resource,
        boolean isRedirect) {
    }

    public void onNetworkResourceResponseFinished(NetworkResource resource) {
    }

    public void onNetworkResourceResponseStarted(NetworkResource resource) {
    }

    public void onNetworkResourceUpdated(NetworkResource resource) {
    }

    public void onUiEventFinished(UiEvent event) {
    }
  }

  private static ListenerLogger logger;

  public static ListenerLogger getLogger() {
    // Lazy initialize the logger
    if (logger == null) {
      if (ClientConfig.isDebugMode()) {
        logger = new DebugListenerLogger();
      } else {
        logger = new ReleaseListenerLogger();
      }
    }

    return logger;
  }
}
