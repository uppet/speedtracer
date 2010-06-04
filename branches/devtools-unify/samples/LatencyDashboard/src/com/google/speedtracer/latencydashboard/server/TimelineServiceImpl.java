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
import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import com.google.speedtracer.latencydashboard.client.TimelineService;
import com.google.speedtracer.latencydashboard.shared.CustomDashboardRecord;
import com.google.speedtracer.latencydashboard.shared.DashboardRecord;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * AppEngine hosted service that sends timeline data to the client.
 */
@SuppressWarnings("serial")
public class TimelineServiceImpl extends RemoteServiceServlet implements
    TimelineService {

  /**
   * Retrieve the latest custom records from the dashboard in the datastore.
   */
  public CustomDashboardRecord[] getCustomDashboardLatestRecords(int n) {
    Iterator<CustomDashboardRecord> origResults = CustomDashboardRecordStore.getLatest(
        DatastoreServiceFactory.getDatastoreService(), n);
    List<CustomDashboardRecord> listResults = new ArrayList<CustomDashboardRecord>();
    while (origResults.hasNext()) {
      CustomDashboardRecord item = origResults.next();
      listResults.add(item);
    }
    // Copy the list into an array to pass over RPC
    CustomDashboardRecord arrayResults[] = null;
    arrayResults = listResults.toArray(new CustomDashboardRecord[listResults.size()]);

    return arrayResults;
  }

  /**
   * Retrieve the latest records from the dashboard in the datastore.
   */
  public DashboardRecord[] getDashboardLatestRecords(int n) {
    Iterator<DashboardRecord> origResults = DashboardRecordStore.getLatest(
        DatastoreServiceFactory.getDatastoreService(), n);
    List<DashboardRecord> listResults = new ArrayList<DashboardRecord>();
    while (origResults.hasNext()) {
      DashboardRecord item = origResults.next();
      listResults.add(item);
    }
    // Copy the list into an array to pass over RPC
    DashboardRecord arrayResults[] = null;
    arrayResults = listResults.toArray(new DashboardRecord[listResults.size()]);

    return arrayResults;
  }
}
