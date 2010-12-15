package com.google.speedtracer.client.model;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.coreext.client.DataBag;

/**
 * Base class for inspector resource messages.
 */
public class InspectorResourceMessage extends ResourceRecord {
  public static class Data extends DataBag {
    protected Data() {
    }

    public final double getTime() {
      return getDoubleProperty("time");
    }
  }

  protected InspectorResourceMessage() {
  }

  public static native <T extends InspectorResourceMessage> T create(
      int type, double normalizedTime, JavaScriptObject data) /*-{
    return {
      type: type,
      time: normalizedTime,
      data:data
    };
  }-*/;
}
