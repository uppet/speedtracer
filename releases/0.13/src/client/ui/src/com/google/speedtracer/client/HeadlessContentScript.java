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

import com.google.gwt.chrome.crx.client.ContentScript;
import com.google.gwt.chrome.crx.client.ContentScript.ManifestInfo;

/**
 * Content script used for the headless extension.
 * 
 * The headless content script ("headless_content_script.js") injects a
 * JavaScript API ("headless_api.js") into the page. The API allows code in the
 * page to turn SpeedTracer monitoring on and off, and to retrieve a copy of the
 * profiling data.
 * 
 * The API communicates with the content script through an event fired on a pair
 * of DIVs. The content script then communicates with the
 * {@link HeadlessBackgroundPage} using a Chrome extension {@link com.google.gwt.chrome.crx.client.Port}.
 */
@ManifestInfo(path = "headless_content_script.js", whiteList = {
    "http://*/*", "https://*/*", "file:///*"}, runAt = ContentScript.DOCUMENT_START)
public class HeadlessContentScript extends ContentScript {
  // just a placeholder.
}
