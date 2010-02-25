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
 * JSO based API for binding {@link DataModel} to an arbitrary data source.
 */
public class DataInstance extends JavaScriptObject {
  /**
   * In order to provide concrete implementations of DataInstance, we delegate
   * the implementation of this DataProxy interface.
   * 
   * Instance dependencies should be passed to the concrete implementation's
   * constructor.
   */
  public interface DataProxy {
    void load(DataInstance dataInstance);

    void resumeMonitoring();

    void setBaseTime(double baseTime);

    void setProfilingOptions(boolean enableStackTraces,
        boolean enableCpuProfiling);

    void stopMonitoring();

    void unload();
  }

  /**
   * Static Factory method for obtaining an instance of DataInstance that
   * delegates to the specified DataProxy.
   * 
   * @param proxy the {@link DataProxy} object that this DataInstance will
   *          delegate to.
   * @return the DataInstance.
   */
  public static native DataInstance create(DataProxy proxy) /*-{
    var dataInstance = {
      Load: function(callback) {
        this._callback = callback;
        proxy.@com.google.speedtracer.client.model.DataInstance.DataProxy::load(Lcom/google/speedtracer/client/model/DataInstance;)(dataInstance);
      },

      Resume: function() {
        proxy.@com.google.speedtracer.client.model.DataInstance.DataProxy::resumeMonitoring()();
      },

      Stop: function() {
        proxy.@com.google.speedtracer.client.model.DataInstance.DataProxy::stopMonitoring()();
      },

      Unload: function() {        
        proxy.@com.google.speedtracer.client.model.DataInstance.DataProxy::unload()();
        // Remove the connection back to the monitor's model in order to prevent
        // a memory leak.
        this._callback = null;
      },

      SetBaseTime: function(baseTime) {
        proxy.@com.google.speedtracer.client.model.DataInstance.DataProxy::setBaseTime(D)(baseTime);
      },

      SetOptions: function(enableStackTraces, enableCpuProfiling) {
        proxy.@com.google.speedtracer.client.model.DataInstance.DataProxy::setProfilingOptions(ZZ)(enableStackTraces, enableCpuProfiling);
      }
    };
    // Initialize the sequence number count to 0
    dataInstance.seqCount = 0;
    return dataInstance;
  }-*/;

  protected DataInstance() {
  }

  /**
   * Binds an {@link DataModel} to this DataInstance. Data will be sent to the
   * specified {@link DataModel}.
   * 
   * @param model
   */
  public final native void load(DataModel model) /*-{
    var callback = {
      // This gets called by everyone else (file loader and devtools data instances).
      onEventRecord: function(record) {
        model.@com.google.speedtracer.client.model.DataModel::onEventRecord(Lcom/google/speedtracer/client/model/EventRecord;)(record);
      },

      // This gets called from the plugin.
      onEventRecordString: function(sequence, recordString) {
        var data = JSON.parse(recordString);
        // Populate the sequence field in the record so that we don't have
        // to keep passing it out of band along with the record.
        data.sequence = sequence;
        model.@com.google.speedtracer.client.model.DataModel::onEventRecord(Lcom/google/speedtracer/client/model/EventRecord;)(data);
      }
    };
    this.Load(callback);
  }-*/;

  /**
   * Forwards an EventRecord to the callback. Calling this before we have set a
   * callback in load() will cause an NPE.
   * 
   * @param record
   */
  public final native void onEventRecord(EventRecord record) /*-{
    record.sequence = this.seqCount;
    this._callback.onEventRecord(record);
    this.seqCount = this.seqCount + 1;
  }-*/;

  public final native void resumeMonitoring() /*-{
    this.Resume();
  }-*/;

  public final native void setBaseTime(double baseTime) /*-{
    this.SetBaseTime(baseTime);
  }-*/;

  public final native void setProfilingOptions(boolean enableStackTraces,
      boolean enableCpuProfiling) /*-{
    this.SetOptions(enableStackTraces, enableCpuProfiling);
  }-*/;

  public final native void stopMonitoring() /*-{
    this.Stop();
  }-*/;

  public final native void unload()/*-{
    this.Unload();
  }-*/;
}