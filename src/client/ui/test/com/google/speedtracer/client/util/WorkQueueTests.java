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
package com.google.speedtracer.client.util;

import com.google.gwt.junit.client.GWTTestCase;
import com.google.speedtracer.client.Logging;
import com.google.speedtracer.client.util.WorkQueue.Node;

/**
 * Tests the WorkQueue class
 */
public class WorkQueueTests extends GWTTestCase {

  private int workOrder = 0;
  private int sum = 0;

  private class PrependWorkNode implements Node {
    private int value;

    public PrependWorkNode(int value) {
      this.value = value;
    }

    public void execute() {
      workOrder++;
      sum += workOrder * value;
    }

    public String getDescription() {
      return "prepend worker";
    }
  }

  @Override
  public String getModuleName() {
    return "com.google.speedtracer.Common";
  }

  @Override
  protected void gwtSetUp() throws Exception {
    Logging.createListenerLogger(null);
  }

  public void testWorkQueue1() {
    delayTestFinish(2000);
    WorkQueue wq = new WorkQueue();
    wq.append(new Node() {
      public void execute() {
        int expectedValue = 1 * 10 + 2 * 100 + 3 * 1000 + 4 * 10000 + 5
            * 100000;
        assertEquals("Sum", expectedValue, sum);
        finishTest();
      }

      public String getDescription() {
        return "sum worker";
      }
    });

    wq.prepend(new PrependWorkNode(100000));
    wq.prepend(new PrependWorkNode(10000));
    wq.prepend(new PrependWorkNode(1000));
    wq.prepend(new PrependWorkNode(100));
    wq.prepend(new PrependWorkNode(10));
  }
}
