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
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.TextResource;
import com.google.gwt.xhr.client.XMLHttpRequest;
import com.google.speedtracer.client.model.NetworkResource;
import com.google.speedtracer.client.model.ServerEvent;
import com.google.speedtracer.client.util.MockXhr;
import com.google.speedtracer.client.util.dom.WindowExt;

/**
 * Tests {@link ServerEventController}.
 */
public class ServerEventControllerTests extends GWTTestCase {

  // TODO(knorton): Move into WindowExt.
  private static native WindowExt getCurrentWindow() /*-{
    return window;
  }-*/;

  /**
   * Bundles tests data.
   */
  public interface Resources extends ClientBundle {
    @Source("resources/appstats.json")
    TextResource appStatsJson();
  }

  /**
   * Mocks a failing Xhr request.
   */
  private static class FailedXhr extends MockXhr.Delegate {
    private final int status;

    FailedXhr(int status) {
      this.status = status;
    }

    @Override
    public void onSend(XMLHttpRequest xhr, String data) {
      respond(xhr, XMLHttpRequest.DONE, status, "", "");
    }
  }

  /**
   * Mocks a successful Xhr request.
   */
  private static class SuccessfulXhr extends MockXhr.Delegate {
    @Override
    public void onSend(XMLHttpRequest xhr, String data) {
      final Resources resources = GWT.create(Resources.class);
      respond(xhr, XMLHttpRequest.DONE, 200, "",
          resources.appStatsJson().getText());
    }
  }

  private static native NetworkResource.HeaderMap createResponseHeaders(
      String traceUrl) /*-{
    return { "X-TraceUrl": traceUrl };
  }-*/;

  private static NetworkResource createMockNetworkResource(int identifier,
      String url, String traceUrl) {
    return new NetworkResource(
        0, // startTime
        identifier,
        url,
        false, // isMainResource
        "GET",
        JavaScriptObject.createObject().<NetworkResource.HeaderMap> cast(),
        200, // statusCode
        createResponseHeaders(traceUrl)) {
    };
  }

  @Override
  public String getModuleName() {
    return "com.google.speedtracer.Common";
  }

  /**
   * {@link AuthenticationDelegate} that asserts authentication is not required.
   */
  private static class NoAuthentication implements
      ServerEventController.AuthenticationDelegate {

    public void onAuthenticationRequired(String url) {
      fail("Authentication should not be required.");
    }

    public void onAuthenticationSucceeded(String url) {
      fail("Authentication should not be required.");
    }
  }

  /**
   * Tests
   * {@link ServerEventController#requestTraceFor(NetworkResource, com.google.speedtracer.client.ServerEventController.RequestTraceCallback)}
   * when {@link com.google.speedtracer.client.util.Xhr} returns successfully. .
   */
  public void testSuccessfulRequestTrace() {
    final ServerEventController controller = new ServerEventController(
        new NoAuthentication());

    final MockXhr.Restorer restorer = MockXhr.setDelegate(getCurrentWindow(),
        new SuccessfulXhr());
    try {
      final boolean[] didSucceed = new boolean[1];
      controller.requestTraceFor(createMockNetworkResource(1,
          "http://localhost/", "/foo"),
          new ServerEventController.RequestTraceCallback() {
            public void onFailure() {
            }

            public void onSuccess(ServerEvent event) {
              didSucceed[0] = true;
            }
          });
      assertTrue("Trace request should succeed.", didSucceed[0]);
    } finally {
      restorer.restore();
    }
  }

  /**
   * Tests
   * {@link ServerEventController#requestTraceFor(NetworkResource, com.google.speedtracer.client.ServerEventController.RequestTraceCallback)}
   * when {@link com.google.speedtracer.client.util.Xhr} returns an error code.
   */
  public void testFailedRequestTrace() {
    final ServerEventController controller = new ServerEventController(
        new NoAuthentication());

    final MockXhr.Restorer restorer = MockXhr.setDelegate(getCurrentWindow(),
        new FailedXhr(404));
    try {
      final boolean[] didFail = new boolean[1];
      controller.requestTraceFor(createMockNetworkResource(1,
          "http://localhost/", "/foo"),
          new ServerEventController.RequestTraceCallback() {
            public void onFailure() {
              didFail[0] = true;
            }

            public void onSuccess(ServerEvent event) {
            }
          });
      assertTrue("Trace request should fail.", didFail[0]);
    } finally {
      restorer.restore();
    }
  }

  /**
   * Tests
   * {@link ServerEventController#serverHasValidTrace(NetworkResource, com.google.speedtracer.client.ServerEventController.HasTraceCallback)}
   * when {@link com.google.speedtracer.client.util.Xhr} returns successfully.
   */
  public void testSuccessfulHasTrace() {
    final ServerEventController controller = new ServerEventController(
        new NoAuthentication());
    final MockXhr.Restorer restorer = MockXhr.setDelegate(getCurrentWindow(),
        new SuccessfulXhr());
    try {
      final boolean[] didSucceed = new boolean[1];
      controller.serverHasValidTrace(createMockNetworkResource(1,
          "http://localhost/", "/foo/a"),
          new ServerEventController.HasTraceCallback() {
            public void onResponse(boolean hasTrace) {
              didSucceed[0] = true;
              assertTrue("Should have a trace.", hasTrace);
            }
          });
      assertTrue("Request should succeed.", didSucceed[0]);
    } finally {
      restorer.restore();
    }
  }

  /**
   * Tests
   * {@link ServerEventController#serverHasValidTrace(NetworkResource, com.google.speedtracer.client.ServerEventController.HasTraceCallback)}
   * when {@link com.google.speedtracer.client.util.Xhr} returns an error code.
   */
  public void testFailedHasTrace() {
    final ServerEventController controller = new ServerEventController(
        new NoAuthentication());
    final MockXhr.Restorer restorer = MockXhr.setDelegate(getCurrentWindow(),
        new FailedXhr(404));
    try {
      final boolean[] didSucceed = new boolean[1];
      controller.serverHasValidTrace(createMockNetworkResource(1,
          "http://localhost/", "/foo/b"),
          new ServerEventController.HasTraceCallback() {
            public void onResponse(boolean hasTrace) {
              didSucceed[0] = true;
              assertFalse("Should not have a trace.", hasTrace);
            }
          });
      assertTrue("Request should succeed.", didSucceed[0]);
    } finally {
      restorer.restore();
    }
  }
}
