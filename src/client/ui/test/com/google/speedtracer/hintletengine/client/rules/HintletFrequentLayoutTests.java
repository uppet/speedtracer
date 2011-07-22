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

import com.google.gwt.junit.client.GWTTestCase;

/**
 * Tests {@link HintletFrequentLayout}.
 */
public class HintletFrequentLayoutTests extends GWTTestCase {

  @Override
  public String getModuleName() {
    return "com.google.speedtracer.hintletengine.HintletEngineTest";
  }

  public void testRealDataWithHint() {
    HintletTestHelper.runTest(new HintletFrequentLayout(), getTestCaseRealDataWithHint());
  }

  public void testNoLayoutEventNoHint() {
    HintletTestHelper.runTest(new HintletFrequentLayout(), getTestCaseNoLayoutEventNoHint());
  }

  public void testNonLayoutSubEvents1NoHint() {
    HintletTestHelper.runTest(new HintletFrequentLayout(), getTestCaseNonLayoutSubEvents1NoHint());
  }

  public void testNonLayoutSubEvents2NoHint() {
    HintletTestHelper.runTest(new HintletFrequentLayout(), getTestCaseNonLayoutSubEvents2NoHint());
  }

  public void testNonLayoutSubEvents3NoHint() {
    HintletTestHelper.runTest(new HintletFrequentLayout(), getTestCaseNonLayoutSubEvents3NoHint());
  }

  public void testSingleLayoutNoHint() {
    HintletTestHelper.runTest(new HintletFrequentLayout(), getTestCaseSingleLayoutNoHint());
  }

  public void testThreeLayoutsWithHint() {
    HintletTestHelper.runTest(new HintletFrequentLayout(), getTestCaseThreeLayoutsWithHint());
  }

  private native static HintletTestCase getTestCaseRealDataWithHint()/*-{
    return {
      "inputs" : [
        {
          "data" : {
            "type" : "click"
          },
          "children" : [
            {
              "data" : {
                "scriptName" : "file:///usr/local/google/users/sarahgsmith/starter_proj/notes/frequent_layout_example.html",
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
      ],
      "expectedHints" : [
        {
          "hintletRule" : "Frequent Layout activity",
          "timestamp" : 1,
          "description" : "Event triggered 12 layouts taking 120ms.",
          "refRecord" : 1,
          "severity" : 2
        }
      ],
      "description" : "This is a capture from a live browser that triggers the rule"
    };
  }-*/;

  private native static HintletTestCase getTestCaseSingleLayoutNoHint()/*-{
    return {
      "inputs" : [
        {
          "children" : [],
          "data" : {},
          "duration" : 2000,
          "time" : 2,
          "type" : @com.google.speedtracer.shared.EventRecordType::LAYOUT_EVENT,
          "sequence" : 2
        }
      ],
      "expectedHints" : [],
      "description" : "A single layout event should not trigger the rule"
    };
  }-*/;

  private native static HintletTestCase getTestCaseThreeLayoutsWithHint()/*-{
    return {
      "inputs" : [
        {
          "children" : [
            {
              "children" : [],
              "data" : {},
              "duration" : 50,
              "time" : 123,
              "type" : @com.google.speedtracer.shared.EventRecordType::LAYOUT_EVENT
            },
            {
              "children" : [],
              "data" : {},
              "duration" : 50,
              "time" : 123,
              "type" : @com.google.speedtracer.shared.EventRecordType::LAYOUT_EVENT
            },
            {
              "children" : [],
              "data" : {},
              "duration" : 50,
              "time" : 123,
              "type" : @com.google.speedtracer.shared.EventRecordType::LAYOUT_EVENT
            }
          ],
          "data" : {
            "x" : 1,
            "y" : 2,
            "width" : 100,
            "height" : 200
          },
          "duration" : 2000,
          "time" : 3,
          "type" : @com.google.speedtracer.shared.EventRecordType::PAINT_EVENT,
          "sequence" : 3
        }
      ],
      "expectedHints" : [
        {
          "hintletRule" : "Frequent Layout activity",
          "timestamp" : 3,
          "description" : "Event triggered 3 layouts taking 150ms.",
          "refRecord" : 3,
          "severity" : 2
        }
      ],
      "description" : "3 sub-events of 50 ms each should trigger it"
    };
  }-*/;

  private native static HintletTestCase getTestCaseNonLayoutSubEvents1NoHint()/*-{
    return {
      "inputs" : [
        {
          "children" : [
            {
              "children" : [],
              "data" : {
                "length" : 1000,
                "startLine" : 1,
                "endLine" : 2
              },
              "duration" : 50,
              "time" : 123,
              "type" : @com.google.speedtracer.shared.EventRecordType::PARSE_HTML_EVENT
            },
            {
              "children" : [],
              "data" : {
                "length" : 1000,
                "startLine" : 5,
                "endLine" : 8
              },
              "duration" : 50,
              "time" : 123,
              "type" : @com.google.speedtracer.shared.EventRecordType::PARSE_HTML_EVENT
            },
            {
              "children" : [],
              "data" : {
                "length" : 1000,
                "startLine" : 11,
                "endLine" : 20
              },
              "duration" : 50,
              "time" : 123,
              "type" : @com.google.speedtracer.shared.EventRecordType::PARSE_HTML_EVENT
            }
          ],
          "data" : {},
          "duration" : 200,
          "time" : 4,
          "type" : @com.google.speedtracer.shared.EventRecordType::LAYOUT_EVENT,
          "sequence" : 4
        }
      ],
      "expectedHints" : [],
      "description" : "sub-event time should be subtracted, so this one should not trigger"
    };
  }-*/;

  private native static HintletTestCase getTestCaseNonLayoutSubEvents2NoHint()/*-{
    return {
      "inputs" : [
        {
          "children" : [
            {
              "children" : [
                {
                  "children" : [],
                  "data" : {
                    "length" : 1000,
                    "startLine" : 11,
                    "endLine" : 20
                  },
                  "duration" : 48,
                  "time" : 123,
                  "type" : @com.google.speedtracer.shared.EventRecordType::PARSE_HTML_EVENT
                }
              ],
              "data" : {},
              "duration" : 50,
              "time" : 123,
              "type" : @com.google.speedtracer.shared.EventRecordType::LAYOUT_EVENT
            },
            {
              "children" : [
                {
                  "children" : [],
                  "data" : {
                    "length" : 1000,
                    "startLine" : 23,
                    "endLine" : 25

                  },
                  "duration" : 48,
                  "time" : 123,
                  "type" : @com.google.speedtracer.shared.EventRecordType::PARSE_HTML_EVENT
                }
              ],
              "data" : {},
              "duration" : 50,
              "time" : 123,
              "type" : @com.google.speedtracer.shared.EventRecordType::LAYOUT_EVENT
            },
            {
              "children" : [],
              "data" : {},
              "duration" : 50,
              "time" : 123,
              "type" : @com.google.speedtracer.shared.EventRecordType::LAYOUT_EVENT
            }
          ],
          "data" : {
            "length" : 1000,
            "startLine" : 61,
            "endLine" : 65
          },
          "duration" : 2000,
          "time" : 5,
          "type" : @com.google.speedtracer.shared.EventRecordType::PARSE_HTML_EVENT,
          "sequence" : 5
        }
      ],
      "expectedHints" : [],
      "description" : "Three sub-events of 50 ms with child events of their own that eat up most of the time."
    };
  }-*/;

  private native static HintletTestCase getTestCaseNonLayoutSubEvents3NoHint()/*-{
    return {
      "inputs" : [
        {
          "children" : [
            {
              "children" : [
                {
                  "children" : [],
                  "data" : {
                    "usedHeapSizeDelta" : 600
                  },
                  "duration" : 45,
                  "time" : 6,
                  "type" : @com.google.speedtracer.shared.EventRecordType::GC_EVENT
                }
              ],
              "data" : {},
              "duration" : 50,
              "time" : 5,
              "type" : @com.google.speedtracer.shared.EventRecordType::LAYOUT_EVENT
            },
            {
              "children" : [
                {
                  "children" : [],
                  "data" : {
                    "usedHeapSizeDelta" : 600
                  },
                  "duration" : 45,
                  "time" : 106,
                  "type" : @com.google.speedtracer.shared.EventRecordType::GC_EVENT
                }
              ],
              "data" : {},
              "duration" : 100,
              "time" : 5,
              "type" : @com.google.speedtracer.shared.EventRecordType::LAYOUT_EVENT
            },
            {
              "children" : [
                {
                  "children" : [],
                  "data" : {
                    "usedHeapSizeDelta" : 600
                  },
                  "duration" : 45,
                  "time" : 156,
                  "type" : @com.google.speedtracer.shared.EventRecordType::GC_EVENT
                }
              ],
              "data" : {},
              "duration" : 50,
              "time" : 155,
              "type" : @com.google.speedtracer.shared.EventRecordType::LAYOUT_EVENT
            }
          ],
          "data" : {
            "type" : ""
          },
          "duration" : 200,
          "time" : 6,
          "type" : @com.google.speedtracer.shared.EventRecordType::DOM_EVENT,
          "sequence" : 6
        }
      ],
      "expectedHints" : [],
      "description" : "Three layouts with large aggregate child events"
    };
  }-*/;

  private native static HintletTestCase getTestCaseNoLayoutEventNoHint()/*-{
    return {
      "inputs" : [
        {
          "children" : [
            {
              "children" : [],
              "data" : {
                "usedHeapSizeDelta" :600
              },
              "duration" : 200,
              "time" : 3,
              "type" : @com.google.speedtracer.shared.EventRecordType::GC_EVENT
            }
          ],
          "data" : {
            "type" : "",
          },
          "duration" : 200,
          "time" : 7,
          "type" : @com.google.speedtracer.shared.EventRecordType::DOM_EVENT,
          "sequence" : 7
        }
      ],
      "expectedHints" : [],
      "description" : "No Layout event"
    };
  }-*/;
}
