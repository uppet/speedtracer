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
package com.google.speedtracer.client.visualizations.view;

import com.google.gwt.graphics.client.Color;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.CssResource.Strict;
import com.google.gwt.topspin.ui.client.Container;
import com.google.gwt.topspin.ui.client.Div;
import com.google.speedtracer.client.MonitorResources;
import com.google.speedtracer.client.model.HintRecord;
import com.google.speedtracer.client.util.JSOArray;

/**
 * An indicator for a table row that indicates there are hints on this
 * entity. Represented by a colored bubble with a number inside indicating the
 * number of hint records.
 * 
 */
public class HintletIndicator extends Div {

  /**
   * CSS.
   */
  public interface Css extends CssResource {
    String hintletIndicator();

    String hintletIndicatorCritical();

    String hintletIndicatorInfo();

    String hintletIndicatorWarning();

    String hintletSeverityColorCritical();

    String hintletSeverityColorInfo();

    String hintletSeverityColorWarning();
  }

  /**
   * Externalized Resource interface.
   */
  public interface Resources extends ClientBundle {

    @Source("resources/HintletIndicator.css")
    @Strict
    Css hintletIndicatorCss();
  }

  public static String getSeverityColor(int severity) {
    Css css = MonitorResources.getResources().hintletIndicatorCss();
    switch (severity) {
      case HintRecord.SEVERITY_CRITICAL:
        return css.hintletSeverityColorCritical();
      case HintRecord.SEVERITY_WARNING:
        return css.hintletSeverityColorWarning();
      case HintRecord.SEVERITY_INFO:
        return css.hintletSeverityColorInfo();
    }
    return Color.BLACK.toString();
  }

  private String tooltipText;

  public HintletIndicator(Container container, int severity, int numHints,
      String tooltip, HintletIndicator.Resources resources) {
    super(container);
    init(severity, numHints, tooltip, resources);
  }

  public HintletIndicator(Container container,
      JSOArray<HintRecord> hintRecords,
      HintletIndicator.Resources resources) {
    super(container);
    // Look through hint records and tally severity.
    int criticalCount = 0;
    int warningCount = 0;
    int infoCount = 0;
    int numHints = hintRecords.size();

    for (int i = 0; i < numHints; i++) {
      HintRecord rec = hintRecords.get(i);
      switch (rec.getSeverity()) {
        case HintRecord.SEVERITY_CRITICAL:
          criticalCount++;
          break;
        case HintRecord.SEVERITY_WARNING:
          warningCount++;
          break;
        case HintRecord.SEVERITY_INFO:
          infoCount++;
          break;
      }
    }

    // Create the tooltip text and figure out the max severity.
    String tooltip = "";
    int maxSeverity = HintRecord.SEVERITY_INFO;

    if (infoCount > 0) {
      tooltip += infoCount + " Info";
      numHints = infoCount;
    }

    if (warningCount > 0) {
      if (infoCount > 0) {
        tooltip = ", " + tooltip;
      }
      tooltip = warningCount + " Warning" + tooltip;
      numHints = warningCount;
      maxSeverity = HintRecord.SEVERITY_WARNING;
    }

    if (criticalCount > 0) {
      if (warningCount > 0 || infoCount > 0) {
        tooltip = ", " + tooltip;
      }
      tooltip = criticalCount + " Critical" + tooltip;
      numHints = criticalCount;
      maxSeverity = HintRecord.SEVERITY_CRITICAL;
    }

    init(maxSeverity, numHints, tooltip, resources);
  }

  public String getTooltipText() {
    return tooltipText;
  }

  private void init(int severity, int numHints, String tooltip,
      HintletIndicator.Resources resources) {
    String className = "";
    Css css = resources.hintletIndicatorCss();
    switch (severity) {
      case HintRecord.SEVERITY_CRITICAL:
        className = css.hintletIndicatorCritical();
        break;
      case HintRecord.SEVERITY_WARNING:
        className = css.hintletIndicatorWarning();
        break;
      case HintRecord.SEVERITY_INFO:
        className = css.hintletIndicatorInfo();
        break;
    }

    getElement().setClassName(className + " " + css.hintletIndicator());
    getElement().setInnerText("" + numHints);
    this.tooltipText = tooltip;

    getElement().setAttribute("title", tooltipText);
  }
}
