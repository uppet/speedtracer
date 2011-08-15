/*
 * Copyright 2011 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.speedtracer.hintletengine.client;

import com.google.gwt.junit.client.GWTTestCase;
import com.google.speedtracer.client.model.NetworkResource;
import com.google.speedtracer.client.model.NetworkResponseReceivedEvent;

/**
 * Tests {@link WebInspectorType}.
 */
public class WebInspectorTypeTests extends GWTTestCase {

  @Override
  public String getModuleName() {
    return "com.google.speedtracer.hintletengine.HintletEngineTest";
  }

  /**
   * Creates a NetworkResponseReceivedEvent with no Content-Type in headers
   */
  private static NetworkResponseReceivedEvent getTestData(){
    return new NetworkResponseReceivedEventBuilder().getEvent();
  }

  /**
   * Creates a NetworkResponseReceivedEvent with Content-Type:mimeType in headers
   */
  private static NetworkResponseReceivedEvent getTestData(String mimeType){
    NetworkResponseReceivedEventBuilder builder = new NetworkResponseReceivedEventBuilder();
    builder.setResponseHeaderContentType(mimeType + "; charset=UTF-8");
    return builder.getEvent();
  }

  /**
   * Create a NetworkResource using the default start event and the given response event.
   */
  private NetworkResource getResource(NetworkResponseReceivedEvent responseEvent) {
    return getResource(responseEvent, "www.example.com");
  }

  /**
   * Create a NetworkResource using the default start event, the given URL, and the given response
   * event.
   */
  private NetworkResource getResource(NetworkResponseReceivedEvent responseEvent, String url) {
    NetworkResource resource = new NetworkResource(HintletEventRecordBuilder.createResourceSendRequest(url));
    resource.update(responseEvent);
    return resource;
  }

  /**
   * Runs a test by created a NetworkResource with the given mimeType and confirming that the
   * expected WebInspectorType is returned
   */
  private void contentTypeTest(String mimeType, WebInspectorType expectedType) {
    NetworkResource resource = getResource(getTestData(mimeType));
    assertTrue(mimeType + " should have type " + expectedType,
        WebInspectorType.getResourceType(resource) == expectedType);
  }
  
  public void testDocumentType() {
    contentTypeTest("text/plain", WebInspectorType.DOCUMENT);
    contentTypeTest("text/html", WebInspectorType.DOCUMENT);
    contentTypeTest("text/xml", WebInspectorType.DOCUMENT);
    contentTypeTest("application/xml", WebInspectorType.DOCUMENT);
    contentTypeTest("application/json", WebInspectorType.DOCUMENT);
  }
  
  public void testStylesheetType() {
    contentTypeTest("text/css", WebInspectorType.STYLESHEET);
  }
  
  public void testScriptType() {
    contentTypeTest("text/javascript", WebInspectorType.SCRIPT);
  }
  
  public void testFaviconType() {
    contentTypeTest("image/vnd.microsoft.icon", WebInspectorType.FAVICON);
    assertTrue(
        "url 'http://example.com/favicon.ico' with mimeType image/png should have type FAVICON",
        WebInspectorType.getResourceType(
            getResource(getTestData("image/png"), "http://example.com/favicon.ico"))
            == WebInspectorType.FAVICON);
  }
  
  public void testImageType() {
    contentTypeTest("image/jpeg", WebInspectorType.IMAGE);
    contentTypeTest("image/gif", WebInspectorType.IMAGE);
    contentTypeTest("image/png", WebInspectorType.IMAGE);
  }
  
  public void testOtherType() {
    contentTypeTest(";image/png", WebInspectorType.OTHER);
    contentTypeTest("blah", WebInspectorType.OTHER);
    contentTypeTest("", WebInspectorType.OTHER);
    assertTrue("no Content-Type should have OTHER type",
        WebInspectorType.getResourceType(getResource(getTestData())) == WebInspectorType.OTHER);
  }

}
