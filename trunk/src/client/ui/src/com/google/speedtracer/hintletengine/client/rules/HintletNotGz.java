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
import com.google.speedtracer.hintletengine.client.HintletCacheUtils;
import com.google.speedtracer.hintletengine.client.HintletHeaderUtils;
import com.google.speedtracer.hintletengine.client.HintletNetworkResources;
import com.google.speedtracer.hintletengine.client.HintletOnHintListener;
import com.google.speedtracer.hintletengine.client.WebInspectorType;
import com.google.speedtracer.shared.EventRecordType;

/**
 * Hintlet that flags network resources that are not gzip'ed.
 * 
 * We are looking for resources that do NOT contain Content-Encoding: that indicates compression
 * (e.g. gzip) and have Content-Length > 150 bytes.
 * 
 */
public class HintletNotGz extends HintletRule {

  // We consider 150 bytes to be the break-even point for using gzip.
  private static final int SIZE_THRESHOLD = 150;

  public HintletNotGz() {
  }

  public HintletNotGz(HintletOnHintListener onHint) {
    setOnHintCallback(onHint);
  }
  
  @Override
  public String getHintletName() {
    return "Uncompressed Resource";
  }

  @Override
  public void onEventRecord(EventRecord eventRecord) {

    if (eventRecord.getType() != EventRecordType.RESOURCE_FINISH) {
      return;
    }

    ResourceRecord resourceFinishEvent = eventRecord.cast();

    NetworkResource savedNetworkResource =
        HintletNetworkResources.getInstance().getResourceData(resourceFinishEvent.getRequestId());
    if (savedNetworkResource == null) {
      return;
    }

    // Don't suggest compressing very small components.
    int size = savedNetworkResource.getDataLength();
    if (size < SIZE_THRESHOLD) {
      return;
    }

    if(!HintletCacheUtils.isCompressibleResourceType(WebInspectorType.getResourceType(savedNetworkResource))){
      return;
    }

    //add hint if not compressed
    if (!HintletHeaderUtils.isCompressed(savedNetworkResource.getResponseHeaders())) {
      addHint(getHintletName(), savedNetworkResource.getResponseReceivedTime(), "URL " + savedNetworkResource.getUrl()
          + " was not compressed with gzip or bzip2", resourceFinishEvent.getSequence(),
          HintRecord.SEVERITY_INFO);
    }
  }
}
