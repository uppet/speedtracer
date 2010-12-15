package com.google.speedtracer.client.model;

import com.google.gwt.coreext.client.DataBag;
import com.google.speedtracer.client.model.InspectorDidReceiveResponse.Response;
import com.google.speedtracer.client.model.NetworkResource.HeaderMap;

/**
 * Overlay type for inspector resource messages that are sent before issuing a
 * request for a resource.
 */
public final class InspectorWillSendRequest extends InspectorResourceMessage {
  public static final class Request extends DataBag {
    protected Request() {
    }

    public HeaderMap getHeaders() {
      return getJSObjectProperty("httpHeaderFields").<HeaderMap>cast();
    }
  }

  public static final class RedirectResponse extends Response {
    protected RedirectResponse() {
    }

    public boolean isNull() {
      return getBooleanProperty("isNull");
    }
  }

  public static final class Data extends InspectorResourceMessage.Data {
    protected Data() {
    }

    public Request getRequest() {
      return getJSObjectProperty("request").<Request>cast();
    }

    public RedirectResponse getRedirectResponse() {
      return getJSObjectProperty("redirectResponse").<RedirectResponse>cast();
    }
  }

  protected InspectorWillSendRequest() {
  }
}
