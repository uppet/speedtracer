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
package com.google.speedtracer.hintletengine.client.rules;

import com.google.speedtracer.client.model.EventRecord;
import com.google.speedtracer.client.model.HintRecord;
import com.google.speedtracer.client.model.NetworkResource;
import com.google.speedtracer.client.model.ResourceFinishEvent;
import com.google.speedtracer.hintletengine.client.HintletNetworkResources;
import com.google.speedtracer.hintletengine.client.HintletOnHintListener;
import com.google.speedtracer.shared.EventRecordType;

/**
 * Hintlet based on large resource downloads
 */
public class HintletTotalBytes extends HintletRule {

  private static final int TOTAL_BYTES_INFO_THRESHOLD = 500000,
      TOTAL_BYTES_WARNING_THRESHOLD = 1000000;

  public HintletTotalBytes() {
  }

  public HintletTotalBytes(HintletOnHintListener onHint) {
    setOnHintCallback(onHint);
  }

  @Override
  public String getHintletName() {
    return "Total Bytes Downloaded";
  }

  @Override
  public void onEventRecord(EventRecord dataRecord) {

    if (dataRecord.getType() != EventRecordType.RESOURCE_FINISH) {
      return;
    }

    ResourceFinishEvent finishEvent = dataRecord.cast();
    NetworkResource resource =
        HintletNetworkResources.getInstance().getResourceData(finishEvent.getIdentifier());
    int resourceSize = resource.getDataLength();

    if (resourceSize > TOTAL_BYTES_WARNING_THRESHOLD) {
      addHint(getHintletName(), dataRecord.getTime(),
          formatMessage(resourceSize, TOTAL_BYTES_WARNING_THRESHOLD), dataRecord.getSequence(),
          HintRecord.SEVERITY_WARNING);
    } else if (resourceSize > TOTAL_BYTES_INFO_THRESHOLD) {
      addHint(getHintletName(), dataRecord.getTime(),
          formatMessage(resourceSize, TOTAL_BYTES_INFO_THRESHOLD), dataRecord.getSequence(),
          HintRecord.SEVERITY_INFO);
    }
  }

  private String formatMessage(int bytes, int threshold) {
    return bytes + " bytes downloaded, exceeds threshold of " + threshold + " bytes.";
  }

}
