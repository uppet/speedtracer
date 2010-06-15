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
import com.google.speedtracer.client.model.JsSymbolMap;
import com.google.speedtracer.client.util.Command;
import com.google.speedtracer.client.util.Url;
import com.google.speedtracer.client.util.Command.Method;

/**
 * Tests {@link SymbolServerController}.
 */
public class SymbolServerControllerTests extends GWTTestCase {
  private class TestableSymbolServerController extends SymbolServerController {
    TestableSymbolServerController(Url mainResourceUrl, Url symbolManifestUrl) {
      super(mainResourceUrl, symbolManifestUrl);
    }
  }

  private static final int TEST_FINISH_DELAY = 10000;

  // Key used for looking up our test symbol manifest.
  private String relativeUrl = "1587583491351748F4F66117168CA45B.cache.html";

  // Located in the public directory for this test.
  private Url testManifestUrl = new Url("testsymbolmanifest.json");
  
  private Url testInvalidManifestUrl = new Url("invalidmanifest.json");

  @Override
  public String getModuleName() {
    return "com.google.speedtracer.ModelTests";
  }

  /**
   * Tests that when fetched via the {@link SymbolServerService}, the
   * SymbolServerController unregisters itself on failure.
   */
  public void testSymbolServerFailAndUnregister() {
    final Url testUrl = new Url(Window.Location.getHref());
    Url doesNotExistManifestUrl = new Url("IDontExist.json");
    SymbolServerService.registerSymbolServerController(testUrl,
        doesNotExistManifestUrl);
    SymbolServerController controller = SymbolServerService.getSymbolServerController(testUrl);

    // Should be true synchronously.
    assertTrue(controller != null);

    // Wait a bit. It should unregister on failure.
    Command.defer(new Method() {
      public void execute() {
        // Should now be null.
        assertTrue(null == SymbolServerService.getSymbolServerController(testUrl));
        finishTest();
      }
    }, TEST_FINISH_DELAY / 2);

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
        fail("Fetch was expected to fail");
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
        fail("Symbol fetch failed :(");
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
   * Tests that the SymbolServerController correctly deals with an invalid
   * symbol manifest.
   */
  public void testSymbolServerParseFailed() {
    Url testUrl = new Url(Window.Location.getHref());
    SymbolServerController ssController = new TestableSymbolServerController(
        testUrl, testInvalidManifestUrl);
    String url = testUrl.getResourceBase()
        + "1587583491351748F4F66117168CA45B.cache.html";
    ssController.requestSymbolsFor(url, new Callback() {
      public void onSymbolsFetchFailed(int errorReason) {
        assertEquals(errorReason,
            SymbolServerController.ERROR_MANIFEST_NOT_LOADED);
        finishTest();
      }

      public void onSymbolsReady(JsSymbolMap symbols) {
        fail("Got symbols for an invalid symbol manifest");
      }
    });
  }

  /**
   * Tests that a SymbolServerController can correctly fetch a symbol manifest
   * for a URL specified relatively.
   */
  public void testSymbolServerRelativeUrlFetch() {
    Url testUrl = new Url(Window.Location.getHref());
    SymbolServerController ssController = new TestableSymbolServerController(
        testUrl, testManifestUrl);

    ssController.requestSymbolsFor(relativeUrl, new Callback() {

      public void onSymbolsFetchFailed(int errorReason) {
        fail("Symbol fetch failed :(");
      }

      public void onSymbolsReady(JsSymbolMap symbols) {
        assertTrue(symbols != null);
        assertTrue(symbols.getSymbolCount() > 0);
        finishTest();
      }
    });

    this.delayTestFinish(TEST_FINISH_DELAY);
  }
}
