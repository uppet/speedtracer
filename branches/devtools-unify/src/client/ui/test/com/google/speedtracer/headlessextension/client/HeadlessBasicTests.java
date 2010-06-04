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
package com.google.speedtracer.headlessextension.client;

import com.google.gwt.core.client.JavaScriptException;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Style.BorderStyle;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.user.client.Timer;
import com.google.speedtracer.client.HeadlessApi;
import com.google.speedtracer.client.HeadlessApi.MonitoringCallback;

import java.util.ArrayList;
import java.util.List;

/**
 * Tests for the Headless Extension. These will have to be run against Chrome
 * with the headless extension installed.
 * 
 * The design of this test class is a little bit unusual. It uses a state
 * machine in order to make it easer to re-use the logic for starting up the API
 * and retrieving data. Each test can be run standalone, but some of the tests
 * are simply subsets of the larger tests.
 * 
 * The state machine function {{@link #runStateMachine(Transition)} contains a
 * switch statement indicating what to do when running each state. Each state
 * executes its logic, and then returns a transition name back to the state
 * machine. The state machine uses the transitions to lookup in a table to
 * determine how to proceed from one state to the next.
 */
public class HeadlessBasicTests extends GWTTestCase {
  enum State {
    IDLE, LOAD_API, START_MONITORING, STOP_MONITORING, GET_DUMP, // wrap to next
    // line
    ANALYZE_DUMP_1, MAKE_TIMELINE_DATA, TEST_SUCESSFUL, WAITING_FOR_API_LOAD
  }

  enum Transition {
    START, ACTION_COMPLETE, ACTION_FALIED, WAITING, TERMINATE
  }

  /**
   * TODO(zundel): I'm sure there is a more OO way to create a state machine.
   * This is pretty much a straight port from how I've done it in "C".
   */
  private class StateTable {
    private final List<StateTableEntry> transitions = new ArrayList<StateTableEntry>();
    private final DivElement statusDiv = Document.get().createDivElement();
    private final String name;
    private int transitionCount = 0;

    public StateTable(String name) {
      this.name = name;
      statusDiv.getStyle().setBorderColor("#aa0");
      statusDiv.getStyle().setBorderWidth(1, Unit.PX);
      statusDiv.getStyle().setBorderStyle(BorderStyle.SOLID);
      statusDiv.setInnerHTML("<h1>Test: " + name + "</h1>");
      Document.get().getBody().appendChild(statusDiv);
    }

    public void add(State current, Transition transition, State next) {
      transitions.add(new StateTableEntry(current, transition, next));
    }

    public State nextState(State current, Transition transition) {
      // Default to staying in the same state.
      State next = null;
      for (StateTableEntry entry : transitions) {
        if (entry.currentState == current && entry.transition == transition) {
          next = entry.nextState;
          break;
        }
      }

      assertNotNull("Missing state transition in table: " + name + " state: "
          + current.name() + " transition: " + transition.name(), next);

      // Update an indicator in the browser for monitoring the test.
      statusDiv.setInnerHTML(statusDiv.getInnerHTML()
          + "<div style='padding-left: 15px;'> currentState: " + current.name()
          + " transition: " + transition.name() + " nextState: " + next.name()
          + " count: " + transitionCount++ + "</div>");

      return next;
    }

    public void updateStatus(State current, Transition transition,
        Throwable throwable) {
      statusDiv.setInnerHTML(statusDiv.getInnerHTML()
          + "<div sytle='padding-left: 15px'>Results: currentState: "
          + current.name() + " transition: " + transition.name() + "</div>"
          + (throwable == null ? "" : throwable.toString()));
    }

    public void updateStatus(State current, Transition transition) {
      updateStatus(current, transition, null);
    }
  }

  private static class StateTableEntry {
    State currentState;
    Transition transition;
    State nextState;

    public StateTableEntry(State current, Transition transition, State next) {
      this.currentState = current;
      this.transition = transition;
      this.nextState = next;
    }
  }

  private static final int ASYNC_WAIT_MS = 10000;
  private State currentState = State.IDLE;
  private String currentDump = null;
  private StateTable stateTable = null;

  @Override
  public String getModuleName() {
    return "com.google.speedtracer.headlessextension.HeadlessTests";
  }

  /**
   * Tests injecting and loading the API from the chrome extension URL. This
   * test is a subset of the other tests, but if this one passes, it tells us
   * that the extension is loaded, has the correct key, and the API is being
   * Successfully served.
   */
  public void test00LoadApi() {
    delayTestFinish(ASYNC_WAIT_MS);
    stateTable = new StateTable("ApiLoad");
    stateTable.add(State.IDLE, Transition.START, State.LOAD_API);
    stateTable.add(State.LOAD_API, Transition.ACTION_COMPLETE,
        State.WAITING_FOR_API_LOAD);
    stateTable.add(State.WAITING_FOR_API_LOAD, Transition.ACTION_COMPLETE,
        State.TEST_SUCESSFUL);

    currentState = State.IDLE;
    runStateMachine(Transition.START);
  }

  /**
   * This test will show that communication from the extension all the way back
   * to the API is working properly.
   */
  public void test01MonitoringOn() {
    delayTestFinish(ASYNC_WAIT_MS);
    stateTable = new StateTable("MonitoringOn");
    stateTable.add(State.IDLE, Transition.START, State.LOAD_API);
    stateTable.add(State.LOAD_API, Transition.ACTION_COMPLETE,
        State.WAITING_FOR_API_LOAD);
    stateTable.add(State.WAITING_FOR_API_LOAD, Transition.ACTION_COMPLETE,
        State.START_MONITORING);
    stateTable.add(State.START_MONITORING, Transition.ACTION_COMPLETE,
        State.TEST_SUCESSFUL);

    currentState = State.IDLE;
    runStateMachine(Transition.START);
  }

  /**
   * This test shows that the API is able to extract timeline data from the
   * browser and successfully send it all the way back to the page being
   * monitored.
   */
  public void test02GetDump() {
    delayTestFinish(ASYNC_WAIT_MS);
    stateTable = new StateTable("GetDump");
    stateTable.add(State.IDLE, Transition.START, State.LOAD_API);
    stateTable.add(State.LOAD_API, Transition.ACTION_COMPLETE,
        State.WAITING_FOR_API_LOAD);
    stateTable.add(State.WAITING_FOR_API_LOAD, Transition.ACTION_COMPLETE,
        State.START_MONITORING);
    stateTable.add(State.START_MONITORING, Transition.ACTION_COMPLETE,
        State.MAKE_TIMELINE_DATA);
    stateTable.add(State.MAKE_TIMELINE_DATA, Transition.ACTION_COMPLETE,
        State.STOP_MONITORING);
    stateTable.add(State.STOP_MONITORING, Transition.ACTION_COMPLETE,
        State.GET_DUMP);
    stateTable.add(State.GET_DUMP, Transition.ACTION_COMPLETE,
        State.ANALYZE_DUMP_1);
    stateTable.add(State.ANALYZE_DUMP_1, Transition.ACTION_COMPLETE,
        State.TEST_SUCESSFUL);

    currentState = State.IDLE;
    runStateMachine(Transition.START);
  }

  private Transition doAnalyzeDump1() {
    assertNotNull("Expected non-null dump", currentDump);
    assertTrue("Expected non-empty dump", currentDump.length() > 0);
    return Transition.ACTION_COMPLETE;
  }

  private Transition doGetDump() {
    currentDump = null;
    HeadlessApi.getDump(new HeadlessApi.GetDumpCallback() {
      public void callback(String dump) {
        currentDump = dump;
        runStateMachine(Transition.ACTION_COMPLETE);
      }
    });
    return Transition.WAITING;
  }

  private Transition doIdle() {
    return Transition.WAITING;
  }

  private Transition doLoadApi() {
    if (!HeadlessApi.isLoaded()) {
      HeadlessApi.loadApi();
    }
    return Transition.ACTION_COMPLETE;
  }

  /**
   * Creates some timers as a way to create activity in the browser that should
   * cause timeline data to be recorded.
   */
  private Transition doMakeTimelineData() {
    Timer t = new Timer() {
      int count = 0;

      public void run() {
        Document doc = Document.get();
        DivElement div = doc.createDivElement();
        div.setInnerHTML("<b>(dyanmically added div)</b>");
        div.getStyle().setBorderStyle(BorderStyle.SOLID);
        div.getStyle().setBorderColor("#b33");
        div.getStyle().setBorderWidth(1.0, Unit.PX);
        doc.getBody().appendChild(div);
        if (count++ > 6) {
          runStateMachine(Transition.ACTION_COMPLETE);
        } else {
          this.schedule(250);
        }
      }
    };
    t.schedule(50);
    return Transition.WAITING;
  }

  /**
   * Invokes the asynchronous 'speedTracer.startTimeline()' function and waits
   * for a response.
   */
  private Transition doStartMonitoring() {
    HeadlessApi.MonitoringOnOptions options = HeadlessApi.MonitoringOnOptions.createObject().cast();
    options.clearData();
    HeadlessApi.startMonitoring(options, new MonitoringCallback() {
      public void callback() {
        runStateMachine(Transition.ACTION_COMPLETE);
      }
    });

    return Transition.WAITING;
  }

  /**
   * Invokes the asynchronous 'speedTracer.startTimeline()' function and waits
   * for a response.
   */
  private Transition doStopMonitoring() {
    HeadlessApi.stopMonitoring(new MonitoringCallback() {
      public void callback() {
        runStateMachine(Transition.ACTION_COMPLETE);
      }
    });
    return Transition.WAITING;
  }

  /**
   * Invoked at the end of a successful test, this method reports success to
   * JUnit and cleans up the state machine for the next run.
   */
  private Transition doTestSuccessful() {
    Document doc = Document.get();
    DivElement statusDiv = doc.createDivElement();
    statusDiv.setInnerText("called finishTest()");
    doc.getBody().appendChild(statusDiv);
    finishTest();
    currentState = State.IDLE;
    return Transition.TERMINATE;
  }

  /**
   * A polling wait for the injected script to load.
   */
  private Transition doWaitingForApiLoad() {
    if (HeadlessApi.isLoaded()) {
      return Transition.ACTION_COMPLETE;
    }
    Timer t = new Timer() {
      @Override
      public void run() {
        runStateMachine(doWaitingForApiLoad());
      }
    };
    t.schedule(250);
    return Transition.WAITING;
  }

  private Transition runState() {
    Transition nextTransition = Transition.WAITING;
    switch (currentState) {
      case IDLE:
        nextTransition = doIdle();
        break;
      case LOAD_API:
        nextTransition = doLoadApi();
        break;
      case WAITING_FOR_API_LOAD:
        nextTransition = doWaitingForApiLoad();
        break;
      case START_MONITORING:
        nextTransition = doStartMonitoring();
        break;
      case MAKE_TIMELINE_DATA:
        nextTransition = doMakeTimelineData();
        break;
      case STOP_MONITORING:
        nextTransition = doStopMonitoring();
        break;
      case GET_DUMP:
        nextTransition = doGetDump();
        break;
      case ANALYZE_DUMP_1:
        nextTransition = doAnalyzeDump1();
        break;
      case TEST_SUCESSFUL:
        nextTransition = doTestSuccessful();
        break;
      default:
        fail("Missing case in switch");
    }
    return nextTransition;
  }

  /**
   * Keep running the state machine until we are told to stop or wait on an
   * asynchronous event.
   * 
   * @param transition the transition to use along with the current state to
   *          lookup the next state to go to in the state table.
   */
  private void runStateMachine(Transition transition) {
    while (transition != Transition.WAITING
        && transition != Transition.TERMINATE) {
      currentState = stateTable.nextState(currentState, transition);
      try {
        transition = runState();
        stateTable.updateStatus(currentState, transition);
      } catch (JavaScriptException ex) {
        stateTable.updateStatus(currentState, Transition.ACTION_FALIED, ex);
        fail();
      }
    }
  }
}
