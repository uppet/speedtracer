/*
 * Copyright 2009 Google Inc.
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
package com.google.speedtracer.client.visualizations.view;

import com.google.gwt.junit.client.GWTTestCase;
import com.google.speedtracer.client.model.AggregateTimeVisitor;
import com.google.speedtracer.client.model.LayoutEvent;
import com.google.speedtracer.client.model.ParseHtmlEvent;
import com.google.speedtracer.client.model.UiEvent;

/**
 * Tests for {@link EventFilter}.
 */
public class EventFilterTests extends GWTTestCase {

  @Override
  public String getModuleName() {
    return "com.google.speedtracer.Common";
  }

  public void testEventFilterDuration() {
    EventFilter filter = new EventFilter();

    filter.setMinDuration(10);
    UiEvent uiEvent = createUiEventDuration(LayoutEvent.TYPE, 1.0);
    assertTrue("1 ms event ", filter.shouldFilter(uiEvent));
    uiEvent = createUiEventDuration(LayoutEvent.TYPE, 9.0);
    assertTrue("1 ms event ", filter.shouldFilter(uiEvent));
    uiEvent = createUiEventDuration(LayoutEvent.TYPE, 100.0);
    assertFalse("100 ms event ", filter.shouldFilter(uiEvent));
    uiEvent = createUiEventDuration(LayoutEvent.TYPE, 10.0);
    assertFalse("10 ms event ", filter.shouldFilter(uiEvent));
  }

  public void testEventFilterType() {
    EventFilter filter = new EventFilter();

    filter.setEventType(LayoutEvent.TYPE);
    UiEvent uiEvent = createUiEventDuration(LayoutEvent.TYPE, 1.0);
    assertFalse("TYPE LayoutEvent filter", filter.shouldFilter(uiEvent));

    filter = new EventFilter();
    filter.setEventType(ParseHtmlEvent.TYPE);
    assertTrue("TYPE ParseHtmlEvent filter", filter.shouldFilter(uiEvent));
  }

  public void testEventFilterTypePercent() {
    EventFilter filter = new EventFilter();

    filter.setEventType(LayoutEvent.TYPE);
    filter.setMinEventTypePercent(.5);
    UiEvent uiEvent = createUiEventTypePercentDuration(LayoutEvent.TYPE, 10.0,
        ParseHtmlEvent.TYPE, 1.0);
    assertFalse("TYPE LayoutEvent filter", filter.shouldFilter(uiEvent));

    uiEvent = createUiEventTypePercentDuration(LayoutEvent.TYPE, 10.0,
        ParseHtmlEvent.TYPE, 9.0);
    assertTrue("TYPE LayoutEvent filter", filter.shouldFilter(uiEvent));

    filter.setEventType(ParseHtmlEvent.TYPE);
    filter.setMinEventTypePercent(.5);
    uiEvent = createUiEventTypePercentDuration(LayoutEvent.TYPE, 10.0,
        ParseHtmlEvent.TYPE, 9.0);
    assertFalse("TYPE ParseHtmlEvent filter", filter.shouldFilter(uiEvent));

    uiEvent = createUiEventTypePercentDuration(LayoutEvent.TYPE, 10.0,
        ParseHtmlEvent.TYPE, 1.0);
    assertTrue("TYPE ParseHtmlEvent filter", filter.shouldFilter(uiEvent));

    filter.setEventType(LayoutEvent.TYPE);
    filter.setMinEventTypePercent(.5);
    uiEvent = createUiEventTypePercentDuration(LayoutEvent.TYPE, 1.0,
        ParseHtmlEvent.TYPE, 1.0, LayoutEvent.TYPE, 8.0);
    assertFalse("TYPE LayoutEvent filter", filter.shouldFilter(uiEvent));

    uiEvent = createUiEventTypePercentDuration(LayoutEvent.TYPE, 10.0,
        ParseHtmlEvent.TYPE, 9.0, ParseHtmlEvent.TYPE, 1.0);
    assertTrue("TYPE LayoutEvent filter", filter.shouldFilter(uiEvent));
  }

  public void testEventHasHints() {
    EventFilter filter = new EventFilter();

    filter.setMinDuration(10);
    filter.setFilterHints(true);
    UiEvent uiEvent = createUiEventDuration(LayoutEvent.TYPE, 11.0);
    assertFalse("hintlets 1", filter.shouldFilter(uiEvent));
    setHasHints(uiEvent);
    assertFalse("hintlets 2", filter.shouldFilter(uiEvent));
    filter.setMinDuration(20);
    assertTrue("hintlets 3", filter.shouldFilter(uiEvent));
    filter.setFilterHints(false);
    assertFalse("hintlets 4", filter.shouldFilter(uiEvent));
  }

  public void testEventHasUserLogs() {
    EventFilter filter = new EventFilter();

    filter.setMinDuration(10);
    filter.setFilterUserLogs(true);
    UiEvent uiEvent = createUiEventDuration(LayoutEvent.TYPE, 11.0);
    assertFalse("user logs 1", filter.shouldFilter(uiEvent));
    setHasUserLogs(uiEvent);
    assertFalse("user logs 2", filter.shouldFilter(uiEvent));
    filter.setMinDuration(20);
    assertTrue("user logs 3", filter.shouldFilter(uiEvent));
    filter.setFilterUserLogs(false);
    assertFalse("user logs 4", filter.shouldFilter(uiEvent));
  }

  private void calcSelfTime(UiEvent uiEvent) {
    AggregateTimeVisitor.apply(uiEvent);
  }

  private UiEvent createUiEventDuration(int eventType, double duration) {
    UiEvent uiEvent = createUiEventDurationNative(eventType, duration);
    calcSelfTime(uiEvent);
    return uiEvent;
  }

  private native UiEvent createUiEventDurationNative(int eventType,
      double duration) /*-{
    return {
    'children':[],
    'data':{'creationTime':1240951618036.53,'windowEventType':'focus'},
    'duration':duration,'time':694.3199999928474,'type':eventType};
  }-*/;

  private UiEvent createUiEventTypePercentDuration(int outerType,
      double durationOuter, int childType, double durationChild) {
    UiEvent uiEvent = createUiEventTypePercentNative(outerType, durationOuter,
        childType, durationChild);
    calcSelfTime(uiEvent);
    return uiEvent;
  }

  private UiEvent createUiEventTypePercentDuration(int outerType,
      double durationOuter, int childType, double durationChild,
      int grandChildType, double durationGrandChild) {
    UiEvent uiEvent = createUiEventTypePercentNative(outerType, durationOuter,
        childType, durationChild, grandChildType, durationGrandChild);
    calcSelfTime(uiEvent);
    return uiEvent;
  }

  private native UiEvent createUiEventTypePercentNative(int outerType,
      double durationOuter, int childType, double durationChild) /*-{
    return { 'children':[{'time':694.319999,'duration':durationChild,'type':childType}],
    'data':{'creationTime':1240951618036.53,'windowEventType':'focus'},
    'duration':durationOuter,'time':694.3199999928474,'type':outerType};
  }-*/;

  private native UiEvent createUiEventTypePercentNative(int outerType,
      double durationOuter, int childType, double durationChild,
      int grandChildType, double durationGrandChild) /*-{
    return { 'children':[{'children':[{'time':694.319999,'duration':durationGrandChild,'type':grandChildType}], 
    'time':694.319999,'duration':durationChild,'type':childType}],
    'data':{'creationTime':1240951618036.53,'windowEventType':'focus'},
    'duration':durationOuter,'time':694.3199999928474,'type':outerType};
  }-*/;

  private native UiEvent setHasHints(UiEvent uiEvent) /*-{
    uiEvent.hints = [{bogusHintlet:'bogusHintlet'}];
  }-*/;

  private native UiEvent setHasUserLogs(UiEvent uiEvent) /*-{
    uiEvent.hasUserLogs = true;
  }-*/;
}
