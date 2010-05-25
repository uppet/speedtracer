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
import com.google.speedtracer.latencydashboard.shared.CustomDashboardRecord;
import com.google.speedtracer.latencydashboard.shared.DashboardRecord;

import java.io.IOException;
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

  private static final String WAVE_MT_PREFIX = "__Wave_stats_event";

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {

    // Read the SpeedTrace data.
    String rawJson = ServerUtilities.streamToString(req.getInputStream());

    // Tuck away the raw data into an internal format.
    SpeedTraceRecord speedTraceRecord = null;
    try {
      speedTraceRecord = new SpeedTraceRecord(rawJson);
    } catch (JsonException ex) {
      System.err.println("Bad Json: \n" + format100(rawJson));
      throw new RuntimeException(
          "Failure converting Json string to speedTrace record: " + ex, ex);
    }

    System.out.println("Captured timeline record.  " + rawJson.length()
        + " bytes long.");

    // We could store the entire speed trace here. You could pull it up in
    // SpeedTracer later if you want (but its a lot of data).
    // Store.storeEntireTrace(speedTraceRecord);

    SpeedTraceAnalyzer analyzer = null;
    try {
      analyzer = new SpeedTraceAnalyzer(speedTraceRecord.getDataObject());
    } catch (JsonException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
      return;
    }

    // Extract some statistics from the data
    DashboardRecord dashboardRecord = null;
    try {
      dashboardRecord = processForDashboard(analyzer, speedTraceRecord);
    } catch (JsonException ex) {
      System.err.println("Malformed Json passed to processForDashboards: " + ex);
      ex.printStackTrace();
    }

    CustomDashboardRecord customRecord = null;
    try {
      customRecord = processCustomForDashBoard(analyzer, speedTraceRecord);
    } catch (JsonException ex) {
      System.err.println("Malformed Json passed to custom processForDashboards: "
          + ex);
      ex.printStackTrace();
    }

    // Store the statistics in the datastore
    if (dashboardRecord != null) {
      DashboardRecordStore.put(DatastoreServiceFactory.getDatastoreService(),
          dashboardRecord);
    }
    if (customRecord != null && customRecord.isValid()) {
      CustomDashboardRecordStore.put(
          DatastoreServiceFactory.getDatastoreService(), customRecord);
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
   * @param speedTraceRecord
   * @return
   */
  private CustomDashboardRecord processCustomForDashBoard(
      SpeedTraceAnalyzer analyzer, SpeedTraceRecord speedTraceRecord)
      throws JsonException {
    CustomDashboardRecord customRecord = new CustomDashboardRecord(
        speedTraceRecord.getTimestamp(), speedTraceRecord.getName(),
        speedTraceRecord.getRevision());

    MarkTimelineAnalyzer markTimelineAnalyzer = new MarkTimelineAnalyzer(
        analyzer, WAVE_MT_PREFIX);
    markTimelineAnalyzer.registerMeasurementSet("client_load");
    markTimelineAnalyzer.registerMeasurementSet("wave_prefetch_cache_fill");
    markTimelineAnalyzer.registerMeasurementSet("wave_page");
    markTimelineAnalyzer.registerMeasurementSet("digests_search");
    markTimelineAnalyzer.registerMeasurementSet("contact-sort");

    markTimelineAnalyzer.analyze();
    markTimelineAnalyzer.store(customRecord);

    if (customRecord.isValid()) {
      System.out.println("Custom Record: " + customRecord.getFormattedRecord());
    }
    return customRecord;
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
  private DashboardRecord processForDashboard(SpeedTraceAnalyzer analyzer,
      SpeedTraceRecord speedTraceRecord) throws JsonException {
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
    System.out.println("Dashboard Record:\n" + record.getFormattedRecord());

    return record;
  }
}
