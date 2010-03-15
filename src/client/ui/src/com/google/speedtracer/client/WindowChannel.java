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

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.events.client.Event;
import com.google.gwt.events.client.EventListener;
import com.google.gwt.events.client.EventListenerRemover;
import com.google.speedtracer.client.util.dom.WindowExt;

/**
 * Provides a communication channel between two GWT modules. A
 * {@link WindowChannel.Server} must be started first, and is responsible for
 * create Client channel pairs. One or more {@link WindowChannel.Client}s can
 * connect to a single {@link WindowChannel.Server}. Each connection request
 * creates a corresponding {@link WindowChannel.Client} endpoint on the server
 * side (completing the channel pairing).
 */
public class WindowChannel {
  /**
   * An endpoint in a Channel pair. The object each side uses to send and the
   * object each side subscribes to to receive messages.
   */
  public static class Client {
    /**
     * Creates and connects a new channel.
     * 
     * connect is asynchronous and it is not safe to call
     * {@link #sendMessage(int, Message)} or {@link #close()} before
     * {@link Listener#onChannelConnected(Client)} has been invoked.
     * 
     * @param window a shared window
     * @param name a shared name for the channel
     * @param listener
     * @return
     */
    public static Client connect(WindowExt window, String name,
        Listener listener) {
      final String property = PROPERTY_NAME + name;
      final Connector connector = window.getObjectProperty(property).cast();
      assert connector != null;

      final Client client = new Client(listener);

      // Server will invoke the setSocketCallback setting our socket and will
      // call his own onChannelConnected. If the Server sends a message during
      // onChannelConnected, handleSend will make sure that our own
      // onChannelConnected is invoked before that message is delivered. If the
      // Server does not send a message, we will manually call
      // onChannelConnected after this call returns.
      connector.connect(Socket.create(client), SetSocketCallback.create(client));
      assert client.socket != null;

      // This will call listener.onChannelConnected if the Server did not send a
      // message during his own onChannelConnected.
      client.maybeConnectChannel();

      final EventListenerRemover[] remover = new EventListenerRemover[1];
      remover[0] = window.addUnloadListener(new EventListener() {
        public void handleEvent(Event event) {
          remover[0].remove();
          client.close();
        }
      });

      return client;
    }

    private boolean connected;

    private final Listener listener;

    private Socket socket;

    private Client(Listener listener) {
      this.listener = listener;
    }

    /**
     * Closes the channel.
     * 
     * It is safe to call this method on a closed channel. However, it is not
     * safe to call before {@link Listener#onChannelConnected(Client)} has been
     * invoked.
     */
    public void close() {
      if (socket != null) {
        socket.close();
        socket = null;
        connected = false;
        listener.onChannelClosed(this);
      }
    }

    /**
     * Sends a message to the channel peer.
     * 
     * It is an error to call this method before
     * {@link Listener#onChannelConnected(Client)} is invoked.
     * 
     * @param type a type id to use to dispatch the message
     * @param message the message payload
     */
    public void sendMessage(int type, Message message) {
      assert socket != null;
      socket.send(type, message);
    }

    @SuppressWarnings("unused")
    private void handleClose() {
      socket = null;
      connected = false;
      listener.onChannelClosed(this);
    }

    @SuppressWarnings("unused")
    private void handleSend(int type, Message message) {
      // Since it is common for listeners to send messages in their
      // onChannelConnected callbacks, we must make sure that onChannelConnected
      // is called before any messages are delivered.
      maybeConnectChannel();
      listener.onMessage(this, type, message.<Message> cast());
    }

    private void maybeConnectChannel() {
      if (!connected) {
        connected = true;
        listener.onChannelConnected(this);
      }
    }

    private void setSocket(Socket socket) {
      assert !connected;
      this.socket = socket;
    }
  }

  /**
   * listener interface to receive channel events.
   */
  public interface Listener {
    /**
     * Called when the channel disconnects from its peer.
     * 
     * @param client the channel generating the event
     */
    void onChannelClosed(Client client);

    /**
     * Called when the channel connects to its peer.
     * 
     * @param client the channel generating the event
     */
    void onChannelConnected(Client client);

    /**
     * Called when a message is sent from the peer channel.
     * 
     * @param client the client that received the event.
     * @param type a user specified type id
     * @param data the message payload
     */
    void onMessage(Client client, int type, Message data);
  }

  /**
   * A overlay tag type to give all WindowChannel messages a common base type.
   */
  public static class Message extends JavaScriptObject {
    protected Message() {
    }
  }

  /**
   * A wrapper object passed to a {@link WindowChannel.ServerListener} when a
   * client tries to connect to the channel. It releases the server side's
   * client endpoint.
   */
  public static class Request {
    private final SetSocketCallback callback;
    private final Socket socket;

    private Request(Socket socket, SetSocketCallback callback) {
      this.socket = socket;
      this.callback = callback;
    }

    public Client accept(Listener listener) {
      final Client client = new Client(listener);
      client.setSocket(socket);
      callback.setSocket(Socket.create(client));
      client.maybeConnectChannel();
      return client;
    }
  }

  /**
   * Server responsible for creating and establishing
   * {@link WindowChannel.Client} channel pairs.
   */
  public static class Server {

    public static Server listen(WindowExt window, String name,
        ServerListener listener) {
      final String property = PROPERTY_NAME + name;
      assert window.getObjectProperty(property) == null;
      final Server server = new Server(window, property, listener);
      window.setObjectProperty(property, Connector.create(server));
      return server;
    }

    private final ServerListener listener;

    private final String property;

    private final WindowExt window;

    private Server(WindowExt window, String property, ServerListener listener) {
      this.window = window;
      this.property = property;
      this.listener = listener;
    }

    public void close() {
      assert window.getObjectProperty(property) != null;
      window.setObjectProperty(property, null);
    }

    @SuppressWarnings("unused")
    private void handleConnect(Socket socket, SetSocketCallback callback) {
      listener.onClientChannelRequested(new Request(socket, callback));
    }
  }

  /**
   * Listener interface for receiving connection requests from clients.
   */
  public interface ServerListener {
    void onClientChannelRequested(Request request);
  }

  /**
   * An overlay type around JavaScript function. The Server places an instance
   * on the shared window. The slave channel, the second to connect, calls
   * {@link #connect(Socket, SetSocketCallback)} to establish a connection.
   */
  private static final class Connector extends JavaScriptObject {
    public static native Connector create(Server server) /*-{
      return function(socket, callback) {
        server.@com.google.speedtracer.client.WindowChannel$Server::handleConnect(Lcom/google/speedtracer/client/WindowChannel$Socket;Lcom/google/speedtracer/client/WindowChannel$SetSocketCallback;)(
            socket, callback);
      };
    }-*/;

    @SuppressWarnings("all")
    protected Connector() {
    }

    public native void connect(Socket socket, SetSocketCallback callback) /*-{
      return this(socket, callback);
    }-*/;
  }

  /**
   * An overlay type around a JavaScript function. The Server will use this
   * function to set the slaves's socket.
   */
  private static final class SetSocketCallback extends JavaScriptObject {
    public static native SetSocketCallback create(Client client) /*-{
      return function(socket) {
        client.@com.google.speedtracer.client.WindowChannel$Client::setSocket(Lcom/google/speedtracer/client/WindowChannel$Socket;)(
            socket);
      };
    }-*/;

    @SuppressWarnings("all")
    protected SetSocketCallback() {
    }

    public native void setSocket(Socket socket) /*-{
      this(socket);
    }-*/;
  }

  /**
   * The JavaScriptObject implementation that facilitates all messaging between
   * the peers. This object is simply a {@link JavaScriptObject} (an array)
   * encapsulating two functions, one for messaging and one for connection tear
   * down.
   */
  private static final class Socket extends JavaScriptObject {
    static native Socket create(Client client) /*-{
      return [
        function() {
          client.@com.google.speedtracer.client.WindowChannel$Client::handleClose()();
        },
        function(type, message) {
          client.@com.google.speedtracer.client.WindowChannel$Client::handleSend(ILcom/google/speedtracer/client/WindowChannel$Message;)(
              type, message);
        }];
    }-*/;

    @SuppressWarnings("all")
    protected Socket() {
    }

    public native void close() /*-{
      this[0]();
    }-*/;

    public native void send(int type, JavaScriptObject data) /*-{
      this[1](type, data);
    }-*/;
  }

  private static final String PROPERTY_NAME = "$WindowChannel$";

  private WindowChannel() {
  }
}
