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
import com.google.speedtracer.client.model.EventRecord;
import com.google.speedtracer.client.model.HintRecord;
import com.google.speedtracer.hintletengine.client.HintletEventRecordBuilder;

import static com.google.speedtracer.hintletengine.client.HintletEventRecordBuilder.createNetworkDataRecieved;
import static com.google.speedtracer.hintletengine.client.HintletEventRecordBuilder.createResourceFinish;
import static com.google.speedtracer.hintletengine.client.HintletEventRecordBuilder.createResourceSendRequest;

/**
 * Tests {@link HintletTotalBytes}
 */
public class HintletTotatBytesTests extends GWTTestCase {

  private HintletTotalBytes rule;

  @Override
  public String getModuleName() {
    return "com.google.speedtracer.hintletengine.HintletEngineTest";
  }

  @Override
  protected void gwtSetUp() {
    rule = new HintletTotalBytes();
  }

  private String formatMessage(int bytes, int threshold) {
    return bytes + " bytes downloaded, exceeds threshold of " + threshold + " bytes.";
  }

  public void testOneResourceGood() {
    int size = 100;
    JSOArray<EventRecord> inputs = JSOArray.create();
    inputs.push(createResourceSendRequest("http://www.google.com"));
    inputs.push(createNetworkDataRecieved(size));
    inputs.push(createResourceFinish());
    HintletTestCase test =
        HintletTestCase.createTestCase("one small resource doesn't fire", inputs);
    HintletTestHelper.runTest(new HintletTotalBytes(), test);
  }

  public void testOneLargeResourceInfo() {
    int size = 500001;
    JSOArray<EventRecord> inputs = JSOArray.create();
    inputs.push(createResourceSendRequest("http://www.google.com"));
    inputs.push(createNetworkDataRecieved(size));
    inputs.push(createResourceFinish());
    String hintDescription = formatMessage(size, 500000);
    HintRecord expectedHint =
        HintRecord.create(rule.getHintletName(), HintletEventRecordBuilder.DEFAULT_TIME,
            HintRecord.SEVERITY_INFO, hintDescription, HintletEventRecordBuilder.DEFAULT_SEQUENCE);
    HintletTestCase test =
        HintletTestCase.createTestCase("one medium resource fires info", inputs, expectedHint);
    HintletTestHelper.runTest(rule, test);
  }

  public void testOneLargeResourceWarning() {
    int size = 1000001;
    JSOArray<EventRecord> inputs = JSOArray.create();
    inputs.push(createResourceSendRequest("http://www.google.com"));
    inputs.push(createNetworkDataRecieved(size));
    inputs.push(createResourceFinish());
    String hintDescription = formatMessage(size, 1000000);
    HintRecord expectedHint =
        HintRecord.create(rule.getHintletName(), HintletEventRecordBuilder.DEFAULT_TIME,
            HintRecord.SEVERITY_WARNING, hintDescription,
            HintletEventRecordBuilder.DEFAULT_SEQUENCE);
    HintletTestCase test =
        HintletTestCase.createTestCase("one large resource fires warning", inputs, expectedHint);
    HintletTestHelper.runTest(rule, test);
  }

  public void testMultipleRecordsGood() {
    JSOArray<EventRecord> inputs = JSOArray.create();
    inputs.push(createResourceSendRequest("http://www.google.com"));
    inputs.push(createNetworkDataRecieved(5000));
    inputs.push(createNetworkDataRecieved(100));
    inputs.push(createNetworkDataRecieved(10));
    inputs.push(createNetworkDataRecieved(1000));
    inputs.push(createResourceFinish());
    HintletTestCase test =
        HintletTestCase.createTestCase("multiple small resources don't fire", inputs);
    HintletTestHelper.runTest(rule, test);
  }

  public void testMultipleResourcesInfo() {
    JSOArray<EventRecord> inputs = JSOArray.create();
    inputs.push(createResourceSendRequest("http://www.google.com"));
    inputs.push(createNetworkDataRecieved(100000));
    inputs.push(createNetworkDataRecieved(200000));
    inputs.push(createNetworkDataRecieved(300000));
    inputs.push(createResourceFinish());
    String hintDescription = formatMessage(600000, 500000);
    HintRecord expectedHint =
        HintRecord.create(rule.getHintletName(), HintletEventRecordBuilder.DEFAULT_TIME,
            HintRecord.SEVERITY_INFO, hintDescription, HintletEventRecordBuilder.DEFAULT_SEQUENCE);
    HintletTestCase test =
        HintletTestCase.createTestCase("multiple medium resources fire info", inputs, expectedHint);
    HintletTestHelper.runTest(rule, test);
  }

  public void testMultipleResourceWarning() {
    JSOArray<EventRecord> inputs = JSOArray.create();
    inputs.push(createResourceSendRequest("http://www.google.com"));
    inputs.push(createNetworkDataRecieved(100000));
    inputs.push(createNetworkDataRecieved(500000));
    inputs.push(createNetworkDataRecieved(600000));
    inputs.push(createResourceFinish());
    String hintDescription = formatMessage(1200000, 1000000);
    HintRecord expectedHint =
        HintRecord.create(rule.getHintletName(), HintletEventRecordBuilder.DEFAULT_TIME,
            HintRecord.SEVERITY_WARNING, hintDescription,
            HintletEventRecordBuilder.DEFAULT_SEQUENCE);
    HintletTestCase test = HintletTestCase.createTestCase(
        "multiple large resources fire warning", inputs, expectedHint);
    HintletTestHelper.runTest(rule, test);
  }
}
