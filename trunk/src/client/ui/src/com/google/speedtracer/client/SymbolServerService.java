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

import com.google.speedtracer.client.util.IterableFastStringMap;
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

  public static void registerSymbolServerController(Url resourceUrl,
      String symbolManifestUrl) {
    SymbolServerController ssController = symbolServerControllers.get(symbolManifestUrl);
    if (ssController == null && symbolManifestUrl != null
        && !symbolManifestUrl.equals("")) {
      ssController = symbolServerControllers.put(
          resourceUrl.getApplicationUrl(), new SymbolServerController(
              resourceUrl.getOrigin(), symbolManifestUrl));
    }
  }

  private SymbolServerService() {
  }
}
