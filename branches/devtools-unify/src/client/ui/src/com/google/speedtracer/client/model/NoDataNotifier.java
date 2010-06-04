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

import com.google.speedtracer.client.NotificationSlideout;
import com.google.speedtracer.client.timeline.Constants;
import com.google.speedtracer.client.timeline.DomainObserver;
import com.google.speedtracer.client.timeline.TimeLineModel;
import com.google.speedtracer.client.util.Command;

/**
 * Class that waits for the first bit of data to come in to timeline. If no data
 * comes in after 5 seconds, we notify using the supplied notification widget.
 * When the first bit of data somes in, we hide the notification widget and
 * neuter our observation on the TimeLineModel.
 */
public class NoDataNotifier implements DomainObserver {

  private static final int NO_DATA_TIMEOUT = 6000;

  private boolean hasData;

  private final TimeLineModel model;

  private final NotificationSlideout slideout;

  public NoDataNotifier(TimeLineModel model, NotificationSlideout slideout) {
    this.model = model;
    this.slideout = slideout;
    this.hasData = false;

    model.addDomainObserver(this);

    // Set a timeout to check for data in 5 seconds.
    Command.defer(new Command.Method() {
      public void execute() {
        if (hasData) {
          return;
        }
        displayNotification();
      }
    }, NO_DATA_TIMEOUT);
  }

  public void onDomainChange(double newValue) {
    // We have some data that came in.
    this.hasData = true;

    // Hide any notifications that may have been displayed.
    NoDataNotifier.this.slideout.hide();

    // Remove ourselves from the domain observer sometime later.
    Command.defer(new Command.Method() {
      public void execute() {
        NoDataNotifier.this.model.removeDomainObserver(NoDataNotifier.this);
      }
    });
  }

  private void displayNotification() {
    slideout.setContentHtml("No data received in "
        + (NO_DATA_TIMEOUT / 1000)
        + " seconds. Chrome must be run with the flag: <br/> '<span style=\"color:green\">--enable-extension-timeline-api</span>'."
        + "<br/> See <a target='_blank' href='" + Constants.HELP_URL
        + "'>getting started</a> for more details.");
    slideout.show();
  }
}
