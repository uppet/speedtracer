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

import com.google.speedtracer.client.Logging;
import com.google.speedtracer.client.model.DataModel.EventCallbackProxy;
import com.google.speedtracer.client.model.DataModel.EventCallbackProxyProvider;
import com.google.speedtracer.client.util.JsIntegerMap;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Handles profile data records and stores parsed profiles for later retrieval.
 * 
 */
public class JavaScriptProfileModel implements EventCallbackProxyProvider {
  private final EventCallbackProxy profileProxy;
  private final JsIntegerMap<JavaScriptProfile> profileMap = JsIntegerMap.createObject().cast();
  private JavaScriptProfileModelImpl impl;

  /**
   * Sorts in descending order, first by self time, then by time fields.
   */
  private Comparator<JavaScriptProfileNode> nodeTimeComparator = new Comparator<JavaScriptProfileNode>() {

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

  JavaScriptProfileModel(final DataModel dataModel) {
    profileProxy = new EventCallbackProxy() {
      public void onEventRecord(EventRecord data) {
        JavaScriptProfileEvent profileData = data.cast();
        // Add a reference to this record from the preceding event.
        int refSequence = data.getSequence() - 1;
        UiEvent rec = (UiEvent) dataModel.findEventRecord(refSequence);
        if (rec != null) {
          rec.setHasJavaScriptProfile();
        }

        // Lazily initialize the impl class. We don't know which one to
        // instantiate until we get the first profile record.
        if (impl == null) {
          String format = profileData.getFormat();
          if (format.equals(JavaScriptProfileModelV8Impl.FORMAT)) {
            impl = new JavaScriptProfileModelV8Impl();
          } else {
            Logging.getLogger().logText(
                "No profile model available for profile format: " + format);

            // Create a null implementation that just throws the data away
            impl = new JavaScriptProfileModelImpl("null") {
              @Override
              public String getDebugDumpHtml() {
                return null;
              }

              @Override
              public void parseRawEvent(JavaScriptProfileEvent event,
                  JavaScriptProfile profile) {
              }
            };
          }
        }

        JavaScriptProfile profile = new JavaScriptProfile();
        impl.parseRawEvent(profileData, profile);
        if (profile.getBottomUpProfile() != null) {
          profileMap.put(refSequence, profile);
        }
      }
    };
  }

  public String getDebugDumpHtml() {
    return impl.getDebugDumpHtml();
  }

  public EventCallbackProxy getEventCallback(EventRecord data) {
    if (JavaScriptProfileEvent.isProfileEvent(data)) {
      return profileProxy;
    }
    return null;
  }

  /**
   * Return the profile for the specified event in a simple HTML representation.
   * 
   * TODO(zundel): This method is here just for debugging purposes.
   */
  public String getProfileHtmlForEvent(int sequence) {
    StringBuilder result = new StringBuilder();
    JavaScriptProfile profile = profileMap.get(sequence);
    if (profile == null) {
      return result.toString();
    }

    // Give a table of the time spent in each VM state
    result.append("<h3>VM States</h3>");
    result.append("<table>");
    double total = profile.getTotalTime();
    for (int index = 0; index < JavaScriptProfile.NUM_STATES; ++index) {
      double value = profile.getStateTime(index);
      if (value > 0.0) {
        int percent = (int) ((value / total) * 100.0);
        result.append("<tr><td>" + JavaScriptProfile.stateToString(index)
            + "</td><td>" + value + "ms </td><td>" + percent + "%</td></tr>");
      }
    }
    result.append("</table>");

    // Give the profile
    JavaScriptProfileNode bottomUpProfile = profile.getBottomUpProfile();
    result.append("<h3>Profile:</h3>\n");
    dumpNodeChildrenRecursive(bottomUpProfile, result);

    // Some additional debugging info
    // result.append(impl.getDebugDumpHtml());
    return result.toString();
  }

  /**
   * Helper for getProfileHtmlForEvent().
   */
  private void dumpNodeChildrenRecursive(JavaScriptProfileNode profile,
      StringBuilder result) {
    List<JavaScriptProfileNode> children = profile.getChildren();
    Collections.sort(children, nodeTimeComparator);
    result.append("<ul>\n");
    for (JavaScriptProfileNode child : children) {
      result.append("<li>\n");
      result.append(child.getSymbolName());
      result.append(" selfTime: ");
      result.append(child.getSelfTime());
      result.append("ms time: ");
      result.append(child.getTime());
      result.append("ms </li>\n");
      dumpNodeChildrenRecursive(child, result);
    }
    result.append("</ul>\n");
  }
}
