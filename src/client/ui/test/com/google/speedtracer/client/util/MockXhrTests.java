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
package com.google.speedtracer.client.util;

import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.xhr.client.XMLHttpRequest;
import com.google.speedtracer.client.util.dom.WindowExt;

/**
 * Tests {@link MockXhr}.
 */
public class MockXhrTests extends GWTTestCase {

  private abstract static class TestDelegate extends MockXhr.Delegate {
    boolean didOpen, didSend, didCreate, didSetHeader;

    private final String expectedMethod;
    private final String expectedUrl;
    private final String expectedData;
    private final String expectedContentType;

    TestDelegate(String expectedMethod, String expectedUrl,
        String expectedData, String expectedContentType) {
      this.expectedMethod = expectedMethod;
      this.expectedUrl = expectedUrl;
      this.expectedData = expectedData;
      this.expectedContentType = expectedContentType;
    }

    @Override
    public void onCreate(Xhr xhr) {
      didCreate = true;
    }

    @Override
    public void onOpen(XMLHttpRequest xhr, String method, String url,
        boolean isAsync) {
      didOpen = true;
      assertTrue("onCreate should have been called.", didCreate);
      assertEquals(expectedUrl, url);
      assertEquals(expectedMethod, method);
    }

    @Override
    public void onSend(XMLHttpRequest xhr, String data) {
      didSend = true;
      assertTrue("onCreate should have been called.", didCreate);
      assertTrue("onOpen should have been called.", didOpen);
      assertTrue("onSetRequestHeader should have been called.", didSetHeader);
      assertEquals(expectedData, data);
      issueResponse(xhr, data);
    }

    @Override
    public void onSetRequestHeader(XMLHttpRequest xhr, String name, String value) {
      didSetHeader = true;
      assertEquals("content-type", name.toLowerCase());
      assertEquals(expectedContentType, value);
    }

    protected abstract void issueResponse(XMLHttpRequest xhr, String data);
  }

  @Override
  public String getModuleName() {
    return "com.google.speedtracer.Common";
  }

  /**
   * Tests simulation of failed {@link XMLHttpRequest} through {@link MockXhr}.
   */
  public void testFailure() {
    final String url = "no-place-2";
    final String data = "my-data-2";
    final String contentType = "my-content-type-2";

    final TestDelegate delegate = new TestDelegate("POST", url, data,
        contentType) {
      @Override
      protected void issueResponse(XMLHttpRequest xhr, String data) {
        respond(xhr, XMLHttpRequest.DONE, 500, "", "");
      }
    };

    MockXhr.Restorer restorer = MockXhr.setDelegate(WindowExt.get(), delegate);
    try {
      final boolean[] didFail = new boolean[1];
      Xhr.post(WindowExt.get(), url, data, contentType, new Xhr.XhrCallback() {
        public void onFail(XMLHttpRequest xhr) {
          didFail[0] = true;
        }

        public void onSuccess(XMLHttpRequest xhr) {
          fail("onSuccess should not be called.");
        }
      });
      assertTrue("onCreate not called", delegate.didCreate);
      assertTrue("onOpen not called", delegate.didOpen);
      assertTrue("onSend not called", delegate.didSend);
      assertTrue("onSetHeader not called", delegate.didSetHeader);
      assertTrue("onFail not called", didFail[0]);
    } finally {
      restorer.restore();
    }
  }

  /**
   * Tests simulation of successful {@link XMLHttpRequest} through
   * {@link MockXhr}.
   */
  public void testSuccess() {
    final String url = "no-place";
    final String data = "my-data";
    final String contentType = "my-content-type";

    final TestDelegate delegate = new TestDelegate("POST", url, data,
        contentType) {
      @Override
      protected void issueResponse(XMLHttpRequest xhr, String data) {
        respond(xhr, XMLHttpRequest.DONE, 200, "Yay!", "response-data");
      }
    };

    final MockXhr.Restorer restorer = MockXhr.setDelegate(WindowExt.get(),
        delegate);
    try {
      final boolean[] didSucceed = new boolean[1];
      Xhr.post(WindowExt.get(), url, data, contentType,
          new Xhr.XhrCallback() {
            public void onFail(XMLHttpRequest xhr) {
              fail("onFail should not be called.");
            }

            public void onSuccess(XMLHttpRequest xhr) {
              assertEquals("response-data", xhr.getResponseText());
              didSucceed[0] = true;
            }
          });
      assertTrue("onCreate not called", delegate.didCreate);
      assertTrue("onOpen not called", delegate.didOpen);
      assertTrue("onSend not called", delegate.didSend);
      assertTrue("onSetHeader not called", delegate.didSetHeader);
      assertTrue("onSuccess not called", didSucceed[0]);
    } finally {
      restorer.restore();
    }

    // Setup/Restore one more time to ensure it was properly restored the first
    // time.
    MockXhr.setDelegate(WindowExt.get(), new MockXhr.Delegate() {
      @Override
      public void onSend(XMLHttpRequest xhr, String data) {
      }
    }).restore();
  }
}
