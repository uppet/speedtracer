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

import com.google.speedtracer.client.model.DataModel.DataInstance;

/**
 * The root class in the Monitor's data model. This provides communication with the
 * underlying plumbing and ultimately provides access to the data streaming to
 * and from the browsers.
 */
public interface Model {

  /**
   * A callback interface to receive events from a {@link Model}.
   */
  public interface Listener {

    /**
     * Called when a browser connects to the Monitor infrastructure.
     * 
     * @param id an id used to reference this browser
     * @param name a short name to identify the browser
     * @param version the version of the browser
     */
    void onBrowserConnected(int id, String name, String version);

    /**
     * Called when a browser disconnects from Monitor infrastructure.
     * 
     * @param id the id of the browser
     */
    void onBrowserDisconnected(int id);

    /**
     * Called when some characteristic of the a monitored tab changes.
     * 
     * @param browserId the id of the browser group
     * @param tab updated info about the tab
     */
    void onMonitoredTabChanged(int browserId, TabDescription tab);

    /**
     * Called when a tab begins the monitoring process.
     * 
     * @param browserId the id of the browser group
     * @param tab info about the tab
     * @param dataInstance an opaque handle that can used to create a
     *          {@link DataModel}
     */
    void onTabMonitorStarted(int browserId, TabDescription tab,
        DataInstance dataInstance);

    /**
     * Called when a tab ends the monitoring process.
     * 
     * @param browserId the id of the browser group
     * @param tabID the id of the tab within the browser
     */
    void onTabMonitorStopped(int browserId, int tabID);
  }

  /**
   * Initializes the Monitor infrastructure and provides a delegate for all event
   * callbacks.
   * 
   * @param listener a listener for receiving event callbacks
   */
  void load(Listener listener);
}
