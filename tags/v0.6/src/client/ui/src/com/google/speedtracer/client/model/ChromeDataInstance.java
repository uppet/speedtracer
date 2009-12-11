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
package com.google.speedtracer.client.model;

/**
 * In the Chrome case where we get data from either a loaded file (data_loader
 * content script) or from the devtools API, we use this overlay type as the
 * object we pass to the Monitor UI.
 * 
 * TODO(jaimeyap): This should eventually be what we want to use for hooking
 * into the devtools API.
 */
public class ChromeDataInstance extends DataModelImpl.DataInstance {
  // TODO(jaimeyap): Figure out how to push more of this code into Java. We cant
  // have instance methods on JSOs so it is currently more concise to have the
  // impls in JS.
  public static native ChromeDataInstance create() /*-{
    var dataInstance = {
      Load: function(callback) {
        this._callback = callback;
        if (this._recordBuffer) {
          var ctx = this;
          // Put a task at the end of the event queue to fire the buffered
          // records.
          setTimeout(function() {
            for (var i = 0; i < ctx._recordBuffer.length; i++) {
              ctx.OnEventRecordImpl(ctx._recordBuffer[i]);
            }
            ctx._recordBuffer = null;
          }, 0);
        }
      },

      Resume: function() {
        // TODO(jaimeyap): This will be used eventually when we move over to
        // devtools API.
      },

      Stop: function() {
        // TODO(jaimeyap): This will be used eventually when we move over to
        // devtools API.
      },

      OnEventRecordImpl: function(recordString) {
        if (this._callback) {
          this._callback.onEventRecord(this.seqCount, recordString);
          this.seqCount = this.seqCount + 1;
        }
      }
    };
    // Initialize the sequence number count to 0
    dataInstance.seqCount = 1;
    dataInstance._recordBuffer = [];
    return dataInstance;
  }-*/;

  protected ChromeDataInstance() {
  }

  public final native void onEventRecord(String recordString) /*-{
    if (this._recordBuffer) {
      this._recordBuffer.push(recordString);
    } else {
      this.OnEventRecordImpl(recordString);
    }
  }-*/;
}
