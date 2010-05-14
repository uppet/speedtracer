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

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.SortDirection;
import com.google.speedtracer.latencydashboard.shared.DashboardRecord;

import java.util.Iterator;
import java.util.Map;

/**
 * Handles persistence for {@link DashboardRecord}.
 * 
 */
public class DashboardRecordStore {
  private static class DashboardRecordIterator implements
      Iterator<DashboardRecord> {
    private final Iterator<Entity> iter;

    private DashboardRecordIterator(Iterator<Entity> iter) {
      this.iter = iter;
    }

    public boolean hasNext() {
      return iter.hasNext();
    }

    public DashboardRecord next() {
      return DashboardRecordStore.get(iter.next());
    }

    public void remove() {
      iter.remove();
    }
  }

  private static final String KEY_PROP_NAME = "name";
  private static final String KEY_PROP_REVISION = "revision";
  private static final String KEY_PROP_TIMESTAMP = "timeStamp";

  private static final String KIND = "DashboardRecord";

  private static final String PROP_BOOTSTRAP_DURATION = "bootstrap_duration";
  private static final String PROP_BOOTSTRAP_START_TIME = "bootstrap_start_time";
  private static final String PROP_DOM_CONTENT_LOADED_TIME = "dom_content_loaded_time";
  private static final String PROP_EVAL_SCRIPT_DURATION = "eval_script_duration";
  private static final String PROP_GARBAGE_COLLECTION_DURATION = "garbage_collection_duration";
  private static final String PROP_JAVASCRIPT_EXECUTION_DURATION = "javascript_execution_duration";
  private static final String PROP_LAYOUT_DURATION = "layout_duration";
  private static final String PROP_LOAD_EVENT_TIME = "load_event_time";
  private static final String PROP_LOAD_EXTERNAL_REFS_DURATION = "load_external_refs_duration";
  private static final String PROP_LOAD_EXTERNAL_REFS_TIME = "load_external_refs_time";
  private static final String PROP_MAIN_RESOURCE_REQUEST_TIME = "main_resource_request_time";
  private static final String PROP_MAIN_RESOURCE_RESPONSE_TIME = "main_resource_response_time";
  private static final String PROP_MODULE_EVAL_DURATION = "module_eval_duration";
  private static final String PROP_RECALCULATE_STYLE_DURATION = "recalculate_style_duration";
  private static final String PROP_MODULE_STARTUP_DURATION = "module_startup_duration";
  private static final String PROP_MODULE_STARTUP_TIME = "module_startup_time";
  private static final String PROP_PAINT_DURATION = "paint_duration";
  private static final String PROP_PARSE_HTML_DURATION = "parse_html_duration";

  public static DashboardRecord get(Entity entity) {
    Map<String, Object> properties = entity.getProperties();

    Double timestamp = (Double) properties.get(KEY_PROP_TIMESTAMP);
    DashboardRecord result = new DashboardRecord(
        timestamp.longValue(),
        (String) properties.get(KEY_PROP_NAME),
        (String) properties.get(KEY_PROP_REVISION));

    result.setBootstrapDuration(getDoubleProperty(properties,
        PROP_BOOTSTRAP_DURATION));
    result.setBootstrapStartTime(getDoubleProperty(properties,
        PROP_BOOTSTRAP_START_TIME));
    result.setDomContentLoadedTime(getDoubleProperty(properties,
        PROP_DOM_CONTENT_LOADED_TIME));
    result.setEvalScriptDuration(getDoubleProperty(properties,
        PROP_EVAL_SCRIPT_DURATION));
    result.setGarbageCollectionDuration(getDoubleProperty(properties,
        PROP_GARBAGE_COLLECTION_DURATION));
    result.setJavaScriptExecutionDuration(getDoubleProperty(properties,
        PROP_JAVASCRIPT_EXECUTION_DURATION));
    result.setLayoutDuration(getDoubleProperty(properties, PROP_LAYOUT_DURATION));
    result.setLoadEventTime(getDoubleProperty(properties, PROP_LOAD_EVENT_TIME));
    result.setLoadExternalRefsDuration(getDoubleProperty(properties,
        PROP_LOAD_EXTERNAL_REFS_DURATION));
    result.setLoadExternalRefsTime(getDoubleProperty(properties,
        PROP_LOAD_EXTERNAL_REFS_TIME));
    result.setMainResourceRequestTime(getDoubleProperty(properties,
        PROP_MAIN_RESOURCE_REQUEST_TIME));
    result.setMainResourceResponseTime(getDoubleProperty(properties,
        PROP_MAIN_RESOURCE_RESPONSE_TIME));
    result.setModuleEvalDuration(getDoubleProperty(properties,
        PROP_MODULE_EVAL_DURATION));
    result.setModuleStartupTime(getDoubleProperty(properties,
        PROP_MODULE_STARTUP_TIME));
    result.setModuleStartupDuration(getDoubleProperty(properties,
        PROP_MODULE_STARTUP_DURATION));
    result.setPaintDuration(getDoubleProperty(properties, PROP_PAINT_DURATION));
    result.setParseHtmlDuration(getDoubleProperty(properties,
        PROP_PARSE_HTML_DURATION));
    result.setRecalculateStyleDuration(getDoubleProperty(properties,
        PROP_RECALCULATE_STYLE_DURATION));

    return result;
  }

  public static Iterator<DashboardRecord> getLatest(DatastoreService store,
      int n) {
    return new DashboardRecordIterator(
        store.prepare(
            new Query(KIND).addSort(KEY_PROP_TIMESTAMP,
                SortDirection.DESCENDING)).asIterator(
            FetchOptions.Builder.withLimit(n)));
  }

  public static void put(DatastoreService store, DashboardRecord dashboardRecord) {
    Entity entity = new Entity(KIND);
    entity.setProperty(KEY_PROP_TIMESTAMP, dashboardRecord.getTimestamp());
    entity.setProperty(KEY_PROP_NAME, dashboardRecord.getName());
    entity.setProperty(KEY_PROP_REVISION, dashboardRecord.getRevision());

    entity.setProperty(PROP_BOOTSTRAP_DURATION,
        dashboardRecord.bootstrapDuration);
    entity.setProperty(PROP_BOOTSTRAP_START_TIME,
        dashboardRecord.bootstrapStartTime);
    entity.setProperty(PROP_DOM_CONTENT_LOADED_TIME,
        dashboardRecord.domContentLoadedTime);
    entity.setProperty(PROP_EVAL_SCRIPT_DURATION,
        dashboardRecord.evalScriptDuration);
    entity.setProperty(PROP_GARBAGE_COLLECTION_DURATION,
        dashboardRecord.garbageCollectionDuration);
    entity.setProperty(PROP_JAVASCRIPT_EXECUTION_DURATION,
        dashboardRecord.javaScriptExecutionDuration);
    entity.setProperty(PROP_LAYOUT_DURATION, dashboardRecord.layoutDuration);
    entity.setProperty(PROP_LOAD_EVENT_TIME, dashboardRecord.loadEventTime);
    entity.setProperty(PROP_LOAD_EXTERNAL_REFS_DURATION,
        dashboardRecord.loadExternalRefsDuration);
    entity.setProperty(PROP_LOAD_EXTERNAL_REFS_TIME,
        dashboardRecord.mainResourceRequestTime);
    entity.setProperty(PROP_MAIN_RESOURCE_REQUEST_TIME,
        dashboardRecord.mainResourceRequestTime);
    entity.setProperty(PROP_MAIN_RESOURCE_RESPONSE_TIME,
        dashboardRecord.mainResourceResponseTime);
    entity.setProperty(PROP_MODULE_STARTUP_TIME,
        dashboardRecord.moduleStartupTime);
    entity.setProperty(PROP_MODULE_EVAL_DURATION,
        dashboardRecord.moduleEvalDuration);
    entity.setProperty(PROP_MODULE_STARTUP_DURATION,
        dashboardRecord.moduleStartupDuration);
    entity.setProperty(PROP_PAINT_DURATION, dashboardRecord.paintDuration);
    entity.setProperty(PROP_PARSE_HTML_DURATION,
        dashboardRecord.parseHtmlDuration);
    entity.setProperty(PROP_RECALCULATE_STYLE_DURATION,
        dashboardRecord.recalculateStyleDuration);
    store.put(entity);
  }

  private static double getDoubleProperty(Map<String, Object> properties,
      String key) {
    Double value = (Double) properties.get(key);
    if (value != null) {
      return value;
    }
    return Double.NaN;
  }
}
