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
package com.google.speedtracer.client.util.dom;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.SpanElement;
import com.google.gwt.dom.client.Style.Display;
import com.google.gwt.resources.client.ImageResource;

/**
 * Utility methods for creating Elements from an {@link ImageResource}.
 */
public class ImageResourceElementCreator {
  public static Element createElementFrom(ImageResource resource) {
    SpanElement img = DocumentExt.get().createSpanElement();
    String style = "url(" + resource.getURL() + ") no-repeat "
        + (-resource.getLeft() + "px ") + (-resource.getTop() + "px");
    img.getStyle().setProperty("background", style);
    img.getStyle().setPropertyPx("width", resource.getWidth());
    img.getStyle().setPropertyPx("height", resource.getHeight());
    img.getStyle().setDisplay(Display.INLINE_BLOCK);
    return img;
  }

  private ImageResourceElementCreator() {
  }
}
