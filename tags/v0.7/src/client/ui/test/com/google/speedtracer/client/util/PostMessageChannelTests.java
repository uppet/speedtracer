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
package com.google.speedtracer.client.util;

import com.google.speedtracer.client.util.PostMessageChannel.Client;
import com.google.speedtracer.client.util.PostMessageChannel.ClientListener;
import com.google.speedtracer.client.util.PostMessageChannel.Message;
import com.google.speedtracer.client.util.PostMessageChannel.Responder;
import com.google.speedtracer.client.util.PostMessageChannel.ResponseListener;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Document;
import com.google.gwt.junit.client.GWTTestCase;

/**
 * Tests {@link PostMessageChannel}.
 */
public class PostMessageChannelTests extends GWTTestCase {
  private static final int TEST_FINISH_DELAY = 30000;

  private static class EchoUntilDoneResponseListener implements
      ResponseListener {
    public void onMessageReceived(PostMessageChannel channel, Message message) {
      final String data = message.getData();
      if (DONE_MESSAGE_DATA.equals(data)) {
        message.respond(message.getData());
        channel.close();
      } else {
        message.respond(ECHO_MESSAGE_PREFIX + message.getData());
      }
    }
  }

  private static final String ECHO_MESSAGE_PREFIX = "m-";

  private static final String DONE_MESSAGE_DATA = "done";

  private static String getBlankUrl() {
    return GWT.getModuleBaseURL() + "blank.html";
  }

  private static String getEchoPeerUrl() {
    return GWT.getModuleBaseURL() + "update-channel-tests.html";
  }

  private static void sendMessagesThenDone(Client client, int n) {
    for (int i = 0; i < n; ++i) {
      client.sendMessage("" + i);
    }
    client.sendMessage("done");
  }

  @Override
  public String getModuleName() {
    return "com.google.speedtracer.Common";
  }

  public void testClientEcho() {
    final int numberOfMessages = 10;

    new Client(Document.get(), getEchoPeerUrl(), new ClientListener() {
      private int count = 0;

      public void onConnected(Client client) {
        sendMessagesThenDone(client, numberOfMessages);
      }

      public void onMessageReceived(PostMessageChannel channel, Message message) {
        final String data = message.getData();
        if (DONE_MESSAGE_DATA.equals(data)) {
          assertEquals(numberOfMessages, count);
          channel.close();
          finishTest();
        } else {
          final int i = Integer.parseInt(data);
          assertTrue(count < numberOfMessages);
          assertEquals(count++, i);
        }
      }
    });

    this.delayTestFinish(TEST_FINISH_DELAY);
  }

  public void testResponderEcho() {
    final int numberOfMessages = 10;

    new Client(Document.get(), getBlankUrl(), new ClientListener() {
      private int count = 0;

      public void onConnected(Client client) {
        new Responder(client.getFrameContentWindow(), client.getOrigin(),
            new EchoUntilDoneResponseListener());

        sendMessagesThenDone(client, numberOfMessages);
      }

      public void onMessageReceived(PostMessageChannel channel, Message message) {
        final String data = message.getData();
        if (DONE_MESSAGE_DATA.equals(data)) {
          channel.close();
          finishTest();
        } else {
          final String expected = ECHO_MESSAGE_PREFIX + (count++);
          assertEquals(expected, data);
        }
      }
    });

    delayTestFinish(TEST_FINISH_DELAY);
  }
}
