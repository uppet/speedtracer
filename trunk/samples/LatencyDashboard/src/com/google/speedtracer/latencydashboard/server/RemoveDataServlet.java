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
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.labs.taskqueue.QueueFactory;
import com.google.appengine.api.labs.taskqueue.TaskOptions;
import com.google.appengine.api.labs.taskqueue.TaskOptions.Method;

import java.io.IOException;
import java.util.ArrayList;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Drop data from the table. To use this, map the following into web.xml:
 * 
 * <servlet>
 * 
 * <servlet-name>removeData</servlet-name>
 * 
 * <servlet-class>com.google.speedtracer.latencydashboard.server.
 * RemoveDataServlet</servlet-class>
 * 
 * </servlet>
 * 
 * <servlet-mapping>
 * 
 * <servlet-name>removeData</servlet-name>
 * 
 * <url-pattern>/latencydashboard/removeData</url-pattern>
 * 
 * </servlet-mapping>
 */
public class RemoveDataServlet extends HttpServlet {
  static final String PASSCODE = "THATSNOTTHEPASSCODE";

  public void doGet(final HttpServletRequest request,
      final HttpServletResponse response) throws IOException {

    response.setContentType("text/plain");

    final String kind = request.getParameter("kind");
    final String passcode = request.getParameter("passcode");

    if (kind == null) {
      throw new NullPointerException();
    }

    if (passcode == null) {
      throw new NullPointerException();
    }

    if (!passcode.equals(PASSCODE)) {
      response.getWriter().println("BAD PASSCODE!");
      return;
    }

    System.err.println("*** deleting entities from " + kind);

    final long start = System.currentTimeMillis();

    int deletedCount = 0;
    boolean isFinished = false;

    final DatastoreService dss = DatastoreServiceFactory.getDatastoreService();

    while (System.currentTimeMillis() - start < 16384) {

      final Query query = new Query(kind);

      query.setKeysOnly();

      final ArrayList<Key> keys = new ArrayList<Key>();

      for (final Entity entity : dss.prepare(query).asIterable(
          FetchOptions.Builder.withLimit(128))) {
        keys.add(entity.getKey());
      }

      keys.trimToSize();

      if (keys.size() == 0) {
        isFinished = true;
        break;
      }

      while (System.currentTimeMillis() - start < 16384) {
        try {
          dss.delete(keys);
          deletedCount += keys.size();
          break;
        } catch (Throwable ignore) {
          continue;
        }
      }
    }

    System.err.println("*** deleted " + deletedCount + " entities from " + kind);

    if (isFinished) {

      System.err.println("*** deletion job for " + kind + " is completed.");

    } else {

      final int taskcount;

      final String tcs = request.getParameter("taskcount");

      if (tcs == null) {
        taskcount = 0;
      } else {
        taskcount = Integer.parseInt(tcs) + 1;
      }

      QueueFactory.getDefaultQueue().add(
          url(
              "/latencydashboard/" + "removeData?kind=" + kind + "&passcode="
                  + PASSCODE + "&taskcount=" + taskcount).method(Method.GET));

      System.err.println("*** deletion task # " + taskcount + " for " + kind
          + " is queued.");
    }

    response.getWriter().println("OK");
  }

  private TaskOptions url(String url) {
    return TaskOptions.Builder.url(url).countdownMillis(2000);
  }
}
