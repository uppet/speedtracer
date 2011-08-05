/*
 * Copyright 2011 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.speedtracer.hintletengine.client.rules;

import com.google.gwt.coreext.client.JSOArray;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.speedtracer.client.model.DomEvent;
import com.google.speedtracer.client.model.EventRecord;
import com.google.speedtracer.client.model.HintRecord;
import com.google.speedtracer.client.model.LayoutEvent;
import com.google.speedtracer.client.model.PaintEvent;
import com.google.speedtracer.client.model.ParseHtmlEvent;
import com.google.speedtracer.hintletengine.client.HintletEventRecordBuilder;

import static com.google.speedtracer.hintletengine.client.HintletEventRecordBuilder.createDomEvent;
import static com.google.speedtracer.hintletengine.client.HintletEventRecordBuilder.createGCEvent;
import static com.google.speedtracer.hintletengine.client.HintletEventRecordBuilder.createLayoutEvent;
import static com.google.speedtracer.hintletengine.client.HintletEventRecordBuilder.createPaintEvent;
import static com.google.speedtracer.hintletengine.client.HintletEventRecordBuilder.createParseHtmlEvent;

/**
 * Tests {@link HintletFrequentLayout}.
 */
public class HintletFrequentLayoutTests extends GWTTestCase {

  private HintletFrequentLayout rule;

  @Override
  public String getModuleName() {
    return "com.google.speedtracer.hintletengine.HintletEngineTest";
  }

  @Override
  public void gwtSetUp() {
    rule = new HintletFrequentLayout();
  }

  /**
   * This trace tests data copied from a real trace
   */
  public void testRealDataWithHint() {
    
    String hintDescription = "Event triggered 12 layouts taking 120ms.";
    HintRecord expectedHint = HintRecord.create(rule.getHintletName(), 1, HintRecord.SEVERITY_WARNING, hintDescription, 1);
    
    HintletTestHelper.runTest(rule, HintletTestCase.createTestCase("Capture from a live browser triggers the rule",
        getInputsRealTrace(), expectedHint));
  }
  
  /**
   * DomEvent:200 
   *   12 each: 
   *     Layout:10 
   *     Paint:10
   */
  public void testManyLayoutsWithHint() {
    DomEvent input = createDomEvent(200);
    for (int i = 0; i < 12; i++) {
      input.addChild(createLayoutEvent(10));
      input.addChild(createPaintEvent(10));
    }

    String hintDescription = "Event triggered 12 layouts taking 120ms.";
    HintRecord expectedHint =
        HintRecord.create(rule.getHintletName(), HintletEventRecordBuilder.DEFAULT_TIME,
            HintRecord.SEVERITY_WARNING, hintDescription,
            HintletEventRecordBuilder.DEFAULT_SEQUENCE);

    HintletTestCase test = HintletTestCase.createTestCase(
        hintDescription, HintletTestCase.singleInputArray(input), expectedHint);
    HintletTestHelper.runTest(rule, test);
  }

  /**
   * DomEvent:200 
   *   GC: 200
   */
  public void testNoLayoutEventNoHint() {
    DomEvent input = createDomEvent(200);
    input.addChild(createGCEvent(200));
    HintletTestCase test =
        HintletTestCase.createTestCase("No Layout event", HintletTestCase.singleInputArray(input));
    HintletTestHelper.runTest(rule, test);
  }

  /**
   * Layout:200 
   *   Parse:50 
   *   Parse:50 
   *   Parse:50
   */
  public void testNonLayoutSubEvents1NoHint() {
    LayoutEvent input = createLayoutEvent(200);
    input.addChild(createParseHtmlEvent(50));
    input.addChild(createParseHtmlEvent(50));
    input.addChild(createParseHtmlEvent(50));
    HintletTestCase test = HintletTestCase.createTestCase(
        "sub-event time should be subtracted, so this one should not trigger",
        HintletTestCase.singleInputArray(input));
    HintletTestHelper.runTest(rule, test);
  }

  /**
   * ParseHtml:2000 
   *   Layout:50 
   *     ParseHtml:48 
   *   Layout:50 
   *     ParseHtml:48
   *   Layout:50
   */
  public void testNonLayoutSubEvents2NoHint() {
    ParseHtmlEvent input = createParseHtmlEvent(2000);
    
    LayoutEvent child1 = createLayoutEvent(50);
    child1.addChild(createParseHtmlEvent(48));
    input.addChild(child1);
    
    LayoutEvent child2 = createLayoutEvent(50);
    child2.addChild(createParseHtmlEvent(48));
    input.addChild(child2);
    
    input.addChild(createLayoutEvent(50));
    
    HintletTestCase test = HintletTestCase.createTestCase(
        "Three sub-events of 50 ms with child events of their own that eat up most of the time.", 
        HintletTestCase.singleInputArray(input));
    HintletTestHelper.runTest(rule, test);
  }

  /**
   * DomEvent:200 
   *   Layout:50 
   *     GC:45 
   *   Layout:100 
   *     GC:45 
   *   Layout:155 
   *     GC:45
   */
  public void testNonLayoutSubEvents3NoHint() {
    DomEvent input = createDomEvent(200);

    LayoutEvent child50 = createLayoutEvent(50);
    child50.addChild(createGCEvent(45));
    input.addChild(child50);

    LayoutEvent child100 = createLayoutEvent(100);
    child100.addChild(createGCEvent(45));
    input.addChild(child100);

    LayoutEvent child155 = createLayoutEvent(155);
    child155.addChild(createGCEvent(45));
    input.addChild(child155);

    String hintDescription = "Event triggered 3 layouts taking 170ms.";
    HintRecord expectedHint =
        HintRecord.create(rule.getHintletName(), HintletEventRecordBuilder.DEFAULT_TIME,
            HintRecord.SEVERITY_WARNING, hintDescription,
            HintletEventRecordBuilder.DEFAULT_SEQUENCE);

    HintletTestCase test = HintletTestCase.createTestCase(
        hintDescription, HintletTestCase.singleInputArray(input), expectedHint);
    HintletTestHelper.runTest(rule, test);
  }

  /**
   * Layout:2000
   */
  public void testSingleLayoutNoHint() {
    LayoutEvent input = createLayoutEvent(2000);
    HintletTestCase test =
        HintletTestCase.createTestCase("A single layout event should not trigger the rule",
            HintletTestCase.singleInputArray(input));
    HintletTestHelper.runTest(rule, test);
  }

  /**
   * Paint:2000 
   *   Layout:50 
   *   Layout:50 
   *   Layout:50
   */
  public void testThreeLayoutsWithHint() {
    PaintEvent input = createPaintEvent(2000);
    LayoutEvent child = createLayoutEvent(50);
    input.addChild(child);
    input.addChild(child);
    input.addChild(child);

    String hintDescription = "Event triggered 3 layouts taking 150ms.";
    HintRecord expectedHint =
        HintRecord.create(rule.getHintletName(), HintletEventRecordBuilder.DEFAULT_TIME,
            HintRecord.SEVERITY_WARNING, hintDescription,
            HintletEventRecordBuilder.DEFAULT_SEQUENCE);

    HintletTestCase test = HintletTestCase.createTestCase(
        hintDescription, HintletTestCase.singleInputArray(input), expectedHint);
    HintletTestHelper.runTest(rule, test);
  }
  

  private native static JSOArray<EventRecord> getInputsRealTrace()/*-{
    return [
        {
          "data" : {
            "type" : "click"
          },
          "children" : [
            {
              "data" : {
                "scriptName" : "",
                "scriptLine" : 176
              },
              "children" : [
                {
                  "data" : {
                    "message" : "About to Build Table"
                  },
                  "type" : @com.google.speedtracer.shared.EventRecordType::LOG_MESSAGE_EVENT,
                  "usedHeapSize" : 2651416,
                  "totalHeapSize" : 4878592,
                  "time" : 368.94287109375
                },
                {
                  "data" : {
                    "length" : 0,
                    "startLine" : 0,
                    "endLine" : 0
                  },
                  "children" : [],
                  "type" : @com.google.speedtracer.shared.EventRecordType::PARSE_HTML_EVENT,
                  "usedHeapSize" : 2674168,
                  "totalHeapSize" : 4878592,
                  "duration" : 0.02587890625,
                  "time" : 369.0859375
                },
                {
                  "data" : {
                    "length" : 1,
                    "startLine" : 0,
                    "endLine" : 0
                  },
                  "children" : [],
                  "type" : @com.google.speedtracer.shared.EventRecordType::PARSE_HTML_EVENT,
                  "usedHeapSize" : 2674544,
                  "totalHeapSize" : 4878592,
                  "duration" : 0.02099609375,
                  "time" : 369.115966796875
                },
                {
                  "data" : {
                    "length" : 4,
                    "startLine" : 0,
                    "endLine" : 0
                  },
                  "children" : [],
                  "type" : @com.google.speedtracer.shared.EventRecordType::PARSE_HTML_EVENT,
                  "usedHeapSize" : 2717728,
                  "totalHeapSize" : 4878592,
                  "duration" : 0.02783203125,
                  "time" : 369.387939453125
                },
                {
                  "data" : {
                    "length" : 1,
                    "startLine" : 0,
                    "endLine" : 0
                  },
                  "children" : [],
                  "type" : @com.google.speedtracer.shared.EventRecordType::PARSE_HTML_EVENT,
                  "usedHeapSize" : 2718104,
                  "totalHeapSize" : 4878592,
                  "duration" : 0.02001953125,
                  "time" : 369.4189453125
                },
                {
                  "data" : {
                    "length" : 7,
                    "startLine" : 0,
                    "endLine" : 0
                  },
                  "children" : [],
                  "type" : @com.google.speedtracer.shared.EventRecordType::PARSE_HTML_EVENT,
                  "usedHeapSize" : 2720056,
                  "totalHeapSize" : 4878592,
                  "duration" : 0.02587890625,
                  "time" : 369.473876953125
                },
                {
                  "data" : {
                    "length" : 1,
                    "startLine" : 0,
                    "endLine" : 0
                  },
                  "children" : [],
                  "type" : @com.google.speedtracer.shared.EventRecordType::PARSE_HTML_EVENT,
                  "usedHeapSize" : 2720432,
                  "totalHeapSize" : 4878592,
                  "duration" : 0.02001953125,
                  "time" : 369.501953125
                },
                {
                  "data" : {
                    "length" : 4,
                    "startLine" : 0,
                    "endLine" : 0
                  },
                  "children" : [],
                  "type" : @com.google.speedtracer.shared.EventRecordType::PARSE_HTML_EVENT,
                  "usedHeapSize" : 2722184,
                  "totalHeapSize" : 4878592,
                  "duration" : 0.023681640625,
                  "time" : 369.547119140625
                },
                {
                  "data" : {
                    "length" : 1,
                    "startLine" : 0,
                    "endLine" : 0
                  },
                  "children" : [],
                  "type" : @com.google.speedtracer.shared.EventRecordType::PARSE_HTML_EVENT,
                  "usedHeapSize" : 2722560,
                  "totalHeapSize" : 4878592,
                  "duration" : 0.01904296875,
                  "time" : 369.573974609375
                },
                {
                  "data" : {
                    "length" : 6,
                    "startLine" : 0,
                    "endLine" : 0
                  },
                  "children" : [],
                  "type" : @com.google.speedtracer.shared.EventRecordType::PARSE_HTML_EVENT,
                  "usedHeapSize" : 2723016,
                  "totalHeapSize" : 4878592,
                  "duration" : 0.02099609375,
                  "time" : 369.60400390625
                },
                {
                  "data" : {
                    "length" : 1,
                    "startLine" : 0,
                    "endLine" : 0
                  },
                  "children" : [],
                  "type" : @com.google.speedtracer.shared.EventRecordType::PARSE_HTML_EVENT,
                  "usedHeapSize" : 2723392,
                  "totalHeapSize" : 4878592,
                  "duration" : 0.02001953125,
                  "time" : 369.626953125
                },
                {
                  "data" : {
                    "length" : 10,
                    "startLine" : 0,
                    "endLine" : 0
                  },
                  "children" : [],
                  "type" : @com.google.speedtracer.shared.EventRecordType::PARSE_HTML_EVENT,
                  "usedHeapSize" : 2723848,
                  "totalHeapSize" : 4878592,
                  "duration" : 0.02099609375,
                  "time" : 369.656982421875
                },
                {
                  "data" : {
                    "length" : 1,
                    "startLine" : 0,
                    "endLine" : 0
                  },
                  "children" : [],
                  "type" : @com.google.speedtracer.shared.EventRecordType::PARSE_HTML_EVENT,
                  "usedHeapSize" : 2724224,
                  "totalHeapSize" : 4878592,
                  "duration" : 0.019287109375,
                  "time" : 369.6796875
                },
                {
                  "data" : {
                    "length" : 7,
                    "startLine" : 0,
                    "endLine" : 0
                  },
                  "children" : [],
                  "type" : @com.google.speedtracer.shared.EventRecordType::PARSE_HTML_EVENT,
                  "usedHeapSize" : 2724680,
                  "totalHeapSize" : 4878592,
                  "duration" : 0.02197265625,
                  "time" : 369.7080078125
                },
                {
                  "data" : {
                    "length" : 1,
                    "startLine" : 0,
                    "endLine" : 0
                  },
                  "children" : [],
                  "type" : @com.google.speedtracer.shared.EventRecordType::PARSE_HTML_EVENT,
                  "usedHeapSize" : 2725056,
                  "totalHeapSize" : 4878592,
                  "duration" : 0.01904296875,
                  "time" : 369.73193359375
                },
                {
                  "data" : {
                    "length" : 21,
                    "startLine" : 0,
                    "endLine" : 0
                  },
                  "children" : [],
                  "type" : @com.google.speedtracer.shared.EventRecordType::PARSE_HTML_EVENT,
                  "usedHeapSize" : 2725512,
                  "totalHeapSize" : 4878592,
                  "duration" : 0.02099609375,
                  "time" : 369.760009765625
                },
                {
                  "data" : {
                    "length" : 1,
                    "startLine" : 0,
                    "endLine" : 0
                  },
                  "children" : [],
                  "type" : @com.google.speedtracer.shared.EventRecordType::PARSE_HTML_EVENT,
                  "usedHeapSize" : 2725888,
                  "totalHeapSize" : 4878592,
                  "duration" : 0.018798828125,
                  "time" : 369.783935546875
                },
                {
                  "data" : {},
                  "children" : [],
                  "type" : @com.google.speedtracer.shared.EventRecordType::RECALC_STYLE_EVENT,
                  "usedHeapSize" : 2744504,
                  "totalHeapSize" : 4878592,
                  "duration" : 0.089111328125,
                  "time" : 370.02685546875
                },
                {
                  "data" : {},
                  "children" : [],
                  "type" : @com.google.speedtracer.shared.EventRecordType::LAYOUT_EVENT,
                  "usedHeapSize" : 2744880,
                  "totalHeapSize" : 4878592,
                  "duration" : 10,
                  "time" : 370.117919921875
                },
                {
                  "data" : {},
                  "children" : [],
                  "type" : @com.google.speedtracer.shared.EventRecordType::RECALC_STYLE_EVENT,
                  "usedHeapSize" : 2750408,
                  "totalHeapSize" : 4878592,
                  "duration" : 0.041015625,
                  "time" : 370.332763671875
                },
                {
                  "data" : {},
                  "children" : [],
                  "type" : @com.google.speedtracer.shared.EventRecordType::LAYOUT_EVENT,
                  "usedHeapSize" : 2750784,
                  "totalHeapSize" : 4878592,
                  "duration" : 10,
                  "time" : 370.376953125
                },
                {
                  "data" : {},
                  "children" : [],
                  "type" : @com.google.speedtracer.shared.EventRecordType::RECALC_STYLE_EVENT,
                  "usedHeapSize" : 2753016,
                  "totalHeapSize" : 4878592,
                  "duration" : 0.041015625,
                  "time" : 370.48681640625
                },
                {
                  "data" : {},
                  "children" : [],
                  "type" : @com.google.speedtracer.shared.EventRecordType::LAYOUT_EVENT,
                  "usedHeapSize" : 2753392,
                  "totalHeapSize" : 4878592,
                  "duration" : 10,
                  "time" : 370.531005859375
                },
                {
                  "data" : {},
                  "children" : [],
                  "type" : @com.google.speedtracer.shared.EventRecordType::RECALC_STYLE_EVENT,
                  "usedHeapSize" : 2754264,
                  "totalHeapSize" : 4878592,
                  "duration" : 0.0419921875,
                  "time" : 370.64306640625
                },
                {
                  "data" : {},
                  "children" : [],
                  "type" : @com.google.speedtracer.shared.EventRecordType::LAYOUT_EVENT,
                  "usedHeapSize" : 2754640,
                  "totalHeapSize" : 4878592,
                  "duration" : 10,
                  "time" : 370.686767578125
                },
                {
                  "data" : {},
                  "children" : [],
                  "type" : @com.google.speedtracer.shared.EventRecordType::RECALC_STYLE_EVENT,
                  "usedHeapSize" : 2755520,
                  "totalHeapSize" : 4878592,
                  "duration" : 0.0771484375,
                  "time" : 370.769775390625
                },
                {
                  "data" : {},
                  "children" : [],
                  "type" : @com.google.speedtracer.shared.EventRecordType::LAYOUT_EVENT,
                  "usedHeapSize" : 2755896,
                  "totalHeapSize" : 4878592,
                  "duration" : 10,
                  "time" : 370.849853515625
                },
                {
                  "data" : {},
                  "children" : [],
                  "type" : @com.google.speedtracer.shared.EventRecordType::RECALC_STYLE_EVENT,
                  "usedHeapSize" : 2756768,
                  "totalHeapSize" : 4878592,
                  "duration" : 0.0400390625,
                  "time" : 370.93798828125
                },
                {
                  "data" : {},
                  "children" : [],
                  "type" : @com.google.speedtracer.shared.EventRecordType::LAYOUT_EVENT,
                  "usedHeapSize" : 2757144,
                  "totalHeapSize" : 4878592,
                  "duration" : 10,
                  "time" : 370.98583984375
                },
                {
                  "data" : {},
                  "children" : [],
                  "type" : @com.google.speedtracer.shared.EventRecordType::RECALC_STYLE_EVENT,
                  "usedHeapSize" : 2758704,
                  "totalHeapSize" : 4878592,
                  "duration" : 0.099365234375,
                  "time" : 371.104736328125
                },
                {
                  "data" : {},
                  "children" : [],
                  "type" : @com.google.speedtracer.shared.EventRecordType::LAYOUT_EVENT,
                  "usedHeapSize" : 2759080,
                  "totalHeapSize" : 4878592,
                  "duration" : 10,
                  "time" : 371.205810546875
                },
                {
                  "data" : {},
                  "children" : [],
                  "type" : @com.google.speedtracer.shared.EventRecordType::RECALC_STYLE_EVENT,
                  "usedHeapSize" : 2759872,
                  "totalHeapSize" : 4878592,
                  "duration" : 0.043212890625,
                  "time" : 371.372802734375
                },
                {
                  "data" : {},
                  "children" : [],
                  "type" : @com.google.speedtracer.shared.EventRecordType::LAYOUT_EVENT,
                  "usedHeapSize" : 2760248,
                  "totalHeapSize" : 4878592,
                  "duration" : 10,
                  "time" : 371.41796875
                },
                {
                  "data" : {},
                  "children" : [],
                  "type" : @com.google.speedtracer.shared.EventRecordType::RECALC_STYLE_EVENT,
                  "usedHeapSize" : 2761040,
                  "totalHeapSize" : 4878592,
                  "duration" : 0.0419921875,
                  "time" : 371.505859375
                },
                {
                  "data" : {},
                  "children" : [],
                  "type" : @com.google.speedtracer.shared.EventRecordType::LAYOUT_EVENT,
                  "usedHeapSize" : 2761416,
                  "totalHeapSize" : 4878592,
                  "duration" : 10,
                  "time" : 371.550048828125
                },
                {
                  "data" : {},
                  "children" : [],
                  "type" : @com.google.speedtracer.shared.EventRecordType::RECALC_STYLE_EVENT,
                  "usedHeapSize" : 2762208,
                  "totalHeapSize" : 4878592,
                  "duration" : 0.041015625,
                  "time" : 371.637939453125
                },
                {
                  "data" : {},
                  "children" : [],
                  "type" : @com.google.speedtracer.shared.EventRecordType::LAYOUT_EVENT,
                  "usedHeapSize" : 2762584,
                  "totalHeapSize" : 4878592,
                  "duration" : 10,
                  "time" : 371.681884765625
                },
                {
                  "data" : {},
                  "children" : [],
                  "type" : @com.google.speedtracer.shared.EventRecordType::RECALC_STYLE_EVENT,
                  "usedHeapSize" : 2763352,
                  "totalHeapSize" : 4878592,
                  "duration" : 0.041015625,
                  "time" : 371.768798828125
                },
                {
                  "data" : {},
                  "children" : [],
                  "type" : @com.google.speedtracer.shared.EventRecordType::LAYOUT_EVENT,
                  "usedHeapSize" : 2763728,
                  "totalHeapSize" : 4878592,
                  "duration" : 10,
                  "time" : 371.81298828125
                },
                {
                  "data" : {},
                  "children" : [],
                  "type" : @com.google.speedtracer.shared.EventRecordType::RECALC_STYLE_EVENT,
                  "usedHeapSize" : 2764520,
                  "totalHeapSize" : 4878592,
                  "duration" : 0.041015625,
                  "time" : 371.89990234375
                },
                {
                  "data" : {},
                  "children" : [],
                  "type" : @com.google.speedtracer.shared.EventRecordType::LAYOUT_EVENT,
                  "usedHeapSize" : 2764896,
                  "totalHeapSize" : 4878592,
                  "duration" : 10,
                  "time" : 371.94384765625
                },
                {
                  "data" : {
                    "message" : "Table Finished"
                  },
                  "type" : @com.google.speedtracer.shared.EventRecordType::LOG_MESSAGE_EVENT,
                  "usedHeapSize" : 2770680,
                  "totalHeapSize" : 4878592,
                  "time" : 372.10595703125
                }
              ],
              "type" : @com.google.speedtracer.shared.EventRecordType::JAVASCRIPT_EXECUTION,
              "usedHeapSize" : 2770680,
              "totalHeapSize" : 4878592,
              "duration" : 3.426025390625,
              "time" : 368.700927734375
            }
          ],
          "type" : @com.google.speedtracer.shared.EventRecordType::DOM_EVENT,
          "usedHeapSize" : 2770680,
          "totalHeapSize" : 4878592,
          "duration" : 3.969970703125,
          "time" : 1,
          "sequence" : 1
        }
      ];
  }-*/;

}
