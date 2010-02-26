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
package com.google.speedtracer.client.model;

import com.google.gwt.chrome.crx.client.Port;
import com.google.gwt.core.client.JavaScriptObject;

/**
 * This class is used in Chrome when we get data from a loaded file (data_loader
 * content script). We use this overlay type as the object we pass to the
 * Monitor UI.
 */
public class LoadFileDataInstance extends DataInstance {
  private static class Proxy implements DataProxy {
    private final Port port;

    Proxy(Port port) {
      this.port = port;
    }

    public void load(DataInstance dataInstance) {
      port.postMessage(createAck());
    }

    public void resumeMonitoring() {
    }

    public void setBaseTime(double baseTime) {
    }

    public void setProfilingOptions(boolean enableStackTraces,
        boolean enableCpuProfiling) {
    }

    public void stopMonitoring() {
    }

    public void unload() {
    }
  }

  /**
   * Static Factory method for obtaining an instance of
   * {@link LoadFileDataInstance}.
   * 
   * @param port the {@link Port} that we will use connect to.
   * @return
   */
  public static LoadFileDataInstance create(Port port) {
    return DataInstance.create(new Proxy(port)).cast();
  }

  public static native JavaScriptObject createAck() /*-{
    return {
      ready: true
    };
  }-*/;

  protected LoadFileDataInstance() {
  }
}
