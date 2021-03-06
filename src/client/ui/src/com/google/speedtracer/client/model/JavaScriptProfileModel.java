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

import com.google.gwt.core.client.JsArrayNumber;
import com.google.gwt.coreext.client.JSOArray;
import com.google.gwt.coreext.client.JsIntegerMap;
import com.google.speedtracer.client.ClientConfig;
import com.google.speedtracer.client.Logging;
import com.google.speedtracer.client.model.DataDispatcher.DataDispatcherDelegate;
import com.google.speedtracer.client.util.TimeStampFormatter;
import com.google.speedtracer.client.util.WorkQueue;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Handles profile data records and stores parsed profiles for later retrieval.
 */
public class JavaScriptProfileModel implements DataDispatcherDelegate {
  /**
   * A callback object for processing each event contained within a
   * {@link JavaScriptProfileModel}.
   * 
   * @see JavaScriptProfileModel#processEventsWithProfiles(EventProcessor)
   */
  public interface EventProcessor {
    void onCompleted();

    void process(UiEvent event);
  }

  private class EventProcessorWorker implements WorkQueue.Node {
    public final EventProcessor processor;
    private final int currentEventSequence;
    private final JsArrayNumber eventsWithProfiles;

    public EventProcessorWorker(EventProcessor processor,
        JsArrayNumber eventsWithProfiles, int currentEventSequence) {
      this.processor = processor;
      this.eventsWithProfiles = eventsWithProfiles;
      this.currentEventSequence = currentEventSequence;
    }

    public void execute() {
      for (int i = currentEventSequence, length = eventsWithProfiles.length(); i < length; ++i) {
        int eventSequence = (int) eventsWithProfiles.get(i);
        UiEvent event = eventRecordLookup.findEventRecordFromSequence(eventSequence).cast();
        assert UiEvent.isUiEvent(event);
        assert event.hasJavaScriptProfile();
        processor.process(event);
        if (workQueue.isTimeSliceExpired()) {
          workQueue.append(new EventProcessorWorker(processor,
              eventsWithProfiles, i + 1));
          return;
        }
      }
      processor.onCompleted();
    }

    public String getDescription() {
      return "EventProcessorWorker";
    }
  }

  /**
   * Sorts in descending order, first by self time, then by time fields.
   */
  public static final Comparator<JavaScriptProfileNode> nodeTimeComparator = new Comparator<JavaScriptProfileNode>() {

    public int compare(JavaScriptProfileNode o1, JavaScriptProfileNode o2) {
      if (o1.getSelfTime() > o2.getSelfTime()) {
        return -1;
      } else if (o1.getSelfTime() < o2.getSelfTime()) {
        return 1;
      }
      if (o1.getTime() > o2.getTime()) {
        return -1;
      } else if (o1.getTime() < o2.getTime()) {
        return 1;
      }
      return 0;
    }
  };
  private final EventRecordLookup eventRecordLookup;
  private JavaScriptProfileModelImpl impl;
  private final JsIntegerMap<JavaScriptProfile> profileMap = JsIntegerMap.createObject().cast();
  private final WorkQueue workQueue = new WorkQueue();

  JavaScriptProfileModel(final EventRecordLookup eventRecordLookup) {
    this.eventRecordLookup = eventRecordLookup;
  }

  public void clearData() {
    // no-op
  }

  public String getDebugDumpHtml() {
    return impl.getDebugDumpHtml();
  }

  public JavaScriptProfile getProfileForEvent(int sequence) {
    return profileMap.get(sequence);
  }

  /**
   * TODO(zundel): This method is here just for debugging purposes. Eventually
   * we need to remove it or put behind deferred binding.
   * 
   * Return the profile for the specified event in a simple HTML representation.
   * 
   * @param sequence the event sequence number to look for
   * @param profileType one of {@link JavaScriptProfileModel} PROFILE_TYPE
   *          definitions.
   */
  public String getProfileHtmlForEvent(int sequence, int profileType) {
    StringBuilder result = new StringBuilder();
    JavaScriptProfile profile = profileMap.get(sequence);
    if (profile == null) {
      return result.toString();
    }

    profile.getVmStateHtml(result);
    result.append("<p></p>");

    JavaScriptProfileNode topNode = profile.getProfile(profileType);
    if (topNode != null) {

      // Give the profile
      switch (profileType) {
        case JavaScriptProfile.PROFILE_TYPE_FLAT:
          dumpNodeChildrenFlat(profile.getTotalTime(), topNode, result);
          break;
        case JavaScriptProfile.PROFILE_TYPE_BOTTOM_UP:
        case JavaScriptProfile.PROFILE_TYPE_TOP_DOWN:
          dumpNodeChildrenRecursive(profile.getTotalTime(), topNode, result);
          break;
        default:
          assert false;
      }
    }

    // Some additional debugging info
    // result.append(impl.getDebugDumpHtml());
    return result.toString();
  }

  public void onEventRecord(EventRecord data) {
    if (JavaScriptProfileEvent.isProfileEvent(data)) {
      final JavaScriptProfileEvent profileData = data.cast();
      // Add a reference to this record from the preceding event, unless this
      // profile is marked as not belonging to a timeline event (log entries
      // that were created between events).
      UiEvent rec = null;
      if (!profileData.isOrphaned()) {
        rec = eventRecordLookup.findEventRecordFromSequence(profileData.getSequence() - 1).cast();
      }
      processProfileData(rec, profileData);
    }
  }

  /**
   * Iterates over any {@link UiEvent} that has processed profile data
   * associated with it. The iteration will be carried out asynchronously and
   * calls to {@link EventProcessor#process(UiEvent)} may be deferred via Timer.
   * 
   * @param processor
   */
  public void processEventsWithProfiles(EventProcessor processor) {
    workQueue.append(new EventProcessorWorker(processor, profileMap.getKeys(),
        0));
  }

  private void dumpNodeChildrenFlat(double totalTime,
      JavaScriptProfileNode profile, StringBuilder result) {
    List<JavaScriptProfileNode> children = profile.getChildren();
    if (children == null) {
      return;
    }
    Collections.sort(children, nodeTimeComparator);
    result.append("<table>\n");
    result.append("<tr>");
    result.append("<th>Symbol</th>");
    result.append("<th>Self Time</th>");
    result.append("<th>Time</th>");
    result.append("</tr>\n");

    for (int i = 0, length = children.size(); i < length; ++i) {
      JavaScriptProfileNode child = children.get(i);

      result.append("<tr>");
      result.append("<td>"
          + formatSymbolName(child.getSymbol().getSymbolName()) + "</td>");
      double relativeSelfTime = (totalTime > 0
          ? (child.getSelfTime() / totalTime) * 100 : 0);
      double relativeTime = (totalTime > 0
          ? (child.getTime() / totalTime) * 100 : 0);
      result.append("<td><b>");
      result.append(TimeStampFormatter.formatToFixedDecimalPoint(
          relativeSelfTime, 1));
      result.append("%</b></td>");
      result.append("<td>");
      result.append(TimeStampFormatter.formatToFixedDecimalPoint(relativeTime,
          1));
      result.append("%</td>");
      result.append("</tr>\n");
    }
    result.append("</table>\n");
  }

  /**
   * Helper for getProfileHtmlForEvent().
   */
  private void dumpNodeChildrenRecursive(double totalTime,
      JavaScriptProfileNode profile, StringBuilder result) {
    List<JavaScriptProfileNode> children = profile.getChildren();
    if (children == null) {
      return;
    }
    Collections.sort(children, nodeTimeComparator);
    result.append("<ul>\n");
    for (int i = 0, length = children.size(); i < length; ++i) {
      JavaScriptProfileNode child = children.get(i);
      int relativeSelfTime = (int) (totalTime > 0
          ? (child.getSelfTime() / totalTime) * 100 : 0);
      int relativeTime = (int) (totalTime > 0
          ? (child.getTime() / totalTime) * 100 : 0);

      result.append("<li>\n");
      result.append(formatSymbolName(child.getSymbol().getSymbolName()));
      result.append(" <b>self: ");
      result.append(TimeStampFormatter.formatToFixedDecimalPoint(
          relativeSelfTime, 1));
      result.append("%</b> ");
      result.append(" (");
      result.append(TimeStampFormatter.formatToFixedDecimalPoint(relativeTime,
          1));
      result.append("%) ");
      result.append("</li>\n");
      dumpNodeChildrenRecursive(totalTime, child, result);
    }
    result.append("</ul>\n");
  }

  private String formatSymbolName(String symbolName) {
    JSOArray<String> vals = JSOArray.splitString(symbolName, " ");
    StringBuilder result = new StringBuilder();
    for (int i = 0, length = vals.size(); i < length; ++i) {
      String val = vals.get(i);
      if (val.startsWith("http://") || val.startsWith("https://")
          || val.startsWith("file://") || val.startsWith("chrome://")
          || val.startsWith("chrome-extension://")) {
        // Presenting the entire URL takes too much space. Just show the
        // last path component.
        String resource = val.substring(val.lastIndexOf("/") + 1);

        resource = "".equals(resource) ? val : resource;
        result.append(resource);
      } else {
        result.append(val);
      }
      result.append(" ");
    }
    return result.toString();
  }

  /**
   * Delegates processing to the implementation specific profile data parser.
   * 
   * @param recordSequence If >= 0, associate this profile with the specified
   *          node.
   * @param profileData the unprocessed profile data timeline record.
   */
  private void processProfileData(UiEvent refRecord,
      JavaScriptProfileEvent profileData) {
    // Lazily initialize the impl class. We don't know which one to
    // instantiate until we get the first profile record.
    if (impl == null) {
      String format = profileData.getFormat();
      if (format.equals(JavaScriptProfileModelV8Impl.FORMAT)) {
        impl = new JavaScriptProfileModelV8Impl(workQueue);
      } else {
        if (ClientConfig.isDebugMode()) {
          Logging.getLogger().logText(
              "No profile model available for profile format: " + format);
        }

        // Create a null implementation that just throws the data away
        impl = new JavaScriptProfileModelImpl("null") {
          @Override
          public String getDebugDumpHtml() {
            return null;
          }

          @Override
          public void parseRawEvent(JavaScriptProfileEvent event,
              UiEvent refRecord, JavaScriptProfile profile) {
          }
        };
      }
    }
    JavaScriptProfile profile = new JavaScriptProfile();
    impl.parseRawEvent(profileData, refRecord, profile);
    if (refRecord != null) {
      profileMap.put(refRecord.getSequence(), profile);
    }
  }
}
