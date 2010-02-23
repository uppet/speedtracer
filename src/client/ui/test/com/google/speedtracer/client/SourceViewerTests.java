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

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.TableRowElement;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.TextResource;
import com.google.speedtracer.client.SourceViewer.SourceViewerLoadedCallback;

/**
 * Tests {@link SourceViewer}.
 * 
 * This test makes use of "test-source.js" in the public directory of our tests.
 * We are not able to test <code>SourceViewer.create()</code> since that depends
 * on Chrome Extension functionality (via the resource fetcher proxy html).
 */
public class SourceViewerTests extends GWTTestCase {
  interface Resources extends ClientBundle {
    @Source("../pub/test-source.js")
    TextResource testSource();
  }

  private static final int TEST_FINISH_DELAY = 10000;

  @Override
  public String getModuleName() {
    return "com.google.speedtracer.ModelTests";
  }

  /**
   * Tests that we get notified when a resource fetch fails.
   * 
   * WARNING: These tests are asynchronous and depend on callbacks from the
   * underlying iframe and XHR ready state changes.Failed assertions don't seem
   * to log to the JUNIT logger. We detect them simply as timeouts.
   */
  public void testFailedFetch() {
    SourceViewer.Resources resources = GWT.create(SourceViewer.Resources.class);
    SourceViewer.create(Document.get().getBody(), "does-not-existsource.js", resources,
        new SourceViewerLoadedCallback() {
          public void onSourceFetchFail(int statusCode, SourceViewer viewer) {
            assertTrue(statusCode != 200);
            finishTest();            
          }

          public void onSourceViewerLoaded(SourceViewer viewer) {
            assertTrue("Fetch should have failed.", false);            
          }
        });

    this.delayTestFinish(TEST_FINISH_DELAY);
  }
  
  /**
   * Tests that we can retrieve the line contents given a line number.
   * 
   * WARNING: These tests are asynchronous and depend on callbacks from the
   * underlying iframe and XHR ready state changes.Failed assertions don't seem
   * to log to the JUNIT logger. We detect them simply as timeouts.
   */
  public void testGetLineContents() {
    Resources testResources = GWT.create(Resources.class);
    final String[] testSource = testResources.testSource().getText().split(
        "\n\r|\n");
    SourceViewer.Resources resources = GWT.create(SourceViewer.Resources.class);
    SourceViewer.create(Document.get().getBody(), "test-source.js", resources,
        new SourceViewerLoadedCallback() {
          public void onSourceFetchFail(int statusCode, SourceViewer viewer) {
            assertTrue("Fetch failed.", false);
          }

          public void onSourceViewerLoaded(SourceViewer viewer) {
            for (int i = 0; i < testSource.length; i++) {
              // 1 based indexes for line numbers.
              int index = i + 1;
              String lineContents = viewer.getLineContents(index);
              // Line contents are stripped of new lines. So we can compare
              // directly with out testSource array above.
              assertEquals("Line " + index + "'" + lineContents + "' != '"
                  + testSource[i] + "'", testSource[i], lineContents);
            }
            // Clean up test.
            Document.get().getBody().removeChild(viewer.getElement());
            finishTest();
          }
        });

    this.delayTestFinish(TEST_FINISH_DELAY);
  }

  /**
   * Tests that when you highlight a row, that the appropriate CSS class name
   * was set on the row.
   * 
   * Not really a pretty test since I need to reach inside the implementation
   * and match styles.
   */
  public void testHighlightRow() {
    final SourceViewer.Resources resources = GWT.create(SourceViewer.Resources.class);
    SourceViewer.create(Document.get().getBody(), "test-source.js", resources,
        new SourceViewerLoadedCallback() {
          public void onSourceFetchFail(int statusCode, SourceViewer viewer) {
            assertTrue("Fetch failed.", false);
          }

          public void onSourceViewerLoaded(SourceViewer viewer) {
            SourceViewer.CodeCss styles = resources.sourceViewerCodeCss();

            viewer.highlightLine(2);
            checkHasClassName(viewer, 2, styles.highlightedLine(), true);

            viewer.highlightLine(4);
            checkHasClassName(viewer, 2, styles.highlightedLine(), false);
            checkHasClassName(viewer, 4, styles.highlightedLine(), true);

            viewer.highlightLine(6);
            checkHasClassName(viewer, 4, styles.highlightedLine(), false);
            checkHasClassName(viewer, 6, styles.highlightedLine(), true);

            // Clean up test.
            Document.get().getBody().removeChild(viewer.getElement());
            finishTest();
          }
        });

    this.delayTestFinish(TEST_FINISH_DELAY);
  }

  private void checkHasClassName(SourceViewer viewer, int lineNumber,
      String className, boolean shouldHaveClassName) {
    TableRowElement lineRow = viewer.getTableRowElement(lineNumber);
    boolean found = (lineRow.getClassName().indexOf(className) >= 0);

    assertTrue("Highlight failed for line: " + lineNumber,
        (found == shouldHaveClassName));
  }
}
