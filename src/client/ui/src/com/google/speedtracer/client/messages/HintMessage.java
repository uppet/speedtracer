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
package com.google.speedtracer.client.messages;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.coreext.client.JSON;
import com.google.speedtracer.client.model.HintRecord;

/**
 * Message sent back over PostMessage from
 * {@link com.google.speedtracer.hintletengine.client.HintletEngine} to
 * {@link com.google.speedtracer.client.model.HintletEngineHost}.
 */
public class HintMessage extends JavaScriptObject {
  public static final int HINT = 2;
  public static final int LOG = 1;

  public static HintMessage create(String hintMessageStr) {
    return JSON.parse(hintMessageStr).cast();
  }

  protected HintMessage() {
  }

  public final native HintRecord getHint() /*-{
    return this.payload;
  }-*/;

  public final native String getLog() /*-{
    return this.payload;
  }-*/;

  public final native int getType() /*-{
    return this.type;
  }-*/;

  public final boolean isHint() {
    return getType() == HINT;
  }

  public final boolean isLog() {
    return getType() == LOG;
  }
}
