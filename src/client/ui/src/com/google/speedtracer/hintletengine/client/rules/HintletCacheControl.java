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
package com.google.speedtracer.hintletengine.client.rules;

import com.google.speedtracer.client.model.EventRecord;
import com.google.speedtracer.client.model.HintRecord;
import com.google.speedtracer.client.model.NetworkResource;
import com.google.speedtracer.client.model.ResourceFinishEvent;
import com.google.speedtracer.hintletengine.client.HintletNetworkResources;

import static com.google.speedtracer.hintletengine.client.HintletCacheUtils.freshnessLifetimeGreaterThan;
import static com.google.speedtracer.hintletengine.client.HintletCacheUtils.hasExplicitExpiration;
import static com.google.speedtracer.hintletengine.client.HintletCacheUtils.isCacheableResourceType;
import static com.google.speedtracer.hintletengine.client.HintletCacheUtils.isExplicitlyNonCacheable;
import static com.google.speedtracer.hintletengine.client.WebInspectorType.getResourceType;

import java.util.ArrayList;
import java.util.List;

/**
 * Fires hintlets based on various cache related rules.
 * Ripped off from Page Speed cacheControlLint.js, as much as possible
 */
public class HintletCacheControl extends HintletRule {

  // Note: To comply with RFC 2616, we don't want to require that Expires is
  // a full year in the future. Set the minimum to 11 months. Note that this
  // isn't quite right because not all months have 30 days, but we err on the
  // side of being conservative, so it works fine for the rule.
  private static final double SECONDS_IN_A_MONTH = 60 * 60 * 24 * 30;
  private static final double MS_IN_A_MONTH = 1000 * SECONDS_IN_A_MONTH;
  
  private List<CacheRule> cacheRules;
  
  private interface CacheRule {
    public void onResourceFinish(NetworkResource resource, double timestamp, int refRecord);
  }
  
  public HintletCacheControl() {
    cacheRules = new ArrayList<CacheRule>();

    // Expiration
    cacheRules.add(new CacheRule() {
      public void onResourceFinish(NetworkResource resource, double timestamp, int refRecord) {
        if (!isCacheableResourceType(getResourceType(resource))) {
          return;
        }
        if (resource.getResponseHeaders().get("Set-Cookie") != null) {
          return;
        }
        if (isExplicitlyNonCacheable(
            resource.getResponseHeaders(), resource.getUrl(), resource.getStatusCode())) {
          return;
        }
        if (!hasExplicitExpiration(resource.getResponseHeaders())) {
          addHint(getHintletName(), timestamp, formatMessage(resource), refRecord,
              HintRecord.SEVERITY_CRITICAL);
        }
      }

      private String formatMessage(NetworkResource resource) {
        return "The following resources are missing a cache expiration."
            + " Resources that do not specify an expiration may not be cached by"
            + " browsers. Specify an expiration at least one month in the future"
            + " for resources that should be cached, and an expiration in the past"
            + " for resources that should not be cached: " + resource.getUrl();
      }
    });
  }
  
  @Override
  public String getHintletName() {
    return "Resource Caching";
  }

  @Override
  public void onEventRecord(EventRecord dataRecord) {
    if (!(dataRecord.getType() == ResourceFinishEvent.TYPE)) {
      return;
    }
    
    ResourceFinishEvent finish = dataRecord.cast();
    NetworkResource resource = HintletNetworkResources.getInstance().getResourceData(finish.getIdentifier());
    
    if(resource.getResponseHeaders() == null) {
      return;
    }
    
    for (CacheRule rule : cacheRules) {
      rule.onResourceFinish(resource, dataRecord.getTime(), dataRecord.getSequence());
    }
    
  }

}
