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
import com.google.speedtracer.client.model.NetworkResource.HeaderMap;
import com.google.speedtracer.client.model.ResourceFinishEvent;
import com.google.speedtracer.hintletengine.client.HintletHeaderUtils;
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
 * See http://code.google.com/speed/page-speed/docs/caching.html
 */
public class HintletCacheControl extends HintletRule {

  // Note: To comply with RFC 2616, we don't want to require that Expires is
  // a full year in the future. Set the minimum to 11 months. Note that this
  // isn't quite right because not all months have 30 days, but we err on the
  // side of being conservative, so it works fine for the rule.
  public static final double SECONDS_IN_A_MONTH = 60 * 60 * 24 * 30;
  public static final double MS_IN_A_MONTH = 1000 * SECONDS_IN_A_MONTH;
  
  private List<CacheRule> cacheRules;
  
  private interface CacheRule {
    public void onResourceFinish(NetworkResource resource, double timestamp, int refRecord);
  }
  
  public HintletCacheControl() {
    cacheRules = new ArrayList<CacheRule>();

    // Cache rule which generates hints when the Expires field 
    // is needed but not present.
    cacheRules.add(new CacheRule() {
      public void onResourceFinish(NetworkResource resource, double timestamp, int refRecord) {
        if (!isCacheableResourceType(getResourceType(resource))) {
          return;
        }
        HeaderMap responseHeaders = resource.getResponseHeaders();
        if(HintletHeaderUtils.hasCookie(responseHeaders) != null) {
          return;
        }
        if (isExplicitlyNonCacheable(responseHeaders, resource.getUrl(), resource.getStatusCode())) {
          return;
        }
        if (!hasExplicitExpiration(responseHeaders)) {
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
   
    // Internet Explorer does not cache any resources that are served 
    // with the Vary header and any fields but Accept-Encoding and User-Agent. 
    // To ensure these resources are cached by IE, make sure to strip out any 
    // other fields from the Vary header, or remove the Vary header altogether if possible
    cacheRules.add(new CacheRule() {
      public void onResourceFinish(NetworkResource resource, double timestamp, int refRecord) {
        HeaderMap responseHeaders = resource.getResponseHeaders();
        String varyHeader = responseHeaders.get("Vary");
        if (varyHeader == null) {
          return;
        }
        if (!isCacheableResourceType(getResourceType(resource))) {
          return;
        }
        if (!freshnessLifetimeGreaterThan(responseHeaders, 0)) {
          return;
        }

        // MS documentation indicates that IE will cache
        // responses with Vary Accept-Encoding or
        // User-Agent, but not anything else. So we
        // strip these strings from the header, as well
        // as separator characters (comma and space),
        // and trigger a warning if there is anything
        // left in the header.
        varyHeader = removeValidVary(varyHeader);
        if (varyHeader.length() > 0) {
          addHint(getHintletName(), timestamp, formatMessage(resource), refRecord,
              HintRecord.SEVERITY_CRITICAL);
        }
      }
      
      private native String removeValidVary(String header) /*-{
        //var newHeader = header;
        header = header.replace(/User-Agent/gi, '');
        header = header.replace(/Accept-Encoding/gi, '');
        var patt = new RegExp('[, ]*', 'g');
        header = header.replace(patt, '');
        return header;
      }-*/;
 
      private String formatMessage(NetworkResource resource) {
        return "The following resources specify a 'Vary' header that"
            + " disables caching in most versions of Internet Explorer. Fix or remove"
            + " the 'Vary' header for the following resources: " + resource.getUrl();
      }
    });
    
    // Freshness
    // Set Expires to a minimum of one month for cacheable (static) resource,
    // preferably up to one year.
    // TODO (sarahgsmith) : Not more than a year as this violates RFC guidelines. 
    //   (We prefer Expires over Cache-Control: max-age because it is is more widely supported.)
    cacheRules.add(new CacheRule() {
      public void onResourceFinish(NetworkResource resource, double timestamp, int refRecord) {
        // for this hint:
        // must have cacheable resource type
        if (!isCacheableResourceType(getResourceType(resource))) {
          return;
        }
        HeaderMap responseHeaders = resource.getResponseHeaders();
        // must not set cookies
        if(HintletHeaderUtils.hasCookie(responseHeaders) != null) {
          return;
        }
        // must have a freshness lifetime which is greater than 0
        if(!freshnessLifetimeGreaterThan(responseHeaders, 0)) {
          return;
        }

        // if the freshness is less than a month, fire the hint
        if (!freshnessLifetimeGreaterThan(responseHeaders, MS_IN_A_MONTH)) {
          addHint(getHintletName(), timestamp, formatLessThanMonthMessage(resource), refRecord,
              HintRecord.SEVERITY_WARNING);
          return;
        }

        // if the freshness is more than a month but less than a year, fire info hint
        if (!freshnessLifetimeGreaterThan(responseHeaders, MS_IN_A_MONTH * 11)) {
          addHint(getHintletName(), timestamp, formatLessThanYearMessage(resource), refRecord,
              HintRecord.SEVERITY_INFO);
        }
      }
      
      private String formatLessThanMonthMessage(NetworkResource resource) {
        return "The following cacheable resources have a short"
            + " freshness lifetime. Specify an expiration at least one month in the"
            + " future for the following resources: " + resource.getUrl();
      }
      
      private String formatLessThanYearMessage(NetworkResource resource) {
        return "To further improve cache hit rate, specify an expiration"
            + " one year in the future for the following cacheable resources: "
            + resource.getUrl();
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
