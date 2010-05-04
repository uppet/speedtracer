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

import com.google.gwt.core.client.JsArray;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.speedtracer.client.SourceViewerServer.OpaqueParam;

/**
 * Tests {@link SourceViewerServer}.
 */
public class SourceViewerServerTests extends GWTTestCase {

  static final String param1 = "Some Opaque Param";
  static final String param2 = "Some other opaque param";
  static final String url = "http://SomeFakeUrl:8080/servlet";

  private static native SourceViewerServer getMockSourceViewerServer() /*-{
    return {
    url: @com.google.speedtracer.client.SourceViewerServerTests::url,
    _paramParam1: @com.google.speedtracer.client.SourceViewerServerTests::param1, 
    _paramParam2: @com.google.speedtracer.client.SourceViewerServerTests::param2,
    };
  }-*/;

  @Override
  public String getModuleName() {
    return "com.google.speedtracer.Common";
  }

  /**
   * Tests getting the opaque params that we forward on each Jump-To-IDE
   * request.
   */
  public void testGetOpaqueUrlParams() {
    SourceViewerServer svServer = getMockSourceViewerServer();
    JsArray<OpaqueParam> opaqueParams = svServer.getOpaqueUrlParams();
    assertEquals(url, svServer.getUrl());
    assertEquals("Param1", opaqueParams.get(0).getKey());
    assertEquals(param1, opaqueParams.get(0).getValue());
    assertEquals("Param2", opaqueParams.get(1).getKey());
    assertEquals(param2, opaqueParams.get(1).getValue());
  }

  /**
   * Tests issuing a request to Jump-To-IDE based on a given SourceViewerServer.
   */
  public void testJumpToIde() {
    // TODO(jaimeyap): Fill in test case once we have ability to mock XHRs.
  }
}
