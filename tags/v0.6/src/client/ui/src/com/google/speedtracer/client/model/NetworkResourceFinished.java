/*
 * Copyright 2008 Google Inc.
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
 * Single event record with no duration corresponding to the completion of a
 * network resource request.
 */
public class NetworkResourceFinished extends NetworkResourceRecord {
  public static final int TYPE = EventRecordType.NETWORK_RESOURCE_FINISH;

  static NetworkResourceFinished create(String resourceId, double endTime) {
    NetworkResourceFinished resource = createImpl(resourceId, TYPE, endTime);
    return resource;
  }

  private static native NetworkResourceFinished createImpl(String id, int type,
      double endTime) /*-{
    return {
      type: type,
      time: endTime,
      data: {
        resourceId: id
      }   
    };
  }-*/;

  protected NetworkResourceFinished() {
  }
}
