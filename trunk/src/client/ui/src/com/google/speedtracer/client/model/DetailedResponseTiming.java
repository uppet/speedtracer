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
public final class DetailedResponseTiming extends JavaScriptObject {
  /**
   * Does a paranoid computation of duration given a start time and and end
   * time. If both values are -1, -1 will be returned as that is used by many
   * properties to represent special cases. If either of the values is
   * <code>undefined</code>, 0 will be returned.
   */
  private static native double computeDuration(double start, double end) /*-{
    var duration = (end == -1) ? -1 : end - start;
    return isNaN(duration) ? 0 : duration;
  }-*/;

  protected DetailedResponseTiming() {
  }

  public native double getConnectDuration() /*-{
    return @com.google.speedtracer.client.model.DetailedResponseTiming::computeDuration(DD)(this.connectStart, this.connectEnd);
  }-*/;

  public native double getDnsDuration() /*-{
    return @com.google.speedtracer.client.model.DetailedResponseTiming::computeDuration(DD)(this.dnsStart, this.dnsEnd);
  }-*/;

  public native double getProxyDuration() /*-{
    return @com.google.speedtracer.client.model.DetailedResponseTiming::computeDuration(DD)(this.proxyStart, this.proxyEnd);
  }-*/;

  public native double getRequestTime() /*-{
    return this.requestTime || 0;
  }-*/;

  public native double getSendDuration() /*-{
    // NOTE: Unlike other durations, this cannot be -1.
    return @com.google.speedtracer.client.model.DetailedResponseTiming::computeDuration(DD)(this.sendStart, this.sendEnd);
  }-*/;

  public native double getSslDuration() /*-{
    return @com.google.speedtracer.client.model.DetailedResponseTiming::computeDuration(DD)(this.sslStart, this.sslEnd);
  }-*/;

  /**
   * Allow for callers to check for older versions of
   * {@link DetailedResponseTiming} objects. Older objects used the single
   * property 'sendDuration'; newer versions use 'sendStart' and 'sendEnd'.
   */
  public native boolean isValid() /*-{
    return this.hasOwnProperty('sendStart') && this.hasOwnProperty('sendEnd');
  }-*/;

  public native void setRequestTime(double requestTime) /*-{
    this.requestTime = requestTime;
  }-*/;

}
