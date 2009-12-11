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
package com.google.speedtracer.client.util.dom;

import com.google.gwt.dom.client.Document;

/**
 * Simple static methods to change the mouse cursor globally.
 * Used mainly in conjunction with MouseCapture.
 */
public class MouseCursor {

  public static void setCrosshair() {
    Document.get().getBody().getStyle().setProperty("cursor", "crosshair");
  };
  
  public static void setDefault() {
    Document.get().getBody().getStyle().setProperty("cursor", "default");
  };
  
  public static void setEResize() {
    Document.get().getBody().getStyle().setProperty("cursor", "e-resize");
  };
  
  public static void setHelp() {
    Document.get().getBody().getStyle().setProperty("cursor", "help");
  };
  
  public static void setMove() {
    Document.get().getBody().getStyle().setProperty("cursor", "move");
  };
  
  public static void setPointer() {
    Document.get().getBody().getStyle().setProperty("cursor", "pointer");
  };
  
  public static void setWResize() {
    Document.get().getBody().getStyle().setProperty("cursor", "w-resize");
  };
}
