package com.google.speedtracer.client.model;

import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DeferredCommand;
import com.google.speedtracer.client.model.DataModel.EventCallbackProxy;
import com.google.speedtracer.client.util.JsIntegerMap;

public class JavaScriptProfileModelTests extends GWTTestCase {

  private static class Lookup implements EventRecordLookup {
    private JsIntegerMap<EventRecord> eventRecordMap = JsIntegerMap.create();

    public EventRecord findEventRecord(int sequence) {
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
    EventCallbackProxy cb = profileModel.getEventCallback(profileEvent);
    assert cb != null;
    cb.onEventRecord(profileEvent);
    final JavaScriptProfile profile = profileModel.getProfileForEvent(1);
    assert profile != null;

    DeferredCommand.addCommand(new Command() {
      public void execute() {
        if (testEvent.hasJavaScriptProfile()) {
          assertEquals(1.0, profile.getTotalTime(), .001);
          finishTest();
        } else {
          DeferredCommand.addCommand(this);
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
    EventCallbackProxy cb;

    testEvent = makeUiEvent(1);
    lookup.put(1, testEvent);
    profileEvent = makeV8ProfileEvent(mockProfileData, 2);

    cb = profileModel.getEventCallback(profileEvent);
    assert cb != null;
    cb.onEventRecord(profileEvent);

    testEvent = makeUiEvent(3);
    lookup.put(3, testEvent);
    profileEvent = makeV8ProfileEvent(mockProfileData, 4);
    cb = profileModel.getEventCallback(profileEvent);
    assert cb != null;
    cb.onEventRecord(profileEvent);

    // Wait until processing is done, then run some more checks
    DeferredCommand.addCommand(new Command() {
      public void execute() {
        UiEvent event1 = (UiEvent) lookup.findEventRecord(1);
        UiEvent event3 = (UiEvent) lookup.findEventRecord(3);
        if (event1.hasJavaScriptProfile() && event3.hasJavaScriptProfile()) {
          doTestVisit(profileModel);
        } else {
          DeferredCommand.addCommand(this);
        }
      }
    });
    delayTestFinish(10000);
  }

  // After the profile data is parsed, the visit code can be exercised.
  private void doTestVisit(final JavaScriptProfileModel profileModel) {
    profileModel.visitEventsWithProfiles(new EventVisitor() {
      boolean foundEvent[] = new boolean[4];

      public void postProcess() {
        if (foundEvent[1] && foundEvent[3]) {
          finishTest();
        } else {
          fail();
        }
      }

      public void visitUiEvent(UiEvent e) {
        int sequence = e.getSequence();
        foundEvent[sequence] = true;
      }
    });

  }
}
