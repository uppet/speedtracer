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
package com.google.speedtracer.latencydashboard.shared;

import java.io.Serializable;

/**
 * Object stored in the datastore to represent a single run of the application.
 */
public class DashboardRecord implements Serializable {

  private static final long serialVersionUID = 1936899457413931843L;

  /**
   * Milliseconds elapsed between the "bootstrap" "start" gwt metric and
   * "bootstrap" "end".
   */
  public double bootstrapDuration;

  /**
   * Milliseconds elapsed since the mainResourceRequest time to the gwt metric
   * "bootstrap" "start".
   */
  public double bootstrapStartTime;

  /**
   * Milliseconds elapsed since the mainResourceRequest time to the first DOM
   * Event of the type "DOMContentLoaded".
   */
  public double domContentLoadedTime;

  /**
   * Milliseconds attributed to Evaluate Script event self times.
   */
  public double evalScriptDuration;

  /**
   * Milliseconds attributed to Garbage Collection event self times.
   */
  public double garbageCollectionDuration;

  /**
   * Milliseconds attributed to JavaScript Execution event self times.
   */
  public double javaScriptExecutionDuration;

  /**
   * Milliseconds attributed to layout event self times.
   */
  public double layoutDuration;

  /**
   * Milliseconds elapsed since the mainResourceRequest time to the first DOM
   * Event of the type "pageLoad".
   */
  public double loadEventTime;

  /**
   * Milliseconds elapsed between the "loadExternalRefs" "start" gwt metric and
   * "loadExternalRefs" "end".
   */
  public double loadExternalRefsDuration;

  /**
   * Milliseconds elapsed since the mainResourceRequest time to the gwt metric
   * "loadExternalRefs" "start" event.
   */
  public double loadExternalRefsTime;

  /**
   * Milliseconds since 1970 that the main resource was loaded. This is used as
   * the base time for all other time values.
   */
  public double mainResourceRequestTime;

  /**
   * Milliseconds since 1970 that the first data was returned from the main
   * resource.
   */
  public double mainResourceResponseTime;

  /**
   * Milliseconds elapsed between the "moduleStartup" "moduleEvalStart" gwt
   * metric and "moduleStartup" "moduleEvalEnd".
   */
  public double moduleEvalDuration;

  /**
   * Milliseconds elapsed between the "moduleStartup" "moduleRequested" gwt
   * metric and "moduleStartup" "end".
   */
  public double moduleStartupDuration;

  /**
   * Milliseconds elapsed since the mainResourceRequest time to the gwt metric
   * "moduleStartup" "moduleRequested" event.
   */
  public double moduleStartupTime;

  /**
   * Milliseconds attributed to Paint event self times.
   */
  public double paintDuration;

  /**
   * Milliseconds attributed to Parse Html event self times.
   */
  public double parseHtmlDuration;

  /**
   * Milliseconds attributed to style recalculation event self times.
   */
  public double recalculateStyleDuration;

  /**
   * A descriptive name provided as a header to the data dump.
   */
  private String name;

  /**
   * A revision string provided as a header to the data dump.
   */
  private String revision;

  /**
   * The time the dump was generated.
   */
  private double timeStamp;

  public DashboardRecord() {
  }

  public DashboardRecord(long timeStamp, String name, String revision) {
    this.timeStamp = timeStamp;
    this.name = name;
    this.revision = revision;
  }

  /**
   * A human readable representation of this record.
   */
  public String getFormattedRecord() {
    StringBuilder builder = new StringBuilder();
    builder.append("name: " + name + "\n");
    builder.append("timeStamp: " + timeStamp + "\n");
    builder.append("revision: " + revision + "\n");
    builder.append("bootstrapStartTime = " + bootstrapStartTime + "\n");
    builder.append("bootstrapDuration = " + bootstrapDuration + "\n");
    builder.append("domContentLoadedTime = " + domContentLoadedTime + "\n");
    builder.append("evalScriptDuration" + evalScriptDuration + "\n");
    builder.append("garbageCollectionDuration" + garbageCollectionDuration
        + "\n");
    builder.append("javaScriptExecutionDuration = "
        + javaScriptExecutionDuration + "\n");
    builder.append("layoutDuration = " + layoutDuration + "\n");
    builder.append("loadEventTime = " + loadEventTime + "\n");
    builder.append("loadExternalRefsDuration = " + loadExternalRefsDuration
        + "\n");
    builder.append("loadExternalRefsTime = " + loadExternalRefsTime + "\n");
    builder.append("mainResourceTime = " + mainResourceRequestTime + "\n");
    builder.append("mainResourceResponseTime = " + mainResourceResponseTime
        + "\n");
    builder.append("moduleStartupTime = " + moduleStartupTime + "\n");
    builder.append("moduleEvalDuration = " + moduleEvalDuration + "\n");
    builder.append("moduleStartupDuration = " + moduleStartupDuration + "\n");
    builder.append("moduleStartupTime = " + moduleStartupTime + "\n");
    builder.append("paintDuration = " + paintDuration + "\n");
    builder.append("parseHtmlDuration = " + parseHtmlDuration + "\n");
    builder.append("recalculateStyleDuration = " + recalculateStyleDuration
        + "\n");
    return builder.toString();
  }

  public String getName() {
    return name;
  }

  public String getRevision() {
    return this.revision;
  }

  public double getTimestamp() {
    return this.timeStamp;
  }

  public void setBootstrapDuration(double bootstrapDuration) {
    this.bootstrapDuration = bootstrapDuration;
  }

  public void setBootstrapStartTime(double bootstrapStartTime) {
    this.bootstrapStartTime = bootstrapStartTime;
  }

  public void setDomContentLoadedTime(double domContentLoadedTime) {
    this.domContentLoadedTime = domContentLoadedTime;
  }

  public void setEvalScriptDuration(double evalScriptDuration) {
    this.evalScriptDuration = evalScriptDuration;
  }

  public void setGarbageCollectionDuration(double garbageCollectionDuration) {
    this.garbageCollectionDuration = garbageCollectionDuration;
  }

  public void setJavaScriptExecutionDuration(double javaScriptExecutionDuration) {
    this.javaScriptExecutionDuration = javaScriptExecutionDuration;
  }

  public void setLayoutDuration(double layoutDuration) {
    this.layoutDuration = layoutDuration;
  }

  public void setLoadEventTime(double loadEventTime) {
    this.loadEventTime = loadEventTime;
  }

  public void setLoadExternalRefsDuration(double loadExternalRefsDuration) {
    this.loadExternalRefsDuration = loadExternalRefsDuration;
  }

  public void setLoadExternalRefsTime(double loadExternalRefsTime) {
    this.loadExternalRefsTime = loadExternalRefsTime;
  }

  public void setMainResourceRequestTime(double mainResourceRequestTime) {
    this.mainResourceRequestTime = mainResourceRequestTime;
  }

  public void setMainResourceResponseTime(double mainResourceResponseTime) {
    this.mainResourceResponseTime = mainResourceResponseTime;
  }

  public void setModuleEvalDuration(double moduleEvalDuration) {
    this.moduleEvalDuration = moduleEvalDuration;
  }

  public void setModuleStartupDuration(double moduleStartupDuration) {
    this.moduleStartupDuration = moduleStartupDuration;
  }

  public void setModuleStartupTime(double moduleStartupTime) {
    this.moduleStartupTime = moduleStartupTime;
  }

  public void setPaintDuration(double paintDuration) {
    this.paintDuration = paintDuration;
  }

  public void setParseHtmlDuration(double parseHtmlDuration) {
    this.parseHtmlDuration = parseHtmlDuration;
  }

  public void setRecalculateStyleDuration(double recalculateStyleDuration) {
    this.recalculateStyleDuration = recalculateStyleDuration;
  }
}
