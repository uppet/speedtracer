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
import com.google.speedtracer.client.model.DataDispatcher;
import com.google.speedtracer.client.model.HintletException;
import com.google.speedtracer.client.model.HintletInterface;
import com.google.speedtracer.client.model.NetworkEventDispatcher;
import com.google.speedtracer.client.model.NetworkResource;
import com.google.speedtracer.client.model.UiEvent;
import com.google.speedtracer.client.model.UiEventDispatcher;
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
      NetworkEventDispatcher.Listener, UiEventDispatcher.UiEventListener,
      HintletInterface.ExceptionListener {

    private final ZippyLogger zippyLogger;

    public DebugListenerLogger() {
      zippyLogger = ZippyLogger.get();
    }

    public void listenTo(DataDispatcher dispatcher) {
      dispatcher.getNetworkEventDispatcher().addListener(this);
      dispatcher.getUiEventDispatcher().addUiEventListener(this);
      dispatcher.getHintletEngineHost().addExceptionHandler(this);
    }

    public void logHtml(String html) {
      zippyLogger.logHtml(html);
      GWT.log(html, null);
    }
    
    public void logHtmlError(String html) {
      zippyLogger.logHtmlError(html);
      GWT.log(html, null);
    }

    public void logText(String text) {
      zippyLogger.logText(text);
      GWT.log(text, null);
    }

    public void logTextError(String text) {
      zippyLogger.logTextError(text);
      GWT.log(text, null);
    }
    
    public void onHintletException(HintletException hintletException) {
      String text = "HintletEngine: " + hintletException.getException();
      logTextError(text);
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
    void listenTo(DataDispatcher dispatcher);

    void logHtml(String html);
    void logHtmlError(String html);

    void logText(String text);
    void logTextError(String text);
  }

  /**
   * This is a stub logging implementation to make stray log messages safe in a
   * release build.
   */
  public static class ReleaseListenerLogger implements ListenerLogger,
      NetworkEventDispatcher.Listener, UiEventDispatcher.UiEventListener,
      HintletInterface.ExceptionListener {

    public ReleaseListenerLogger() {
    }

    public void listenTo(DataDispatcher dispatcher) {
    }

    public void logHtml(String html) {
    }
    
    public void logHtmlError(String html) {
    }

    public void logText(String text) {
    }
    
    public void logTextError(String text) {
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
