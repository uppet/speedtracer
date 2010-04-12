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

import com.google.gwt.xhr.client.ReadyStateChangeHandler;
import com.google.gwt.xhr.client.XMLHttpRequest;

/**
 * Simple utility class for using {@link XMLHttpRequest}.
 */
public class Xhr {
  /**
   * Interface for getting notified when an XHR successfully completes, or
   * errors out.
   */
  public interface XhrCallback {
    void onFail(XMLHttpRequest xhr);

    void onSuccess(XMLHttpRequest xhr);
  }

  /**
   * Replacement for XMHttpRequest.create() to allow using this method in a
   * Chrome Extensions background page.
   */
  public static native XMLHttpRequest create() /*-{
    return new XMLHttpRequest();
  }-*/;

  private static class Handler implements ReadyStateChangeHandler {
    private final XhrCallback callback;

    private Handler(XhrCallback callback) {
      this.callback = callback;
    }

    public void onReadyStateChange(XMLHttpRequest xhr) {
      if (xhr.getReadyState() == XMLHttpRequest.DONE) {
        if (xhr.getStatus() == 200) {
          callback.onSuccess(xhr);
          return;
        }
        callback.onFail(xhr);
      }
    }
  }

  public static void get(String url, final XhrCallback callback) {
    XMLHttpRequest xhr = Xhr.create();
    xhr.setOnReadyStateChange(new Handler(callback));
    xhr.open("GET", url);
    xhr.send();
  }

  public static void post(String url, String requestData, String contentType,
      final XhrCallback callback) {
    XMLHttpRequest xhr = Xhr.create();
    xhr.setOnReadyStateChange(new Handler(callback));
    xhr.open("POST", url);
    xhr.setRequestHeader("Content-type", contentType);
    xhr.send(requestData);
  }
}
