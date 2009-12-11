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
package com.google.speedtracer.client.view;

import com.google.speedtracer.client.view.SortableTableHeader.SortToggleListener;

import java.util.ArrayList;
import java.util.List;

/**
 * Allows a group of table headers to be linked together. This class is
 * currently only responsible for managing turning off the visual sort
 * indicators of other headers in the group.
 */
public class SortableTableHeaderGroup {

  private List<SortableTableHeader> headers = new ArrayList<SortableTableHeader>();

  public SortableTableHeaderGroup() {
  }

  /**
   * Adds a new header to the group.
   */
  public void add(final SortableTableHeader header) {
    this.headers.add(header);
    header.addSortToggleListener(new SortToggleListener() {
      public void onSortToggle(boolean ascending) {
        // Turn off the sorting on all the other headers.
        int length = headers.size();
        for (int i = 0; i < length; ++i) {
          SortableTableHeader peer = headers.get(i);
          if (!peer.equals(header)) {
            peer.setNotSorting();
          }
        }
      }
    });
  }

}
