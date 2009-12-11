/*
 * Copyright 2008 Google Inc.
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
package com.google.speedtracer.client;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.IFrameElement;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.topspin.ui.client.Window;
import com.google.speedtracer.client.WindowChannel.Client;
import com.google.speedtracer.client.WindowChannel.Message;
import com.google.speedtracer.client.WindowChannel.Request;
import com.google.speedtracer.client.WindowChannel.Server;
import com.google.speedtracer.client.util.dom.WindowExt;

import java.util.ArrayList;
import java.util.List;

/**
 * Tests {@link WindowChannelServer}.
 */
public class WindowChannelTests extends GWTTestCase {
  private static final String PROPERTY_NAME = "$WindowChannel$";

  /**
   * Simple class to wrap a {@link WindowChannel.Client} and a
   * {@link WindowChannel.Listener} pair.
   */
  private static class ServerClientAndListener {
    final Client channel;
    final TestListener listener;

    ServerClientAndListener(Client channel, TestListener listener) {
      this.listener = listener;
      this.channel = channel;
    }
  }

  /**
   * A simple {@link WindowChannel.Listener} that tracks callback state.
   */
  private static class TestListener implements WindowChannel.Listener {
    boolean connected = false;
    int count = 0;

    public int getMessageCount() {
      return count;
    }

    public boolean isConnected() {
      return connected;
    }

    public void onChannelClosed(Client channel) {
      connected = false;
    }

    public void onChannelConnected(Client channel) {
      connected = true;
    }

    public void onMessage(Client channel, int type, Message data) {
      final TestMessage message = data.cast();
      assertEquals(type, message.getType());
      ++count;
    }
  }

  /**
   * A simple message that encapsulates an int.
   */
  private static final class TestMessage extends WindowChannel.Message {
    public static native TestMessage create(int type) /*-{
      return { type: type };
    }-*/;

    protected TestMessage() {
    }

    public native int getType() /*-{
      return this.type;
    }-*/;
  }

  /**
   * A simple {@link WindowChannelServer.Listener} that keeps a collection of
   * {@link WindowChannel.Listener} and {@link WindowChannel} pairs.
   */
  private static class TestServerListener implements
      WindowChannel.ServerListener {

    protected List<ServerClientAndListener> listenersAndChannels = new ArrayList<ServerClientAndListener>();

    public ServerClientAndListener getListenerAndChannelAt(int index) {
      assertTrue(index <= listenersAndChannels.size());
      return listenersAndChannels.get(index);
    }

    public void onClientChannelRequested(Request request) {
      TestListener listener = new TestListener();
      listenersAndChannels.add(new ServerClientAndListener(
          request.accept(listener), listener));
    }
  }

  private static IFrameElement createBlankFrame(Document document) {
    final IFrameElement elem = document.createIFrameElement();
    elem.setSrc("blank.html");
    return elem;
  }

  private static native Window getContentWindow(IFrameElement frame) /*-{
    return frame.contentWindow;
  }-*/;

  @Override
  public String getModuleName() {
    return "com.google.speedtracer.Common";
  }

  /**
   * Tests that a {@link WindowChannel.Client} channel pair can be connected
   * using a {@link WindowChannel.Server}.
   */
  public void testClientConnect() {
    final String channelName = "testConnect";
    final WindowExt window = WindowExt.get();
    final TestServerListener serverListener = new TestServerListener();
    Server server = Server.listen(window, channelName, serverListener);

    final TestListener clientListener = new TestListener();
    final Client client = WindowChannel.Client.connect(window, channelName,
        clientListener);

    ServerClientAndListener serverPair = serverListener.getListenerAndChannelAt(0);
    assertTrue("Channel is null.", serverPair.channel != null);
    assertTrue("Listener is null.", serverPair.listener != null);

    assertTrue("server failed to connect.", serverPair.listener.isConnected());
    assertTrue("client failed to connect.", clientListener.isConnected());

    serverPair.channel.close();
    assertFalse("server failed to close.", serverPair.listener.isConnected());
    assertFalse("client failed to close.", clientListener.isConnected());

    // Ensures there is no error calling close on a channel that was previously
    // closed.
    client.close();
    server.close();
    assertTrue("Server Connector did not null out after close.",
        window.getObjectProperty(PROPERTY_NAME + channelName) == null);
  }

  /**
   * Tests that a {@link WindowChannel.Client} channel pair can be connected
   * using a {@link WindowChannel.Server}.
   */
  public void testMultiClientConnect() {
    final String channelName = "testMultiConnect";
    final WindowExt window = WindowExt.get();
    final TestServerListener serverListener = new TestServerListener();
    Server server = Server.listen(window, channelName, serverListener);

    final TestListener clientListenerA = new TestListener();
    final Client clientA = Client.connect(window, channelName, clientListenerA);

    ServerClientAndListener serverPairA = serverListener.getListenerAndChannelAt(0);
    assertTrue("Channel is null.", serverPairA.channel != null);
    assertTrue("Listener is null.", serverPairA.listener != null);

    assertTrue("server failed to connect.", serverPairA.listener.isConnected());
    assertTrue("client failed to connect.", clientListenerA.isConnected());

    final TestListener clientListenerB = new TestListener();
    final Client clientB = Client.connect(window, channelName, clientListenerB);

    ServerClientAndListener serverPairB = serverListener.getListenerAndChannelAt(1);
    assertTrue("Channel is null.", serverPairB.channel != null);
    assertTrue("Listener is null.", serverPairB.listener != null);

    assertTrue("server failed to connect.", serverPairB.listener.isConnected());
    assertTrue("client failed to connect.", clientListenerB.isConnected());

    serverPairA.channel.close();
    assertFalse("server failed to close.", serverPairA.listener.isConnected());
    assertFalse("client failed to close.", clientListenerA.isConnected());

    serverPairB.channel.close();
    assertFalse("server failed to close.", serverPairB.listener.isConnected());
    assertFalse("client failed to close.", clientListenerB.isConnected());

    // Ensures there is no error calling close on a channel that was previously
    // closed.
    clientA.close();
    clientB.close();
    server.close();
    assertTrue("Server Connector did not null out after close.",
        window.getObjectProperty(PROPERTY_NAME + channelName) == null);
  }

  /**
   * Tests that
   * {@link WindowChannel#sendMessage(int, com.google.gwt.core.client.JavaScriptObject)}
   * properly delivers messages.
   */
  public void testSend() {
    final String channelName = "testSend";
    final int numberOfMessagesToSend = 10;
    final WindowExt window = WindowExt.get();
    final TestServerListener serverListener = new TestServerListener();
    Server server = Server.listen(window, channelName, serverListener);

    final TestListener clientListener = new TestListener();
    final Client client = Client.connect(window, channelName, clientListener);

    ServerClientAndListener serverPair = serverListener.getListenerAndChannelAt(0);
    assertTrue("Channel is null.", serverPair.channel != null);
    assertTrue("Listener is null.", serverPair.listener != null);

    assertTrue("server failed to connect.", serverPair.listener.isConnected());
    assertTrue("client failed to connect.", clientListener.isConnected());

    for (int i = 0; i < numberOfMessagesToSend; ++i) {
      serverPair.channel.sendMessage(i, TestMessage.create(i));
      client.sendMessage(i, TestMessage.create(i));
    }

    assertEquals("server received wrong # of messages.",
        numberOfMessagesToSend, serverPair.listener.getMessageCount());
    assertEquals("client received wrong # of messages.",
        numberOfMessagesToSend, clientListener.getMessageCount());

    server.close();
    assertTrue("Server Connector did not null out after close.",
        window.getObjectProperty(PROPERTY_NAME + channelName) == null);
  }

  /**
   * Tests that {@link WindowChannel.Client} works as expected when peers call
   * {@link WindowChannel.Client#sendMessage(int, WindowChannel.Message)} from
   * within
   * {@link WindowChannel.Listener#onChannelConnected(WindowChannel.Client)}.
   */
  public void testSendInConnectCallback() {
    class SendInConnectListener extends TestListener {
      public boolean didReceiveMessage() {
        return super.getMessageCount() > 0;
      }

      @Override
      public void onChannelConnected(Client channel) {
        assertFalse(isConnected());
        super.onChannelConnected(channel);
        channel.sendMessage(0, TestMessage.create(0));
      }

      @Override
      public void onMessage(Client channel, int type, WindowChannel.Message data) {
        assertTrue(isConnected());
        super.onMessage(channel, type, data);
      }
    }

    class SendInConnectServerListener extends TestServerListener {
      @Override
      public void onClientChannelRequested(Request request) {
        TestListener listener = new SendInConnectListener();
        listenersAndChannels.add(new ServerClientAndListener(
            request.accept(listener), listener));
      }
    }

    final String channelName = "testSendInConnectCallback";

    final WindowExt window = WindowExt.get();

    final TestServerListener serverListener = new SendInConnectServerListener();
    Server server = Server.listen(window, channelName, serverListener);
    final SendInConnectListener clientListener = new SendInConnectListener();
    final Client client = Client.connect(window, channelName, clientListener);

    ServerClientAndListener serverPair = serverListener.getListenerAndChannelAt(0);
    assertTrue("Channel is null.", serverPair.channel != null);
    assertTrue("Listener is null.", serverPair.listener != null);

    assertTrue("server failed to connect.", serverPair.listener.isConnected());
    assertTrue("client failed to connect.", clientListener.connected);

    assertTrue("client did not receive message",
        clientListener.didReceiveMessage());
    assertTrue("server did not receive message",
        ((SendInConnectListener) serverPair.listener).didReceiveMessage());

    client.close();
    server.close();
    assertTrue("Server Connector did not null out after close.",
        window.getObjectProperty(PROPERTY_NAME + channelName) == null);
  }

  /**
   * Tests that a {@link WindowChannel.Server} can be started and stopped.
   */
  public void testServerStartStop() {
    final String channelName = "testStartStop";
    final WindowExt window = WindowExt.get();
    final TestServerListener serverListener = new TestServerListener();
    WindowChannel.Server server = WindowChannel.Server.listen(window,
        channelName, serverListener);

    assertTrue("Server Connector is null.",
        window.getObjectProperty(PROPERTY_NAME + channelName) != null);
    server.close();
    assertTrue("Server Connector did not null out after close.",
        window.getObjectProperty(PROPERTY_NAME + channelName) == null);
  }

  /**
   * Tests that the channel property closes when the underlying window unloads.
   */
  public void testWindowUnloadClosesChannel() {
    final String channelName = "testWindowUnloadClosesChannel";

    final Document document = Document.get();
    final IFrameElement frame = createBlankFrame(document);
    document.getBody().appendChild(frame);

    final WindowExt window = getContentWindow(frame).cast();

    final TestServerListener serverListener = new TestServerListener();
    Server.listen(window, channelName, serverListener);

    final TestListener clientListener = new TestListener();
    Client.connect(window, channelName, clientListener);

    ServerClientAndListener serverPair = serverListener.getListenerAndChannelAt(0);
    assertTrue("Channel is null.", serverPair.channel != null);
    assertTrue("Listener is null.", serverPair.listener != null);

    assertTrue("server failed to connect.", serverPair.listener.isConnected());
    assertTrue("client failed to connect.", clientListener.isConnected());

    document.getBody().removeChild(frame);

    assertFalse("server failed to auto-close.",
        serverPair.listener.isConnected());
    assertFalse("client failed to auto-close.", clientListener.isConnected());
  }
}
