/*
 * Copyright 2011 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.speedtracer.hintletengine.client;

import com.google.gwt.junit.client.GWTTestCase;
import com.google.speedtracer.client.model.ResourceUpdateEvent;

/**
 * Tests {@link WebInspectorType}.
 */
public class WebInspectorTypeTests extends GWTTestCase {

  @Override
  public String getModuleName() {
    return "com.google.speedtracer.hintletengine.HintletEngineTest";
  }

  private native static ResourceUpdateEvent getTestData1()/*-{
    //no content type
    return {
      'data' : {
        'responseHeaders' : {}
      }
    };
  }-*/;

  private native static ResourceUpdateEvent getTestData2(String mimeType)/*-{
    var contentType = mimeType + "; charset=UTF-8";
    return {
        'data' : {
            'responseHeaders' : {
              'Content-Type' : contentType
            },
            'url' : 'http://foo'
        },
        'type' : @com.google.speedtracer.hintletengine.client.WebInspectorType::OTHER
    };
  }-*/;

  private native static ResourceUpdateEvent getTestData3()/*-{
    return {
        'data' : {
            'responseHeaders' : {
              'Content-Type' : 'image/png'
            },
            'url' : 'http://example.com/favicon.ico'
        },
        'type' : @com.google.speedtracer.hintletengine.client.WebInspectorType::OTHER
    };
  }-*/;

  private native static ResourceUpdateEvent getTestData4()/*-{
    //mime type will not match
    return {
      'data' : {
        'responseHeaders' : {
          'Content-Type' : ';image/png'
        }
      }
    };
  }-*/;

  private void contentTypeTest(String mimeType, WebInspectorType expectedType) {
    assertTrue(WebInspectorType.getResourceType(getTestData2(mimeType)) == expectedType);
  }

  public void testGetResourceType() {
    assertTrue(WebInspectorType.getResourceType(getTestData1()) == WebInspectorType.OTHER);

    contentTypeTest("text/plain", WebInspectorType.DOCUMENT);
    contentTypeTest("text/html", WebInspectorType.DOCUMENT);
    contentTypeTest("text/xml", WebInspectorType.DOCUMENT);
    contentTypeTest("application/xml", WebInspectorType.DOCUMENT);
    contentTypeTest("application/json", WebInspectorType.DOCUMENT);

    contentTypeTest("text/css", WebInspectorType.STYLESHEET);

    contentTypeTest("text/javascript", WebInspectorType.SCRIPT);

    contentTypeTest("image/vnd.microsoft.icon", WebInspectorType.FAVICON);

    contentTypeTest("image/jpeg", WebInspectorType.IMAGE);
    contentTypeTest("image/gif", WebInspectorType.IMAGE);
    contentTypeTest("image/png", WebInspectorType.IMAGE);

    assertTrue(WebInspectorType.getResourceType(getTestData3()) == WebInspectorType.FAVICON);
    assertTrue(WebInspectorType.getResourceType(getTestData4()) == WebInspectorType.OTHER);
  }
}
