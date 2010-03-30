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

/**
 * Tests {@link Url}.
 */
public class UrlTests extends GWTTestCase {

  private static final Url urlWithHash = new Url(
      "https://wave.google.com/a/somedomain.com/#hashmark:foo:blah.baz!w%25stuff");

  private static final Url urlWithPort = new Url(
      "https://wave.google.com:8080/a/somedomain.com/?somequery=1#hashmark:foo:blah.baz!w%25stuff");

  private static final Url urlWithQuery = new Url(
      "https://wave.google.com/a/somedomain.com/?somequery=1");

  private static final Url urlWithQueryAndHash = new Url(
      "https://wave.google.com/a/somedomain.com/?somequery=1#hashmark:foo:blah.baz!w%25stuff");

  @Override
  public String getModuleName() {
    return "com.google.speedtracer.Common";
  }

  /**
   * Tests converting a URL to one that is relative to some base url.
   */
  public void testConvertToRelativeUrl() {
    String base = "https://wave.google.com/a";
    assertEquals("somedomain.com/#hashmark:foo:blah.baz!w%25stuff",
        Url.convertToRelativeUrl(base, urlWithHash.getUrl()));
    assertEquals("somedomain.com/?somequery=1", Url.convertToRelativeUrl(base,
        urlWithQuery.getUrl()));
    assertEquals("somedomain.com/?somequery=1#hashmark:foo:blah.baz!w%25stuff",
        Url.convertToRelativeUrl(base, urlWithQueryAndHash.getUrl()));

    base = "http://totallyDifferentBase";
    assertEquals(urlWithPort.getUrl(), Url.convertToRelativeUrl(base,
        urlWithPort.getUrl()));

    // Now repeat with the base urls having a trailing slash.
    base = "https://wave.google.com/a/";
    assertEquals("somedomain.com/#hashmark:foo:blah.baz!w%25stuff",
        Url.convertToRelativeUrl(base, urlWithHash.getUrl()));
    assertEquals("somedomain.com/?somequery=1", Url.convertToRelativeUrl(base,
        urlWithQuery.getUrl()));
    assertEquals("somedomain.com/?somequery=1#hashmark:foo:blah.baz!w%25stuff",
        Url.convertToRelativeUrl(base, urlWithQueryAndHash.getUrl()));

    base = "http://totallyDifferentBase/";
    assertEquals(urlWithPort.getUrl(), Url.convertToRelativeUrl(base,
        urlWithPort.getUrl()));
  }

  /**
   * Tests retrieving a url as an "application url" that has the hash and query
   * params removed.
   */
  public void testGetApplicationUrl() {
    String applicationUrl = "https://wave.google.com/a/somedomain.com/";
    assertEquals(applicationUrl, urlWithHash.getApplicationUrl());
    assertEquals(applicationUrl, urlWithQuery.getApplicationUrl());
    assertEquals(applicationUrl, urlWithQueryAndHash.getApplicationUrl());

    applicationUrl = "https://wave.google.com:8080/a/somedomain.com/";
    assertEquals(applicationUrl, urlWithPort.getApplicationUrl());
  }

  /**
   * Tests extracting the origin from a URL. The origin is the portion of the
   * URL that contains the protocol, domain, and port number.
   */
  public void testGetOrigin() {
    String origin = "https://wave.google.com";
    assertEquals(origin, urlWithHash.getOrigin());
    assertEquals(origin, urlWithQuery.getOrigin());
    assertEquals(origin, urlWithQueryAndHash.getOrigin());

    origin = "https://wave.google.com:8080";
    assertEquals(origin, urlWithPort.getOrigin());
  }

  /**
   * Tests getting the base of the URL, which is the URL minus the last path
   * component.
   */
  public void testGetResourceBase() {
    String urlBase = "https://wave.google.com/a/somedomain.com/";
    assertEquals(urlBase, urlWithHash.getResourceBase());
    assertEquals(urlBase, urlWithQuery.getResourceBase());
    assertEquals(urlBase, urlWithQueryAndHash.getResourceBase());
    
    urlBase = "https://wave.google.com:8080/a/somedomain.com/";
    assertEquals(urlBase, urlWithPort.getResourceBase());
  }
}
