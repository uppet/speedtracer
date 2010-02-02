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

import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.user.client.Window;
import com.google.speedtracer.client.SymbolServerController.Callback;
import com.google.speedtracer.client.util.Url;
import com.google.speedtracer.client.visualizations.model.JsSymbolMap;

/**
 * Tests {@link SymbolServerController}.
 */
public class SymbolServerControllerTests extends GWTTestCase {
  private static final int TEST_FINISH_DELAY = 10000;

  // Located in the public directory for this test.
  private String testManifestUrl = "testsymbolmanifest.json";

  @Override
  public String getModuleName() {
    return "com.google.speedtracer.Common";
  }

  private class TestableSymbolServerController extends SymbolServerController {

    TestableSymbolServerController(Url mainResourceUrl, String symbolManifestUrl) {
      super(mainResourceUrl, symbolManifestUrl);
    }
  }

  /**
   * Tests that a SymbolServerController can correctly fetch a symbol manifest
   * for a URL specified relatively.
   */
  public void testSymbolServerRelativeUrlFetch() {
    Url testUrl = new Url(Window.Location.getHref());
    SymbolServerController ssController = new TestableSymbolServerController(
        testUrl, testManifestUrl);
    String relativeUrl = "1587583491351748F4F66117168CA45B.cache.html";
    ssController.requestSymbolsFor(relativeUrl, new Callback() {

      public void onSymbolsFetchFailed(int errorReason) {
        assertTrue("Symbol fetch failed :(", false);
      }

      public void onSymbolsReady(JsSymbolMap symbols) {
        assertTrue(symbols != null);
        assertTrue(symbols.getSymbolCount() > 0);
        finishTest();
      }
    });

    this.delayTestFinish(TEST_FINISH_DELAY);
  }

  /**
   * Tests that a SymbolServerController can correctly fetch a symbol manifest
   * for a full URL that should resolve to a resource key that was specified
   * relatively.
   */
  public void testSymbolServerFullUrlFetch() {
    Url testUrl = new Url(Window.Location.getHref());
    SymbolServerController ssController = new TestableSymbolServerController(
        testUrl, testManifestUrl);
    String url = testUrl.getResourceBase()
        + "1587583491351748F4F66117168CA45B.cache.html";
    ssController.requestSymbolsFor(url, new Callback() {

      public void onSymbolsFetchFailed(int errorReason) {
        assertTrue("Symbol fetch failed :(", false);
      }

      public void onSymbolsReady(JsSymbolMap symbols) {
        assertTrue(symbols != null);
        assertTrue(symbols.getSymbolCount() > 0);
        finishTest();
      }
    });

    this.delayTestFinish(TEST_FINISH_DELAY);
  }

  /**
   * Tests that a SymbolServerController reports a failure for non-existent
   * resources.
   */
  public void testSymbolServerFailedFetch() {
    Url testUrl = new Url(Window.Location.getHref());
    SymbolServerController ssController = new TestableSymbolServerController(
        testUrl, testManifestUrl);
    String url = "Idontexist";
    ssController.requestSymbolsFor(url, new Callback() {

      public void onSymbolsFetchFailed(int errorReason) {
        finishTest();
      }

      public void onSymbolsReady(JsSymbolMap symbols) {
        assertTrue("Fetch was expected to fail", false);
      }
    });

    this.delayTestFinish(TEST_FINISH_DELAY);
  }
}
