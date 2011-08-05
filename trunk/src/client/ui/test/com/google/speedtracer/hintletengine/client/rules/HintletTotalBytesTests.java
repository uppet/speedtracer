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
import com.google.speedtracer.client.model.HintRecord;
import com.google.speedtracer.hintletengine.client.HintletEventRecordBuilder;

import static com.google.speedtracer.hintletengine.client.HintletEventRecordBuilder.createNetworkDataRecieved;
import static com.google.speedtracer.hintletengine.client.HintletEventRecordBuilder.createResourceFinish;
import static com.google.speedtracer.hintletengine.client.HintletEventRecordBuilder.createResourceSendRequest;

/**
 * Tests {@link HintletTotalBytes}
 */
public class HintletTotalBytesTests extends GWTTestCase {

  private HintletTotalBytes rule;
  private HintletTestCase test;

  @Override
  public String getModuleName() {
    return "com.google.speedtracer.hintletengine.HintletEngineTest";
  }

  @Override
  protected void gwtSetUp() {
    rule = new HintletTotalBytes();
    test = HintletTestCase.getHintletTestCase();
  }

  private String formatMessage(int bytes, int threshold) {
    return bytes + " bytes downloaded, exceeds threshold of " + threshold + " bytes.";
  }

  /**
   * one small resource doesn't fire
   */
  public void testOneResourceGood() {
    int size = 100;
    test.addInput(createResourceSendRequest("http://www.google.com"));
    test.addInput(createNetworkDataRecieved(size));
    test.addInput(createResourceFinish());
    HintletTestHelper.runTest(rule, test);
  }

  /**
   * one medium resource fires info
   */
  public void testOneMediumResourceInfo() {
    int size = 500001;
    test.addInput(createResourceSendRequest("http://www.google.com"));
    test.addInput(createNetworkDataRecieved(size));
    test.addInput(createResourceFinish());
    
    String hintDescription = formatMessage(size, 500000);
    test.addExpectedHint(
        HintRecord.create(rule.getHintletName(), HintletEventRecordBuilder.DEFAULT_TIME,
            HintRecord.SEVERITY_INFO, hintDescription, HintletEventRecordBuilder.DEFAULT_SEQUENCE));

    HintletTestHelper.runTest(rule, test);
  }

  /**
   * one large resource fires warning
   */
  public void testOneLargeResourceWarning() {
    int size = 1000001;
    test.addInput(createResourceSendRequest("http://www.google.com"));
    test.addInput(createNetworkDataRecieved(size));
    test.addInput(createResourceFinish());
    
    String hintDescription = formatMessage(size, 1000000);
    test.addExpectedHint(
        HintRecord.create(rule.getHintletName(), HintletEventRecordBuilder.DEFAULT_TIME,
            HintRecord.SEVERITY_WARNING, hintDescription,
            HintletEventRecordBuilder.DEFAULT_SEQUENCE));

    HintletTestHelper.runTest(rule, test);
  }

  /**
   * multiple small resources don't fire
   */
  public void testMultipleRecordsGood() {
    test.addInput(createResourceSendRequest("http://www.google.com"));
    test.addInput(createNetworkDataRecieved(5000));
    test.addInput(createNetworkDataRecieved(100));
    test.addInput(createNetworkDataRecieved(10));
    test.addInput(createNetworkDataRecieved(1000));
    test.addInput(createResourceFinish());
    HintletTestHelper.runTest(rule, test);
  }

  /**
   * multiple medium resources fire info
   */
  public void testMultipleResourcesInfo() {
    test.addInput(createResourceSendRequest("http://www.google.com"));
    test.addInput(createNetworkDataRecieved(100000));
    test.addInput(createNetworkDataRecieved(200000));
    test.addInput(createNetworkDataRecieved(300000));
    test.addInput(createResourceFinish());
    
    String hintDescription = formatMessage(600000, 500000);
    test.addExpectedHint(
        HintRecord.create(rule.getHintletName(), HintletEventRecordBuilder.DEFAULT_TIME,
            HintRecord.SEVERITY_INFO, hintDescription, HintletEventRecordBuilder.DEFAULT_SEQUENCE));

    HintletTestHelper.runTest(rule, test);
  }

  /**
   * multiple large resources fire warning
   */
  public void testMultipleResourceWarning() {
    test.addInput(createResourceSendRequest("http://www.google.com"));
    test.addInput(createNetworkDataRecieved(100000));
    test.addInput(createNetworkDataRecieved(500000));
    test.addInput(createNetworkDataRecieved(600000));
    test.addInput(createResourceFinish());
    
    String hintDescription = formatMessage(1200000, 1000000);
    test.addExpectedHint(
        HintRecord.create(rule.getHintletName(), HintletEventRecordBuilder.DEFAULT_TIME,
            HintRecord.SEVERITY_WARNING, hintDescription,
            HintletEventRecordBuilder.DEFAULT_SEQUENCE));

    HintletTestHelper.runTest(rule, test);
  }
}
