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
 * Single event record with no duration corresponding to a Network Resource
 * Error.
 */
public class NetworkResourceError extends NetworkResourceRecord {
  public static final int TYPE = EventRecordType.NETWORK_RESOURCE_ERROR;

  static NetworkResourceError create(String resourceId, double endTime,
      int contentLength) {
    NetworkResourceError resource = createImpl(resourceId, TYPE, endTime,
        contentLength);
    return resource;
  }

  private static native NetworkResourceError createImpl(String id, int type,
      double endTime, int contentLength) /*-{
    return {
      type: type,
      time: endTime,
      data: {
        resourceId: id,
        contentLength: contentLength
      }
    };
  }-*/;

  protected NetworkResourceError() {
  }
}
