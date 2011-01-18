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
package com.google.speedtracer.headlessextension.client;

import com.google.gwt.core.client.Duration;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.coreext.client.DataBag;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.speedtracer.client.util.Command;
import com.google.speedtracer.client.util.Command.Method;

/**
 * Tests functions within the headless_content_script.js file.
 */
public class HeadlessContentScriptTests extends GWTTestCase {
  interface PostMessageCallback {
    void postMessage(JavaScriptObject message);
  }

  private static int POLL_INTERVAL = 250;
  // Gets set to true if postMessageMethod is invoked
  private static boolean postMessageCallbackInvoked = false;
  private static PostMessageCallback postMessageMethod = null;

  private static int TEST_DELAY = 10000;

  public static native void ensureInjected() /*-{
    // don't run the injection twice
    if ($wnd.__isTest) return;

    try {
      // mock out functions
      $wnd.__isTest = true;
      if (!$wnd.chrome) $wnd.chrome = {};
      if (!$wnd.chrome.extension) $wnd.chrome.extension = {};
      if (!$wnd.chrome.extension.connect) $wnd.chrome.extension.connect = function() {
          return { 
          'postMessage' : function(msg) {
            @com.google.speedtracer.headlessextension.client.HeadlessContentScriptTests::postMessageCallback(Lcom/google/gwt/core/client/JavaScriptObject;)(msg);
          },
          'onMessage' : {'addListener' : function(handler) {}},
        }
      };
    } catch (ex) {
      $wnd.alert("Exception mocking: " + ex);
    }

    // inject script
    try {
      var scriptTag = $doc.createElement("script");
      scriptTag.setAttribute("src", "headless_content_script.js");
      $doc.body.appendChild(scriptTag);
    } catch (ex) {
      $wnd.alert("exception injecting: " + ex);
    }
  }-*/;

  public static native boolean isParsed() /*-{
    return !!(typeof $wnd.handleMessagesFromBackgroundPage === "function");
  }-*/;

  // Invoked from JavaScript to call back the currently set postMessageMethod
  private static void postMessageCallback(JavaScriptObject msg) {
    if (postMessageMethod != null) {
      postMessageMethod.postMessage(msg);
      postMessageCallbackInvoked = true;
    }
  }

  public void doInjectedTest(final Method method) {
    postMessageCallbackInvoked = false;
    ensureInjected();
    delayTestFinish(TEST_DELAY);
    final double startTime = Duration.currentTimeMillis();

    Command.defer(new Method() {
      int retries = 0;

      public void execute() {
        System.out.println("Retries = " + retries++);
        if (!isParsed()) {
          if ((Duration.currentTimeMillis() - startTime) > (TEST_DELAY - POLL_INTERVAL)) {
            postMessageMethod = null;
            fail("looks like script tag never parsed.");
          }
          Command.defer(this, POLL_INTERVAL);
          return;
        }
        method.execute();
        postMessageMethod = null;
        finishTest();
      }
    });
  }

  @Override
  public String getModuleName() {
    return "com.google.speedtracer.headlessextension.HeadlessTests";
  }

  public void testLog() {
    doInjectedTest(new Method() {
      public void execute() {
        nativeLog("Test Message");
      }
    });
  }

  public void testParseQueryStringBad() {
    postMessageMethod = new PostMessageCallback() {
      public void postMessage(JavaScriptObject message) {
        DataBag messageBag = message.cast();
        assertEquals("type", 103, messageBag.getIntProperty("type"));
      }
    };
    doInjectedTest(new Method() {
      public void execute() {
        nativeParseQueryString("http://foo.com?this-is-a-test",
            "?this-is-a-test");
        assertFalse("callback invoked", postMessageCallbackInvoked);
      }

    });
  }

  public void testParseQueryStringGood() {
    postMessageMethod = new PostMessageCallback() {
      public void postMessage(JavaScriptObject message) {
        DataBag messageBag = message.cast();
        assertEquals("type", 103, messageBag.getIntProperty("type"));
        JavaScriptObject options = messageBag.getJSObjectProperty("options");
        assertNotNull("options", options);
        DataBag optionsBag = options.cast();
        assertEquals("reload url", "http://foo.com?foo=good",
            optionsBag.getStringProperty("reload"));
      }
    };
    doInjectedTest(new Method() {
      public void execute() {
        nativeParseQueryString("http://foo.com?foo=good&SpeedTracer=monitor",
            "?foo=good&SpeedTracer=monitor");
        assertTrue("callback invoked", postMessageCallbackInvoked);
      }
    });
  }
  
  public void testParseQueryStringPersistent() {
    postMessageMethod = new PostMessageCallback() {
      public void postMessage(JavaScriptObject message) {
        DataBag messageBag = message.cast();
        assertEquals("type", 103, messageBag.getIntProperty("type"));
        JavaScriptObject options = messageBag.getJSObjectProperty("options");
        assertNotNull("options", options);
        DataBag optionsBag = options.cast();
        assertEquals("reload url", "http://foo.com?foo=good&SpeedTracer=xhr(http://hiddenmonitor.com),header(header:monitor),timeout(10)&speed=ludicrous",
            optionsBag.getStringProperty("reload"));
      }
    };
    doInjectedTest(new Method() {
      public void execute() {
        nativeParseQueryString("http://foo.com?foo=good&SpeedTracer=xhr(http://hiddenmonitor.com),header(header:monitor),monitor,blah,timeout(10)&speed=ludicrous",
            "?foo=good&SpeedTracer=xhr(http://hiddenmonitor.com),header(header:monitor),monitor,blah,timeout(10)");
        assertTrue("callback invoked", postMessageCallbackInvoked);
      }
    });
  }
  
  public void testParseQueryStringAfter() {
    postMessageMethod = new PostMessageCallback() {
      public void postMessage(JavaScriptObject message) {
        DataBag messageBag = message.cast();
        assertEquals("type", 103, messageBag.getIntProperty("type"));
        JavaScriptObject options = messageBag.getJSObjectProperty("options");
        assertNotNull("options", options);
        DataBag optionsBag = options.cast();
        assertEquals("reload url", "http://foo.com?foo=SpeedTracer,asdf&speed=ludicrous",
            optionsBag.getStringProperty("reload"));
      }
    };
    doInjectedTest(new Method() {
      public void execute() {
        nativeParseQueryString("http://foo.com?foo=SpeedTracer,asdf&SpeedTracer=monitor,blah&speed=ludicrous",
            "?foo=SpeedTracer,asdf&SpeedTracer=monitor,blah&speed=ludicrous");
        assertTrue("callback invoked", postMessageCallbackInvoked);
      }
    });
  }
  
  public void testRemoveQuerySubString() {
    doInjectedTest(new Method() {
      public void execute() {
        String result;
        result = nativeRemoveQuerySubString("?foo=1&SpeedTracer=monitor&bar=2",
            "SpeedTracer=monitor");
        assertEquals("?foo=1&bar=2", result);
        result = nativeRemoveQuerySubString("?SpeedTracer=monitor&bar=2",
            "SpeedTracer=monitor");
        assertEquals("?bar=2", result);
        result = nativeRemoveQuerySubString("?foo=1&SpeedTracer=monitor",
            "SpeedTracer=monitor");
        assertEquals("?foo=1", result);
      }
    });
  }

  private native void nativeLog(String msg) /*-{
    $wnd.log(msg);
  }-*/;

  private native void nativeParseQueryString(String originalUrl,
      String queryString) /*-{
    $wnd.parseQueryString(originalUrl, queryString);
  }-*/;

  private native String nativeRemoveQuerySubString(String queryString,
      String querySubString) /*-{
    return $wnd.removeQuerySubString(queryString, querySubString);
  }-*/;

}
