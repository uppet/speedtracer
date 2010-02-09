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
package com.google.speedtracer.client.visualizations.view;

import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.TableCellElement;
import com.google.gwt.topspin.ui.client.Anchor;
import com.google.gwt.topspin.ui.client.ClickEvent;
import com.google.gwt.topspin.ui.client.ClickListener;
import com.google.gwt.topspin.ui.client.Container;
import com.google.gwt.topspin.ui.client.DefaultContainerImpl;
import com.google.gwt.topspin.ui.client.Div;
import com.google.gwt.topspin.ui.client.Table;
import com.google.speedtracer.client.Logging;
import com.google.speedtracer.client.SymbolServerController;
import com.google.speedtracer.client.model.JavaScriptProfile;
import com.google.speedtracer.client.model.JavaScriptProfileModel;
import com.google.speedtracer.client.model.JavaScriptProfileNode;
import com.google.speedtracer.client.util.JSOArray;
import com.google.speedtracer.client.util.TimeStampFormatter;
import com.google.speedtracer.client.util.dom.EventCleanup.EventCleanupTrait;

import java.util.Collections;
import java.util.List;

/**
 * Presents a UI for a JavaScriptprofile
 * 
 */
public class JavaScriptProfileRenderer extends EventCleanupTrait {

  public interface SourceClickCallback {
    public void onSourceClick(String resourceUrl, int lineNumber);
  }

  public interface ResizeCallback {
    public void onResize();
  }

  private final Div profileDiv;
  private JavaScriptProfile profile;
  private final SourceClickCallback sourceClickCallback;
  private final SymbolServerController symbolServerController;
  private final ResizeCallback resizeCallback;

  public JavaScriptProfileRenderer(Container container,
      JavaScriptProfile profile, SourceClickCallback sourceClickCallback,
      SymbolServerController symbolServerController,
      ResizeCallback resizeCallback) {
    profileDiv = new Div(container);
    this.profile = profile;
    this.sourceClickCallback = sourceClickCallback;
    this.symbolServerController = symbolServerController;
    this.resizeCallback = resizeCallback;
  }

  /**
   * Display the specified profile.
   * 
   * @param profileType one of JavaScriptProfile.PROFILE_TYPE_XXX values
   */
  public void show(int profileType) {
    JavaScriptProfileNode profileRoot = profile.getProfile(profileType);
    profileDiv.setHtml("");
    Container container = new DefaultContainerImpl(profileDiv.getElement());
    // show VM states (Garbage Collect, etc)
    StringBuilder result = new StringBuilder();
    result.append("<h3>VM States</h3>");
    profile.getVmStateHtml(result);
    result.append("<br/>");
    switch (profileType) {
      case JavaScriptProfile.PROFILE_TYPE_FLAT:
        DivElement vmStatesDiv = container.getDocument().createDivElement();
        vmStatesDiv.setInnerHTML(result.toString());
        profileDiv.getElement().appendChild(vmStatesDiv);
        dumpNodeChildrenFlat(container, profileRoot);
        break;
      case JavaScriptProfile.PROFILE_TYPE_BOTTOM_UP:
      case JavaScriptProfile.PROFILE_TYPE_TOP_DOWN:
        dumpNodeChildrenRecursive(profileRoot, result);
        profileDiv.setHtml(result.toString());
        break;
      default:
        Logging.getLogger().logText("Unknown Profile type: " + profileType);
        assert (false);
    }
  }

  private void addFlatChild(final JavaScriptProfileNode child,
      Table profileTable, int rowIndex, double totalTime) {
    double relativeSelfTime = (totalTime > 0
        ? (child.getSelfTime() / totalTime) * 100 : 0);
    double relativeTime = (totalTime > 0 ? (child.getTime() / totalTime) * 100
        : 0);
    profileTable.appendRow();
    final TableCellElement symbolNameCell = profileTable.appendCell(rowIndex);
    symbolNameCell.setInnerText(formatResourceUrl(child.getSymbolName(), -1));
    final TableCellElement resourceCell = profileTable.appendCell(rowIndex);
    final Anchor anchor = new Anchor(new DefaultContainerImpl(resourceCell));
    anchor.setText(formatResourceUrl(child.getResourceUrl(),
        child.getResourceLineNumber()));
    anchor.setHref("javascript:;");
    trackRemover(anchor.addClickListener(new ClickListener() {
      public void onClick(ClickEvent event) {
        Logging.getLogger().logText(
            "opening resource " + child.getResourceUrl() + " line: "
                + child.getResourceLineNumber());
        sourceClickCallback.onSourceClick(child.getResourceUrl(),
            child.getResourceLineNumber());
      }
    }));
    profileTable.appendCell(rowIndex).setInnerHTML(
        "<b>"
            + TimeStampFormatter.formatToFixedDecimalPoint(relativeSelfTime, 1)
            + "%</b></td>");
    profileTable.appendCell(rowIndex).setInnerHTML(
        "<b>" + TimeStampFormatter.formatToFixedDecimalPoint(relativeTime, 1)
            + "%</td>");

    // TODO(zundel): Request resymbolization of this symbol.
  }

  private void dumpNodeChildrenFlat(Container container,
      JavaScriptProfileNode profileRoot) {
    final List<JavaScriptProfileNode> children = profileRoot.getChildren();
    if (children == null) {
      return;
    }
    Collections.sort(children, JavaScriptProfileModel.nodeTimeComparator);
    final Table profileTable = new Table(container);
    int rowIndex = 0;
    profileTable.appendRow();
    profileTable.appendCell(rowIndex).setInnerHTML("<b>Symbol</b>");
    profileTable.appendCell(rowIndex).setInnerHTML("<b>Resource</b>");
    profileTable.appendCell(rowIndex).setInnerHTML("<b>Self Time</b>");
    profileTable.appendCell(rowIndex).setInnerHTML("<b>Time</b>");

    final double totalTime = profile.getTotalTime();

    for (int length = children.size(); rowIndex < length; ++rowIndex) {
      JavaScriptProfileNode child = children.get(rowIndex);
      // Truncate the display by default to show only nodes where self time
      // occurred.
      if (child.getSelfTime() <= 0 && (length - rowIndex > 4)) {
        break;
      }
      addFlatChild(child, profileTable, rowIndex + 1, totalTime);
    }

    // Profile terminated early, add a "show more" indicator
    if (rowIndex < children.size()) {
      profileTable.appendRow();
      TableCellElement cell = profileTable.appendCell(rowIndex + 1);
      cell.setColSpan(4);
      Anchor anchor = new Anchor(new DefaultContainerImpl(cell));
      anchor.setText("More...");
      anchor.setHref("javascript:;");
      cell.appendChild(anchor.getElement());
      final int moreRowIndex = rowIndex;
      trackRemover(anchor.addClickListener(new ClickListener() {

        public void onClick(ClickEvent event) {
          profileTable.deleteRow(moreRowIndex + 1);
          for (int i = moreRowIndex; i < children.size(); ++i) {
            JavaScriptProfileNode child = children.get(i);
            addFlatChild(child, profileTable, i + 1, totalTime);
          }
          resizeCallback.onResize();
        }
      }));
    }
  }

  /**
   * Helper for getProfileHtmlForEvent().
   */
  private void dumpNodeChildrenRecursive(JavaScriptProfileNode profileRoot,
      StringBuilder result) {
    List<JavaScriptProfileNode> children = profileRoot.getChildren();
    if (children == null) {
      return;
    }
    Collections.sort(children, JavaScriptProfileModel.nodeTimeComparator);
    result.append("<ul>\n");
    double totalTime = profile.getTotalTime();
    for (int i = 0, length = children.size(); i < length; ++i) {
      JavaScriptProfileNode child = children.get(i);
      int relativeSelfTime = (int) (totalTime > 0
          ? (child.getSelfTime() / totalTime) * 100 : 0);
      int relativeTime = (int) (totalTime > 0
          ? (child.getTime() / totalTime) * 100 : 0);

      result.append("<li>\n");
      result.append(formatResourceUrl(child.getSymbolName(),
          child.getResourceLineNumber()));
      result.append(" <b>self: ");
      result.append(TimeStampFormatter.formatToFixedDecimalPoint(
          relativeSelfTime, 1));
      result.append("%</b> ");
      result.append(" (");
      result.append(TimeStampFormatter.formatToFixedDecimalPoint(relativeTime,
          1));
      result.append("%) ");
      result.append("</li>\n");
      dumpNodeChildrenRecursive(child, result);
    }
    result.append("</ul>\n");
  }

  private String formatResourceUrl(String symbolName, int lineNumber) {
    JSOArray<String> vals = JSOArray.splitString(symbolName, " ");
    StringBuilder result = new StringBuilder();
    for (int i = 0, length = vals.size(); i < length; ++i) {
      String val = vals.get(i);
      if (val.startsWith("http://") || val.startsWith("https://")
          || val.startsWith("file://") || val.startsWith("chrome://")
          || val.startsWith("chrome-extension://")) {
        // Presenting the entire URL takes too much space. Just show the last
        // path component.
        String resource = val.substring(val.lastIndexOf("/") + 1);

        resource = "".equals(resource) ? val : resource;
        result.append(resource);
      } else {
        result.append(val);
      }
      if (i < length - 1) {
        result.append(" ");
      }
    }
    if (lineNumber > 0) {
      result.append(":" + lineNumber);
    }
    return result.toString();
  }
}
