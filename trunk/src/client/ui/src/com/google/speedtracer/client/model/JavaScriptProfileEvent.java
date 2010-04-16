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
 * Overlay for a EventRecordType.PROFILE_DATA type record.
 */
public class JavaScriptProfileEvent extends EventRecord {
  public static final int TYPE = EventRecordType.PROFILE_DATA;

  public static boolean isProfileEvent(EventRecord data) {
    return data.getType() == EventRecordType.PROFILE_DATA;
  }

  protected JavaScriptProfileEvent() {
  }

  public final String getFormat() {
    return getData().getStringProperty("format");
  };

  public final String getProfileData() {
    return getData().getStringProperty("profileData");
  }
  
  public final boolean isOrphaned() {
    return getData().getBooleanProperty("orphan");
  }
}
