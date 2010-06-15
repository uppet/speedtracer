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
package com.google.speedtracer.headlessextension.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.StyleInjector;
import com.google.gwt.resources.client.CssResource;

/**
 * Maps Css Resources and images into the page.
 */
public class HeadlessOptionsResources {

  /**
   * Css.
   */
  public interface Css extends CssResource {
    String outer();
  }

  /**
   * Aggregated CSS and Image Resources into an inner class for the different UI
   * components, then make sure that this interface extends all of them.
   */
  public interface Resources extends WhitelistTable.Resources,
      TableControls.Resources {
    @Source("resources/OptionsPage.css")
    Css optionsPageCss();
  }

  private static Resources resources;

  public static Resources getResources() {
    assert resources != null;
    return resources;
  }

  /**
   * Initializes resources. This must be called before
   * {@link DashboardResources#getResources()}.
   * 
   * <p>
   * This allows us to avoid an evil static initializer that would pollute all
   * the getResources call sites.
   * </p>
   */
  public static void init() {
    resources = GWT.create(Resources.class);
    StyleInjector.inject(resources.whitelistTableCss().getText()
        + resources.optionsPageCss().getText()
        + resources.tableControlsCss().getText());
  }

  private HeadlessOptionsResources() {
    // Do not instantiate
  }
}
