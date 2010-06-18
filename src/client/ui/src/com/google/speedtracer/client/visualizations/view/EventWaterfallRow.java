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

import com.google.gwt.coreext.client.JSOArray;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.graphics.client.Color;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.topspin.ui.client.Container;
import com.google.gwt.topspin.ui.client.DefaultContainerImpl;
import com.google.speedtracer.client.model.AggregateTimeVisitor;
import com.google.speedtracer.client.model.HintRecord;
import com.google.speedtracer.client.model.LogMessageVisitor;
import com.google.speedtracer.client.model.UiEvent;
import com.google.speedtracer.client.timeline.Constants;
import com.google.speedtracer.client.util.TimeStampFormatter;
import com.google.speedtracer.client.util.dom.DocumentExt;
import com.google.speedtracer.client.visualizations.model.SluggishnessModel;
import com.google.speedtracer.client.visualizations.model.SluggishnessVisualization;
import com.google.speedtracer.client.visualizations.view.FilteringScrollTable.Cell;
import com.google.speedtracer.client.visualizations.view.FilteringScrollTable.TableRow;

/**
 * A row in the table that contains a waterfall bar and summary data about a
 * UiEvent.
 * 
 * TODO (jaimeyap): Re-examine the class hierarchy with FilteringScrollTable and
 * its associated Row classes to see if there is a simplifying refactor.
 */
public class EventWaterfallRow extends TableRow {
  /**
   * Styles.
   */
  public interface Css extends CssResource {
    double calloutPadding();

    String colorBox();

    String durationCallout();

    String eventBar();

    String iconContainer();

    String logMessageAnnotation();
  }

  /**
   * Externalized Resources.
   */
  public interface Resources extends HintletIndicator.Resources,
      EventTraceBreakdown.Resources, EventWaterfallRowDetails.Resources {
    @Source("resources/EventWaterfallRow.css")
    Css eventWaterfallRowCss();
  }

  /**
   * Cell that contains the pillbox graph for the UiEvent. It is essentially the
   * waterfall component for the row.
   * 
   * Normal Cells have only String contents. This cell needs to be able to
   * lazily grab an Element to display.
   */
  public class UiEventPillboxCell extends Cell {
    private DivElement durationCallout;

    public UiEventPillboxCell() {
      eventWaterfall.super("", -1);
    }

    @Override
    protected Element createElement() {
      Element elem = super.createElement();
      elem.appendChild(eventBreakdown.getRenderedCanvasElement());
      sizeEventBar(elem);
      return elem;
    }

    private void sizeEventBar(Element elem) {
      SluggishnessVisualization visualization = eventWaterfall.getVisualization();
      int cellWidth = visualization.getTimeline().getCurrentGraphWidth();
      SluggishnessModel model = visualization.getModel();
      double totalDuration = model.getCurrentRight() - model.getCurrentLeft();

      assert totalDuration > 0 : "Attempted to render row with 0 duration window";

      double duration = getEvent().getDuration();
      double domainToPixels = (cellWidth / totalDuration);

      double barPixelWidth = Math.max(duration * domainToPixels, 1);
      double barOffset = (getEvent().getTime() - model.getCurrentLeft())
          * domainToPixels;

      EventWaterfallRow.Css css = resources.eventWaterfallRowCss();
      final Element graphElem = eventBreakdown.getRenderedCanvasElement();
      final Style graphStyle = graphElem.getStyle();
      graphElem.setClassName(css.eventBar());
      graphStyle.setWidth(barPixelWidth, Unit.PX);
      graphStyle.setLeft(barOffset, Unit.PX);

      if (durationCallout == null) {
        durationCallout = elem.getOwnerDocument().createDivElement();
        durationCallout.setClassName(css.durationCallout());
        durationCallout.setInnerText(TimeStampFormatter.format(duration));
        elem.appendChild(durationCallout);
      }

      double calloutOffset = barOffset + barPixelWidth + css.calloutPadding();
      // TODO (jaimeyap): Position callout to the left of the bar if it hangs
      // off the right. Only issue is that nothing is attached to the DOM yet so
      // measuring the offsetWidth of the callout is not currently possible.
      durationCallout.getStyle().setLeft(calloutOffset, Unit.PX);
    }
  }

  /**
   * Cell that displays the name of the UiEvent and an icon if a hintlet is
   * present for this event.
   * 
   * The appearance of the hintlet is changes depending on the most severe
   * hintlet record present.
   * 
   */
  public class UiEventTitleCell extends Cell {
    private HintletIndicator hintIcon;
    private Container iconContainer;

    public UiEventTitleCell() {
      eventWaterfall.super("", Constants.GRAPH_PIXEL_OFFSET);
      runVisitors();
    }

    public void refresh() {
      if (!isCreated()) {
        return;
      }
      updateIndicator();
    }

    @Override
    protected Element createElement() {
      EventWaterfallRow.Css css = resources.eventWaterfallRowCss();
      // Create the element that backs this Cell.
      Element elem = super.createElement();
      elem.getStyle().setProperty("borderRight", "1px solid #999");
      // Create the Event's color box and the title.
      addTitle(elem);
      // Create a container for the hint and log message icons.
      Element iconElem = elem.getOwnerDocument().createDivElement();
      iconElem.setClassName(css.iconContainer());
      elem.appendChild(iconElem);
      iconContainer = new DefaultContainerImpl(iconElem);
      // Add a hintlet indicator icon if there are hints.
      maybeAddHintIndicator(iconContainer);
      // If there is a log message in this event, show the info bubble.
      maybeAddLogIcon(iconElem);
      return elem;
    }

    private void addTitle(Element elem) {
      String title = UiEvent.typeToDetailedTypeString(event);
      Color color = EventRecordColors.getColorForType(event.getType());
      DivElement colorBox = elem.getOwnerDocument().createDivElement();
      colorBox.setClassName(resources.eventWaterfallRowCss().colorBox());
      colorBox.getStyle().setBackgroundColor(color.toString());
      elem.appendChild(colorBox);
      elem.appendChild(elem.getOwnerDocument().createTextNode(title));
    }

    private void annotateWithLogIcon(Element elem) {
      DivElement infoBubble = DocumentExt.get().createDivWithClassName(
          resources.eventWaterfallRowCss().logMessageAnnotation());
      elem.appendChild(infoBubble);
    }

    private void maybeAddHintIndicator(Container parentContainer) {
      JSOArray<HintRecord> hintletRecords = event.getHintRecords();
      if (hintletRecords != null) {
        hintIcon = new HintletIndicator(parentContainer, hintletRecords,
            resources);
      }
    }

    private void maybeAddLogIcon(Element iconElem) {
      if (event.hasUserLogs()) {
        // Annotate the parent row
        annotateWithLogIcon(iconElem);
      }
    }

    private void runVisitors() {
      AggregateTimeVisitor.apply(event);
      LogMessageVisitor.apply(event);
    }

    private void updateIndicator() {
      if (hintIcon == null) {
        maybeAddHintIndicator(iconContainer);
      } else {
        hintIcon.update(event.getHintRecords());
      }
    }
  }

  private final UiEvent event;

  private final EventTraceBreakdown eventBreakdown;

  private final EventWaterfall eventWaterfall;

  private final UiEventPillboxCell pillBoxCell;

  private final EventWaterfallRow.Resources resources;

  private final UiEventTitleCell titleCell;

  public EventWaterfallRow(EventWaterfall eventWaterfall, UiEvent event,
      EventWaterfallRow.Resources resources) {
    // We subclass a non-static inner class. So it needs an enclosing instance.
    eventWaterfall.super(event.getDuration());
    this.resources = resources;
    this.eventWaterfall = eventWaterfall;
    this.event = event;
    eventBreakdown = new EventTraceBreakdown(event,
        eventWaterfall.getPresenter(), resources);

    // Create cells.
    titleCell = new UiEventTitleCell();
    pillBoxCell = new UiEventPillboxCell();
    addCell(this.titleCell);
    addCell(this.pillBoxCell);
  }

  /**
   * Creates and sets the {@link EventWaterfallRowDetails} as this Row instances
   * RowDetails panel.
   */
  public void createDetails() {
    setDetails(new EventWaterfallRowDetails(eventWaterfall, this, resources));
  }

  public UiEvent getEvent() {
    return this.event;
  }

  public EventTraceBreakdown getEventBreakdown() {
    return eventBreakdown;
  }

  public UiEventTitleCell getTitleCell() {
    return titleCell;
  }

  @Override
  public void onResize() {
    pillBoxCell.sizeEventBar(pillBoxCell.getElement());
  }
}
