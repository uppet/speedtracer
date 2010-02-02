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
package com.google.speedtracer.client.util;

import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DeferredCommand;

import java.util.ArrayList;
import java.util.List;

/**
 * A class that uses deferred command to keep a queue of tasks going. It allows
 * you to insert new tasks at either the beginning or end of the queue to
 * control priority.
 */
public class WorkQueue implements Command {

  /**
   * Implement WorkQueueNode to create a new task to add to the
   * {@link WorkQueue}
   */
  public interface Node extends Command {
    public String getDescription();
  }

  // Makes sure only one command is enqueued at a time.
  private boolean commandQueued = false;
  private List<Node> queue = new ArrayList<Node>();

  public WorkQueue() {
  }

  /**
   * Add work to the back of the queue.
   */
  public void append(Node node) {
    queue.add(node);
    enqueueCommand();
  }

  public void execute() throws RuntimeException {
    commandQueued = false;
    // Execute the first item off the queue.
    Node node = queue.remove(0);
    node.execute();
    enqueueCommand();
  }

  /**
   * Add work to the front of the queue.
   */
  public void prepend(Node node) {
    queue.add(0, node);
    enqueueCommand();
  }

  private void enqueueCommand() {
    // If there is anything else on the queue, queue up a deferred command.
    if (queue.size() > 0 && commandQueued == false) {
      DeferredCommand.addCommand(this);
      commandQueued = true;
    }
  }
}
