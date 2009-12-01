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
   * Corresponds to {@link InitializeMonitorMessage}.
   */
  static final int INITIALIZE_MONITOR_TYPE = 1;
  static final int PAGE_TRANSITION_TYPE = 2;
  static final int REQUEST_INITIALIZATION_TYPE = 3;
  static final int REQUEST_FILE_LOAD_TYPE = 4;
  static final int RECORD_DATA_TYPE = 5;
  
  private MessageType() {
  }
}
