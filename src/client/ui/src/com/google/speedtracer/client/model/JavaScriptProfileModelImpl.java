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

/**
 * Common interfaces to parse profiler data from a JavaScript engine
 * implementation.
 */
public abstract class JavaScriptProfileModelImpl {
  private final String format;

  public JavaScriptProfileModelImpl(String format) {
    this.format = format;
  }

  /**
   * Returns a string identifying the format handled by this parser.
   */
  public String getFormat() {
    return format;
  }

  /**
   * Load raw profile data from the data stream, and produce a JavaScriptProfile
   * if possible.
   * 
   * @param event A profile record from the instrumented browser.
   * @param profile Profiling data extracted from the profile record.
   */
  public abstract void parseRawEvent(JavaScriptProfileEvent event,
      JavaScriptProfile profile);

  /**
   * Dumps the internal state of the parser for debugging. The output might
   * include the symbol/address map, for example.
   */
  public abstract String getDebugDumpHtml();

}
