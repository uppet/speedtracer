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

import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;

/**
 * Utility extension to support common patterns and idioms.
 */
public class DocumentExt extends Document {
  /**
   * Gets an instance of the containing Document ($doc).
   * 
   * @return the Document instance as a DocumentExt.
   */
  public static final DocumentExt get() {
    return Document.get().cast();
  }

  /**
   * Returns the current Document.
   * 
   * @return the current Document.
   */
  public static final native DocumentExt getCurrentDocument() /*-{
    return document;
  }-*/;

  protected DocumentExt() {
  }

  /**
   * Convenience method. Now that we are using CssResource and
   * ImmutableResourceBundle we are calling setClassName all over the place.
   * 
   * @param className the class name css selector
   * @return the contructed DivElement with the classname set
   */
  public final DivElement createDivWithClassName(String className) {
    DivElement elem = createDivElement();
    elem.setClassName(className);
    return elem;
  }

  /**
   * Convenience method. Now that we are using CssResource and
   * ImmutableResourceBundle we are calling setClassName all over the place.
   * 
   * @param tagName the tag name of the Element to be created
   * @param className the class name css selector
   * @return the contructed Element with the classname set
   */
  public final Element createElementWithClassName(String tagName,
      String className) {
    Element elem = createElement(tagName);
    elem.setClassName(className);
    return elem;
  }
}
