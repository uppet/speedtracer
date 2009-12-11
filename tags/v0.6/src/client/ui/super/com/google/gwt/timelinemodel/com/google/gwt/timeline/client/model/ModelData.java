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
package com.google.gwt.timeline.client.model;

import com.google.gwt.core.client.JavaScriptObject;

/**
 * Super source version of ModelData.
 */
public class ModelData {

  @SuppressWarnings("unused")
  private JavaScriptObject jsObject;

  private double maxEncounteredValue;

  public ModelData() {
    jsObject = createJSArray();
    maxEncounteredValue = 0;
  }

  public void add(DataPoint val) {
    if (val.getY() > maxEncounteredValue) {
      maxEncounteredValue = val.getY();
    }
    nativePush(val);
  }
  
  // We do no type checking in production. Assume that the
  // data structure is intact.
  public native DataPoint get(int i) /*-{
    var value = (this.@com.google.gwt.timeline.client.model.ModelData::jsObject)[i];
    return value;
  }-*/;

  public double getMaxEncounteredValue() {
    return maxEncounteredValue;
  }

  public native int size() /*-{
    return (this.@com.google.gwt.timeline.client.model.ModelData::jsObject).length;
  }-*/;

  public native void truncateBy(int indicesToAxe) /*-{
    var size = (this.@com.google.gwt.timeline.client.model.ModelData::jsObject).length;
    (this.@com.google.gwt.timeline.client.model.ModelData::jsObject).length = size - indicesToAxe;
  }-*/;

  private native JavaScriptObject createJSArray() /*-{
    return [];
  }-*/;
  
  private native void nativePush(DataPoint val) /*-{
    (this.@com.google.gwt.timeline.client.model.ModelData::jsObject).push(val);
  }-*/;
}
