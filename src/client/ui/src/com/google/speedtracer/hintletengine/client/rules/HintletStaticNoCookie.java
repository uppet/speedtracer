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
import com.google.speedtracer.client.model.ResourceRecord;
import com.google.speedtracer.hintletengine.client.HintletHeaderUtils;
import com.google.speedtracer.hintletengine.client.HintletNetworkResources;
import com.google.speedtracer.hintletengine.client.WebInspectorType;
import com.google.speedtracer.shared.EventRecordType;

/**
 * Rule to find static content served from a domain that sets cookies. From Page Speed, "Static
 * content, such as images, JS and CSS files, don't need to be accompanied by cookies, as there is
 * no user interaction with these resources. You can decrease request latency by serving static
 * resources from a domain that doesn't serve cookies. This technique is especially useful for pages
 * referencing large volumes of rarely cached static content, such as frequently changing image
 * thumbnails, or infrequently accessed image archives. We recommend this technique for any page
 * that serves more than 5 static resources. (For pages that serve fewer resources than this, it's
 * not worth the cost of setting up an extra domain.)"
 */
public class HintletStaticNoCookie extends HintletRule {

  @Override
  public String getHintletName() {
    return "Static Resource served from domains with cookies";
  }

  @Override
  public void onEventRecord(EventRecord eventRecord) {

    if (eventRecord.getType() != EventRecordType.RESOURCE_FINISH) {
      return;
    }

    ResourceRecord resourceFinishEvent = eventRecord.cast();

    NetworkResource savedNetworkResource =
        HintletNetworkResources.getInstance().getResourceData(resourceFinishEvent.getIdentifier());
    if (savedNetworkResource == null) {
      return;
    }

    // Make sure this is a static resource
    WebInspectorType resourceType = WebInspectorType.getResourceType(savedNetworkResource);
    switch (resourceType) {
      case STYLESHEET:
      case SCRIPT:
      case IMAGE:
      case MEDIA:
        break;
      default:
        return;
    }

    String cookie = HintletHeaderUtils.hasCookie(savedNetworkResource.getResponseHeaders());
    if (cookie != null) {
      addHint(getHintletName(), savedNetworkResource.getResponseReceivedTime(), "URL "
          + savedNetworkResource.getUrl() + " is static content that should be "
          + "served from a domain that does not set cookies.  Found " + (cookie.length() + 8)
          + " extra bytes from cookie.", eventRecord.getSequence(), HintRecord.SEVERITY_INFO);
    }
  }
}
