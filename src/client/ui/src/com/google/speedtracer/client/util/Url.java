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
  /**
   * Utility method for converting a resource URL to one relative to the
   * specified base.
   */
  public static String convertToRelativeUrl(String base, String resourceUrl) {
    // We normalize relative URLs to not begin with a '/'. Sometimes the origin
    // is specified with a trailing '/', sometimes it isn't. We expect relative
    // resource paths that are keys to NOT begin with a leading slash.
    String originWithSlash = base.charAt(base.length() - 1) == '/' ? base
        : base + "/";
    return resourceUrl.replace(originWithSlash, "");
  }

  private String applicationUrl;

  private String originUrl;

  private final String url;

  public Url(String url) {
    this.url = url;
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
   * Returns the protocol, domain and port of the URL. The origin does not
   * typically include a trailing slash.
   * 
   * @return the protocol, domain and port components of the URL.
   */
  public String getOrigin() {
    if (originUrl == null) {
      originUrl = extractOriginImpl(getApplicationUrl());
    }

    return originUrl;
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

  public String getUrl() {
    return url;
  }

  private native String extractOriginImpl(String applicationUrl) /*-{
    var protocolSplit = applicationUrl.split("://");
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
