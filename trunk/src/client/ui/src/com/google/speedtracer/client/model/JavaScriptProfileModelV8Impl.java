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

import com.google.gwt.core.client.Duration;
import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.speedtracer.client.Logging;
import com.google.speedtracer.client.model.V8SymbolTable.AliasableEntry;
import com.google.speedtracer.client.model.V8SymbolTable.V8Symbol;
import com.google.speedtracer.client.util.Csv;
import com.google.speedtracer.client.util.JSOArray;
import com.google.speedtracer.client.util.WorkQueue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Parses the v8 profiler data from Chromium on behalf of
 * {@link JavaScriptProfile}.
 */
public class JavaScriptProfileModelV8Impl extends JavaScriptProfileModelImpl {

  /**
   * Used to store actions for parsing lines in the log.
   */
  interface LogAction {
    void doAction(JsArrayString logLine);
  }

  /**
   * Dummy entry in the action table that has no implementation.
   */
  class UnimplementedCommandMethod implements LogAction {
    private final String commandName;

    UnimplementedCommandMethod(String commandName) {
      this.commandName = commandName;
    }

    public void doAction(JsArrayString logLine) {
      Logging.getLogger().logText(
          "Unimplemented command: " + commandName + " alias: " + logLine.get(0));
    }
  }

  private static class ActionType extends AliasableEntry {
    public ActionType(String name, int value) {
      super(name, value);
    }
  }

  private static class DebugStats {
    public int lookupMisses;
    public int removeMisses;
    public int addCollisions;
    public int moveMisses;
  }

  /**
   * Wraps the {@link #procLogLines} method to run the processing of the log
   * iteratively using the work queue.
   */
  private class LogLineWorker implements WorkQueue.Node {
    public final JSOArray<String> logLines;
    public final UiEvent refRecord;
    public int currentOffset;

    public LogLineWorker(JSOArray<String> logLines, UiEvent refRecord,
        int currentOffset) {
      this.logLines = logLines;
      this.refRecord = refRecord;
      this.currentOffset = currentOffset;
    }

    public void execute() {
      processLogLines(refRecord, logLines, currentOffset);
    }

    public String getDescription() {
      return "LogLineWorkQueueNode seq " + refRecord.getSequence() + " offset "
          + currentOffset;
    }
  }

  /**
   * Introduces a new profile to be processed on the work queue.
   */
  private class NewProfileDataWorker implements WorkQueue.Node {
    public final UiEvent refRecord;
    public final JavaScriptProfile profile;
    public final JavaScriptProfileEvent rawEvent;

    public NewProfileDataWorker(UiEvent refRecord, JavaScriptProfile profile,
        JavaScriptProfileEvent rawEvent) {
      this.refRecord = refRecord;
      this.profile = profile;
      this.rawEvent = rawEvent;
    }

    public void execute() {
      currentProfile = profile;
      JSOArray<String> logLines = JSOArray.splitString(
          rawEvent.getProfileData(), "\n");
      workQueue.prepend(new LogLineWorker(logLines, refRecord, 0));
    }

    public String getDescription() {
      return "NewProfileDataWorkQueueNode seq " + refRecord.getSequence();
    }
  }

  private static class V8SymbolType extends AliasableEntry {
    public V8SymbolType(String name, int value) {
      super(name, value);
    }
  }

  static final DebugStats debugStats = new DebugStats();

  public static final int VM_STATE_JS = 0;
  public static final int VM_STATE_GC = 1;
  public static final int VM_STATE_COMPILER = 2;
  public static final int VM_STATE_OTHER = 3;
  public static final int VM_STATE_EXTERNAL = 4;

  /**
   * String constant in the data that identifies the profile data record as
   * coming from v8.
   */
  public static final String FORMAT = "v8";

  static final String ADDRESS_TAG_CODE = "code";
  static final String ADDRESS_TAG_CODE_MOVE = "code-move";
  static final String ADDRESS_TAG_STACK = "stack";
  static final String ADDRESS_TAG_SCRATCH = "scratch";

  private static final int ACTION_TYPE_ALIAS = 1;
  private static final int ACTION_TYPE_PROFILER = 2;
  private static final int ACTION_TYPE_CODE_CREATION = 3;
  private static final int ACTION_TYPE_CODE_MOVE = 4;
  private static final int ACTION_TYPE_CODE_DELETE = 5;
  private static final int ACTION_TYPE_TICK = 6;
  private static final int ACTION_TYPE_REPEAT = 7;

  private static final int SYMBOL_TYPE_BUILTIN = 8;
  private static final int SYMBOL_TYPE_CALL_DEBUG_BREAK = 9;
  private static final int SYMBOL_TYPE_CALL_DEBUG_PREPARE_STEP_IN = 10;
  private static final int SYMBOL_TYPE_CALL_IC = 11;
  private static final int SYMBOL_TYPE_CALL_INITIALIZE = 12;
  private static final int SYMBOL_TYPE_CALL_MEGAMORPHIC = 13;
  private static final int SYMBOL_TYPE_CALL_MISS = 14;
  private static final int SYMBOL_TYPE_CALL_NORMAL = 15;
  private static final int SYMBOL_TYPE_PRE_MONOMORPHIC = 16;
  private static final int SYMBOL_TYPE_CALLBACK = 17;
  private static final int SYMBOL_TYPE_EVAL = 18;
  private static final int SYMBOL_TYPE_FUNCTION = 19;
  private static final int SYMBOL_TYPE_LOAD_IC = 20;
  private static final int SYMBOL_TYPE_KEYED_CALL_IC = 21;
  private static final int SYMBOL_TYPE_KEYED_LOAD_IC = 22;
  private static final int SYMBOL_TYPE_KEYED_STORE_IC = 23;
  private static final int SYMBOL_TYPE_LAZY_COMPILE = 24;
  private static final int SYMBOL_TYPE_REG_EXP = 25;
  private static final int SYMBOL_TYPE_SCRIPT = 26;
  private static final int SYMBOL_TYPE_STORE_IC = 27;
  private static final int SYMBOL_TYPE_STUB = 28;

  private static final int LOG_PROCESS_CHUNK_TIME_MS = 60;

  // TODO(zundel): this method is just for debugging. Not for production use.
  static void getProfileBreakdownText(StringBuilder result,
      JavaScriptProfileEvent rawEvent) {
    HashMap<String, Integer> commandMap = new HashMap<String, Integer>();
    String profileData = rawEvent.getProfileData();
    if (profileData == null || profileData.length() == 0) {
      return;
    }

    String[] logLines = profileData.split("\n");
    for (int i = 0, logLinesLength = logLines.length; i < logLinesLength; ++i) {
      String logLine = logLines[i];
      String[] logEntries = logLine.split(",");
      String command = logEntries[0];
      Integer count = commandMap.get(command);
      if (count == null) {
        commandMap.put(command, 1);
      } else {
        commandMap.put(command, count + 1);
      }
    }

    // Dump the command map as the output
    for (Entry<String, Integer> entry : commandMap.entrySet()) {
      result.append("Command: " + entry.getKey() + " " + entry.getValue()
          + "\n");
    }
  }

  private Map<String, V8SymbolType> symbolTypeMap = new HashMap<String, V8SymbolType>();
  private Map<String, ActionType> actionTypeMap = new HashMap<String, ActionType>();
  private Map<ActionType, LogAction> logActions = new HashMap<ActionType, LogAction>();
  private V8SymbolTable symbolTable = new V8SymbolTable();
  private Map<String, Double> addressTags = new HashMap<String, Double>();
  private static Element scrubbingDiv = Document.get().createDivElement();
  private V8LogDecompressor logDecompressor = null;
  private JavaScriptProfile currentProfile = null;

  private final WorkQueue workQueue;

  public JavaScriptProfileModelV8Impl(boolean useWorkQueue) {
    super("v8");
    workQueue = (useWorkQueue ? new WorkQueue() : null);
    populateAddressTags();
    populateActionTypes();
    populateSymbolTypes();
    // TODO (zundel): populate windows C++ symbols from .map files
  }

  @Override
  public String getDebugDumpHtml() {
    StringBuilder output = new StringBuilder();
    output.append("<h3>Debug Stats</h3>\n");
    output.append("<table>\n");
    output.append("<tr><td>Add Collisions</td><td>" + debugStats.addCollisions
        + "</td></tr>");
    output.append("<tr><td>Lookup Misses</td><td>" + debugStats.lookupMisses
        + "</td></tr>");
    output.append("<tr><td>Remove Misses</td><td>" + debugStats.removeMisses
        + "</td></tr>");
    output.append("<tr><td>Move Misses</td><td>" + debugStats.moveMisses
        + "</td></tr>");
    output.append("</table>\n");
    // symbolTable.debugDumpHtml(output);
    return output.toString();
  }

  /**
   * Take a raw timeline record and convert it into a bottom up profile.
   * 
   * @param rawEvent a raw JSON timeline event of type EventType.PROFILE_DATA
   */
  @Override
  public void parseRawEvent(final JavaScriptProfileEvent rawEvent,
      final UiEvent refRecord, JavaScriptProfile profile) {
    assert rawEvent.getFormat().equals(FORMAT);
    String profileData = rawEvent.getProfileData();
    if (profileData == null || profileData.length() == 0) {
      refRecord.setHasJavaScriptProfile(false);
      return;
    }

    if (workQueue == null) {
      // Process the event synchronously
      currentProfile = profile;
      JSOArray<String> logLines = JSOArray.splitString(
          rawEvent.getProfileData(), "\n");
      processLogLines(refRecord, logLines, 0);
    } else {
      // Process the log entries using a deferred command to keep from blocking
      // the UI thread.
      refRecord.setProcessingJavaScriptProfile();
      workQueue.append(new NewProfileDataWorker(refRecord, profile, rawEvent));
    }
  }

  /**
   * This method is intended for use by the unit tests only.
   */
  V8Symbol findSymbol(double address) {
    return symbolTable.lookup(address);
  }

  /**
   * Returns a number corresponding to the address string.
   * 
   * @param address a string formatted as a leading + or - followed by a hex
   *          number.
   * 
   * @return a number corresponding to the address string.
   */
  double parseAddress(String addressString, String addressTag) {

    // TODO(zundel): Try using JavaScript's parseInt() rather than
    // Long.parseLong() for better performance. Will performance
    // in Development mode still be acceptable?
    if (addressString.equals("overflow")) {
      return 0;
    } else if (addressString.startsWith("0x")) {
      return Long.parseLong(addressString.substring(2), 16);
    } else if (addressString.startsWith("0")) {
      return Long.parseLong(addressString, 8);
    }

    double baseAddress = 0;
    if (addressTag != null) {
      baseAddress = addressTags.get(addressTag);
    }
    double address = 0;
    if (addressString.startsWith("+")) {
      addressString = addressString.substring(1);
      address = baseAddress + Long.parseLong(addressString, 16);
    } else if (addressString.startsWith("-")) {
      addressString = addressString.substring(1);
      address = baseAddress - Long.parseLong(addressString, 16);
    } else {
      address = Long.parseLong(addressString, 16);
    }
    if (addressTag != null) {
      addressTags.put(addressTag, address);
    }
    return address;
  }

  /**
   * Scrubs a string of any embedded HTML or JavaScript.
   */
  String scrubStringForXSS(String input) {
    scrubbingDiv.setInnerText(input);
    return scrubbingDiv.getInnerText();
  }

  /**
   * Convenience method to create a new ActionType and add it to the map.
   */
  private ActionType createActionType(String actionName, int actionValue) {
    ActionType type = new ActionType(actionName, actionValue);
    actionTypeMap.put(actionName, type);
    return type;
  }

  /**
   * Convenience method to create a new SymbolType and add it to the map.
   */
  private void createSymbolType(String symbolName, int symbolValue) {
    V8SymbolType type = new V8SymbolType(symbolName, symbolValue);
    symbolTypeMap.put(symbolName, type);
  }

  /**
   * Given an array of the fields in a single log line, execute the appropriate
   * action on that entry based on the first field.
   */
  private void parseLogEntry(JsArrayString logEntries) {
    if (logEntries.length() == 0) {
      return;
    }
    String command = logEntries.get(0);
    if (command.length() == 0) {
      return;
    }
    LogAction cmdMethod = logActions.get(actionTypeMap.get(command));
    if (cmdMethod != null) {
      cmdMethod.doAction(logEntries);
    } else {
      Logging.getLogger().logText("Unknown v8 profiler command: " + command);
    }
  }

  /**
   * Process an 'alias' command. Simply aliases a command to a different string.
   * The format of this log entry is:
   * 
   * alias, aliasName, originalName
   */
  private void parseV8AliasEntry(JsArrayString logEntries) {
    assert logEntries.length() == 3;

    String originalName = logEntries.get(2);
    String aliasName = logEntries.get(1);

    V8SymbolType symbol = symbolTypeMap.get(originalName);
    if (symbol != null) {
      symbolTypeMap.put(aliasName, symbol);
    } else {
      ActionType action = actionTypeMap.get(originalName);
      if (action != null) {
        actionTypeMap.put(aliasName, action);
      } else {
        Logging.getLogger().logText(
            "Unable to find command: '" + logEntries.get(2)
                + "' to match alias:" + logEntries.get(1));
      }
    }
  }

  /**
   * New code was added to the virtual machine. The format of this log entry is:
   * 
   * code-creation, symbolType, offset, length, "symbolName"
   * 
   * e.g. code-creation,lic,-5910913e,179,"parentNode"
   * 
   */
  private void parseV8CodeCreationEntry(JsArrayString logEntries) {
    assert logEntries.length() == 5;
    V8SymbolType symbolType = symbolTypeMap.get(logEntries.get(1));

    String name = logEntries.get(4);
    double address = parseAddress(logEntries.get(2), ADDRESS_TAG_CODE);
    int executableSize = Integer.parseInt(logEntries.get(3));

    // Keep some debugging stats around
    V8Symbol found = symbolTable.lookup(address);
    if (found != null) {
      debugStats.addCollisions++;
    }

    V8Symbol symbol = new V8Symbol(scrubStringForXSS(name), symbolType,
        address, executableSize);
    symbolTable.add(symbol);
  }

  /**
   * Process a code-delete entry in the log.
   * 
   * The format of this entry is:
   * 
   * code-delete, address
   */
  private void parseV8CodeDeleteEntry(JsArrayString logEntries) {
    assert logEntries.length() == 2;
    double address = parseAddress(logEntries.get(1), ADDRESS_TAG_CODE);
    V8Symbol symbol = symbolTable.lookup(address);
    if (symbol != null) {
      symbolTable.remove(symbol);
    } else {
      // update debugging stats
      debugStats.removeMisses++;
    }
  }

  /**
   * Process a code-move entry
   * 
   * The format of this entry is:
   * 
   * code-move, fromAddress, toAddress
   */
  private void parseV8CodeMoveEntry(JsArrayString logEntries) {
    assert logEntries.length() == 3;
    double fromAddress = parseAddress(logEntries.get(1), ADDRESS_TAG_CODE);
    double toAddress = parseAddress(logEntries.get(2), ADDRESS_TAG_CODE_MOVE);
    V8Symbol symbol = symbolTable.lookup(fromAddress);
    if (symbol != null) {
      symbolTable.remove(symbol);
      V8Symbol newSymbol = new V8Symbol(symbol.getName(),
          symbol.getSymbolType(), toAddress,
          symbol.getAddressSpan().getLength());
      symbolTable.add(newSymbol);
    } else {
      // update debugging stats
      debugStats.moveMisses++;
    }
  }

  /**
   * Parse a profiler entry
   * 
   * The format of this entry is:
   * 
   * profiler, "type", ...
   */
  private void parseV8ProfilerEntry(JsArrayString logEntries) {
    final String arg = logEntries.get(1);
    if (arg.equals("compression")) {
      int windowSize = Integer.parseInt(logEntries.get(2));
      this.logDecompressor = new V8LogDecompressor(windowSize);
    } else if (arg.equals("begin")) {
      // TODO(zundel): make sure all state is reset
      populateAddressTags();
    } else if (arg.equals("pause") || arg.equals("resume")) {
      // ignore pause and resume entries.
    } else {
      Logging.getLogger().logText(
          "Ignoring profiler command: " + logEntries.join());
    }
  }

  /**
   * A repeat entry is used to indicate that the command following is repeated
   * multiple times.
   */
  private void parseV8RepeatEntry(JsArrayString logEntries) {
    int numRepeats = Integer.parseInt(logEntries.get(1));
    // run the command after the first 2 arguments numRepeats times.
    logEntries.shift();
    logEntries.shift();
    for (int i = 0; i < numRepeats; ++i) {
      parseLogEntry(logEntries);
    }
  }

  /**
   * Process a tick entry in the v8 log. The format of this log entry is:
   * 
   * command, codeOffset, stackOffset, type, <codeOffset2, <codeOffset3, <...>>>
   * 
   * e.g.: t,-7364bb,+45c,0
   */
  private void parseV8TickEntry(JsArrayString logEntries) {
    assert logEntries.length() >= 4;
    double address = parseAddress(logEntries.get(1), ADDRESS_TAG_CODE);
    // stack address is currently ignored, but it must be parsed to keep the
    // stack address tag up to date if anyone else ever wants to use it.
    // double stackAddress = parseAddress(logEntries.get(2), ADDRESS_TAG_STACK);
    int vmState = Integer.parseInt(logEntries.get(3));
    currentProfile.addStateTime(vmState, 1.0);

    List<V8Symbol> symbols = new ArrayList<V8Symbol>();
    V8Symbol found = symbolTable.lookup(address);
    if (found != null) {
      symbols.add(found);
    }
    addressTags.put(ADDRESS_TAG_SCRATCH, address);
    for (int i = 4; i < logEntries.length(); ++i) {
      address = parseAddress(logEntries.get(i), ADDRESS_TAG_SCRATCH);
      found = symbolTable.lookup(address);
      if (found != null) {
        symbols.add(found);
      }
    }

    updateFlatProfile(symbols, vmState);
    updateBottomUpProfile(symbols, vmState);
    updateTopDownProfile(symbols, vmState);
  }

  private void populateActionTypes() {
    ActionType aliasType = createActionType("alias", ACTION_TYPE_ALIAS);
    ActionType profilerType = createActionType("profiler", ACTION_TYPE_PROFILER);
    ActionType codeCreationType = createActionType("code-creation",
        ACTION_TYPE_CODE_CREATION);
    ActionType codeMoveType = createActionType("code-move",
        ACTION_TYPE_CODE_MOVE);
    ActionType codeDeleteType = createActionType("code-delete",
        ACTION_TYPE_CODE_DELETE);
    ActionType tickType = createActionType("tick", ACTION_TYPE_TICK);
    ActionType repeatType = createActionType("repeat", ACTION_TYPE_REPEAT);

    logActions.put(aliasType, new LogAction() {
      public void doAction(JsArrayString logLine) {
        parseV8AliasEntry(logLine);
      }
    });
    logActions.put(profilerType, new LogAction() {
      public void doAction(JsArrayString logLine) {
        parseV8ProfilerEntry(logLine);
      }
    });
    logActions.put(codeCreationType, new LogAction() {
      public void doAction(JsArrayString logLine) {
        parseV8CodeCreationEntry(logLine);
      }
    });
    logActions.put(codeMoveType, new LogAction() {
      public void doAction(JsArrayString logLine) {
        parseV8CodeMoveEntry(logLine);
      }
    });
    logActions.put(codeDeleteType, new LogAction() {
      public void doAction(JsArrayString logLine) {
        parseV8CodeDeleteEntry(logLine);
      }
    });
    logActions.put(tickType, new LogAction() {
      public void doAction(JsArrayString logLine) {
        parseV8TickEntry(logLine);
      }
    });
    logActions.put(repeatType, new LogAction() {
      public void doAction(JsArrayString logLine) {
        parseV8RepeatEntry(logLine);
      }
    });
  }

  private void populateAddressTags() {
    addressTags.put(ADDRESS_TAG_CODE, 0.);
    addressTags.put(ADDRESS_TAG_CODE_MOVE, 0.);
    addressTags.put(ADDRESS_TAG_STACK, 0.);
  }

  private void populateSymbolTypes() {
    createSymbolType("Builtin", SYMBOL_TYPE_BUILTIN);
    createSymbolType("CallDebugBreak", SYMBOL_TYPE_CALL_DEBUG_BREAK);
    createSymbolType("CallDebugPrepareStepIn",
        SYMBOL_TYPE_CALL_DEBUG_PREPARE_STEP_IN);
    createSymbolType("CallIC", SYMBOL_TYPE_CALL_IC);
    createSymbolType("CallInitialize", SYMBOL_TYPE_CALL_INITIALIZE);
    createSymbolType("CallMegamorphic", SYMBOL_TYPE_CALL_MEGAMORPHIC);
    createSymbolType("CallMiss", SYMBOL_TYPE_CALL_MISS);
    createSymbolType("CallNormal", SYMBOL_TYPE_CALL_NORMAL);
    createSymbolType("CallPreMonomorphic", SYMBOL_TYPE_PRE_MONOMORPHIC);
    createSymbolType("Callback", SYMBOL_TYPE_CALLBACK);
    createSymbolType("Eval", SYMBOL_TYPE_EVAL);
    createSymbolType("Function", SYMBOL_TYPE_FUNCTION);
    createSymbolType("KeyedCallIC", SYMBOL_TYPE_KEYED_CALL_IC);
    createSymbolType("KeyedLoadIC", SYMBOL_TYPE_KEYED_LOAD_IC);
    createSymbolType("KeyedStoreIC", SYMBOL_TYPE_KEYED_STORE_IC);
    createSymbolType("LazyCompile", SYMBOL_TYPE_LAZY_COMPILE);
    createSymbolType("LoadIC", SYMBOL_TYPE_LOAD_IC);
    createSymbolType("RegExp", SYMBOL_TYPE_REG_EXP);
    createSymbolType("Script", SYMBOL_TYPE_SCRIPT);
    createSymbolType("StoreIC", SYMBOL_TYPE_STORE_IC);
    createSymbolType("Stub", SYMBOL_TYPE_STUB);
  }

  /**
   * Process a portion of the logLines array. If the workQueue is enabled, exit
   * early if the timeslice expires.
   */
  private void processLogLines(final UiEvent refRecord,
      final JSOArray<String> logLines, int currentLine) {
    final double endTime = Duration.currentTimeMillis()
        + LOG_PROCESS_CHUNK_TIME_MS;
    final int logLinesLength = logLines.size();

    for (; currentLine < logLinesLength; ++currentLine) {
      if (workQueue != null) {
        // Occasionally check to see if the time to run this chunk has expired.
        if ((currentLine % 10 == 0)
            && (Duration.currentTimeMillis() >= endTime)) {
          break;
        }
      }

      String logLine = logLines.get(currentLine);
      if (logDecompressor != null && logLine.length() > 0) {
        logLine = logDecompressor.decompressLogEntry(logLine);
      }
      JsArrayString decompressedLogLine = Csv.split(logLine);
      if (decompressedLogLine.length() > 0) {
        parseLogEntry(decompressedLogLine);
      }

      // force gc on processed log lines.
      logLines.set(currentLine, null);
    }

    if (currentLine < logLinesLength) {
      // Schedule this record to be the next thing run off the queue
      workQueue.prepend(new LogLineWorker(logLines, refRecord, currentLine));
    } else {
      // All done!
      if (currentProfile.getProfile(JavaScriptProfile.PROFILE_TYPE_BOTTOM_UP) == null) {
        refRecord.setHasJavaScriptProfile(false);
      } else {
        refRecord.setHasJavaScriptProfile(true);
      }
    }
  }

  private JavaScriptProfileNode recordAddressInProfile(
      JavaScriptProfileNode profileNode, V8Symbol symbol,
      boolean recordedSelfTime) {
    assert profileNode != null;
    String name = symbol.getName();
    JavaScriptProfileNode child = profileNode.getOrInsertChild("".equals(name)
        ? "(unknown)" : name);
    assert child != null;

    if (!recordedSelfTime) {
      child.addSelfTime(1.0);
    } else {
      child.addTime(1.0);
    }

    child.setSymbolType((symbol.getSymbolType().getName()));

    return child;
  }

  /**
   * No entry has been found in the symbol table. Add or update an unknown
   * entry.
   */
  private void recordUnknownTick(JavaScriptProfileNode profileNode, int vmState) {
    JavaScriptProfileNode child = profileNode.getOrInsertChild("unknown - "
        + JavaScriptProfile.stateToString(vmState));
    child.addSelfTime(1.0);
  }

  /**
   * Add this stack frame of resolved symbols to the bottom up profile.
   */
  private void updateBottomUpProfile(List<V8Symbol> symbols, int vmState) {
    JavaScriptProfileNode bottomUpProfile = currentProfile.getOrCreateProfile(JavaScriptProfile.PROFILE_TYPE_BOTTOM_UP);
    bottomUpProfile.addTime(1.0);

    if (symbols.size() == 0) {
      recordUnknownTick(bottomUpProfile, vmState);
    } else {
      JavaScriptProfileNode child = bottomUpProfile;
      for (int i = 0, length = symbols.size(); i < length; ++i) {
        child = recordAddressInProfile(child, symbols.get(i), i > 0);
      }
    }
  }

  /**
   * Add this stack frame of resolved symbols to the flat profile.
   */
  private void updateFlatProfile(List<V8Symbol> symbols, int vmState) {
    JavaScriptProfileNode flatProfile = currentProfile.getOrCreateProfile(JavaScriptProfile.PROFILE_TYPE_FLAT);
    flatProfile.addTime(1.0);

    if (symbols.size() == 0) {
      recordUnknownTick(flatProfile, vmState);
    } else {
      for (int i = 0; i < symbols.size(); ++i) {
        recordAddressInProfile(flatProfile, symbols.get(i), i > 0);
      }
    }
  }

  /**
   * Add this stack frame of resolved symbols to the top down profile.
   */
  private void updateTopDownProfile(List<V8Symbol> symbols, int vmState) {
    JavaScriptProfileNode topDownProfile = currentProfile.getOrCreateProfile(JavaScriptProfile.PROFILE_TYPE_TOP_DOWN);
    topDownProfile.addTime(1.0);

    if (symbols.size() == 0) {
      recordUnknownTick(topDownProfile, vmState);
    } else {
      JavaScriptProfileNode child = topDownProfile;
      Collections.reverse(symbols);
      for (int i = 0, length = symbols.size(); i < length; ++i) {
        child = recordAddressInProfile(child, symbols.get(i),
            !(i == (symbols.size() - 1)));
      }
    }
  }
}
