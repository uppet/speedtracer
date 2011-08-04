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
import com.google.speedtracer.client.model.ResourceDataReceivedEvent;
import com.google.speedtracer.client.model.ResourceFinishEvent;
import com.google.speedtracer.client.model.ResourceResponseEvent;
import com.google.speedtracer.client.model.ResourceWillSendEvent;
import com.google.speedtracer.client.model.TabChangeEvent;

/**
 * Utility class for creating various EventRecord objects for
 * use with test Hintlets.
 */
public class HintletEventRecordBuilder {

  public static final int DEFAULT_TIME = 1;
  public static final int DEFAULT_SEQUENCE = 1;
  public static final String DEFAULT_ID = "1";

  /**
   * This method sets time equal to sequence
   */  
  public static NetworkDataReceivedEvent createNetworkDataRecieved(String identifier,
      int sequence, int dataLength){
    return createNetworkDataRecieved(identifier, sequence, sequence, dataLength);
  }
  
  public native static NetworkDataReceivedEvent createNetworkDataRecieved(String identifier,
      int time, int sequence, int dataLength)/*-{
    return {
      "type" : @com.google.speedtracer.shared.EventRecordType::NETWORK_DATA_RECEIVED,
      "time" : time,
      "data" : {
        "identifier" : identifier,
        "dataLength" : dataLength
      },
      "sequence" : sequence
    };
  }-*/;
  
  public static NetworkDataReceivedEvent createNetworkDataRecieved(int dataLength){
    return createNetworkDataRecieved(DEFAULT_ID, DEFAULT_TIME, DEFAULT_SEQUENCE, dataLength);
  }

  public static ResourceWillSendEvent createResourceSendRequest(String url) {
    return createResourceSendRequest(url, DEFAULT_TIME, DEFAULT_SEQUENCE, DEFAULT_ID);
  }

  /**
   * This method sets time equal to sequence
   */
  public static ResourceWillSendEvent createResourceSendRequest(String identifier, String url, int sequence){
    return createResourceSendRequest(url, sequence, sequence, identifier);
  }
  
  /**
   * Create a start event with the given values
   */
  public static native ResourceWillSendEvent createResourceSendRequest(
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
   * This method sets time equal to sequence
   */
  public static ResourceFinishEvent createResourceFinish(String identifier, int sequence) {
    return createResourceFinish(sequence, sequence, identifier);
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
            "didFail" : false
        }
    }
  }-*/;

  /**
   * This method sets time equal to sequence
   */  
  public native static ResourceResponseEvent createResourceReceiveResponse(String identifier,
      int sequence, String mimeType)/*-{
    return {
      "data" : {
        "identifier" : identifier,
        "statusCode" : 200,
        "mimeType" : mimeType
      },
      "children" : [],
      "type" : @com.google.speedtracer.shared.EventRecordType::RESOURCE_RECEIVE_RESPONSE,
      "duration" : 0.029052734375,
      "time" : sequence,
      "sequence" : sequence
    };
  }-*/;
  
  /**
   * This method sets time equal to sequence
   */
  public native static ResourceDataReceivedEvent createResourceDataReceived(String identifier,
      int sequence)/*-{
    return {
      "data" : {
        "identifier" : identifier
      },
      "children" : [],
      "type" : @com.google.speedtracer.shared.EventRecordType::RESOURCE_DATA_RECEIVED,
      "duration" : 0.02,
      "time" : sequence,
      "sequence" : sequence
    };
  }-*/;

  /**
   * This method sets time equal to sequence
   */
  public native static TabChangeEvent createTabChanged(String url, int sequence)/*-{
    return {
        "data" : {
            "url" : url
        },
        "type" : @com.google.speedtracer.shared.EventRecordType::TAB_CHANGED,
        "time" : sequence,
        "sequence" : sequence
    };
  }-*/;
}
