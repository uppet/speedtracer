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
package com.google.speedtracer.client.timeline;

import com.google.gwt.graphics.client.Color;

/**
 * Magic values for UI.
 */
public class Constants {
  public static final int DEFAULT_GRAPH_WINDOW_SIZE = 4000;
  // the width of the graph title
  public static final int GRAPH_HEADER_WIDTH = 185;
  // the width of the graph title + border
  public static final int GRAPH_PIXEL_OFFSET = GRAPH_HEADER_WIDTH + 1;
  public static final Color GRAPH_STROKE_COLOR = new Color("#175094");
  public static final String HELP_URL = "http://code.google.com/webtoolkit/speedtracer/get-started.html";
  public static final Color NETWORK_GRAPH_COLOR = new Color("#6e99b9");
  public static final int PLOT_PRECISION = 200;
  public static final int REFRESH_RATE = 1000;
  public static final int SCROLL_VELOCITY_CAP = 3;
  public static final Color SLUGGISHNESS_GRAPH_COLOR = new Color("#6d5da7");
  public static final int ZOOM_DURATION = 600;
}
