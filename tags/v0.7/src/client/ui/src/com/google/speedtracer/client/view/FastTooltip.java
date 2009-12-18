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
package com.google.speedtracer.client.view;

import com.google.gwt.dom.client.Element;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.CssResource.Strict;
import com.google.gwt.topspin.ui.client.Container;
import com.google.gwt.topspin.ui.client.Div;
import com.google.speedtracer.client.util.dom.DocumentExt;

/**
 * This acts like a title tooltip, but it comes up faster and and is styled.
 */
public class FastTooltip extends Div {
  /**
   * CSS.
   */
  public interface Css extends CssResource {
    String fastTooltip();

    String fastTooltipInner();
  }

  /**
   * Externalized interface.
   */
  public interface Resources extends ClientBundle {
    @Source("resources/FastTooltip.css")
    @Strict
    FastTooltip.Css fastTooltipCss();
  }

  private Element inner;

  public FastTooltip(Container container, String htmlTip,
      FastTooltip.Resources resources) {
    super(container);
    // TODO(zundel): I tried in vain to set the size in the stylesheet and
    // failed. It doesn't really matter, this outer div is invisible
    setWidth(20);
    setHeight(20);
    getElement().setClassName(resources.fastTooltipCss().fastTooltip());
    inner = DocumentExt.get().createDivElement();
    inner.setClassName(resources.fastTooltipCss().fastTooltipInner());
    // TODO(zundel): truncate text if longer than a certain amount? The div
    // sizes dynamically, I don't think its necessary.
    inner.setInnerText(htmlTip);
    this.getElement().appendChild(inner);
  }
}
