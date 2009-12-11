/*
 * Copyright 2009 Google Inc.
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
package com.google.speedtracer.client.model;

/**
 * Base class for overlays passed up for NetworkResource related EventRecords.
 */
public class NetworkResourceRecord extends EventRecord {
  public static String generateResourceId(int redirectCount, int resourceId) {
    return redirectCount + "-" + resourceId;
  }

  public static final boolean isNetworkResourceRecord(EventRecord rec) {
    switch (rec.getType()) {
      case EventRecordType.NETWORK_RESOURCE_ERROR:
      case EventRecordType.NETWORK_RESOURCE_FINISH:
      case EventRecordType.NETWORK_RESOURCE_RESPONSE:
      case EventRecordType.NETWORK_RESOURCE_START:
        return true;
    }
    return false;
  }

  protected NetworkResourceRecord() {
  }

  public final String getResourceId() {
    return getData().getStringProperty("resourceId");
  }
}
