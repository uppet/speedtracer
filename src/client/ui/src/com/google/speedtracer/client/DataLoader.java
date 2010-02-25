/*
 * Copyright 2009 Google Inc.
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
 * Stub class with annotation so we can get the content script referenced in our
 * manifest.
 */
@ManifestInfo(path = "data_loader.js", whiteList = {
    "http://*/*", "https://*/*", "file:///*"}, runAt = ContentScript.DOCUMENT_END)
public class DataLoader extends ContentScript {
  public static final String DATA_LOAD = "DATA_LOAD";
  
  public static final String RAW_DATA_LOAD = "RAW_DATA_LOAD";
}
