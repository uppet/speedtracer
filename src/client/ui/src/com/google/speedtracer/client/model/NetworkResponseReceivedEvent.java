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
package com.google.speedtracer.client.model;

import com.google.gwt.coreext.client.DataBag;
import com.google.speedtracer.client.model.NetworkResource.HeaderMap;

/**
 * Overlay type for network resource messages that are sent when we receive a
 * response for a resource request.
 */
public class NetworkResponseReceivedEvent extends NetworkEvent {
  /**
   * 
   */
  public static final class Data extends NetworkEvent.Data {
    protected Data() {
    }

    public Response getResponse() {
      return getJSObjectProperty("response");
    }
  }

  /**
   *
   */
  public static class Response extends DataBag {
    protected Response() {
    }

    public final int getConnectionID() {
      return getIntProperty("connectionId");
    }

    public final boolean getConnectionReused() {
      return getBooleanProperty("connectionReused");
    }

    public final DetailedResponseTiming getDetailedTiming() {
      return getJSObjectProperty("timing").<DetailedResponseTiming>cast();
    }

    public final HeaderMap getHeaders() {
      return getJSObjectProperty("headers").<HeaderMap>cast();
    }

    public final String getUrl() {
      return getStringProperty("url");
    }

    public final boolean wasCached() {
      return getBooleanProperty("fromDiskCache");
    }
  }

  protected NetworkResponseReceivedEvent() {
  }
}
