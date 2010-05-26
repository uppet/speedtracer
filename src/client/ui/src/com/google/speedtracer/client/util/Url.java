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

/**
 * Utility class for extracting parts of a URL String.
 */
public class Url {
  public static final String SCHEME_FILE = "file://";
  public static final String SCHEME_HTTP = "http://";
  public static final String SCHEME_HTTPS = "https://";

  /**
   * Utility method for converting a resource URL to one relative to the
   * specified base.
   * 
   * The returned relative URL should not start with a '/'.
   */
  public static String convertToRelativeUrl(String base, String resourceUrl) {
    if (resourceUrl.startsWith(base, 0)) {
      String relativeUrl = resourceUrl.substring(base.length(),
          resourceUrl.length());
      return (relativeUrl.charAt(0) == '/') ? relativeUrl.substring(1)
          : relativeUrl;
    } else {
      return resourceUrl;
    }
  }

  private String applicationUrl;

  // We cache this because this is queried a crap ton during CPU profiling.
  private String lastPathComponent;

  private String originUrl;

  private final String url;

  public Url(String url) {
    this.url = (url == null) ? "" : url;
  }

  /**
   * Gets the url minus the hash and query params.
   * 
   * @return the url minus the hash and query params.
   */
  public String getApplicationUrl() {
    if (applicationUrl == null) {
      applicationUrl = getApplicationUrlImpl();
    }

    return applicationUrl;
  }

  /**
   * Returns the URL last path component.
   * 
   * @return the resource name
   */
  public String getLastPathComponent() {
    if (lastPathComponent == null) {
      int lastSlashIndex = url.lastIndexOf('/');
      lastPathComponent = url.substring(lastSlashIndex + 1, url.length());
    }
    return lastPathComponent;
  }

  /**
   * Returns the protocol, domain and port of the URL. The origin does not
   * typically include a trailing slash.
   * 
   * @return the protocol, domain and port components of the URL.
   */
  public String getOrigin() {
    if (originUrl == null) {
      originUrl = extractOriginImpl(url);
    }

    return originUrl;
  }

  /**
   * Returns the path component of the resource url.
   * 
   * @return the resource path.
   */
  public String getPath() {
    return convertToRelativeUrl(getOrigin() + "/", url);
  }

  /**
   * Returns the URL minus the last path component.
   * 
   * @return the URL base, which is the URL minus the last path component.
   */
  public String getResourceBase() {
    int lastSlashIndex = url.lastIndexOf('/');
    return url.substring(0, lastSlashIndex + 1);
  }

  /**
   * Returns the protocol portion of the URL.
   * 
   * @return
   */
  public String getScheme() {
    int schemeIndex = url.indexOf("://");
    if (schemeIndex < 0) {
      return "";
    } else {
      return url.substring(0, schemeIndex).toLowerCase() + "://";
    }
  }

  public String getUrl() {
    return url;
  }

  private native String extractOriginImpl(String url) /*-{
    var protocolSplit = url.split("://");
    // Early out if the URL does not have an origin.
    if (protocolSplit.length == 1) {
      return "";
    }
    var originEndIndex = protocolSplit[1].indexOf("/");
    // If there is no trailing slash, then we can assume that the end is the end
    // of the origin. 
    originEndIndex = (originEndIndex < 0) ? protocolSplit[1].length : originEndIndex;
    var domainAndPort = protocolSplit[1].substring(0,originEndIndex);
    // We have enough to return the origin.
    return protocolSplit[0] + "://" + domainAndPort;
  }-*/;

  private String getApplicationUrlImpl() {
    String strippedString = this.url;
    int hashIndex = strippedString.indexOf('#');
    if (hashIndex >= 0) {
      strippedString = strippedString.substring(0, hashIndex);
    }
    int queryParamIndex = strippedString.indexOf('?');
    if (queryParamIndex >= 0) {
      strippedString = strippedString.substring(0, queryParamIndex);
    }
    return strippedString;
  }
}
