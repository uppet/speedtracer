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

package com.google.speedtracer.extension.client;

import com.google.gwt.core.client.Duration;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.speedtracer.client.util.Command;
import com.google.speedtracer.client.util.Command.Method;

/**
 * Tests the functions in data_loader.js.
 */
public class DataLoaderTests extends GWTTestCase {

  interface PostMessageCallback {
    void postMessage(JavaScriptObject message);
  }

  // sanity test
  private static final String[] dataset1 = {"{object:1}"};

  // Tests the basic parser's understanding of special characters {, }, ', and "
  private static final String[] dataset2 = {"{ object: 1 }\n", //
      "{ object: 2, data:'foo'}\n", //
      "{ object: 3, data:'}bar'}\n", //
      "{ object: 4, data:'{bar'}\n", //
      "{ object: 5, data:'\\'bar'}\n", //
      "{ object: 6, data:\"baz\" }\n", //
      "{ object: 7, data:\"{baz\" }\n", //
      "{ object: 8, data:\"}baz\" }\n", //
      "{ object: 9, data:\"\\'baz\" }\n", //
      "{ object: 10, data:{ subobject: 1, data:'}foo'} }"//
  };

  // Tests the whitespace between JSON objects
  private static final String[] dataset3 = {"{ object: 1 }", //
      "{ object: 2, data:'foo'}\n", //
      "{ object: 3, data:'}bar'}  ", //
      "{ object: 4, data:'{bar'}\n", //
      "{ object: 5, data:'\\'bar'}\n\n\n\n\n", //
      "{ object: 6, data:\"baz\" }\n", //
      "{ object: 7, data:\"{baz\" }\n\t\t   \n\n" //
  };

  // Tests the whitespace and garbage between JSON objects
  private static final String[] dataset4 = {"{ object: 1 }\n", //
      "{ object: 2, data:'foo'} }}}}}}\n", //
      "{ object: 3, data:'}bar'}  GARBAGE   ", //
      "{ object: 4, data:'}bar'}  TRAILING GARBAGE   ", //
      "{ object: 5, data:'}bar'}  { TRAILING GARBAGE WITH CURLY", //
  };

  private static final String[] dataset4Expected = {"{ object: 1 }", //
      "{ object: 2, data:'foo'}", //
      "{ object: 3, data:'}bar'}", //
      "{ object: 4, data:'}bar'}", //
      "{ object: 5, data:'}bar'}", //
  };

  private static int POLL_INTERVAL = 250;
  private static int TEST_DELAY = 10000;

  private static native void ensureInjected() /*-{
    // Don't run the injection twice
    if ($wnd.__isTest) return;

    $wnd.__isTest = true;

    // Inject script
    try {
      var scriptTag = $doc.createElement("script");
      scriptTag.setAttribute("src", "data_loader.js");
      $doc.body.appendChild(scriptTag);
    } catch (ex) {
      $wnd.alert("exception injecting: " + ex);
    }
  }-*/;

  private static native boolean isParsed() /*-{
    return !!(typeof $wnd.nextJsonObject === "function");
  }-*/;

  @Override
  public String getModuleName() {
    return "com.google.speedtracer.extension.ExtensionTests";
  }

  public void testNextJsonObject() {
    doInjectedTest(new Method() {
      public void execute() {
        runDataset(dataset1, null);
        runDataset(dataset2, null);
        runDataset(dataset3, null);
        runDataset(dataset4, dataset4Expected);
      }
    });
  }

  private void doInjectedTest(final Method method) {
    ensureInjected();
    delayTestFinish(TEST_DELAY);
    final double startTime = Duration.currentTimeMillis();

    Command.defer(new Method() {
      int retries = 0;

      public void execute() {
        System.out.println("Retries = " + retries++);
        if (!isParsed()) {
          if ((Duration.currentTimeMillis() - startTime) > (TEST_DELAY - POLL_INTERVAL)) {
            fail("looks like script tag never parsed.");
          }
          Command.defer(this, POLL_INTERVAL);
          return;
        }
        method.execute();
        finishTest();
      }
    });
  }

  private native JsArrayString parseJsonDataset(String dataset) /*-{
    var result = [];
     var json_result = [0]
    while (true) {
      var json_result = $wnd.nextJsonObject(json_result[0], dataset);
      if (json_result == null) {
        break;
      }
      result.push(json_result[1]);
    }
    return result;
  }-*/;

  private void runDataset(String[] dataset, String[] expectedDataset) {
    JsArrayString result;
    StringBuilder builder = new StringBuilder();
    for (int i = 0; i < dataset.length; i++) {
      builder.append(dataset[i]);
      builder.append("\n");
    }
    result = parseJsonDataset(builder.toString());
    assertEquals("length of dataset", dataset.length, result.length());
    for (int i = 0; i < dataset.length; i++) {
      String expected;
      if (expectedDataset == null) {
        expected = dataset[i].trim();
      } else {
        expected = expectedDataset[i];
      }
      assertEquals("dataset[" + i + "]", expected, result.get(i));
    }
  }

}
