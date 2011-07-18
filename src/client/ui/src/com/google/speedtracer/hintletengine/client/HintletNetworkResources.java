/*
 * Copyright 2011 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.speedtracer.hintletengine.client;

import com.google.speedtracer.client.model.EventRecord;
import com.google.speedtracer.client.model.NetworkEventDispatcher;
import com.google.speedtracer.client.model.NetworkResource;

/**
 * Keep track of network resources
 */
public final class HintletNetworkResources {

  // Reuse NetworkEventDispatcher. No listeners attached.
  // Since hintlet runs in web worker, a separate instance is needed  
  private final NetworkEventDispatcher networkEventDispatcher;
  private static HintletNetworkResources resources;

  private HintletNetworkResources() {
    networkEventDispatcher = new NetworkEventDispatcher();
  }

  public static HintletNetworkResources getInstance() {
    if (resources == null) {
      resources = new HintletNetworkResources();
    }
    return resources;
  }

  public void onEventRecord(EventRecord data) {
    networkEventDispatcher.onEventRecord(data);
  }
  
  /**
   * Getter for the accumulated information about a resource.
   */
  public NetworkResource getResourceData(final int identifier) {
    return networkEventDispatcher.getResource(identifier);
  }

}
