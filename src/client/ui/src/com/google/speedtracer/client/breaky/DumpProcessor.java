package com.google.speedtracer.client.breaky;

import com.google.gwt.user.client.IncrementalCommand;

public class DumpProcessor implements IncrementalCommand {
  public interface DumpEntryHandler {
    /**
     * A handler to process a single entry in the dump
     * 
     * @param dumpEntry A single entry in the dump file to process.
     * @return true if the entry is valid, false otherwise
     */
    public boolean onDumpEntry(String dumpEntry);
    
    /**
     * Called when finished parsing all records.
     * Not called if a record is reported as invalid
     */
    public void onFinished();
  }

  private DumpProcessor.DumpEntryHandler handler;
  private String[] events;
  private int eventIndex = 0;

  public DumpProcessor(DumpProcessor.DumpEntryHandler handler, String[] events) {
    this.handler = handler;
    this.events = events;
  }
  
  public boolean execute() {
    String entry = events[eventIndex++];
    if(entry.length() > 0) {
      boolean isValid = handler.onDumpEntry(entry);
      boolean moreRecords = eventIndex < events.length;
      if(isValid && !moreRecords) {
        handler.onFinished();
      }
      return isValid && moreRecords;
    } else {
      //ignore empty lines in the record data
      return true;
    }
  }
}