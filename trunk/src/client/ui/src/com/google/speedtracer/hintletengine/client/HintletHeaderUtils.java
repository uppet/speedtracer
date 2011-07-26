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

import com.google.speedtracer.client.model.NetworkResource.HeaderMap;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Header related methods
 */
public final class HintletHeaderUtils {

  private static Set<String> compressionIndicatorSet = null;

  private HintletHeaderUtils() {
  }

  /**
   * Determines if the given headers contain a header. Performs a case-insensitive comparison.
   * 
   * @param headers An object with a key for each header and a value containing the contents of that
   *          header.
   * @param targetHeader The header to match.
   * @return actual header value if found. {@code null} otherwise.
   */
  public static String hasHeader(HeaderMap headers, String targetHeader) {
    if (headers == null) {
      return null;
    }

    final String targetHeaderLc = targetHeader.toLowerCase();
    final class HeaderMapIterCallBack implements HeaderMap.IterationCallBack {
      public String headerFound = null;

      public void onIteration(String key, String value) {
        if (key.toLowerCase().equals(targetHeaderLc)) {
          headerFound = value;
        }
      }
    }

    HeaderMapIterCallBack callBack = new HeaderMapIterCallBack();
    headers.iterate(callBack);

    return callBack.headerFound;
  }

  /**
   * Determines if the given headers contain a header (case-insensitive matching)that contains the
   * target string (case-insensitive and multiline matching ).
   * 
   * @param headers An object with a key for each header and a value containing the contents of that
   *          header.
   * @param targetHeader The header to match.
   * @param targetString The string to search for in the header.
   * @return {@code true} iff the headers contain the given header and it contains the target
   *         string.
   */
  public static boolean headerContains(HeaderMap headers, String targetHeader, String targetString) {
    String value = hasHeader(headers, targetHeader);
    if (value == null) {
      return false;
    }
    return stringMatchIM(value, targetString);
  }

  /**
   * @param headers
   * @Return {@code true} if the 'Content-Encoding' header indicates this request is compressed.
   */
  public static boolean isCompressed(HeaderMap headers) {
    if (headers == null) {
      return false;
    }

    String prop = hasHeader(headers, "Content-Encoding");

    if (compressionIndicatorSet == null) {
      String[] compressionIndicators = {"compress", "deflate", "gzip", "pack200-gzip",// JavaArchives
        "bzip2", // Not registered with IANA, but supported by some browsers
        "sdch" // Not registered with IANA, but supported by Google Toolbar
      };
      compressionIndicatorSet = new HashSet<String>(Arrays.asList(compressionIndicators));
    }

    return prop == null ? false : compressionIndicatorSet.contains(prop.toLowerCase());
  }

  /**
   * @param headers
   * @return the cookie header if either "Set-Cookie" or "Cookie" are found, {@code null} otherwise
   */
  public static String hasCookie(HeaderMap headers) {
    String cookie = HintletHeaderUtils.hasHeader(headers, "Set-Cookie");
    if (cookie != null && cookie.length() > 0) {
      return cookie;
    }
    cookie = HintletHeaderUtils.hasHeader(headers, "Cookie");
    if (cookie != null && cookie.length() > 0) {
      return cookie;
    }
    return null;
  }
  
  private native static boolean stringMatchIM(String sourceString, String targetString)/*-{
    var re = new RegExp(targetString, 'im');
    return (sourceString.match(re) != null);
  }-*/;

}
