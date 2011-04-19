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
import com.google.gwt.coreext.client.DataBag;

/**
 * Base class for inspector resource messages.
 */
public class InspectorResourceMessage extends ResourceRecord {
  /**
   *
   */
  public static class Data extends DataBag {
    protected Data() {
    }

    public final double getTimeStamp() {
      return getDoubleProperty("timestamp");
    }
  }

  public static native <T extends InspectorResourceMessage> T create(int type,
      double normalizedTime, JavaScriptObject data) /*-{
    return {
      type: type,
      time: normalizedTime,
      data:data
    };
  }-*/;

  protected InspectorResourceMessage() {
  }
}
