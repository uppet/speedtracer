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
package com.google.speedtracer.hintletengine.client;

import com.google.gwt.webworker.client.DedicatedWorkerEntryPoint;

/**
 * The entrypoint for the HintletEngine that runs in a worker thread.
 */
public class HintletEngine extends DedicatedWorkerEntryPoint {

  @Override
  public void onWorkerLoad() {
    // TODO(zundel): rewrite hintlet_main.js and hintlet_api.js with GWT.
    // For now lets just run hintlet_main.js.
    getGlobalScope().importScript("hintlet_main.js");
  }
}
