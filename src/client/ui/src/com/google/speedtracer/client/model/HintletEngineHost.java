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
import com.google.gwt.core.client.GWT.UncaughtExceptionHandler;
import com.google.gwt.coreext.client.JSON;
import com.google.gwt.webworker.client.ErrorEvent;
import com.google.gwt.webworker.client.ErrorHandler;
import com.google.gwt.webworker.client.MessageEvent;
import com.google.gwt.webworker.client.MessageHandler;
import com.google.gwt.webworker.client.Worker;
import com.google.speedtracer.client.ClientConfig;
import com.google.speedtracer.client.Logging;
import com.google.speedtracer.client.messages.HintMessage;
import com.google.speedtracer.client.model.DataDispatcher.EventRecordDispatcher;
import com.google.speedtracer.client.model.HintletInterface.ExceptionListener;
import com.google.speedtracer.client.model.HintletInterface.HintListener;
import com.google.speedtracer.shared.EventRecordType;

import java.util.ArrayList;
import java.util.List;

/**
 * Class responsible for communicating with the hintlet engine worker thread and
 * for providing API for submitting records for analysis and for calling back
 * when a hint fires from a hintlet.
 */
public class HintletEngineHost implements EventRecordDispatcher {

  private final List<ExceptionListener> exceptionListeners = new ArrayList<ExceptionListener>();
  private final Worker hintletEngineWorker;
  private final List<HintListener> hintListeners = new ArrayList<HintListener>();

  HintletEngineHost() {
    // Fire up the Dedicated worker that will run the actual hintlet engine.
    hintletEngineWorker = Worker.create("../hintletengine/hintletengine.nocache.js");
    init();
  }

  public void addExceptionHandler(ExceptionListener listener) {
    exceptionListeners.add(listener);
  }

  public void addHintListener(HintListener listener) {
    hintListeners.add(listener);
  }

  /**
   * Stops the worker. After calling this function, it is no longer safe to make
   * method calls in this instance - the object should be discarded.
   */
  public void destroy() {
    hintletEngineWorker.terminate();
  }

  public void onEventRecord(EventRecord data) {
    if (data.getType() != EventRecordType.PROFILE_DATA) {
      // The hintlet engine does not like profile data
      hintletEngineWorker.postMessage(JSON.stringify(data));
    }
  }

  /**
   * A hint record generated outside of the hintlet worker.
   * 
   * @param hint
   */
  public void onSynthesizedHint(HintRecord hint) {
    onHint(hint);
  }

  public void removeExceptionListener(ExceptionListener listener) {
    exceptionListeners.remove(listener);
  }

  public void removeHintListener(HintListener listener) {
    hintListeners.remove(listener);
  }

  private void init() {
    hintletEngineWorker.setOnError(new ErrorHandler() {
      public void onError(ErrorEvent event) {
        UncaughtExceptionHandler ueh = GWT.getUncaughtExceptionHandler();
        if (ueh != null) {
          try {
            onHintletException(event.<HintletException> cast());
          } catch (Exception ex) {
            ueh.onUncaughtException(ex);
          }
        } else {
          onHintletException(event.<HintletException> cast());
        }
      }
    });

    hintletEngineWorker.setOnMessage(new MessageHandler() {
      public void onMessage(MessageEvent event) {
        UncaughtExceptionHandler ueh = GWT.getUncaughtExceptionHandler();
        if (ueh != null) {
          try {
            fireOnHint(event);
          } catch (Exception ex) {
            ueh.onUncaughtException(ex);
          }
        } else {
          fireOnHint(event);
        }
      }

      private void fireOnHint(MessageEvent event) {
        HintMessage msg = HintMessage.create(event.getDataAsString());

        if (msg.isHint()) {
          onHint(msg.getHint());
        } else if (ClientConfig.isDebugMode()) {
          if (msg.isLog()) {
            Logging.getLogger().logText(msg.getLog());
          } else {
            Logging.getLogger().logText(
                "Unknown message type from hintlet engine: " + msg.getType());
            assert false;
          }
        }
      }
    });
  }

  private void onHint(HintRecord hint) {
    for (int i = 0, n = hintListeners.size(); i < n; i++) {
      HintListener listener = hintListeners.get(i);
      listener.onHint(hint);
    }
  }

  private void onHintletException(HintletException hintletException) {
    for (int i = 0, n = exceptionListeners.size(); i < n; i++) {
      ExceptionListener handler = exceptionListeners.get(i);
      handler.onHintletException(hintletException);
    }
  }
}
