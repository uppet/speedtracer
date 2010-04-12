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

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.events.client.EventListenerRemover;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.topspin.ui.client.ChangeEvent;
import com.google.gwt.topspin.ui.client.ChangeListener;
import com.google.gwt.topspin.ui.client.CheckBox;
import com.google.gwt.topspin.ui.client.ClickEvent;
import com.google.gwt.topspin.ui.client.ClickListener;
import com.google.gwt.topspin.ui.client.Container;
import com.google.gwt.topspin.ui.client.DefaultContainerImpl;
import com.google.gwt.topspin.ui.client.Div;
import com.google.gwt.topspin.ui.client.InputText;
import com.google.gwt.topspin.ui.client.KeyUpEvent;
import com.google.gwt.topspin.ui.client.KeyUpListener;
import com.google.gwt.topspin.ui.client.Select;
import com.google.speedtracer.client.util.JsIntegerMap;
import com.google.speedtracer.client.util.JsIntegerMap.IterationCallBack;
import com.google.speedtracer.client.visualizations.model.SluggishnessModel;

/**
 * The panel that allows you to filter out events in the slugishness detail
 * view.
 */
public class SluggishnessEventFilterPanel extends Div {
  /**
   * Styles.
   */
  public interface Css extends CssResource {
    String filterPanelButton();

    String filterPanelMinInput();

    String filterPanelMinLabel();
  }

  /**
   * Externalized Resources.
   */
  public interface Resources extends ClientBundle {
    @Source("resources/filter-panel-button.png")
    ImageResource filterPanelButton();

    @Source("resources/filter-panel-button-h.png")
    ImageResource filterPanelButtonHover();

    @Source("resources/filter-panel-button-p.png")
    ImageResource filterPanelButtonPress();

    @Source("resources/SluggishnessEventFilterPanel.css")
    Css sluggishnessFiletPanelCss();
  }

  private final SluggishnessEventFilterPanel.Css css;
  private EventFilter eventFilter;
  private EventListenerRemover eventFilterTypeRemover;
  private final EventWaterfall eventTable;
  private Select filterPanelEventTypeSelect;
  private InputText minInput;

  public SluggishnessEventFilterPanel(Container parent,
      EventWaterfall eventTable, EventFilter eventFilter,
      SluggishnessEventFilterPanel.Resources resources, SluggishnessModel model) {
    super(parent);
    this.eventTable = eventTable;
    this.eventFilter = eventFilter;
    this.css = resources.sluggishnessFiletPanelCss();
    buildFilterPanel(parent, model);
  }

  // Refresh the filter panel editable values
  public void refresh(SluggishnessModel model) {
    minInput.setText(Integer.toString((int) eventFilter.getMinDuration()));
    refreshEventTypeSelect(model);
  }

  private void buildFilterPanel(Container parent, SluggishnessModel model) {
    Div row1 = new Div(parent);
    Container row1Container = new DefaultContainerImpl(row1.getElement());
    Document doc = parent.getDocument();

    // Min Duration: ________
    Element minLabel = doc.createSpanElement();
    row1.getElement().appendChild(minLabel);
    minLabel.setInnerText("Minimum duration: ");
    minInput = new InputText(row1Container);
    minInput.setStyleName(css.filterPanelMinInput());
    minInput.addKeyUpListener(new KeyUpListener() {
      public void onKeyUp(KeyUpEvent event) {
        int minValue = 0;
        boolean exceptionEncountered = false;
        try {
          minValue = Integer.valueOf(minInput.getText());
        } catch (NumberFormatException ex) {
          // leave the filter alone
          exceptionEncountered = true;
          minInput.getElement().getStyle().setBackgroundColor("#ebb");
        }
        if (!exceptionEncountered && minValue >= 0) {
          eventFilter.setMinDuration(minValue);
          minInput.getElement().getStyle().setBackgroundColor("#fff");
          eventTable.renderTable();
        }
      }
    });

    // Event Type ======== %
    Element eventTypeLabel = doc.createSpanElement();
    row1.getElement().appendChild(eventTypeLabel);
    eventTypeLabel.getStyle().setPropertyPx("marginLeft", 10);
    eventTypeLabel.setInnerText("Event Type: ");

    createEventTypeSelect(row1Container, model);
    createEventTypePercentSelect(row1Container);

    // Always show events with: o logs o hintlets
    Element alwaysShowLabel = doc.createSpanElement();
    alwaysShowLabel.setInnerText("Always show: ");
    alwaysShowLabel.getStyle().setPropertyPx("marginLeft", 10);
    row1.getElement().appendChild(alwaysShowLabel);

    final CheckBox logsCheckBox = new CheckBox(row1Container);
    logsCheckBox.setChecked(true);
    Element logsLabel = doc.createSpanElement();
    logsLabel.setInnerText("Logs");
    row1.getElement().appendChild(logsLabel);
    logsCheckBox.addClickListener(new ClickListener() {
      public void onClick(ClickEvent event) {
        eventFilter.setFilterUserLogs(!logsCheckBox.isChecked());
        eventTable.renderTable();
      }
    });

    final CheckBox hintletsCheckBox = new CheckBox(row1Container);
    hintletsCheckBox.setChecked(true);
    Element hintletsLabel = doc.createSpanElement();
    hintletsLabel.setInnerText("Hints");
    row1.getElement().appendChild(hintletsLabel);
    hintletsCheckBox.addClickListener(new ClickListener() {
      public void onClick(ClickEvent event) {
        eventFilter.setFilterHints(!hintletsCheckBox.isChecked());
        eventTable.renderTable();
      }
    });
  }

  private void createEventTypePercentSelect(Container row1Container) {
    final Select eventTypePercentSelect = new Select(row1Container);
    eventTypePercentSelect.insertOption("Any %", 0);
    eventTypePercentSelect.insertOption("> 10%", 1);
    eventTypePercentSelect.insertOption("> 50%", 2);
    eventTypePercentSelect.insertOption("> 90%", 3);
    eventTypePercentSelect.addChangeListener(new ChangeListener() {
      public void onChange(ChangeEvent event) {
        switch (eventTypePercentSelect.getSelectedIndex()) {
          case 0:
            eventFilter.setMinEventTypePercent(0.0);
            break;
          case 1:
            eventFilter.setMinEventTypePercent(0.1);
            break;
          case 2:
            eventFilter.setMinEventTypePercent(0.5);
            break;
          case 3:
            eventFilter.setMinEventTypePercent(0.9);
            break;
          default:
            eventFilter.setMinEventTypePercent(0.0);
            break;
        }
        eventTable.renderTable();
      }
    });
  }

  // Creates a select UI component for event types in this data set.
  private void createEventTypeSelect(Container row1Container,
      SluggishnessModel model) {
    filterPanelEventTypeSelect = new Select(row1Container);
    filterPanelEventTypeSelect.insertOption("All", "-1", 0);
    refreshEventTypeSelect(model);
  }

  // Creates/refreshes the sparse list of events - only those that have been
  // pulled in by the {@link SlugishnessModel}
  private void refreshEventTypeSelect(SluggishnessModel model) {
    // Remove existing options, save the first one (ALL)
    int count = filterPanelEventTypeSelect.getOptionCount();
    for (int i = 1; i < count; ++i) {
      filterPanelEventTypeSelect.removeOption(1);
    }
    // Remove the old listener.
    if (eventFilterTypeRemover != null) {
      eventFilterTypeRemover.remove();
      eventFilterTypeRemover = null;
    }

    final JsIntegerMap<String> typesEncountered = model.getTypesEncountered();
    typesEncountered.iterate(new IterationCallBack<String>() {
      int typesIndex = 0;

      public void onIteration(int key, String val) {
        if (typesEncountered.hasKey(key)) {
          filterPanelEventTypeSelect.insertOption(val, String.valueOf(key),
              typesIndex + 1);
          typesIndex++;
        }
      }
    });

    // The click listener has to map the index of the selected item to the
    // sparse array of types we just created.
    eventFilterTypeRemover = filterPanelEventTypeSelect.addChangeListener(new ChangeListener() {
      public void onChange(ChangeEvent event) {
        int eventType = Integer.parseInt(filterPanelEventTypeSelect.getSelectedValue());
        eventFilter.setEventType(eventType);
        eventTable.renderTable();
      }
    });
  }
}