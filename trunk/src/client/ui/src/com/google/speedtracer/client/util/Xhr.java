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
import com.google.speedtracer.client.util.dom.WindowExt;

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

  private static class Handler implements ReadyStateChangeHandler {
    private final XhrCallback callback;

    private Handler(XhrCallback callback) {
      this.callback = callback;
    }

    public void onReadyStateChange(XMLHttpRequest xhr) {
      if (xhr.getReadyState() == XMLHttpRequest.DONE) {
        if (xhr.getStatus() == 200) {
          callback.onSuccess(xhr);
          xhr.clearOnReadyStateChange();
          return;
        }
        callback.onFail(xhr);
        xhr.clearOnReadyStateChange();
      }
    }
  }

  public static void get(String url, XhrCallback callback) {
    request(create(), "GET", url, callback);
  }

  public static void get(WindowExt window, String url, XhrCallback callback) {
    request(create(window), "GET", url, callback);
  }

  public static void head(String url, XhrCallback callback) {
    request(create(), "HEAD", url, callback);
  }

  public static void head(WindowExt window, String url, XhrCallback callback) {
    request(create(window), "HEAD", url, callback);
  }

  public static void post(String url, String requestData, String contentType,
      XhrCallback callback) {
    request(create(), "POST", url, requestData, contentType, callback);
  }

  public static void post(WindowExt window, String url, String requestData,
      String contentType, XhrCallback callback) {
    request(create(window), "POST", url, requestData, contentType, callback);
  }

  /**
   * Replacement for XMHttpRequest.create() to allow using this method in a
   * Chrome Extensions background page.
   */
  private static native XMLHttpRequest create() /*-{
    return new XMLHttpRequest();
  }-*/;

  private static native XMLHttpRequest create(WindowExt window) /*-{
    return new window.XMLHttpRequest();
  }-*/;

  private static void request(XMLHttpRequest xhr, String method, String url,
      String requestData, String contentType, XhrCallback callback) {
    try {
      xhr.setOnReadyStateChange(new Handler(callback));
      xhr.open(method, url);
      xhr.setRequestHeader("Content-type", contentType);
      xhr.send(requestData);
    } catch (Exception e) {
      // Just fail.
      callback.onFail(xhr);
      xhr.clearOnReadyStateChange();
    }
  }

  private static void request(XMLHttpRequest xhr, String method, String url,
      final XhrCallback callback) {
    try {
      xhr.setOnReadyStateChange(new Handler(callback));
      xhr.open(method, url);
      xhr.send();
    } catch (Exception e) {
      // Just fail.
      callback.onFail(xhr);
      xhr.clearOnReadyStateChange();
    }
  }
}
