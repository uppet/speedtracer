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

import com.google.gwt.regexp.shared.MatchResult;
import com.google.gwt.regexp.shared.RegExp;
import com.google.speedtracer.client.model.NetworkResource;
import com.google.speedtracer.client.model.ResourceUpdateEvent;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * These are a subset of the old mozilla types with different numeric values.
 */
public enum WebInspectorType {

  DOCUMENT,
  STYLESHEET,
  IMAGE,
  FONT,
  SCRIPT,
  XMLHTTPREQUEST,
  MEDIA, // moz RESOURCE_TYPE_OBJECT
  OTHER,
  FAVICON; // Custom Hintlet Types

  private static RegExp mimeTypeRegexp = null; // Cached regexps
  private static Set<String> docMimeTyeSet = null;

  /**
   * 
   * @param networkResource 
   * @return the type of resource, given the url and header map, as one of the RESOURCE_TYPE_XXX
   *         constants.
   */
  public static WebInspectorType getResourceType(NetworkResource networkResource) {

    if (mimeTypeRegexp == null) {
      mimeTypeRegexp = RegExp.compile("^[^/;]+/[^/;]+");
    }

    // Looks in the specified dataRecords at the mime type embedded as the
    // prefix of the Content-Type header and returns the appropriate resource
    // type.
    String contentTypeHeader =
        HintletHeaderUtils.hasHeader(networkResource.getResponseHeaders(), "Content-Type");
    if (contentTypeHeader == null) {
      return OTHER;
    }

    MatchResult match = mimeTypeRegexp.exec(contentTypeHeader);
    if (match == null || match.getGroupCount() < 1) {
      return OTHER;
    }

    String mimeType = match.getGroup(0).toLowerCase();

    if (docMimeTyeSet == null) {
      String[] docTypes =
          {"text/plain", "text/html", "text/xml", "application/xml", "application/json"};
      docMimeTyeSet = new HashSet<String>(Arrays.asList(docTypes));
    }
    if (docMimeTyeSet.contains(mimeType)) {
      return DOCUMENT;
    }

    if (mimeType.equals("text/css")) {
      return STYLESHEET;
    }

    if (mimeType.equals("text/javascript")) {
      return SCRIPT;
    }

    // TODO(zundel): this test is less than complete.
    String url = networkResource.getUrl();
    if (mimeType.equals("image/vnd.microsoft.icon")
        || (url != null && url.toLowerCase().endsWith("/favicon.ico"))) {
      return FAVICON;
    }

    if (mimeType.startsWith("image/")) {
      return IMAGE;
    }
    return OTHER;
  }
}
