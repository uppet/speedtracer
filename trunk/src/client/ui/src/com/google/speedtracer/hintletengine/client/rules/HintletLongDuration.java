/*
 * Copyright 2011 Google Inc.
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

package com.google.speedtracer.hintletengine.client.rules;

import com.google.speedtracer.client.model.EventRecord;
import com.google.speedtracer.client.model.HintRecord;
import com.google.speedtracer.client.model.UiEvent;
import com.google.speedtracer.client.util.TimeStampFormatter;
import com.google.speedtracer.hintletengine.client.HintletOnHintListener;

/**
 * Long Duration Hintlet
 * 
 */
public final class HintletLongDuration extends HintletRule {

  // 100 milliseconds, threshold of human perception
  private static final int HINTLET_LOW_DURATION_THRESHOLD = 100;
  // 2 seconds - a very long running event
  private static final int HINTLET_LONG_DURATION_THRESHOLD = 2000;

  public HintletLongDuration() {
  }

  public HintletLongDuration(HintletOnHintListener onHint) {
    setOnHintCallback(onHint);
  }
  
  @Override
  public void onEventRecord(EventRecord eventRecord) {

    if (!UiEvent.isUiEvent(eventRecord)) {
      return;
    }

    double duration = eventRecord.<UiEvent>cast().getDuration();
    if (duration > HINTLET_LONG_DURATION_THRESHOLD) {
      addHint(getHintletName(), eventRecord.getTime(),
          "Event lasted: " + TimeStampFormatter.formatMilliseconds(duration)
              + ".  Exceeded threshold: " + HINTLET_LONG_DURATION_THRESHOLD + "ms",
          eventRecord.getSequence(), HintRecord.SEVERITY_WARNING);
    } else if (duration > HINTLET_LOW_DURATION_THRESHOLD) {
      addHint(getHintletName(), eventRecord.getTime(),
          "Event lasted: " + TimeStampFormatter.formatMilliseconds(duration)
              + ".  Exceeded threshold: " + HINTLET_LOW_DURATION_THRESHOLD + "ms",
          eventRecord.getSequence(), HintRecord.SEVERITY_INFO);
    }
  }

  @Override
  public String getHintletName() {
    return "Long Duration Events";
  }

}
