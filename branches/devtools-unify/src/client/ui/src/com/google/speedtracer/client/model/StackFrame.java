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

import com.google.gwt.core.client.JavaScriptObject;

/**
 * Overlay type for a stack frame in the stack trace attached to a
 * {@link UiEvent}.
 */
public class StackFrame extends JavaScriptObject {
  public static native StackFrame create(String scriptName,
      String functionName, int line, int column) /*-{
    return {
      scriptName: scriptName,
      functionName: functionName,
      lineNumber: line,
      column: column
    };
  }-*/;

  protected StackFrame() {
  }

  public final native int getColumnOffset()/*-{
    return this.column || 0;
  }-*/;

  public final native String getFunctionName()/*-{
    return this.functionName;
  }-*/;

  public final native int getLineNumber()/*-{
    return this.lineNumber || 0;
  }-*/;

  public final native String getScriptName()/*-{
    return this.scriptName;
  }-*/;
}
