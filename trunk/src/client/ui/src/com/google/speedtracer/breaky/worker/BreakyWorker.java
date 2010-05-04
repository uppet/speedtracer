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

package com.google.speedtracer.breaky.worker;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.coreext.client.JSON;
import com.google.gwt.webworker.client.DedicatedWorkerEntryPoint;
import com.google.gwt.webworker.client.MessageEvent;
import com.google.gwt.webworker.client.MessageHandler;
import com.google.speedtracer.breaky.client.DumpValidator;
import com.google.speedtracer.breaky.client.JsonSchema.JsonSchemaResults;

/**
 * A web worker that performs validation on raw event records.
 * On an invalid record, it will report an error to the host
 */
public class BreakyWorker extends DedicatedWorkerEntryPoint implements MessageHandler {

  private DumpValidator validator;
  
  /**
   * Creates a message to send to the host.
   * @param record the raw event record
   * @param message the validation message
   * @return JSO with fields 'sequence' and 'message'
   */
  private native JavaScriptObject createMessage(JavaScriptObject record, String message) /*-{
    var sequence = record.hasOwnProperty("sequence") ? record.sequence : -1;
    return { "sequence" : sequence, "message" : message };
  }-*/;
  
  /**
   * On a message, validate.
   * If invalid, report to the host
   */
  public void onMessage(MessageEvent event) {   
    JavaScriptObject record = JSON.parse(event.getDataAsString());
    JsonSchemaResults results = validator.validate(record);
    if (!results.isValid()) {
      postMessage(JSON.stringify(createMessage(record, results.formatResultsText(event.getDataAsString()))));
    }
  }
  
  @Override
  public void onWorkerLoad() {
    importScript("jsonschema-b4-speedtracer.js");
    validator = new DumpValidator();
    setOnMessage(this);
  }
}
