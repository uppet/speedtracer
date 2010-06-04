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

import com.google.gwt.coreext.client.IterableFastStringMap;
import com.google.speedtracer.client.util.Url;

/**
 * Class responsible for fetching the symbol manifest JSON and returning
 * {@link SymbolServerController} instances.
 */
public class SymbolServerService {

  private static IterableFastStringMap<SymbolServerController> symbolServerControllers = new IterableFastStringMap<SymbolServerController>();

  public static SymbolServerController getSymbolServerController(Url resourceUrl) {
    return symbolServerControllers.get(resourceUrl.getApplicationUrl());
  }

  /**
   * Creates and registers a {@link SymbolServerController} for a given resource
   * URL. Note that the {@link SymbolServerController} instance is immediately
   * available, even if before the the symbol manifest is fetched and parsed.
   * The {@link SymbolServerController} will buffer any requests and service
   * them once the manifest is loaded.
   * 
   * For cases where the manifest fails to be fetched, the associated
   * {@link SymbolServerController} should be unregistered.
   * 
   * @param resourceUrl The {@link Url} that we want to associate a new
   *          {@link SymbolServerController} with.
   * @param symbolManifestUrl The {@link Url} for the Symbol Manifest that will
   *          be used to initialize the {@link SymbolServerController} manifest.
   */
  public static void registerSymbolServerController(Url resourceUrl,
      Url symbolManifestUrl) {
    if (symbolManifestUrl != null && !symbolManifestUrl.equals("")) {
      symbolServerControllers.put(resourceUrl.getApplicationUrl(),
          new SymbolServerController(resourceUrl, symbolManifestUrl));
    }
  }

  /**
   * Removes any {@link SymbolServerController} associated with the specified
   * resource URL key.
   * 
   * @param resourceUrl
   */
  protected static void unregisterSymbolServerController(Url resourceUrl) {
    symbolServerControllers.remove(resourceUrl.getApplicationUrl());
  }

  private SymbolServerService() {
  }
}
