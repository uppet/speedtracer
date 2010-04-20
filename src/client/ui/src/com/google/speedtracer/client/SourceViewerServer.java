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

package com.google.speedtracer.client;

import com.google.gwt.xhr.client.XMLHttpRequest;
import com.google.speedtracer.client.util.Xhr;
import com.google.speedtracer.client.util.Xhr.XhrCallback;

/**
 * Handles Jump To IDE functionality.
 */
public class SourceViewerServer {
  /**
   * Callback interface for determining if a SourceViewerServer is available.
   */
  public interface ActionCompletedCallback {
    void onFail();

    void onSuccess();
  }

  private static final int API_VERSION = 1;

  public static void jumpToIde(String sourceViewerServer,
      String absoluteFilePath, int lineNumber,
      final ActionCompletedCallback actionCallback) {
    if (null == sourceViewerServer || null == absoluteFilePath) {
      actionCallback.onFail();
      return;
    }

    String url = sourceViewerServer + "?apiVersion=" + API_VERSION
        + "&filePath=" + absoluteFilePath + "&lineNumber=" + lineNumber;
    Xhr.get(url, new XhrCallback() {
      public void onFail(XMLHttpRequest xhr) {
        actionCallback.onSuccess();
      }
      public void onSuccess(XMLHttpRequest xhr) {
        actionCallback.onFail();
      }
    });
  }

  private SourceViewerServer() {
  }
}
