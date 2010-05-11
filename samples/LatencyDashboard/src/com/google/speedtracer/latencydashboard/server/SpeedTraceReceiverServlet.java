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
package com.google.speedtracer.latencydashboard.server;

import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.json.serialization.JsonException;
import com.google.speedtracer.latencydashboard.shared.DashboardRecord;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.text.DecimalFormat;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Receives incoming SpeedTrace XHR requests from a HeadlessSpeedTracer session
 * sent by the Headless API method speedtracer.sendDump(). The data is encoded
 * in JSON with two top level properties.
 * 
 * header - object with some metadata about the trace. data - an array of the
 * speedtracer records.
 * 
 * The SpeedTrace data is parsed using the {@link SpeedTraceAnalyzer} and
 * {@link GwtAnalyzer} classes and stored in a {@link DashboardRecord} which is
 * then stored in the AppEngine datastore.
 */
public class SpeedTraceReceiverServlet extends HttpServlet {

  /**
   * 
   */
  private static final long serialVersionUID = 1549873162336369719L;

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {

    // Read the SpeedTrace data.
    InputStreamReader reader = new InputStreamReader(req.getInputStream());
    StringBuilder builder = new StringBuilder();
    ByteBuffer buffer = ByteBuffer.allocate(16 * 1024);
    while (true) {
      buffer.clear();
      int result = reader.read(buffer.asCharBuffer());
      if (result >= 0) {
        builder.append(buffer.asCharBuffer());
      } else if (result < 0) {
        break;
      }
    }

    // Tuck away the raw data into an internal format.
    SpeedTraceRecord speedTraceRecord = null;
    try {
      speedTraceRecord = new SpeedTraceRecord(builder.toString());
    } catch (JsonException ex) {
      System.err.println("Bad Json: \n" + format100(builder.toString()));
      throw new RuntimeException(
          "Failure converting Json string to speedTrace record: " + ex, ex);
    }

    System.out.println("Captured timeline record.  " + builder.length()
        + " bytes long.");

    // We could store the entire speed trace here. You could pull it up in
    // SpeedTracer later if you want (but its a lot of data).
    // Store.storeEntireTrace(speedTraceRecord);

    // Extract some statistics from the data
    DashboardRecord dashboardRecord = null;
    try {
      dashboardRecord = processForDashboard(speedTraceRecord);
    } catch (JsonException ex) {
      System.err.println("Malformed Json passed to processForDashboards: " + ex);
      ex.printStackTrace();
    }

    // Store the statistics in the datastore
    if (dashboardRecord != null) {
      DashboardRecordStore.put(DatastoreServiceFactory.getDatastoreService(),
          dashboardRecord);
    }
  }

  /**
   * When a JSON error is encountered, the problem can be difficult to see. This
   * reformats the buffer so a character position in the dump can be easily
   * found.
   * 
   * @param input A line of long text.
   * @return A human readable buffer that puts 100 chars per line and a leading
   *         column with character position.
   */
  private String format100(String input) {
    StringBuilder builder = new StringBuilder();
    int index = 0;
    while (index < input.length()) {
      DecimalFormat position = new DecimalFormat("######");
      int endIndex = Math.min(input.length(), index + 100);
      builder.append(position.format(index) + ": "
          + input.substring(index, endIndex) + "\n");
      index += 100;
    }
    return builder.toString();
  }

  /**
   * Given a JSON dump of speedtracer data, process the data with the analyzers
   * to produce a record to be stored in the App Engine datastore.
   * 
   * @param speedTraceRecord Incoming SpeedTrace data from the Headless
   *          Extension.
   * @return a populated record with statistics about the trace.
   * @throws JsonException occurs when a property is missing or an unexpected
   *           type encountered in the JSON dump.
   */
  private DashboardRecord processForDashboard(SpeedTraceRecord speedTraceRecord)
      throws JsonException {
    SpeedTraceAnalyzer analyzer = new SpeedTraceAnalyzer(
        speedTraceRecord.getDataObject());

    DashboardRecord record = new DashboardRecord(
        speedTraceRecord.getTimestamp(), speedTraceRecord.getName(),
        speedTraceRecord.getRevision());

    // Populate the record with general statistics from the dump
    analyzer.analyze();
    double baseTime = analyzer.getMainResourceRequestTime();
    record.setMainResourceRequestTime(baseTime);
    record.setMainResourceResponseTime(analyzer.getMainResourceResponseTime()
        - baseTime);

    record.setDomContentLoadedTime(analyzer.getDomContentLoadedTime()
        - baseTime);
    record.setLoadEventTime(analyzer.getLoadEventTime() - baseTime);
    record.setEvalScriptDuration(analyzer.getEvalScriptDuration());
    record.setGarbageCollectionDuration(analyzer.getGarbageCollectionDuration());
    record.setJavaScriptExecutionDuration(analyzer.getJavaScriptExecutionDuration());
    record.setLayoutDuration(analyzer.getLayoutDuration());
    record.setRecalculateStyleDuration(analyzer.getLayoutDuration());
    record.setPaintDuration(analyzer.getPaintDuration());
    record.setParseHtmlDuration(analyzer.getParseHtmlDuration());

    // Look for GWT specific statistics.
    GwtAnalyzer gwtAnalyzer = new GwtAnalyzer(analyzer);
    gwtAnalyzer.analyze();

    if (gwtAnalyzer.getBootstrapStartTime() > 0) {
      record.setBootstrapStartTime(gwtAnalyzer.getBootstrapStartTime()
          - baseTime);
      if (gwtAnalyzer.getBootstrapEndTime() > 0) {
        record.setBootstrapDuration(gwtAnalyzer.getBootstrapEndTime()
            - gwtAnalyzer.getBootstrapStartTime());
      }
    }

    if (gwtAnalyzer.getModuleStartupTime() > 0) {
      record.setModuleStartupTime(gwtAnalyzer.getModuleStartupTime() - baseTime);
      if (gwtAnalyzer.getModuleEvalEndTime() > 0) {
        record.setModuleEvalDuration(gwtAnalyzer.getModuleEvalEndTime()
            - gwtAnalyzer.getModuleEvalStartTime());
      }
      if (gwtAnalyzer.getModuleStartupEndTime() > 0) {
        record.setModuleStartupDuration(gwtAnalyzer.getModuleStartupEndTime()
            - gwtAnalyzer.getModuleStartupTime());
      }
    }

    if (gwtAnalyzer.getLoadExternalRefsStartTime() > 0) {
      record.setLoadExternalRefsTime(gwtAnalyzer.getLoadExternalRefsStartTime()
          - baseTime);
      if (gwtAnalyzer.getLoadExternalRefsEndTime() > 0) {
        record.setLoadExternalRefsDuration(gwtAnalyzer.getLoadExternalRefsEndTime()
            - gwtAnalyzer.getLoadExternalRefsStartTime());
      }
    }

    // This bit of debugging is useful just to see that data is getting across.
    System.out.println("Stats for Dashboard1: " + record.getFormattedRecord());

    return record;
  }
}
