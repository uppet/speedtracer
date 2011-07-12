/*
 * Copyright 2011 Google Inc.
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

package com.google.speedtracer.hintletengine.client.rules;

import com.google.speedtracer.client.model.EventRecord;
import com.google.speedtracer.hintletengine.client.HintletOnHintListener;

/**
 * Parent class for all hintlet rules.
 * 
 */
public abstract class HintletRule {

  public abstract void onEventRecord(EventRecord dataRecord);

  public abstract String getHintletName();

  private HintletOnHintListener onHintCallback;

  public void setOnHintCallback(HintletOnHintListener onHint) {
    onHintCallback = onHint;
  }

  protected void addHint(String hintletRule, double timestamp, String description,
      int refRecord, int severity) {
    if (onHintCallback != null) {
      onHintCallback.onHint(hintletRule, timestamp, description, refRecord, severity);
    }
  }

}
