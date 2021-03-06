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

/**
 * Overlay type for network resource messages that are sent when the resource
 * loader adjusts its estimate of the content length for the response payload.
 */
public class NetworkDataReceivedEvent extends NetworkEvent {
  protected NetworkDataReceivedEvent() {
  }

  /**
   *
   */
  public static final class Data extends NetworkEvent.Data {
    protected Data() {
    }

    public int getLengthReceived() {
      return getIntProperty("dataLength");
    }
  }
  
  public final int getDataLength() { 
    return getData().<NetworkDataReceivedEvent.Data> cast().getLengthReceived();
  }
  
}
