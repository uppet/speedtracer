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

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.SpanElement;
import com.google.gwt.topspin.ui.client.Container;
import com.google.gwt.topspin.ui.client.DefaultContainerImpl;
import com.google.speedtracer.client.model.HintRecord;
import com.google.speedtracer.client.util.JSOArray;
import com.google.speedtracer.client.util.TimeStampFormatter;

/**
 * A tree for displaying HintletRecords.
 */
public class HintletRecordsTree extends Tree {

  /**
   * A tree item for displaying a single hintlet record.
   */
  public static class HintletRecordItem extends Tree.Item {
    private final HintRecord hintletRecord;

    /**
     * Add an item under an existing tree node.
     * 
     * @param parent Parent node of the new node.
     * @param resources Static resources.
     * @param hintletRecord Data to display within this node.
     */
    private HintletRecordItem(Item parent, Tree.Resources resources,
        HintRecord hintletRecord, Tree backRef) {
      super(parent, resources);
      this.hintletRecord = hintletRecord;
      setItemTarget(hintletRecord);
      setText(getTreeItemLabel());
    }

    private String getTreeItemLabel() {
      return hintletRecord.getHintletRule() + " : @"
          + TimeStampFormatter.formatSeconds(hintletRecord.getTimestamp(), 2)
          + "  - " + hintletRecord.getDescription();
    }
  }

  /**
   * Externalized Resource Interface.
   */
  public interface Resources extends HintletIndicator.Resources, Tree.Resources {
  }

  /**
   * A tree item to display severity level and the hintlet records that match
   * that severity as children.
   */
  private class SeverityItem extends Tree.Item {
    /**
     * Adds a top level node presenting the severity of all nodes beneath.
     * 
     * @param backRef The tree to add this node to
     * @param severity The severity constant (HintletRecord.SEVERITY_XXX) that
     *          this node represents
     * @param count The number of records in hintletRecords of the specified
     *          severity. This number has already been calculated by the caller,
     *          so its passed in to keep from calculating it twice.
     * @param hintletRecords List of records to extract records of severity
     *          level 'severity'.
     * @param resources Static resources.
     */
    private SeverityItem(Tree backRef, int severity, int count,
        JSOArray<HintRecord> hintletRecords,
        HintletRecordsTree.Resources resources) {
      super(resources, backRef);
      Container severityContainer = new DefaultContainerImpl(getElement());
      // Make the colored Icon with the count of the number of hintlets
      HintletIndicator indicator = new HintletIndicator(severityContainer,
          severity, count, "", resources);
      getItemLabelElement().appendChild(indicator.getElement());

      // Make a placeholder element for the label text.
      SpanElement labelText = Document.get().createSpanElement();
      labelText.getStyle().setPropertyPx("marginLeft", 20);
      getItemLabelElement().appendChild(labelText);

      switch (severity) {
        case HintRecord.SEVERITY_CRITICAL:
          labelText.setInnerText(" Critical");
          break;
        case HintRecord.SEVERITY_WARNING:
          labelText.setInnerText(" Warning");
          break;
        case HintRecord.SEVERITY_INFO:
          labelText.setInnerText(" Info");
          break;
        default:
          // Unknown severity!
          assert (false);
      }

      // Add child nodes.
      for (int i = 0; i < hintletRecords.size(); i++) {
        HintRecord rec = hintletRecords.get(i);
        if (rec.getSeverity() == severity) {
          new HintletRecordItem(this, getResources(), rec, backRef);
        }
      }
    }
  }

  private JSOArray<HintRecord> hintletRecords;

  /**
   * Constructor.
   * 
   * @param container the parent Container
   * @param hintlets the hintlets to display
   * @param resources our ImmutableResourceBundle resources
   */
  public HintletRecordsTree(Container container,
      JSOArray<HintRecord> hintletRecords,
      HintletRecordsTree.Resources resources) {
    super(container, resources);
    this.hintletRecords = hintletRecords;
    disableSelection(true);
    buildTree();
  }

  public HintletRecordsTree.Resources getResources() {
    return (HintletRecordsTree.Resources) super.getResources();
  }

  /**
   * This call is made to ask the tree to re-examine the list of hintlets and
   * update the tree.
   */
  public void refresh(JSOArray<HintRecord> hintletRecords) {
    this.hintletRecords = hintletRecords;
    // Drop the current nodes and rebuild it.
    clear();
    buildTree();
  }

  /**
   * Creates the items in the tree.
   */
  private void buildTree() {
    boolean hasCritical = false;
    boolean hasWarning = false;
    boolean hasInfo = false;
    int criticalCount = 0;
    int warningCount = 0;
    int infoCount = 0;

    // Only build severity nodes we need
    for (int i = 0; i < hintletRecords.size(); i++) {
      if (hintletRecords.get(i).getSeverity() == HintRecord.SEVERITY_CRITICAL) {
        hasCritical = true;
        criticalCount++;
      } else if (hintletRecords.get(i).getSeverity() == HintRecord.SEVERITY_WARNING) {
        hasWarning = true;
        warningCount++;
      } else if (hintletRecords.get(i).getSeverity() == HintRecord.SEVERITY_INFO) {
        hasInfo = true;
        infoCount++;
      }
    }

    if (hasCritical) {
      new SeverityItem(this, HintRecord.SEVERITY_CRITICAL, criticalCount,
          hintletRecords, getResources());
    }
    if (hasWarning) {
      new SeverityItem(this, HintRecord.SEVERITY_WARNING, warningCount,
          hintletRecords, getResources());
    }
    if (hasInfo) {
      new SeverityItem(this, HintRecord.SEVERITY_INFO, infoCount,
          hintletRecords, getResources());
    }
  }
}