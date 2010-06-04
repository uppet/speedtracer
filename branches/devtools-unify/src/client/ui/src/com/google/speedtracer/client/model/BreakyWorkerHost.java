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

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.GWT.UncaughtExceptionHandler;
import com.google.gwt.coreext.client.DataBag;
import com.google.gwt.coreext.client.JSON;
import com.google.gwt.webworker.client.ErrorEvent;
import com.google.gwt.webworker.client.ErrorHandler;
import com.google.gwt.webworker.client.MessageEvent;
import com.google.gwt.webworker.client.MessageHandler;
import com.google.gwt.webworker.client.Worker;
import com.google.speedtracer.client.ClientConfig;
import com.google.speedtracer.client.Logging;
import com.google.speedtracer.client.model.DataDispatcher.EventRecordDispatcher;

/**
 * Pushes raw event records out to the worker and processes any breaky messages
 * about validation errors.
 * 
 */
public class BreakyWorkerHost implements EventRecordDispatcher {

  private final Worker breakyWorker;
  private final DataDispatcher dataDispatcher;
  private final HintletEngineHost hintletHost;

  BreakyWorkerHost(DataDispatcher dataDispatcher, HintletEngineHost hintletHost) {
    breakyWorker = Worker.create("../breakyworker/breakyworker.nocache.js");
    this.dataDispatcher = dataDispatcher;
    this.hintletHost = hintletHost;
    init();
  }

  /**
   * Send the raw {@link EventRecord} to the web worker.
   */
  public void onEventRecord(EventRecord data) {
    breakyWorker.postMessage(JSON.stringify(data));
  }

  /**
   * Initialize the event handlers for the host.
   */
  private void init() {
    breakyWorker.setOnError(new ErrorHandler() {
      public void onError(ErrorEvent event) {
        UncaughtExceptionHandler ueh = GWT.getUncaughtExceptionHandler();
        if (ueh != null) {
          try {
            onBreakyException(event);
          } catch (Exception ex) {
            ueh.onUncaughtException(ex);
          }
        } else {
          onBreakyException(event);
        }
      }
    });

    breakyWorker.setOnMessage(new MessageHandler() {
      public void onMessage(MessageEvent event) {
        UncaughtExceptionHandler ueh = GWT.getUncaughtExceptionHandler();
        if (ueh != null) {
          try {
            fireOnBreakyMessage(event);
          } catch (Exception ex) {
            ueh.onUncaughtException(ex);
          }
        } else {
          fireOnBreakyMessage(event);
        }
      }

      /**
       * Turn a breaky message into a synthesized hint record.
       * 
       * @param event breaky message
       */
      private void fireOnBreakyMessage(MessageEvent event) {
        JavaScriptObject breakyMessage = event.getDataAsJSO();
        int sequence = DataBag.getIntProperty(breakyMessage, "sequence");
        String message = DataBag.getStringProperty(breakyMessage, "message");
        Logging.getLogger().logText(
            "Breaky Error:(#" + sequence + ") " + message);

        EventRecord record = dataDispatcher.findEventRecord(sequence);
        if (record == null) {
          Logging.getLogger().logText(
              "Breaky cannot find record with sequence #" + sequence);
        } else {
          HintRecord validationHint = HintRecord.create("Validation Error",
              record.getTime(), HintRecord.SEVERITY_VALIDATION, message,
              sequence);
          hintletHost.onSynthesizedHint(validationHint);
        }
      }
    });
  }

  private void onBreakyException(ErrorEvent event) {
    if (ClientConfig.isDebugMode()) {
      Logging.getLogger().logText(
          "Breaky Exception: " + event.getMessage() + " in "
              + event.getFilename() + ":" + event.getLineNumber());
    }
  }

}
