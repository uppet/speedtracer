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

import com.google.gwt.coreext.client.JSOArray;
import com.google.gwt.coreext.client.JsIntegerMap;
import com.google.gwt.dom.client.Document;
import com.google.gwt.events.client.Event;
import com.google.gwt.events.client.EventListener;
import com.google.gwt.events.client.EventListenerRemover;
import com.google.gwt.topspin.ui.client.KeyDownEvent;
import com.google.gwt.topspin.ui.client.KeyUpEvent;

/**
 * Provides a mean for registering global hot key bindings, particularly
 * spring-loaded hot keys.
 * 
 * All hot keys depend on CTRL being depressed so as not to interfere with
 * regular typing.
 */
public class HotKey {
  /**
   * Container for one entry in the HotKey database.
   */
  public static class Data {
    private final String description;
    private final Handler handler;
    private final int keyCode;

    private Data(int keyCode, Handler handler, String description) {
      this.keyCode = keyCode;
      this.handler = handler;
      this.description = description;
    }

    public String getDescription() {
      return description;
    }

    public Handler getHandler() {
      return handler;
    }

    public int getKeyCode() {
      return keyCode;
    }
  }

  /**
   * A handler interface to receive event callbacks.
   */
  public interface Handler {
    /**
     * Called when a hot key is initially pressed.
     * 
     * @param event the underlying event
     */
    void onKeyDown(KeyDownEvent event);

    /**
     * Called when the hot key is released.
     * 
     * @param event the underlying event
     */
    void onKeyUp(KeyUpEvent event);
  }

  // Not all key codes look good as strings. As new entries are added, make
  // sure you initialize keyCodeDescMap if it is not a human readable character.
  public static final int LEFT_ARROW = 37;
  public static final int RIGHT_ARROW = 39;

  private static int handlerCount = 0;
  private static JsIntegerMap<Data> handlers;
  // Human readable descriptions for key codes.
  private static final JsIntegerMap<String> keyCodeDescMap = JsIntegerMap.create().cast();
  private static EventListenerRemover remover;

  static {
    keyCodeDescMap.put(LEFT_ARROW, "Left Arrow");
    keyCodeDescMap.put(RIGHT_ARROW, "Right Arrow");
  }

  /**
   * Returns a copy of the hot key data as an array.
   * 
   * @return an empty array if no hot key data exists.
   */
  public static JSOArray<Data> getHotKeyData() {
    if (handlers != null) {
      return handlers.getValues();
    }
    return JSOArray.createArray().cast();
  }

  /**
   * Looks up a key code and returns a human readable string. Sometimes this is
   * just the key, other times it is the function of the key written out in
   * English.
   * 
   * @return If no description is found, it interprets the keyCode as a Java
   *         character and returns a single character string.
   */
  public static String getKeyCodeDescription(int keyCode) {
    String result = keyCodeDescMap.get(keyCode);
    if (result == null) {
      return Character.toString((char) keyCode);
    }
    return result;
  }

  /**
   * Registers a handler to receive notification when the key corresponding to
   * {@code keyCode} is used.
   * 
   * Only one handler can be tied to a particular key code. Attempting to
   * register a previously registered code will result in an assertion being
   * raised.
   * 
   * @param keyCode the key code to register
   * @param handler a callback handler
   * @param description short human readable description for the action.
   */
  public static void register(int keyCode, Handler handler, String description) {
    if (handlers == null) {
      remover = addEventListeners();
      handlers = JsIntegerMap.create();
    }
    assert handlers.get(keyCode) == null;
    handlers.put(keyCode, new Data(keyCode, handler, description));
    ++handlerCount;
  }

  /**
   * Unregisters a previously registered handler for a particular keyCode.
   * 
   * @param keyCode the key code to unregister
   */
  public static void unregister(int keyCode) {
    assert handlers.get(keyCode) != null;
    if (--handlerCount == 0) {
      remover.remove();
      remover = null;
      handlers = null;
    }
  }

  /**
   * Removes all handlers registered with the
   * {@link #register(int, com.google.speedtracer.client.HotKey.Handler, String)}
   * method.
   */
  public static void unregisterAll() {
    if (handlers != null) {
      remover.remove();
      handlerCount = 0;
      remover = null;
      handlers = null;
    }
  }

  private static EventListenerRemover addEventListeners() {
    final JSOArray<Handler> stack = JSOArray.create();
    final EventListenerRemover downRemover = Event.addEventListener(
        KeyDownEvent.NAME, Document.get(), new EventListener() {
          public void handleEvent(Event event) {
            final Data data = handlers.get(event.getKeyCode());
            if (data == null || !event.getCtrlKey()) {
              return;
            }
            Handler handler = data.getHandler();
            handler.onKeyDown(new KeyDownEvent(handler, event));
            stack.push(handler);
            event.preventDefault();
          }
        });

    final EventListenerRemover upRemover = Event.addEventListener(
        KeyUpEvent.NAME, Document.get(), new EventListener() {
          public void handleEvent(Event event) {
            if (stack.peek() == null) {
              return;
            }

            final Handler handler = stack.pop();
            handler.onKeyUp(new KeyUpEvent(handler, event));
          }
        });

    return new EventListenerRemover() {
      public void remove() {
        downRemover.remove();
        upRemover.remove();
      }
    };
  }

  private HotKey() {
    // This class is automatically instantiated as a singleton through the
    // register() method
  }
}
