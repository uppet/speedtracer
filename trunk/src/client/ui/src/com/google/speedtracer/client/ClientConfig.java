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
package com.google.speedtracer.client;

import com.google.gwt.core.client.GWT;

/**
 * A collection of compile-time settings that are determined through deferred
 * binding properties.
 */
public class ClientConfig {
  public static final String VERSION = "0.23";
  
  @SuppressWarnings("unused")
  private static class DebugMode extends ReleaseMode {
    @Override
    public boolean isDebug() {
      return true;
    }
  }

  private static class LiveDataMode {
    public boolean isMockMode() {
      return false;
    }
  }

  @SuppressWarnings("unused")
  private static class MockDataMode extends LiveDataMode {
    @Override
    public boolean isMockMode() {
      return true;
    }
  }

  private static class ReleaseMode {
    public boolean isDebug() {
      return false;
    }
  }

  /**
   * Indicates whether the module is running in debug mode.
   * 
   * @return
   */
  public static boolean isDebugMode() {
    return GWT.<ReleaseMode> create(ReleaseMode.class).isDebug();
  }

  /**
   * Indicates whether the module is running with mock data.
   * 
   * @return
   */
  public static boolean isMockMode() {
    return GWT.<LiveDataMode> create(LiveDataMode.class).isMockMode();
  }
}
