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

import com.google.gwt.core.client.GWT;
import com.google.speedtracer.client.model.DataModel;
import com.google.speedtracer.client.model.HintletEngineHost;
import com.google.speedtracer.client.model.HintletException;
import com.google.speedtracer.client.model.NetworkResourceError;
import com.google.speedtracer.client.model.NetworkResourceFinished;
import com.google.speedtracer.client.model.NetworkResourceModel;
import com.google.speedtracer.client.model.NetworkResourceResponse;
import com.google.speedtracer.client.model.NetworkResourceStart;
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

    private final ZippyLogger logger;

    public DebugListenerLogger() {
      logger = ZippyLogger.get();
    }

    public void listenTo(DataModel model) {
      model.getNetworkResourceModel().addListener(this);
      model.getUiEventModel().addListener(this);
      model.getHintletEngineHost().addExceptionHandler(this);
    }

    public void logHtml(String html) {
      logger.logHtml(html);
    }

    public void logText(String text) {
      String dressedUpText = "<span style=\"color:"
          + MonitorConstants.LOGGER_NET_HEADER_COLOR + "\">" + text + "</span>";
      logger.logHtml(dressedUpText);
    }

    public void onHintletException(HintletException hintletException) {
      logText("HintletEngine: " + hintletException.getException());
    }

    public void onNetworkResourceRequestStarted(
        NetworkResourceStart resourceStart) {
      // TODO Auto-generated method stub
    }

    public void onNetworkResourceResponseFailed(
        NetworkResourceError resourceError) {
      // TODO Auto-generated method stub
    }

    public void onNetworkResourceResponseFinished(
        NetworkResourceFinished resourceFinish) {
      // TODO Auto-generated method stub
    }

    public void onNetworkResourceResponseStarted(
        NetworkResourceResponse resourceResponse) {
      // TODO Auto-generated method stub
    }

    public void onUiEventFinished(UiEvent event) {
      // TODO Auto-generated method stub
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
   * No-op class for production debug logging.
   */
  public static class NullListenerLogger implements ListenerLogger {
    public NullListenerLogger() {
    }

    public void listenTo(DataModel model) {
    }

    public void logHtml(String html) {
    }

    public void logText(String text) {
    }
  }

  private static ListenerLogger logger;

  public static void createListenerLogger(DataModel model) {
    logger = GWT.create(ListenerLogger.class);
    logger.listenTo(model);
  }

  public static ListenerLogger getLogger() {
    return logger;
  }
}
