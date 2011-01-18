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

package com.google.speedtracer.breaky.client;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.speedtracer.breaky.client.JsonSchema.JsonSchemaResults;

/**
 * This object wraps the jsonschema-b4-speedtracer.js JSONSchemaValidator global object.
 * This object can validate an object instance against a JsonSchema schema.
 */
public class JsonSchemaValidator {
  public static native void hookResolver(JavaScriptObject resolver) /*-{
    var validator = @com.google.speedtracer.breaky.client.JsonSchemaValidator::get()();
    validator.resolveReference = resolver;
  }-*/;
  
  /**
   * Validate an instance object against this schema.
   * 
   * @param obj
   * @return {@link JsonSchemaResults} indicating valid/invalid + info
   */
  public static native JsonSchemaResults validate(JavaScriptObject obj, JsonSchema schema) /*-{ 
    var validator = @com.google.speedtracer.breaky.client.JsonSchemaValidator::get()();
    return results = validator.validate(obj, schema);
  }-*/;

  /**
   * Normal script injection puts it in $wnd, but in the worker thread there is
   * no $wnd.
   * 
   * TODO(conroy): nuke this and correctly put the script into global scope
   * 
   * @return a handle to the jsonschema-b4 JSONSchema object
   */
  private static native JavaScriptObject get() /*-{
    return (typeof JSONSchemaValidator === 'undefined') ? $wnd.JSONSchemaValidator : JSONSchemaValidator;
  }-*/;
  
  protected JsonSchemaValidator() {
  }
}