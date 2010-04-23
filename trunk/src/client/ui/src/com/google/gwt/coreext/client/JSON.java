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
package com.google.gwt.coreext.client;

import com.google.gwt.core.client.JavaScriptException;
import com.google.gwt.core.client.JavaScriptObject;

/**
 * Utility for going parsing and stringifying JSON.
 */
public class JSON {
  /**
   * Thrown when JSON.parse fails on some input.
   */
  @SuppressWarnings("serial")
  public static class JSONParseException extends IllegalArgumentException {
    public JSONParseException(String msg) {
      super(msg);
    }
  }

  public static JavaScriptObject parse(String jsonStr)
      throws JSONParseException {
    try {
      return parseImpl(jsonStr);
    } catch (JavaScriptException jse) {
      throw new JSONParseException("JSON string failed to parse");
    }
  }

  public static native String stringify(JavaScriptObject jso) /*-{
    return JSON.stringify(jso);
  }-*/;

  private static native JavaScriptObject parseImpl(String jsonStr) /*-{
    return JSON.parse(jsonStr);
  }-*/;

  private JSON() {
  }
}
