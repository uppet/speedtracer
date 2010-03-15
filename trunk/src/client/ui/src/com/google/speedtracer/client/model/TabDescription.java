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

import com.google.gwt.core.client.JavaScriptObject;

/**
 * A overlay type around a tab's description object. This contains all overview
 * information about a monitored tab.
 * 
 * TODO(jaimeyap): This could be a subclass of
 * {@link com.google.gwt.chrome.crx.client.Tabs.Tab} from the Chromium
 * extensions lib. But since we only use a subset of the fields on that object,
 * it would be misleading to give a reference to one of these and have half the
 * fields be undefined.
 */
public class TabDescription extends JavaScriptObject {
  public static final native TabDescription create(int id, String title,
      String url) /*-{
    return {id: id, title: title, url: url};
  }-*/;

  protected TabDescription() {
  }

  /**
   * The id for this tab.
   * 
   * @return a numeric id
   */
  public final native int getId() /*-{
    return this.id;
  }-*/;

  /**
   * The title for this tab.
   * 
   * @return the title
   */
  public final native String getTitle() /*-{
    return this.title;
  }-*/;

  /**
   * The url that is currently being hosted in the tab.
   * 
   * @return page url
   */
  public final native String getUrl() /*-{
    return this.url;
  }-*/;

  /**
   * Setter used for updating the URL when we receive a page transition.
   * 
   * @param url new url for the tab
   */
  public final native void updateUrl(String url) /*-{
    this.url = url;
  }-*/;
}