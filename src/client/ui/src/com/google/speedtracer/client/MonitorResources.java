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

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.speedtracer.client.view.Controller;
import com.google.speedtracer.client.view.HoveringPopup;

/**
 * Contains Immutable Resource Bundles for the Monitor.
 */
public class MonitorResources {

  /**
   * Shared CSS styles.
   */
  public interface CommonCss extends CssResource {
    String even();

    String odd();
  }

  /**
   * Shared CSS and (eventually) images.
   */
  public interface CommonResources extends ClientBundle {
    @Source("resources/Common.css")
    CommonCss commonCss();
  }

  /**
   * CSS data defined in the monitor resource bundle.
   */
  public interface Css extends CssResource {
    String buildInfoView();
  }

  /**
   * Aggregated CSS and Image Resources.
   */
  public interface Resources extends HoveringPopup.Resources,
      Controller.Resources, MonitorVisualizationsPanel.Resources {
    @Source("resources/Monitor.css")
    Css monitorCss();
  }

  /**
   * ImmutableResourceBundles for Monitor Module.
   */
  private static Resources resources;

  public static Resources getResources() {
    return resources;
  }

  /**
   * Initializes resources. This must be called before
   * {@link MonitorResources#getResources()}.
   * 
   * <p>
   * This allows us to avoid an evil static initializer that would pollute all
   * the getResources call sites.
   * </p>
   */
  public static void init() {
    resources = GWT.create(Resources.class);
  }
}
