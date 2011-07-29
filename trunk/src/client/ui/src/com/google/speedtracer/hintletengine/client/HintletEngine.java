/*
 * Copyright 2009 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.speedtracer.hintletengine.client;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.coreext.client.JSON;
import com.google.gwt.webworker.client.DedicatedWorkerEntryPoint;
import com.google.gwt.webworker.client.MessageEvent;
import com.google.gwt.webworker.client.MessageHandler;
import com.google.speedtracer.client.model.HintRecord;
import com.google.speedtracer.hintletengine.client.rules.HintletCacheControl;
import com.google.speedtracer.hintletengine.client.rules.HintletFrequentLayout;
import com.google.speedtracer.hintletengine.client.rules.HintletGwtDetect;
import com.google.speedtracer.hintletengine.client.rules.HintletLongDuration;
import com.google.speedtracer.hintletengine.client.rules.HintletNotGz;
import com.google.speedtracer.hintletengine.client.rules.HintletRule;
import com.google.speedtracer.hintletengine.client.rules.HintletStaticNoCookie;
import com.google.speedtracer.hintletengine.client.rules.HintletTotalBytes;

import java.util.ArrayList;
import java.util.List;

/**
 * The entrypoint for the HintletEngine that runs in a worker thread.
 */
public class HintletEngine extends DedicatedWorkerEntryPoint implements MessageHandler {

  private class OnHintCallback implements HintletOnHintListener {

    /**
     * Sends a hint record (as JSON) to the user interface
     */
    public void onHint(String hintletRule, double timestamp, String description, int refRecord,
        int severity) {
      JavaScriptObject value =
          HintRecord.create(hintletRule, timestamp, severity, description, refRecord);
      postMessage(JSON.stringify(createHintMessage(value)));
    }

    private native JavaScriptObject createHintMessage(JavaScriptObject value) /*-{
      return {
        type : 2,
        payload : value
      };
    }-*/;
  }

  private HintletEventRecordProcessor eventRecordProcessor;

  @Override
  public void onWorkerLoad() {
    setOnMessage(this);
  }

  private List<HintletRule> getAllRules() {
    // all rules share the same callback
    OnHintCallback onHintCallback = new OnHintCallback();

    List<HintletRule> rules = new ArrayList<HintletRule>();
    rules.add(new HintletFrequentLayout(onHintCallback));
    rules.add(new HintletLongDuration(onHintCallback));
    rules.add(new HintletGwtDetect(onHintCallback));
    rules.add(new HintletNotGz(onHintCallback));
    rules.add(new HintletTotalBytes(onHintCallback));
    rules.add(new HintletStaticNoCookie(onHintCallback));
    rules.add(new HintletCacheControl(onHintCallback));
    return rules;
  }

  public void onMessage(MessageEvent event) {
    if (eventRecordProcessor == null) {
      eventRecordProcessor = new HintletEventRecordProcessor(getAllRules());
    }
    JavaScriptObject record = JSON.parse(event.getDataAsString());
    eventRecordProcessor.onEventRecord(record);
  }
  
}
