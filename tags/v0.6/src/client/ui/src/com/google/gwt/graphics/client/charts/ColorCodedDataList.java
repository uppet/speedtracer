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
package com.google.gwt.graphics.client.charts;

import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.SpanElement;
import com.google.gwt.dom.client.TableCellElement;
import com.google.gwt.dom.client.TableRowElement;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.CssResource.Strict;
import com.google.gwt.topspin.ui.client.Container;
import com.google.gwt.topspin.ui.client.Table;
import com.google.speedtracer.client.util.TimeStampFormatter;

import java.util.ArrayList;
import java.util.List;

/**
 * Simple class for creating Legend keys for charts.
 */
public class ColorCodedDataList extends Table {

  /**
   * Legend styles.
   */
  public interface Css extends CssResource {
    String entryColor();

    String label();

    String list();
  }

  /**
   * Resources interface.
   */
  public interface Resources extends ClientBundle {
    @Source("resources/ColorCodedDataList.css")
    @Strict()
    ColorCodedDataList.Css colorListCss();
  }

  /**
   * A color and associated label for a DataTuple.
   */
  private class ListEntry {
    private final ColorCodedValue entryData;
    private final SpanElement labelElem;

    public ListEntry(TableRowElement row, ColorCodedValue entryData) {
      this.entryData = entryData;

      DivElement colorElem = Document.get().createDivElement();
      colorElem.setClassName(resources.colorListCss().entryColor());
      colorElem.getStyle().setProperty("backgroundColor",
          entryData.labelColor.toString());
      labelElem = Document.get().createSpanElement();
      labelElem.setClassName(resources.colorListCss().label());

      TableCellElement cell = row.insertCell(-1);
      cell.setAttribute("align", "left");
      cell.appendChild(colorElem);
      cell.appendChild(labelElem);

      updateLabel();
    }

    public void updateLabel() {
      if (usePercent) {
        double percentValue = (entryData.value / total) * 100;
        labelElem.setInnerText(TimeStampFormatter.formatToFixedDecimalPoint(
            percentValue, 1)
            + "% " + entryData.key);
      } else {
        labelElem.setInnerText(Math.round(entryData.value) + entryData.key);
      }
    }
  }

  private final List<ListEntry> entries = new ArrayList<ListEntry>();
  private TableRowElement myRow;
  private final boolean renderHorizontally;
  private final ColorCodedDataList.Resources resources;
  private final double total;
  private final boolean usePercent;

  /**
   * Ctor. If the colorMap doesn't have colors for all the data, we will make it
   * grey. Because grey is sad. Your fault not mine ;).
   * 
   * @param container the parent container we will attach to
   * @param data the data set we are making the list for.
   * @param total data total to support percentage representations
   * @param renderHorizontally if we should line up the list entries
   *          horizontally or vertically
   * @param usePercent if we should display the list values as absolute values
   *          or percentage values
   * @param itemCutoff the max number of items to display
   * @param resources IRB containing our css styles
   */
  public ColorCodedDataList(Container container, List<ColorCodedValue> data,
      int itemCutoff, double total, boolean renderHorizontally,
      boolean usePercent, ColorCodedDataList.Resources resources) {
    super(container);
    getElement().setClassName(resources.colorListCss().list());
    getElement().setAttribute("cellspacing", "0");
    this.resources = resources;
    this.total = total;
    // TODO(jaimeyap): Consider using separate derived classes for rendering
    // horizontally versus vertically so we don't need this flag here in the
    // base class.
    this.renderHorizontally = renderHorizontally;
    this.usePercent = usePercent;

    for (int i = 0, n = data.size(); i < itemCutoff; i++) {
      // We always want to add itemCutoff number of entries to the legend so our
      // table can layout sanely. Basically add an empty <td> if we dont have
      // enough data
      if (i < n) {
        addDataEntry(data.get(i));
      } else {
        addDataEntry(null);
      }
    }
  }

  private void addDataEntry(ColorCodedValue entry) {
    if (entry != null) {
      entries.add(new ListEntry(getContainingRow(), entry));
    } else {
      // Append an empty TD
      getContainingRow().insertCell(-1);
    }
  }

  private TableRowElement getContainingRow() {
    if (renderHorizontally) {
      if (myRow == null) {
        myRow = appendRow();
      }
      return myRow;
    } else {
      return appendRow();
    }
  }
}
