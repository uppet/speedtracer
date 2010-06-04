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

import com.google.gwt.core.client.JavaScriptObject;

/**
 * Overlay type for the Symbol Server Manifest. This looks like: -----
 * 
 * { 
 *   "path/relative/to/origin/resource.js": {
 *     symbols: "relative/to/symbolmanifest/base/resource.map",
 *     sourceServer: "http://source:8080/mysource/",
 *     type: "gwt"
 *   },
 *   "http://localhost/myapp/myapp.nocache.js" : {
 *     symbols: "http://host:8080/myapp.map",
 *     sourceServer: "http://source:8080/",
 *     type: "gwt"
 *   },
 *   "http://localhost/js/jquery.compressed.js" : {
 *     symbols: "http://cdn/jquery.map",
 *     sourceServer: "http://code.google.com/",
 *     type: "funky_chicken"
 *   }
 * }
 * 
 * ----- 
 * Resources that are hosted from the same origin as the main resource
 * need only have relative paths used as the resource key. This allows persons
 * to create a single symbol manifest and host the obfuscated output anywhere
 * they want.
 * 
 * Similarly, the associated symbols file can either be specified with a full
 * URL or with a path relative to the URL base of the symbol manifest. That is,
 * if the symbol manifest were located at:
 * "http://foo.com/symbols/myManifest.json" Then relative paths for the symbols
 * mapping (like "bar/baz.map") would be looked up at:
 * "http://foo.com/symbols/bar/baz.map"
 */
public class SymbolServerManifest extends JavaScriptObject {
  /**
   * Information about a Server referenced in this manifest.
   */
  public static class ResourceSymbolInfo extends JavaScriptObject {
    protected ResourceSymbolInfo() {
    }

    public final native String getSourceServer()/*-{
      return this.sourceServer;
    }-*/;

    public final native SourceViewerServer getSourceViewerServer() /*-{
      return this.sourceViewerServer;
    }-*/;

    public final native String getSymbolMapUrl() /*-{
      return this.symbols;
    }-*/;

    public final native String getType() /*-{
      return this.type;
    }-*/;
  }

  protected SymbolServerManifest() {
  }

  public final native ResourceSymbolInfo getResourceSymbolInfo(
      String resourceUrl) /*-{
    return this[resourceUrl];
  }-*/;
}
