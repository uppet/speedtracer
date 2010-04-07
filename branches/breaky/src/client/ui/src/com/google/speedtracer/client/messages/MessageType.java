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
package com.google.speedtracer.client.messages;

/**
 * This class is just a convenient place to statically declare all the message
 * type id's. This class should be completely pruned by the compiler and all the
 * constants should be inlined.
 */
class MessageType {
  /**
   * {@link com.google.speedtracer.client.WindowChannel.Message} message types.
   */
  static final int WC_INITIALIZE_MONITOR_TYPE = 1;
  static final int WC_PAGE_TRANSITION_TYPE = 2;
  static final int WC_REQUEST_INITIALIZATION_TYPE = 3;
  static final int WC_REQUEST_FILE_LOAD_TYPE = 4;
  static final int WC_RECORD_DATA_TYPE = 5;
  static final int WC_RESEND_PROFILING_DATA_TYPE = 6;
  static final int WC_RESET_BASE_TIME_TYPE = 7;
  
  /**
   * {@link com.google.gwt.chrome.crx.client.Port.Message} message types.
   * The PORT_HEADLESS_XXX values must be kept in sync with the values
   * used in headless_api.js and headless_content_type.js. 
   */
  static final int PORT_EVENT_RECORD_TYPE = 100;
  static final int PORT_PAGE_EVENT_TYPE = 101;
  static final int PORT_HEADLESS_CLEAR_DATA = 102;  
  static final int PORT_HEADLESS_MONITORING_ON = 103;
  static final int PORT_HEADLESS_MONITORING_OFF = 104;
  static final int PORT_HEADLESS_GET_DUMP = 105;
  static final int PORT_HEADLESS_GET_DUMP_ACK = 106;
  static final int PORT_HEADLESS_SEND_DUMP = 107;
  static final int PORT_HEADLESS_SEND_DUMP_ACK = 108;
  static final int PORT_HEADLESS_MONITORING_ON_ACK = 109;
  static final int PORT_HEADLESS_MONITORING_OFF_ACK = 110;
  
  private MessageType() {
  }
}
