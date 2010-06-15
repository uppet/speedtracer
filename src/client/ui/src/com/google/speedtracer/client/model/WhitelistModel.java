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
package com.google.speedtracer.client.model;

import com.google.gwt.core.client.JsArray;
import com.google.speedtracer.client.util.dom.LocalStorage;
import com.google.speedtracer.client.util.dom.WindowExt;

/**
 * Stores and retrieves data from the HTML5 database.
 */
public class WhitelistModel {
  private static WhitelistModel singleton;
  private static final String WHITELIST_KEY = "ST_XHR_WHITELIST";

  /**
   * Returns the singleton instance of the WhitelistModel.
   */
  public static WhitelistModel get() {
    if (singleton == null) {
      singleton = new WhitelistModel();
    }
    return singleton;
  }

  private final LocalStorage localStorage;

  private WhitelistModel() {
    localStorage = WindowExt.getLexicalWindow().getLocalStorage();
  }

  /**
   * Retrieves all currently stored whitelist entries from local storage.
   */
  public JsArray<WhitelistEntry> getWhitelist() {
    return localStorage.getItem(WHITELIST_KEY).cast();
  }

  public void saveEntries(JsArray<WhitelistEntry> entries) {
    localStorage.setItem(WHITELIST_KEY, entries.cast());
  }
}
