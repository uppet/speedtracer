/*
 * Copyright 2008 Google Inc.
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

import com.google.gwt.webworker.client.ErrorEvent;

/**
 * Overlay for Hintlet Exception payload. See HintletEngine::ReportException() a
 * list of the fields populated in this object (populated from the v8::TryCatch
 * object after an exception occurs.)
 */
public class HintletException extends ErrorEvent {

  protected HintletException() {
  }

  public final String asString() {
    return getException();
  }

  /**
   * Returns a string representation of the exception.
   * 
   * @return A string representation of the exception;
   */
  public final String getException() {
    return "Error in file: " + getFilename() + " at line: " + getLineNumber()
        + " message: " + getMessage();
  }
}