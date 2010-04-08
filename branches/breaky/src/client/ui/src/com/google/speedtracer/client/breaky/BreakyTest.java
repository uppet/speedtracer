package com.google.speedtracer.client.breaky;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptException;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.ImageElement;
import com.google.gwt.dom.client.Style.BorderStyle;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.Response;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.IncrementalCommand;
import com.google.gwt.user.client.Timer;
import com.google.gwt.xhr.client.XMLHttpRequest;
import com.google.speedtracer.client.ClientConfig;
import com.google.speedtracer.client.breaky.JsonSchema.JsonSchemaError;
import com.google.speedtracer.client.breaky.JsonSchema.JsonSchemaResults;
import com.google.speedtracer.client.model.MockModelGenerator;
import com.google.speedtracer.client.util.JSOArray;
import com.google.speedtracer.client.util.JSON;
import com.google.speedtracer.client.util.Xhr;
import com.google.speedtracer.client.util.JSON.JSONParseException;
import com.google.speedtracer.client.util.Xhr.XhrCallback;
import com.google.speedtracer.headlessextension.HeadlessApi;
import com.google.speedtracer.headlessextension.HeadlessApi.MonitoringCallback;


public class BreakyTest implements EntryPoint {
  private static final int API_POLL_INTERVAL = 250;
  private static final int MAX_API_POLLS = 5 * (1000/API_POLL_INTERVAL);
  private final DivElement statusDiv = Document.get().createDivElement();
  private int apiPollCount = 0;
  private int validationCount = 0;
  private int wallCount = 100;
  DumpValidator validator = new DumpValidator();

  public void onModuleLoad() {
    this.setupLogging();
    
    if(ClientConfig.isMockMode()) {
      log("Running mock mode. Not loading Headless API.");
      Timer t = new Timer() {
        @Override
        public void run() {
          testValidator();
        }
      };
      t.schedule(10);
    } else {
      log("Running in regular mode. Loading Headless API.");
      this.loadApi();
    }  
  }
  
  private void testValidator() {
    log("fast fail if JSON isn't there");
    JavaScriptObject test = JSON.parse("{\"asdf\" : 3 }");
    log("About to validate");
    
    MockModelGenerator.simulateDump(new MockHandler(), 0);
  }
  
  private class MockHandler implements DumpProcessor.DumpEntryHandler {
    public boolean onDumpEntry(String dumpEntry) {
      String invalid = validateRecord(dumpEntry);
      if(invalid != null){
        log(invalid);
        log("<hr>");
        return false;
      }
      if(++validationCount % 500 == 0) {
        log("Processed " + validationCount + " records.");
      }
      return true;
    }

    public void onFinished() {
      log("Finished");
      reportValid();
    }  
  }
  
  private class RegularHandler implements DumpProcessor.DumpEntryHandler {

    public boolean onDumpEntry(String dumpEntry) {
      String invalid = validateRecord(dumpEntry);
      if(invalid != null) {
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

  private void setupLogging() {
    statusDiv.getStyle().setBorderColor("#aa0");
    statusDiv.getStyle().setBorderWidth(1, Unit.PX);
    statusDiv.getStyle().setBorderStyle(BorderStyle.SOLID);
    Document.get().getBody().appendChild(statusDiv);
  }
  
  private void log(String message) {
    statusDiv.setInnerHTML(statusDiv.getInnerHTML() + "<p>" + message + "</p>");
  }
  
  private void runTest() {
    
    Document doc = Document.get();
    DivElement imgDiv = doc.createDivElement();
    
    ImageElement img = doc.createImageElement();
    img.setSrc(GWT.getModuleBaseURL() + "speedtracer-large.png");
    imgDiv.appendChild(img);
    doc.getBody().appendChild(imgDiv);
    
    log("added image!");
    
    IncrementalCommand cmd = new IncrementalCommand() {
      public boolean execute() {
        wallCount--;
        if(wallCount % 2 == 0) {
          statusDiv.getStyle().setColor("#ff0000");
        } else {
          statusDiv.getStyle().setColor("#000000");
        }
        log(wallCount + " Bottles of Beer on the wall (h:" + 
            String.valueOf(statusDiv.getClientHeight()) + ")");
        statusDiv.getStyle().setBorderWidth(99 - wallCount, Unit.PX);
        statusDiv.getStyle().setBorderColor("#0000ff");
        
        if(wallCount > 0) {
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
  
  private void getDump() {
    HeadlessApi.getDump(new HeadlessApi.GetDumpCallback() {
      public void callback(String dump) {
        validateDump(dump);
      }
    });
    
  }

  private void validateDump(String dump) {
    String[] dumpArray = dump.split("\n");
    DumpProcessor processor = new DumpProcessor(new RegularHandler(), dumpArray);
    DeferredCommand.addCommand(processor);
  }

  private void reportInvalid(String invalid) {
    log("About to report invalid...");
    Xhr.postWorkaround(GWT.getModuleBaseURL() + "invalid", invalid, "text/plain", new XhrCb());
  }
  
  private void reportValid() {
    log("About to report valid..");
    Xhr.postWorkaround(GWT.getModuleBaseURL() + "valid", "", "text/plain", new XhrCb());
  }
  
  private class XhrCb implements XhrCallback {
    public void onFail(XMLHttpRequest xhr) {
      log("XHR: Got an error: " + xhr.getResponseText());
      
    }
    public void onSuccess(XMLHttpRequest xhr) {
      log("XHR: success: " + xhr.getResponseText());
    }
  }
   
  
  private String validateRecord(String rawRecord) {
    try {
      JavaScriptObject record = JSON.parse(rawRecord);
      JsonSchemaResults results = validator.validate(record.cast());
      if(results.isValid()) {
        return null;
      } else {
        return formatResults(rawRecord, results);
      }
    } catch(JSONParseException e) {
      log("Got an exception trying to JSON parse the record: " + rawRecord);
      log(e.getMessage());
      return "Got an Exception!";
    }
  }

  private String formatResults(String objString, JsonSchemaResults results) {
    if(results.isValid()) {
      return "Valid:" + objString + "<br>";
    } else {
      String errorString = "INVALID<br>Object: " + objString + "<br>";
      JSOArray<JsonSchemaError> errors = results.getErrors();
      for(int i = 0; i < errors.size(); i++) {
        errorString += "Property: " + errors.get(i).getProperty() + 
                       "<br>Error: " + errors.get(i).getMessage() + "<br>";
      }
      return errorString; 
    }
  }

  private void fail(String message)  {
    if(ClientConfig.isMockMode()) {
      log("Breaky Failure: " + message);
    } else {
      reportInvalid("Breaky Failure: " + message);
    }
  }


  private void loadApi() {
    if (!HeadlessApi.isLoaded()) {
      HeadlessApi.loadApi();
    }
    pollApi();
  }

  private void pollApi() {
    if(HeadlessApi.isLoaded()) {
      HeadlessApi.MonitoringOnOptions options = HeadlessApi.MonitoringOnOptions.createObject().cast();
      options.clearData();
      log("pollApi() finished");
      
      HeadlessApi.startMonitoring(options, new MonitoringCallback() {
        public void callback() {
          try {
            log("oh hai");
            
            runTest();
          } catch (JavaScriptException e) {
            if (e.getStackTrace().length == 0) {
              Throwable t = e.fillInStackTrace();
              log(t.toString());
            }
          }
        }
      });
      return;
    }
    
    if(apiPollCount++ > MAX_API_POLLS) {
      fail("Unable to load API");
    } else {
      Timer t = new Timer() {
        @Override
        public void run() {
          log("in poll");
          pollApi();
        }
      };
      t.schedule(API_POLL_INTERVAL);
    }
    
  }

}
