/*
 * Copyright 2011 Google Inc.
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
package com.google.speedtracer.hintletengine.client;

import com.google.gwt.junit.client.GWTTestCase;
import com.google.speedtracer.client.model.NetworkResource.HeaderMap;

/**
 * Tests {@link HintletCacheUtils.java}
 */
public class HintletCacheUtilsTests extends GWTTestCase {

  @Override
  public String getModuleName() {
    return "com.google.speedtracer.hintletengine.HintletEngine";
  }
  
  private static HeaderMap getTestData(int index) {
    return HintletTestData.getCacheUtilsTestData().get(index).<HeaderMap>cast();
  }
  
  public void testIsCacheableResponseCode() {
    int[] cacheable = {200, 203, 206, 300, 301, 410, 304};
    int[] notCacheable = {0, -1, 100, 201, 1000};
    for (int val : cacheable) {
      assertTrue(HintletCacheUtils.isCacheableResponseCode(val));
    }
    for (int val : notCacheable) {
      assertFalse(HintletCacheUtils.isCacheableResponseCode(val));
    }
  }
  
  public void testHasExplicitExpiration() {
    // must have Date, Expires, and Cache-Control max-age
    // has max-age but not Expires
    assertTrue(HintletCacheUtils.hasExplicitExpiration(getTestData(0)));
    // has both max-age and Expires
    assertTrue(HintletCacheUtils.hasExplicitExpiration(getTestData(1)));
    // has Expires but not max-age
    assertTrue(HintletCacheUtils.hasExplicitExpiration(getTestData(3)));
    // no Date
    assertFalse(HintletCacheUtils.hasExplicitExpiration(getTestData(5)));
    // no data
    assertFalse(HintletCacheUtils.hasExplicitExpiration(getTestData(6)));
    // date but no Expires or max-age
    assertFalse(HintletCacheUtils.hasExplicitExpiration(getTestData(7)));
  }
  
  public void testIsExplicitlyNonCacheable() {
    String url = "http://www.google.com";
    assertTrue(HintletCacheUtils.isExplicitlyNonCacheable(getTestData(8), url, 200));
    assertTrue(HintletCacheUtils.isExplicitlyNonCacheable(getTestData(9), url, 200));
    assertTrue(HintletCacheUtils.isExplicitlyNonCacheable(getTestData(10), url, 200));
    assertTrue(HintletCacheUtils.isExplicitlyNonCacheable(getTestData(11), url, 200));
    assertTrue(HintletCacheUtils.isExplicitlyNonCacheable(getTestData(12), url, 200));
    assertTrue(HintletCacheUtils.isExplicitlyNonCacheable(getTestData(12), url + "/?val=0", 200));
    assertTrue(HintletCacheUtils.isExplicitlyNonCacheable(getTestData(12), url, 0));
    // cacheable info but not cacheable due to code
    assertTrue(HintletCacheUtils.isExplicitlyNonCacheable(getTestData(5), url, 0));
    
    // cacheable resources
    assertFalse(HintletCacheUtils.isExplicitlyNonCacheable(getTestData(0), url, 200));
    assertFalse(HintletCacheUtils.isExplicitlyNonCacheable(getTestData(2), url, 200));
  }
  
  public void testIsPubliclyCacheable() {
    String url = "http://www.google.com";
    // fails because isExplicitlyNonCacheable
    assertFalse(HintletCacheUtils.isPubliclyCacheable(getTestData(8), url, 200));
    assertFalse(HintletCacheUtils.isPubliclyCacheable(getTestData(4), url, 200));
    assertFalse(HintletCacheUtils.isPubliclyCacheable(getTestData(12), url, 200));
    // public cache-control
    assertTrue(HintletCacheUtils.isPubliclyCacheable(getTestData(0), url, 200));
    // no cache-control (not explicitly private) and no ? in URL
    assertTrue(HintletCacheUtils.isPubliclyCacheable(getTestData(3), url, 200));
    // private cache control
    assertFalse(HintletCacheUtils.isPubliclyCacheable(getTestData(12), url, 200));
  }
  
  public void testFreshnessLifetimeGreaterThan() {    
    assertTrue(HintletCacheUtils.freshnessLifetimeGreaterThan(getTestData(0), 0));
    assertFalse(HintletCacheUtils.freshnessLifetimeGreaterThan(getTestData(0), 1500));
    assertTrue(HintletCacheUtils.freshnessLifetimeGreaterThan(getTestData(1), 20000000));
    
    assertTrue(HintletCacheUtils.freshnessLifetimeGreaterThan(getTestData(2), 1000));
    assertTrue(HintletCacheUtils.freshnessLifetimeGreaterThan(getTestData(3), 1000));
    assertFalse(HintletCacheUtils.freshnessLifetimeGreaterThan(getTestData(4), 1000));
    
    assertFalse(HintletCacheUtils.freshnessLifetimeGreaterThan(getTestData(5), 0));
    assertFalse(HintletCacheUtils.freshnessLifetimeGreaterThan(getTestData(6), 0));
  }
}
