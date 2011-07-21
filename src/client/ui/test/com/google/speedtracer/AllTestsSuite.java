/*
 * + * Copyright 2008 Google Inc.
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
package com.google.speedtracer;

import com.google.gwt.junit.tools.GWTTestSuite;
import com.google.speedtracer.client.JsSymbolMapTests;
import com.google.speedtracer.client.ServerEventControllerTests;
import com.google.speedtracer.client.SourceViewerServerTests;
import com.google.speedtracer.client.SourceViewerTests;
import com.google.speedtracer.client.SymbolServerControllerTests;
import com.google.speedtracer.client.WindowChannelTests;
import com.google.speedtracer.client.model.JavaScriptProfileModelV8ImplTests;
import com.google.speedtracer.client.model.JavaScriptProfileNodeTests;
import com.google.speedtracer.client.model.UiEventTests;
import com.google.speedtracer.client.model.V8LogDecompressorTests;
import com.google.speedtracer.client.model.V8SymbolTableTests;
import com.google.speedtracer.client.timeline.GraphModelTests;
import com.google.speedtracer.client.timeline.HighlightModelTests;
import com.google.speedtracer.client.timeline.ModelDataTests;
import com.google.speedtracer.client.util.CsvTests;
import com.google.speedtracer.client.util.MockXhrTests;
import com.google.speedtracer.client.util.PostMessageChannelTests;
import com.google.speedtracer.client.util.TimeStampFormatterTests;
import com.google.speedtracer.client.util.UrlTests;
import com.google.speedtracer.client.util.WorkQueueTests;
import com.google.speedtracer.client.visualizations.model.ReportDataCollectorTests;
import com.google.speedtracer.client.visualizations.model.UiThreadUtilizationTests;
import com.google.speedtracer.client.visualizations.view.EventFilterTests;
import com.google.speedtracer.extension.client.DataLoaderTests;
import com.google.speedtracer.headlessextension.client.HeadlessContentScriptTests;

import junit.framework.Test;

/**
 * A suite to execute all of Speed Tracer's UI test cases.
 */
public class AllTestsSuite extends GWTTestSuite {

  public static Test suite() {
    final GWTTestSuite suite = new GWTTestSuite("All Tests");
    suite.addTestSuite(WindowChannelTests.class);
    suite.addTestSuite(UiThreadUtilizationTests.class);
    suite.addTestSuite(GraphModelTests.class);
    suite.addTestSuite(TimeStampFormatterTests.class);
    suite.addTestSuite(HighlightModelTests.class);
    suite.addTestSuite(MockXhrTests.class);
    suite.addTestSuite(ModelDataTests.class);
    suite.addTestSuite(PostMessageChannelTests.class);
    suite.addTestSuite(EventFilterTests.class);
    suite.addTestSuite(SourceViewerTests.class);
    suite.addTestSuite(JavaScriptProfileModelV8ImplTests.class);
    suite.addTestSuite(JavaScriptProfileNodeTests.class);
    suite.addTestSuite(V8LogDecompressorTests.class);
    suite.addTestSuite(V8SymbolTableTests.class);
    suite.addTestSuite(JsSymbolMapTests.class);
    suite.addTestSuite(UrlTests.class);
    suite.addTestSuite(SymbolServerControllerTests.class);
    suite.addTestSuite(WorkQueueTests.class);
    suite.addTestSuite(CsvTests.class);
    suite.addTestSuite(UiEventTests.class);
    suite.addTestSuite(HeadlessContentScriptTests.class);
    suite.addTestSuite(ServerEventControllerTests.class);
    suite.addTestSuite(SourceViewerServerTests.class);
    suite.addTestSuite(ReportDataCollectorTests.class);
    suite.addTestSuite(DataLoaderTests.class);
    return suite;
  }
}
