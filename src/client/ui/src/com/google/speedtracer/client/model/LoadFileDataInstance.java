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

import com.google.gwt.chrome.crx.client.Port;
import com.google.speedtracer.client.model.DataModel.DataInstance;

/**
 * This class is used in Chrome when we get data from a loaded file (data_loader
 * content script). We use this overlay type as the object we pass to the
 * Monitor UI.
 */
public class LoadFileDataInstance extends DataInstance {
  // TODO(jaimeyap): Push more of this code into Java.
  public static native LoadFileDataInstance create(Port port) /*-{
    var dataInstance = {
      Load: function(callback) {
        this._callback = callback;
        this.Resume(port);
      },

      Resume: function(port) {
        // Tell the content script to start sending data.
        port.postMessage({
          ready: true
        }); 
      },

      Stop: function() {
      }
    };
    // Initialize the sequence number count to 0
    dataInstance.seqCount = 0;
    return dataInstance;
  }-*/;

  protected LoadFileDataInstance() {
  }

  /**
   * This gets called with an unevaled record string. It gets JSON.parsed and
   * assigned a sequence number. Records that come in before we have assigned a
   * callback get buffered.
   * 
   * @param recordString
   */
  public final native void onEventRecord(String recordString) /*-{
    var record = JSON.parse(recordString);
    record.sequence = this.seqCount;
    this._callback.onEventRecord(record);
    this.seqCount = this.seqCount + 1;
  }-*/;
}
