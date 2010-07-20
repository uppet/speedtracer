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

import com.google.gwt.core.client.JavaScriptObject;

/**
 * The detailed timing information included in a ResourceUpdateEvent object.
 * 
 * Note that these durations are given in units of seconds.
 */
public class DetailedResponseTiming extends JavaScriptObject {
  protected DetailedResponseTiming() {
  }
  
  public final native double getConnectDuration() /*-{
    return this.connectDuration || -1;
  }-*/;
  
  public final native double getDnsDuration() /*-{
    return this.dnsDuration || -1;
  }-*/;

  public final native double getProxyDuration() /*-{
    return this.proxyDuration || 0;
  }-*/;

  public final native double getRequestTime() /*-{
    return this.requestTime || 0;
  }-*/;

  public final native double getSendDuration() /*-{
    return this.sendDuration || 0;
  }-*/;

  public final native double getSslDuration() /*-{
    return this.sslDuration || 0;
  }-*/;

  public final native double getWaitDuration() /*-{
    return this.waitDuration || 0;
  }-*/;

  public final native void setRequestTime(double requestTime) /*-{
    this.requestTime = requestTime;
  }-*/;

}
