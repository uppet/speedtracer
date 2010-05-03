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
package com.google.speedtracer.client;

/**
 * Simple constants class. For non-Timeline related magic numbers.
 */
public class MonitorConstants {
  // Used for giving a 5% pad on the right so we can manipulate things
  // on the boundaries.
  public static final double EXTRA_DOMAIN_PADDING = 0.05;
  public static final double MIN_GRAPH_DATA_RESOLUTION = 35;
}
