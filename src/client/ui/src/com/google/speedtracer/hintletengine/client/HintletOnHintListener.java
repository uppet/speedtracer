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

/**
 * Callback when the rule matches. 
 *
 */
public interface HintletOnHintListener {
  
  /** 
   * @param hintletRule the name of the hintlet rule that generated this record
   * @param timestamp the time associated with the hint description
   * @param description the human readable description of the hint
   * @param refRecord the sequence number of the record that triggered this hint
   *        record
   * @param severity the severity of the problem found.
   */  
  void onHint(String hintletRule, double timestamp, String description, int refRecord, int severity);
}
