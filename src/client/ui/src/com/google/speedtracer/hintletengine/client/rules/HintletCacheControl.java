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
import com.google.speedtracer.hintletengine.client.HintletNetworkResources;
import com.google.speedtracer.hintletengine.client.HintletOnHintListener;
import com.google.speedtracer.hintletengine.client.WebInspectorType;

import static com.google.speedtracer.hintletengine.client.HintletCacheUtils.freshnessLifetimeGreaterThan;
import static com.google.speedtracer.hintletengine.client.HintletCacheUtils.hasExplicitExpiration;
import static com.google.speedtracer.hintletengine.client.HintletCacheUtils.isCacheableResourceType;
import static com.google.speedtracer.hintletengine.client.HintletCacheUtils.isExplicitlyNonCacheable;
import static com.google.speedtracer.hintletengine.client.HintletCacheUtils.isPubliclyCacheable;
import static com.google.speedtracer.hintletengine.client.HintletHeaderUtils.hasCookie;
import static com.google.speedtracer.hintletengine.client.HintletHeaderUtils.headerContains;
import static com.google.speedtracer.hintletengine.client.HintletHeaderUtils.isCompressed;
import static com.google.speedtracer.hintletengine.client.WebInspectorType.getResourceType;

import java.util.ArrayList;
import java.util.List;

/**
 * Fires hintlets based on various cache related rules. Ripped off from Page Speed
 * cacheControlLint.js, as much as possible See
 * http://code.google.com/speed/page-speed/docs/caching.html
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
    createRules();
  }

  public HintletCacheControl(HintletOnHintListener onHint) {
    setOnHintCallback(onHint);
    createRules();
  }

  private void createRules() {
    cacheRules = new ArrayList<CacheRule>();

    /*
     * Expiration Rule It is important to specify one of Expires or Cache-Control max-age, and one
     * of Last-Modified or ETag, for all cacheable resources. "Expires" and "Cache-Control: max-age"
     * specify the "freshness" of the resource and apply unconditionally. Expires is prefered over
     * Cache-Control: max-age becasue it is more widely supported. TODO(sarahgsmith) : Consider
     * adding a rule to check for Last-Modified/ETag
     */
    cacheRules.add(new CacheRule() {
      public void onResourceFinish(NetworkResource resource, double timestamp, int refRecord) {
        if (!isCacheableResourceType(getResourceType(resource))) {
          return;
        }
        HeaderMap responseHeaders = resource.getResponseHeaders();
        if (hasCookie(responseHeaders) != null) {
          return;
        }
        if (isExplicitlyNonCacheable(
            responseHeaders, resource.getUrl(), resource.getStatusCode())) {
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

    /*
     * Vary Rule Internet Explorer does not cache any resources that are served with the Vary header
     * and any fields but Accept-Encoding and User-Agent. To ensure these resources are cached by
     * IE, make sure to strip out any other fields from the Vary header, or remove the Vary header
     * altogether if possible
     */
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

        // We strip Accept-Encoding and User-Agent from the header, as well
        // as separator characters (comma and space), and trigger a hintlet
        // if there is anything left in the header.
        varyHeader = removeValidVary(varyHeader);
        if (varyHeader.length() > 0) {
          addHint(getHintletName(), timestamp, formatMessage(resource), refRecord,
              HintRecord.SEVERITY_CRITICAL);
        }
      }

      private native String removeValidVary(String header) /*-{
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

    /*
     * Freshness Rule Set Expires to a minimum of one month for cacheable (static) resource,
     * preferably up to one year. TODO (sarahgsmith) : Not more than a year as this violates RFC
     * guidelines. (We prefer Expires over Cache-Control: max-age because it is is more widely
     * supported.)
     */
    cacheRules.add(new CacheRule() {
      public void onResourceFinish(NetworkResource resource, double timestamp, int refRecord) {
        // must have cacheable resource type or be a favicon
        if (!isCacheableResourceType(getResourceType(resource))) {
          return;
        }
        HeaderMap responseHeaders = resource.getResponseHeaders();
        if (hasCookie(responseHeaders) != null) {
          return;
        }
        // must have a freshness lifetime which is greater than 0
        if (!freshnessLifetimeGreaterThan(responseHeaders, 0)) {
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
            + " one year in the future for the following cacheable resources: " + resource.getUrl();
      }
    });

    /*
     * Favicon Rule Favicons should have an expiration at least one month in the future this is a
     * separate rule since they do not fit under the "cacheable". Additionally, we want to fire the
     * hint even if no freshness is specified, which the previous freshness rule looks for.
     */
    cacheRules.add(new CacheRule() {
      public void onResourceFinish(NetworkResource resource, double timestamp, int refRecord) {
        // for rule to fire:
        // must be of type FAVICON
        if (getResourceType(resource) != WebInspectorType.FAVICON) {
          return;
        }
        HeaderMap responseHeaders = resource.getResponseHeaders();
        // must not set cookie
        if (hasCookie(responseHeaders) != null) {
          return;
        }
        // if it doesn't have a long enough freshness liftime, fire hint
        if (!freshnessLifetimeGreaterThan(responseHeaders, MS_IN_A_MONTH)) {
          addHint(getHintletName(), timestamp, formatMessage(resource), refRecord,
              HintRecord.SEVERITY_WARNING);
        }
      }

      private String formatMessage(NetworkResource resource) {
        return "Favicons should have an expiration at least one month in the future: "
            + resource.getUrl();
      }
    });

    /*
     * Query Rule Most proxies, most notably Squid up through version 3.0, do not cache resources
     * with a "?" in their URL even if a Cache-control: public header is present in the response. To
     * enable proxy caching for these resources, remove query strings from references to static
     * resources, and instead encode the parameters into the file names themselves.
     */
    cacheRules.add(new CacheRule() {
      public void onResourceFinish(NetworkResource resource, double timestamp, int refRecord) {
        if (resource.getUrl().indexOf('?') == -1) {
          return;
        }
        HeaderMap responseHeaders = resource.getResponseHeaders();
        if (hasCookie(responseHeaders) != null) {
          return;
        }
        if (isPubliclyCacheable(responseHeaders, resource.getUrl(), resource.getStatusCode())) {
          addHint(getHintletName(), timestamp, formatMessage(resource), refRecord,
              HintRecord.SEVERITY_WARNING);
        }
      }

      private String formatMessage(NetworkResource resource) {
        return "Resources with a '?' in the URL are not cached by most"
            + " proxy caching servers. Remove the query string and encode the"
            + " parameters into the URL for the following resources: " + resource.getUrl();
      }
    });

    /**
     * Public Rule and Proxy Bug Rule Enabling public caching in the HTTP headers for static
     * resources allows the browser to download resources from a nearby proxy server rather than
     * from a remote origin server. You use the Cache-control: public header to indicate that a
     * resource can be cached by public web proxies in addition to the browser that issued the
     * request. With some exceptions (described below), you should configure your web server to set
     * this header to public for cacheable resources.
     *
     *  Some public proxies have bugs that do not detect the presence of the Content-Encoding
     * response header. This can result in compressed versions being delivered to client browsers
     * that cannot properly decompress the files. Since these files should always be gzipped by your
     * server, to ensure that the client can correctly read the files, do either of the following:
     * (see Bug Rule) - Set the the Cache-Control header to private. This disables proxy caching
     * altogether for these resources. If your application is multi-homed around the globe and
     * relies less on proxy caches for user locality, this might be an appropriate setting. - Set
     * the Vary: Accept-Encoding response header. This instructs the proxies to cache two versions
     * of the resource: one compressed, and one uncompressed. The correct version of the resource is
     * delivered based on the client request header. This is a good choice for applications that are
     * singly homed and depend on public proxies for user locality.
     */
    cacheRules.add(new CacheRule() {
      public void onResourceFinish(NetworkResource resource, double timestamp, int refRecord) {
        HeaderMap responseHeaders = resource.getResponseHeaders();
        if (!isCacheableResourceType(getResourceType(resource))) {
          return;
        }
        if (isExplicitlyNonCacheable(
            responseHeaders, resource.getUrl(), resource.getStatusCode())) {
          return;
        }
        if (hasCookie(responseHeaders) != null) {
          return;
        }
        // "Some public proxies have bugs..." (see comment for rule)
        if (isCompressed(responseHeaders)
            && !headerContains(responseHeaders, "Vary", "Accept-Encoding")) {
          if (isPubliclyCacheable(responseHeaders, resource.getUrl(), resource.getStatusCode())) {
            addHint(getHintletName(), timestamp, formatMessageBug(resource), refRecord,
                HintRecord.SEVERITY_WARNING);
          }
          return;
        }

        if (headerContains(responseHeaders, "Cache-Control", "public")) {
          return;
        }

        // consider making it publicly cacheable
        addHint(getHintletName(), timestamp, formatMessagePublic(resource), refRecord,
            HintRecord.SEVERITY_INFO);
      }

      private String formatMessagePublic(NetworkResource resource) {
        return "Consider adding a 'Cache-Control: public' header to the" + " following resource: "
            + resource.getUrl();
      }

      private String formatMessageBug(NetworkResource resource) {
        return "Due to a bug, some proxy caching servers do not detect the presence"
            + " of the Content-Encoding response header. This can result in compressed"
            + " versions being delivered to client browsers that cannot properly"
            + " decompress the files. Therefore, use either 'Cache-Control: private'"
            + " or 'Vary: Accept-Encoding' for the following resource: " + resource.getUrl();
      }

    });

    /*
     * Cookie Rule Files with Cookie headers should never be publicly cached. Don't enable proxy
     * caching for resources that set cookies. Setting the header to public effectively shares
     * resources among multiple users, which means that any cookies set for those resources are
     * shared as well. While many proxies won't actually cache any resources with cookie headers
     * set, it's better to avoid the risk altogether. Either set the Cache-Control header to private
     * or serve these resources from a cookieless domain.
     */
    cacheRules.add(new CacheRule() {
      public void onResourceFinish(NetworkResource resource, double timestamp, int refRecord) {
        HeaderMap responseHeaders = resource.getResponseHeaders();
        if (hasCookie(responseHeaders) != null
            && isPubliclyCacheable(responseHeaders, resource.getUrl(), resource.getStatusCode())) {
          addHint(getHintletName(), timestamp, formatMessage(resource), refRecord,
              HintRecord.SEVERITY_CRITICAL);
        }
      }

      private String formatMessage(NetworkResource resource) {
        return "The following publicly cacheable resources contain"
            + " a Set-Cookie header. This security vulnerability can cause cookies"
            + " to be shared by multiple users: " + resource.getUrl();
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
    NetworkResource resource =
        HintletNetworkResources.getInstance().getResourceData(finish.getIdentifier());

    if (resource.getResponseHeaders() == null) {
      return;
    }

    for (CacheRule rule : cacheRules) {
      rule.onResourceFinish(resource, dataRecord.getTime(), dataRecord.getSequence());
    }

  }

}
