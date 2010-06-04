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
package com.google.speedtracer.client.util;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.xhr.client.XMLHttpRequest;
import com.google.speedtracer.client.util.dom.WindowExt;

/**
 * Allows mocking native XMLHttpRequest functionality for tests.
 */
public class MockXhr {

  /**
   * A delegate that allows for mocking and observing various parts of
   * {@link XMLHttpRequest}s.
   * 
   * @see MockXhr#setDelegate(WindowExt, Delegate)
   */
  public abstract static class Delegate {
    /**
     * Invoked when {@link XMLHttpRequest#create()} is called.
     */
    public void onCreate(Xhr xhr) {
    }

    /**
     * Invoked when any of {@link XMLHttpRequest#open(String, String)},
     * {@link XMLHttpRequest#open(String, String, String)} or
     * {@link XMLHttpRequest#open(String, String, String, String)} is called.
     */
    public void onOpen(XMLHttpRequest xhr, String method, String url,
        boolean isAsync) {
    }

    /**
     * Invoked when {@link XMLHttpRequest#send()} or
     * {@link XMLHttpRequest#send(String)} is called. Implementors are
     * responsible for creating responses.
     * 
     * @see #respond(XMLHttpRequest, int, String)
     * @see #respond(XMLHttpRequest, int, int, String, String)
     */
    public abstract void onSend(XMLHttpRequest xhr, String data);

    /**
     * Invoked when {@link XMLHttpRequest#setRequestHeader(String, String)} is
     * called.
     */
    public void onSetRequestHeader(XMLHttpRequest xhr, String name, String value) {
    }

    /**
     * Simulates a readyState change from an {@link XMLHttpRequest} receiving
     * data.
     */
    public void respond(XMLHttpRequest xhr, int readyState, int status,
        String statusText, String responseText) {
      final FakeXhr fakeXhr = xhr.cast();
      fakeXhr.setReadyState(readyState);
      fakeXhr.setStatus(status);
      fakeXhr.setStatusText(statusText);
      fakeXhr.setResponseText(responseText);
      fakeXhr.fireReadyStateChange();
    }

    /**
     * Simulates a readyState change from an {@link XMLHttpRequest} receiving
     * data.
     */
    public void respond(XMLHttpRequest xhr, int readyState, String responseText) {
      final FakeXhr fakeXhr = xhr.cast();
      fakeXhr.setReadyState(readyState);
      fakeXhr.setResponseText(responseText);
      fakeXhr.fireReadyStateChange();
    }
  }

  /**
   * Used to restore the original {@link XMLHttpRequest} functionality.
   */
  public static final class Restorer extends JavaScriptObject {
    protected Restorer() {
    }

    public native void restore() /*-{
      this();
    }-*/;
  }

  private static final class FakeXhr extends JavaScriptObject {
    @SuppressWarnings("all")
    protected FakeXhr() {
    }

    native void fireReadyStateChange() /*-{
      this.onreadystatechange();
    }-*/;

    native void setReadyState(int readyState) /*-{
      this.readyState = readyState;
    }-*/;

    native void setResponseText(String responseText) /*-{
      this.responseText = responseText;
    }-*/;

    native void setStatus(int status) /*-{
      this.status = status;
    }-*/;

    native void setStatusText(String statusText) /*-{
      this.statusText = statusText;
    }-*/;
  }

  /**
   * Stubs out the native {@link XMLHttpRequest} constructor for testing. NOTE:
   * You MUST restore the original constructor with {@link Restorer#restore()}.
   * 
   * @param window
   * @param delegate
   * @return
   */
  public static Restorer setDelegate(WindowExt window, Delegate delegate) {
    assert delegate != null;
    return setDelegateImpl(window, delegate);
  }

  // Called from JSNI.
  @SuppressWarnings("unused")
  private static void assertConstructorIsNotFake(boolean isFake) {
    assert !isFake : "Someone failed to restore the original XMLHttpRequest constructor with Restorer.restore().";
  }

  private static native Restorer setDelegateImpl(WindowExt window,
      Delegate delegate) /*-{
    // Keep a reference to the original constructor.
    var xhrConstructor = window.XMLHttpRequest;

    @com.google.speedtracer.client.util.MockXhr::assertConstructorIsNotFake(Z)(!!xhrConstructor.isFake);

    // Setup our own constructor.
    window.XMLHttpRequest = function() {
      delegate.@com.google.speedtracer.client.util.MockXhr.Delegate::onCreate(Lcom/google/speedtracer/client/util/Xhr;)(this);

      this.open = function(method, url, isAsync) {
        delegate.@com.google.speedtracer.client.util.MockXhr.Delegate::onOpen(Lcom/google/gwt/xhr/client/XMLHttpRequest;Ljava/lang/String;Ljava/lang/String;Z)(this, method, url, isAsync);
      };

      this.send = function(data) {
        delegate.@com.google.speedtracer.client.util.MockXhr.Delegate::onSend(Lcom/google/gwt/xhr/client/XMLHttpRequest;Ljava/lang/String;)(this, data);
      };

      this.setRequestHeader = function(name, value) {
        delegate.@com.google.speedtracer.client.util.MockXhr.Delegate::onSetRequestHeader(Lcom/google/gwt/xhr/client/XMLHttpRequest;Ljava/lang/String;Ljava/lang/String;)(this, name, value);
      };
    };

    // Used to assert that subsequent invocations do no encounter an unrestored constructor.
    window.XMLHttpRequest.isFake = true;

    // Return a means to restore the original constructor.
    return function() {
      window.XMLHttpRequest = xhrConstructor;
    }
  }-*/;
}
