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

import com.google.gwt.coreext.client.JsIntegerMap;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.speedtracer.client.util.Command;

/**
 * Tests for JavaScriptProfileModel.
 */
public class JavaScriptProfileModelTests extends GWTTestCase {

  private static class Lookup implements EventRecordLookup {
    private JsIntegerMap<EventRecord> eventRecordMap = JsIntegerMap.create();

    public EventRecord findEventRecordFromSequence(int sequence) {
      return eventRecordMap.get(sequence);
    }

    public void put(int sequence, EventRecord rec) {
      eventRecordMap.put(sequence, rec);
    }
  }

  private static native UiEvent makeUiEvent(int sequence) /*-{
    // mimic an Eval Script event 
    return {'type':10,'time':sequence,'sequence':sequence,'data':{'url':'http://mock.org','lineNumber':sequence}};
  }-*/;

  private static native JavaScriptProfileEvent makeV8ProfileEvent(
      String profileData, int sequence) /*-{
    return {'type':15,'time':sequence,'sequence':sequence,'data':{'format':"v8",'profileData':profileData}};
  }-*/;

  @Override
  public String getModuleName() {
    return "com.google.speedtracer.Common";
  }

  public void testProfileModel() {
    String mockProfileData = "code-creation,LoadIC,0x100,179,\"parentNode\"\n"
        + "tick,0x100,+1,0\n";
    Lookup lookup = new Lookup();
    JavaScriptProfileModel profileModel = new JavaScriptProfileModel(lookup);
    final UiEvent testEvent = makeUiEvent(1);
    lookup.put(1, testEvent);
    JavaScriptProfileEvent profileEvent = makeV8ProfileEvent(mockProfileData, 2);
    profileModel.onEventRecord(profileEvent);
    final JavaScriptProfile profile = profileModel.getProfileForEvent(1);
    assert profile != null;

    Command.defer(new Command.Method() {
      public void execute() {
        if (testEvent.hasJavaScriptProfile()) {
          assertEquals(1.0, profile.getTotalTime(), .001);
          finishTest();
        } else {
          Command.defer(this);
        }
      }
    });
    delayTestFinish(2000);
  }

  public void testVisitProfileNodes() {
    String mockProfileData = "code-creation,LoadIC,0x100,179,\"parentNode\"\n"
        + "tick,0x100,+1,0\n";
    final Lookup lookup = new Lookup();
    final JavaScriptProfileModel profileModel = new JavaScriptProfileModel(
        lookup);

    UiEvent testEvent;
    JavaScriptProfileEvent profileEvent;

    testEvent = makeUiEvent(1);
    lookup.put(1, testEvent);
    profileEvent = makeV8ProfileEvent(mockProfileData, 2);
    profileModel.onEventRecord(profileEvent);

    testEvent = makeUiEvent(3);
    lookup.put(3, testEvent);
    profileEvent = makeV8ProfileEvent(mockProfileData, 4);
    profileModel.onEventRecord(profileEvent);

    // Wait until processing is done, then run some more checks
    Command.defer(new Command.Method() {
      public void execute() {
        UiEvent event1 = (UiEvent) lookup.findEventRecordFromSequence(1);
        UiEvent event3 = (UiEvent) lookup.findEventRecordFromSequence(3);
        if (event1.hasJavaScriptProfile() && event3.hasJavaScriptProfile()) {
          doTestEventProcessing(profileModel);
        } else {
          Command.defer(this);
        }
      }
    });
    delayTestFinish(10000);
  }

  // After the profile data is parsed, the EventProcessor code can be exercised.
  private void doTestEventProcessing(final JavaScriptProfileModel profileModel) {
    profileModel.processEventsWithProfiles(new JavaScriptProfileModel.EventProcessor() {
      final boolean foundEvent[] = new boolean[4];

      public void onCompleted() {
        if (foundEvent[1] && foundEvent[3]) {
          finishTest();
        } else {
          fail();
        }
      }

      public void process(UiEvent event) {
        foundEvent[event.getSequence()] = true;
      }
    });
  }
}
