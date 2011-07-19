/*
 * Copyright 2010 Google Inc.
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

import com.google.speedtracer.shared.EventRecordType;

/**
 * Base class for overlays passed up for NetworkResource related EventRecords.
 */
public class ResourceRecord extends EventRecord {
  public static final boolean isResourceRecord(EventRecord rec) {
    switch (rec.getType()) {
      case EventRecordType.RESOURCE_SEND_REQUEST:
      case EventRecordType.RESOURCE_RECEIVE_RESPONSE:
      case EventRecordType.RESOURCE_FINISH:
      case EventRecordType.RESOURCE_UPDATED:
      case EventRecordType.NETWORK_REQUEST_WILL_BE_SENT:
      case EventRecordType.NETWORK_DATA_RECEIVED:
      case EventRecordType.NETWORK_RESPONSE_RECEIVED:
        return true;
    }
    return false;
  }

  protected ResourceRecord() {
  }

  public final int getIdentifier() {
    return getData().getIntProperty("identifier");
  }
}
