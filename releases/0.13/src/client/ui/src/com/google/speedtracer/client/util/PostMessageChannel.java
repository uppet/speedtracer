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

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.IFrameElement;
import com.google.gwt.events.client.Event;
import com.google.gwt.events.client.EventListener;
import com.google.gwt.events.client.EventListenerRemover;
import com.google.speedtracer.client.util.dom.WindowExt;

/**
 * A cross-domain messaging channel that relies on postMessage
 * (https://developer.mozilla.org/en/DOM/window.postMessage).
 */
public interface PostMessageChannel {

  /**
   * A channel implementation that is capable of initiating a connection to a
   * foreign server and send it messages. See {@link Responder} for an
   * implementation that only receives and responds to messages.
   */
  public class Client {
    private static IFrameElement createFrame(Document document, String url) {
      final IFrameElement frame = document.createIFrameElement();
      frame.setSrc(url);
      // New WebKit will activate iframes even if they are display:none.
      frame.getStyle().setProperty("cssText", "display:none;");
      return frame;
    }

    private static native WindowExt getContentWindow(IFrameElement frame) /*-{
      return frame.contentWindow;
    }-*/;

    private static native WindowExt getCurrentWindow() /*-{
      return window;
    }-*/;

    private static String getDomain(String url) {
      int protoOffset = url.indexOf("://");
      assert protoOffset >= 0;
      int domainOffset = url.indexOf("/", protoOffset + 3);
      assert domainOffset >= 0;
      return url.substring(0, domainOffset);
    }

    private final String origin;

    private final IFrameElement frame;

    private final EventListenerRemover remover;

    private final Responder receiver;

    // This is only read through assertions, so it should be removed by the
    // compiler.
    private boolean connected;

    /**
     * Creates a new client channel.
     * 
     * @param document the document in which to insert the iframe
     * @param url the url to the remote peer
     * @param listener a callback to be notified on channel events
     */
    public Client(Document document, String url, final ClientListener listener) {
      origin = getDomain(url);
      frame = document.getBody().appendChild(createFrame(document, url));
      receiver = new Responder(getCurrentWindow(), origin, listener);
      remover = Event.addEventListener("load", frame, new EventListener() {
        public void handleEvent(Event event) {
          // This should never call synchronously.
          assert remover != null;
          assert !setConnected(true);
          listener.onConnected(Client.this);
          remover.remove();
        }
      });
    }

    public void close() {
      assert setConnected(false);
      remover.remove();
      frame.getParentElement().removeChild(frame);
      receiver.close();
    }

    /**
     * Sends a message to the remote peer.
     * 
     * @param message
     */
    public void sendMessage(String message) {
      assert connected;
      Message.sendMessage(getContentWindow(frame), message, origin);
    }

    /**
     * Used for testing.
     * 
     * @return
     */
    WindowExt getFrameContentWindow() {
      return getContentWindow(frame);
    }

    /**
     * Used for testing.
     * 
     * @return
     */
    String getOrigin() {
      return origin;
    }

    private boolean setConnected(boolean connected) {
      final boolean was = this.connected;
      this.connected = connected;
      return was;
    }
  }

  /**
   * Event-based listener interface for delivering Channel events.
   */
  public interface ClientListener extends ResponseListener {
    /**
     * Called when the {@link Client} connects. It is not safe to call
     * {@link Client#sendMessage(String)} until this occurs.
     * 
     * @param client the client that has connected
     */
    void onConnected(Client client);
  }

  /**
   * An encapsulation of a message that was sent with either
   * {@link Client#sendMessage(String)} or {@link #respond(String)}. This
   * contains the string data that was sent and a mechanism to respond directly
   * to that message.
   */
  public static class Message extends JavaScriptObject {
    private static native void sendMessage(WindowExt window, String message,
        String origin) /*-{
      window.postMessage(message, origin);
    }-*/;

    protected Message() {
    }

    public final native String getData() /*-{
      return this.data;
    }-*/;

    public final native String getOrigin() /*-{
      return this.origin;
    }-*/;

    public final void respond(String message) {
      sendMessage(getSource(), message, getOrigin());
    }

    private native WindowExt getSource() /*-{
      return this.source;
    }-*/;
  }

  /**
   * A {@link PostMessageChannel} implementation that is lighter weight (does
   * not create an iframe) but only allows for receiving and responding to
   * messages.
   */
  public class Responder implements PostMessageChannel {

    private final EventListenerRemover remover;

    public Responder(WindowExt window, final String origin,
        final ResponseListener listener) {
      final boolean acceptAllOrigins = "*".equals(origin);
      remover = Event.addEventListener("message", window, new EventListener() {
        public void handleEvent(Event event) {
          final Message e = event.cast();
          if (acceptAllOrigins || origin.equals(e.getOrigin())) {
            listener.onMessageReceived(Responder.this, e);
          }
        }
      });
    }

    public void close() {
      remover.remove();
    }
  }

  /**
   * Event-based listener interface for delivering Channel events.
   */
  public interface ResponseListener {
    /**
     * Called when a message arrives on the channel.
     * 
     * @param channel the channel that received the message
     * @param message the message itself
     */
    void onMessageReceived(PostMessageChannel channel, Message message);
  }

  /**
   * Shuts down the client channel and removes all associated resources.
   */
  void close();
}
