/*
 * Copyright 2008 Google Inc.
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
package com.google.speedtracer.client.visualizations.model;

import com.google.speedtracer.client.model.LogEvent;
import com.google.speedtracer.client.model.UiEvent;
import com.google.speedtracer.client.model.EventVisitor.PreOrderVisitor;
import com.google.speedtracer.client.util.JSOArray;

/**
 * Visitor that handles white listing of nodes in the Event Details Tree that
 * contain log messages to make them exempt from hiding of collapsing.
 */
public class LogMessageVisitor implements PreOrderVisitor {

  private static native void markAncestors(UiEvent e) /*-{
    // Stopgap fix for the situation where console logs cause us to render very 
    // large trees. We limit each level of the tree to 20 log messages.
    if (e.parent) {
      var parent = e.parent;
      var count = parent.logCount || 0;
      if (count > 20) {
        return;
      } 
      parent.logCount = count + 1;
    } 

    var currentNode = e;

    // Walk up the parent backref and white list everything until we hit a node
    // that has already been whitelisted or we have no more backreferences.
    while (currentNode && !currentNode.hasUserLogs) {
      currentNode.hasUserLogs = true;
      currentNode = currentNode.parent;
    }
  }-*/;

  private static native void installBackRef(UiEvent parent, UiEvent child) /*-{
    child.parent = parent;
  }-*/;

  public void postProcess() {
    // We do not have a post processing step
  }

  public void visitUiEvent(UiEvent e) {
    if (e.hasUserLogs()) {
      // all done.
      return;
    }

    // Eagerly (and sneekily) Install backref to parent on all the children.
    JSOArray<UiEvent> children = e.getChildren();
    for (int i = 0, n = children.size(); i < n; i++) {
      installBackRef(e, children.get(i));
    }

    if (e.getType() == LogEvent.TYPE) {
      // Walk the parent backref back up to whitelist
      markAncestors(e);
    }
  }

}
