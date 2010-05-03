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
import com.google.appengine.api.datastore.Transaction;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.Query.SortDirection;
import com.google.speedtracer.latencydashboard.shared.CustomDashboardRecord;

import java.util.Iterator;
import java.util.Map;

/**
 * Stores a {@link DashboardCustomRecord}.
 */
public class CustomDashboardRecordStore {

  /**
   * Converts a list of datastore entities into CustomDashbaordRecords.
   */
  private static class CustomDashboardRecordIterator implements
      Iterator<CustomDashboardRecord> {
    private final Iterator<Entity> iter;

    private CustomDashboardRecordIterator(Iterator<Entity> iter) {
      this.iter = iter;
    }

    public boolean hasNext() {
      return iter.hasNext();
    }

    public CustomDashboardRecord next() {
      return CustomDashboardRecordStore.get(iter.next());
    }

    public void remove() {
      iter.remove();
    }
  }

  private final static String KIND = "CustomDashboardRecord";
  private final static String CUSTOM_KEY_PREFIX = "custom_";

  private final static String KEY_PROP_NAME = "name";
  private final static String KEY_PROP_REVISION = "revision";
  private final static String KEY_PROP_TIMESTAMP = "timeStamp";

  /**
   * Create an instance of a CustomDashboardRecord given an entity definition.
   * 
   * @param entity record returned from the persistent store.
   */
  public static CustomDashboardRecord get(Entity entity) {
    Map<String, Object> properties = entity.getProperties();

    CustomDashboardRecord result = new CustomDashboardRecord(
        (Long) properties.get(KEY_PROP_TIMESTAMP),
        (String) properties.get(KEY_PROP_NAME),
        (String) properties.get(KEY_PROP_REVISION));

    for (String key : properties.keySet()) {
      if (key.startsWith(CUSTOM_KEY_PREFIX)) {
        Double data = (Double) properties.get(key);
        String dataLabel = key.substring(CUSTOM_KEY_PREFIX.length());
        result.addCustomMeasure(dataLabel, data);
      }
    }
    return result;
  }

  /**
   * Retreives the n latest records using the timestamp field.
   */
  public static Iterator<CustomDashboardRecord> getLatest(
      DatastoreService store, int n) {
    return new CustomDashboardRecordIterator(
        store.prepare(
            new Query(KIND).addSort(KEY_PROP_TIMESTAMP,
                SortDirection.DESCENDING)).asIterator(
            FetchOptions.Builder.withLimit(n)));
  }

  /**
   * Store a Custom Dashboard record in the persistent store.
   */
  public static void put(DatastoreService store,
      CustomDashboardRecord dashboardRecord) {
    Entity entity = new Entity(KIND);
    entity.setProperty(KEY_PROP_TIMESTAMP, dashboardRecord.getTimestamp());
    entity.setProperty(KEY_PROP_NAME, dashboardRecord.getName());
    entity.setProperty(KEY_PROP_REVISION, dashboardRecord.getRevision());

    Map<String, Double> customMeasures = dashboardRecord.getCustomMetrics();
    for (String key : customMeasures.keySet()) {
      entity.setProperty(CUSTOM_KEY_PREFIX + key, customMeasures.get(key));
    }
    store.put(entity);
  }

  public static String clear(DatastoreService store, String name) {
    Transaction tx = store.beginTransaction();
    store.prepare(new Query(KIND).addFilter("name", FilterOperator.EQUAL, name));
    tx.commit();
    return null;
  }
}
