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
package com.google.speedtracer.hintletengine.client;

import com.google.speedtracer.client.model.NetworkDataReceivedEvent;
import com.google.speedtracer.client.model.ResourceFinishEvent;
import com.google.speedtracer.client.model.ResourceWillSendEvent;

/**
 * Utility class for creating various EventRecord objects for
 * use with test Hintlets.
 */
public class HintletEventRecordBuilder {

  public static final int DEFAULT_TIME = 1;
  public static final int DEFAULT_SEQUENCE = 1;
  public static final String DEFAULT_ID = "1";

  public native static NetworkDataReceivedEvent createNetworkDataRecieved(int dataLength) /*-{
    return {
        "type" : @com.google.speedtracer.shared.EventRecordType::NETWORK_DATA_RECEIVED,
        "time" : 1,
        "data" : {
            "identifier" : 1,
            "dataLength" : dataLength
        }
    };
  }-*/;

  public static ResourceWillSendEvent createResourceStart(String url) {
    return createResourceStart(url, DEFAULT_TIME, DEFAULT_SEQUENCE, DEFAULT_ID);
  }

  /**
   * Create a start event with the given values
   */
  public static native ResourceWillSendEvent createResourceStart(
      String url, int time, int sequence, String identifier) /*-{
    return {
        "type" : @com.google.speedtracer.shared.EventRecordType::RESOURCE_SEND_REQUEST,
        "time" : time,
        "sequence" : sequence,
        "data" : {
            "identifier" : identifier,
            "url" : url,
            "requestMethod" : "GET"
        }
    };
  }-*/;

  /**
   * Get a default finish event
   */
  public static ResourceFinishEvent createResourceFinish() {
    return createResourceFinish(DEFAULT_TIME, DEFAULT_SEQUENCE, DEFAULT_ID);
  }

  /**
   * Create a finish event with the given values.
   */
  public static native ResourceFinishEvent createResourceFinish(
      int time, int sequence, String identifier) /*-{
    return {
        "type" : @com.google.speedtracer.shared.EventRecordType::RESOURCE_FINISH,
        "time" : time,
        "sequence" : sequence,
        "data" : {
            "identifier" : identifier,
            "didFail" : false,
            "networkTime" : 100
        }
    }
  }-*/;

}
