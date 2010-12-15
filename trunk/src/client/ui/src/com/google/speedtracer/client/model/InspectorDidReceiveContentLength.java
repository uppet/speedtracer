package com.google.speedtracer.client.model;

/**
 * Overlay type for inspector resource messages that are sent when the resource
 * loader adjusts its estimate of the content length for the response payload.
 */
public class InspectorDidReceiveContentLength extends InspectorResourceMessage {
  protected InspectorDidReceiveContentLength() {
  }

  public static final class Data extends InspectorResourceMessage.Data {
    protected Data() {
    }

    public int getLengthReceived() {
      return getIntProperty("lengthReceived");
    }
  }
}
