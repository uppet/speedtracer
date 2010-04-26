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
package com.google.speedtracer.breaky.client;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptException;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.coreext.client.JSOArray;
import com.google.gwt.coreext.client.JSON;
import com.google.gwt.coreext.client.JSON.JSONParseException;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.ImageElement;
import com.google.gwt.dom.client.Style.BorderStyle;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.IncrementalCommand;
import com.google.gwt.user.client.Timer;
import com.google.gwt.xhr.client.XMLHttpRequest;
import com.google.speedtracer.client.ClientConfig;
import com.google.speedtracer.breaky.client.JsonSchema.JsonSchemaError;
import com.google.speedtracer.breaky.client.JsonSchema.JsonSchemaResults;
import com.google.speedtracer.client.model.MockModelGenerator;
import com.google.speedtracer.client.util.Xhr;
import com.google.speedtracer.client.util.Xhr.XhrCallback;
import com.google.speedtracer.headlessextension.HeadlessApi;
import com.google.speedtracer.headlessextension.HeadlessApi.MonitoringCallback;

import java.util.Date;

/**
 * The Breaky Test validates the event data that we receive from Chrome. Any
 * unrecognized or malformed data fields should cause the test to fail.
 * 
 * Currently, the test is only driven by a simple page that lacks 100% coverage
 * of all possible timeline events. We should think about ways to improve our
 * coverage of the current events as well as future events. Primarily, this test
 * is meant to be an early warning system for breaking changes in upstream
 * Chrome/WebCore.
 */
public class BreakyTest implements EntryPoint {
  /**
   * A {@link DumpProcessor.DumpEntryHandler} for running the test in mock mode.
   */
  private class MockHandler implements DumpProcessor.DumpEntryHandler {
    public boolean onDumpEntry(String dumpEntry) {
      String invalid = validateRecord(dumpEntry);
      if (invalid != null) {
        log(invalid);
        log("<hr>");
        return false;
      }
      if (++validationCount % 500 == 0) {
        Date date = new Date();
        String now = DateTimeFormat.getFormat("H:m:s").format(date);
        log("Processed " + validationCount + " records. " + now);
      }
      return true;
    }

    public void onFinished() {
      log("Finished");
      reportValid();
    }
  }
  /**
   * A {@link DumpProcessor.DumpEntryHandler} for running the normal test.
   */
  private class RegularHandler implements DumpProcessor.DumpEntryHandler {
    public boolean onDumpEntry(String dumpEntry) {
      String invalid = validateRecord(dumpEntry);
      if (invalid != null) {
        reportInvalid(invalid);
        return false;
      } else {
        return true;
      }
    }

    public void onFinished() {
      reportValid();
    }
  }

  private class XhrCb implements XhrCallback {
    public void onFail(XMLHttpRequest xhr) {
      log("XHR: Got an error: " + xhr.getResponseText());
    }

    public void onSuccess(XMLHttpRequest xhr) {
      log("XHR: success: " + xhr.getResponseText());
    }
  }

  private static int INITIAL_WALL_COUNT = 100;
  
  private final DivElement statusDiv = Document.get().createDivElement();

  private int validationCount = 0;

  private final DumpValidator validator = new DumpValidator();

  private int wallCount = INITIAL_WALL_COUNT;

  /**
   * The entryPoint for the test. If mock mode is active, then the Headless API
   * is not loaded.
   */
  public void onModuleLoad() {
    // Setup the statusDiv
    statusDiv.getStyle().setBorderColor("#aa0");
    statusDiv.getStyle().setBorderWidth(1, Unit.PX);
    statusDiv.getStyle().setBorderStyle(BorderStyle.SOLID);
    Document.get().getBody().appendChild(statusDiv);

    if (ClientConfig.isMockMode()) {
      log("Running mock mode. Not loading Headless API.");
      Timer t = new Timer() {
        @Override
        public void run() {
          mockSimulateDump();
        }
      };
      t.schedule(10);
    } else {
      this.monitorAndRun();
    }
  }

  /**
   * Format the results of the validation.
   * 
   * TODO(conroy): make this more dynamic (html vs plain-text), children, etc...
   * 
   * @param objString
   * @param results
   * @return a message to display to the user
   */
  private String formatResults(String objString, JsonSchemaResults results) {
    if (results.isValid()) {
      return "Valid: " + objString + "<br>";
    } else {
      StringBuilder errorStringBuilder = new StringBuilder();
      errorStringBuilder.append("INVALID<br>Object: ");
      errorStringBuilder.append(objString);
      errorStringBuilder.append("<br>");
      JSOArray<JsonSchemaError> errors = results.getErrors();
      for (int i = 0, length = errors.size(); i < length; i++) {
        errorStringBuilder.append("Property: ");
        errorStringBuilder.append(errors.get(i).getProperty());
        errorStringBuilder.append("<br>Error: ");
        errorStringBuilder.append(errors.get(i).getMessage());
        errorStringBuilder.append("<br>");
      }
      return errorStringBuilder.toString();
    }
  }

  private void getDump() {
    HeadlessApi.getDump(new HeadlessApi.GetDumpCallback() {
      public void callback(String dump) {
        validateDump(dump);
      }
    });
  }

  private void log(String message) {
    statusDiv.setInnerHTML(statusDiv.getInnerHTML() + "<p>" + message + "</p>");
  }

  private void mockSimulateDump() {
    // Fail fast if JSON is not present.
    JSON.parse("{\"asdf\" : 3 }");    
    log("About to validate");
    String[] dump = MockModelGenerator.getDump(0);
    DeferredCommand.addCommand(new DumpProcessor(new MockHandler(), dump));
  }

  /**
   * Tell the breaky server that this data set contained an invalid record.
   * 
   * @param invalid the message describing the invalid data
   */
  private void reportInvalid(String invalid) {
    log("About to report invalid...");
    Xhr.postWorkaround(GWT.getModuleBaseURL() + "invalid", invalid,
        "text/plain", new XhrCb());
  }

  /**
   * Tell the breaky server that this data set passed.
   */
  private void reportValid() {
    log("About to report valid..");
    Xhr.postWorkaround(GWT.getModuleBaseURL() + "valid", "", "text/plain",
        new XhrCb());
  }

  /**
   * Make the browser do something so that we have some data to validate. For
   * now, this does a simple 99 bottles of beer on the wall.
   * 
   * TODO(conroy): improve coverage
   */
  private void runTest() {

    Document doc = Document.get();

    // Add an image (will trigger layout and NetworkResourceEvents)
    DivElement imgDiv = doc.createDivElement();
    ImageElement img = doc.createImageElement();
    img.setSrc(GWT.getModuleBaseURL() + "speedtracer-large.png");
    imgDiv.appendChild(img);
    doc.getBody().appendChild(imgDiv);
    log("added image!");

    // Run a simple countdown
    IncrementalCommand cmd = new IncrementalCommand() {
      public boolean execute() {
        wallCount--;
        if (wallCount % 2 == 0) {
          statusDiv.getStyle().setColor("#ff0000");
        } else {
          statusDiv.getStyle().setColor("#000000");
        }
        log(wallCount + " Bottles of Beer on the wall (h:"
            + String.valueOf(statusDiv.getClientHeight()) + ")");
        statusDiv.getStyle().setBorderWidth(INITIAL_WALL_COUNT - wallCount,
            Unit.PX);
        statusDiv.getStyle().setBorderColor("#0000ff");

        if (wallCount > 0) {
          return true;
        } else {
          HeadlessApi.stopMonitoring(new MonitoringCallback() {
            public void callback() {
              getDump();
              log("all done!");
            }
          });
          return false;
        }
      }
    };
    DeferredCommand.addCommand(cmd);

    log("wait for it...");
  }

  /**
   * Start monitoring, and kick off the test once monitoring is active.
   */
  private void monitorAndRun() {
    if (!HeadlessApi.isLoaded()) {
      log("Headless API is not loaded. Failing now");
      reportInvalid("Headless API is not loaded!");
      return;
    }
    
    HeadlessApi.MonitoringOnOptions options = HeadlessApi.MonitoringOnOptions.createObject().cast();
    options.clearData();
    log("starting monitoring...");
    HeadlessApi.startMonitoring(options, new MonitoringCallback() {
      public void callback() {
        try {
          log("monitoring is on. running test...");
          runTest();
        } catch (JavaScriptException e) {
          if (e.getStackTrace().length == 0) {
            Throwable t = e.fillInStackTrace();
            log(t.toString());
            reportInvalid("Caught a JavaScriptException: " + t.toString());
          }
        } finally {
          // TODO(conroy): report all log messages via XHR for debugging
        }
      }
    });
    return;
  }
  
  /**
   * Validate a raw dump.
   * 
   * @param dump the dump as a raw string, with records separated by \n
   */
  private void validateDump(String dump) {
    String[] dumpArray = dump.split("\n");
    DumpProcessor processor = new DumpProcessor(new RegularHandler(), dumpArray);
    DeferredCommand.addCommand(processor);
  }

  /**
   * Validate a single record. On failure, attempt to produce a useful error
   * message
   * 
   * @param rawRecord the string representing the event record
   * @return
   */
  private String validateRecord(String rawRecord) {
    try {
      JavaScriptObject record = JSON.parse(rawRecord);
      JsonSchemaResults results = validator.validate(record.cast());
      if (results.isValid()) {
        return null;
      } else {
        return formatResults(rawRecord, results);
      }
    } catch (JSONParseException e) {
      log("Got an exception trying to JSON parse the record: " + rawRecord);
      log(e.getMessage());
      return "Got an Exception!";
    }
  }
}
