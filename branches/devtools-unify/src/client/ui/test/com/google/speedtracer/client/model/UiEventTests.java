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

import com.google.gwt.core.client.JsArrayNumber;
import com.google.gwt.coreext.client.JSOArray;
import com.google.gwt.junit.client.GWTTestCase;

/**
 * Tests {@link UiEvent}.
 */
public class UiEventTests extends GWTTestCase {

  private static native UiEvent createMockUiEvent() /*-{
    return {
      time: 0,
      duration: 10,
      type: 1,
      children: [
        {
          time: 1,
          duration: 2,
          type: 1,
          children: [
            {
              time: 1.5,
              duration: .5,
              type: 1,
              children: []
            }          
          ]
        },
        {
          time: 4,
          duration: 2,
          type: 1,
          children: []
        },
        {
          time: 7,
          duration: 2,
          type: 1,
          children: [
            {
              time: 8,
              duration: .2,
              type: 1,
              children: []
            },
            {
              time: 8.3,
              duration: .2,
              type: 1,
              children: []
            },
            {
              time: 8.6,
              duration: .1,
              type: 1,
              children: []
            }                
          ]        
        },
      ]
    };
  }-*/;

  @Override
  public String getModuleName() {
    return "com.google.speedtracer.Common";
  }

  /**
   * Tests
   * {@link UiEvent#apply(com.google.speedtracer.client.model.UiEvent.LeafFirstTraversal)}
   * .
   */
  public void testObjectTraversal() {
    // Change this to work with UiEvents directly.
    final UiEvent event = createMockUiEvent();
    final UiEvent result = event.apply(new UiEvent.LeafFirstTraversal<UiEvent>() {
      public UiEvent visit(UiEvent event, JSOArray<UiEvent> values) {
        final JSOArray<UiEvent> children = event.getChildren();
        assertEquals(children.size(), values.size());
        for (int i = 0, n = children.size(); i < n; ++i) {
          assertEquals(values.get(i), children.get(i));
        }
        return event;
      }
    });
    assertEquals(event, result);
  }

  /**
   * Tests
   * {@link UiEvent#apply(com.google.speedtracer.client.model.UiEvent.LeafFirstTraversalNumber)}
   * .
   */
  public void testNumberTraversal() {
    final UiEvent event = createMockUiEvent();
    final double result = event.apply(new UiEvent.LeafFirstTraversalNumber() {
      public double visit(UiEvent event, JsArrayNumber values) {
        final JSOArray<UiEvent> children = event.getChildren();
        assertEquals(children.size(), values.length());
        for (int i = 0, n = children.size(); i < n; ++i) {
          assertEquals(values.get(i), children.get(i).getDuration());
        }
        return event.getDuration();
      }
    });
    assertEquals(10, result, 0.001);
  }

  /**
   * Tests
   * {@link UiEvent#apply(com.google.speedtracer.client.model.UiEvent.LeafFirstTraversalVoid)}
   * .
   */
  public void testVoidTraveral() {
    final UiEvent event = createMockUiEvent();
    final double[] result = new double[1];
    event.apply(new UiEvent.LeafFirstTraversalVoid() {
      public void visit(UiEvent event) {
        result[0] += event.getDuration();
      }
    });
    assertEquals(17, result[0], 0.001);
  }
}
