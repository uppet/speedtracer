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
package com.google.speedtracer.breaky.client;

import com.google.gwt.user.client.IncrementalCommand;

/**
 * A DumpProcessor drives a {@link DumpEntryHandler} with the data from a dump
 * (mock or real)
 * 
 * TODO(conroy): Use a WorkQueue instead to improve performance.
 */
public class DumpProcessor implements IncrementalCommand {
  /**
   * An interface for incrementally processing dump records.
   */
  public interface DumpEntryHandler {
    /**
     * Handle a single entry in a dump.
     * 
     * @param dumpEntry A single entry in the dump file to process.
     * @return true if the entry is valid, false otherwise
     */
    boolean onDumpEntry(String dumpEntry);

    /**
     * Called when finished parsing all records. Not called if a record is
     * reported as invalid
     */
    void onFinished();
  }

  private int eventIndex = 0;
  private String[] events;
  private DumpProcessor.DumpEntryHandler handler;

  public DumpProcessor(DumpProcessor.DumpEntryHandler handler, String[] events) {
    this.handler = handler;
    this.events = events;
  }

  /**
   * Process a single record. Fast-fail if a record is invalid.
   */
  public boolean execute() {
    String entry = events[eventIndex++];
    if (entry.length() > 0) {
      boolean isValid = handler.onDumpEntry(entry);
      boolean moreRecords = eventIndex < events.length;
      if (isValid && !moreRecords) {
        handler.onFinished();
      }
      return isValid && moreRecords;
    } else {
      // ignore empty lines in the record data
      return true;
    }
  }
}