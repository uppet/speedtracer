/*
 * Copyright 2008 Google Inc.
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

import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.OptionElement;
import com.google.gwt.dom.client.SelectElement;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.resources.client.CssResource.Strict;
import com.google.gwt.resources.client.ImageResource.ImageOptions;
import com.google.gwt.resources.client.ImageResource.RepeatStyle;
import com.google.gwt.topspin.ui.client.Anchor;
import com.google.gwt.topspin.ui.client.Button;
import com.google.gwt.topspin.ui.client.ChangeEvent;
import com.google.gwt.topspin.ui.client.ChangeListener;
import com.google.gwt.topspin.ui.client.ClickEvent;
import com.google.gwt.topspin.ui.client.ClickListener;
import com.google.gwt.topspin.ui.client.Container;
import com.google.gwt.topspin.ui.client.Div;
import com.google.gwt.topspin.ui.client.Panel;
import com.google.gwt.topspin.ui.client.Select;
import com.google.speedtracer.client.Monitor;
import com.google.speedtracer.client.model.DataModel;
import com.google.speedtracer.client.timeline.Constants;
import com.google.speedtracer.client.timeline.DomainObserver;
import com.google.speedtracer.client.timeline.TimeLineModel.WindowBoundsObserver;
import com.google.speedtracer.client.util.JSOArray;
import com.google.speedtracer.client.util.TimeStampFormatter;

/**
 * The top panel with top level controls for a Monitor Instance.
 */
public class Controller extends Panel implements DomainObserver,
    WindowBoundsObserver {

  /**
   * Css stylename declarations for {@link Controller}.
   */
  public interface Css extends ToggleButton.Css {
    String base();

    String control();

    String helpButton();

    String infoScreen();

    String infoScreenTotal();

    String infoScreenTotalLabel();

    String infoScreenZoom();

    String infoScreenZoomLabel();

    String pageSelect();

    String recordStopButton();

    String reportButton();

    String resetButton();

    String saveButton();

    String settingsButton();

    String zoomAllButton();

    String zoomInButton();

    String zoomOutButton();
  }

  /**
   * Resource declarations for {@link Controller}.
   */
  public interface Resources extends HoveringPopup.Resources {
    @Source("resources/controller-background.png")
    @ImageOptions(repeatStyle = RepeatStyle.Horizontal)
    ImageResource controllerBackground();

    @Source("resources/Controller.css")
    @Strict()
    Css controllerCss();

    @Source("resources/help-button.png")
    ImageResource controllerHelpButton();

    @Source("resources/help-button-d.png")
    ImageResource controllerHelpButtonDown();

    @Source("resources/help-button-h.png")
    ImageResource controllerHelpButtonHover();

    @Source("resources/help-button-p.png")
    ImageResource controllerHelpButtonPress();

    @Source("resources/info-screen-background.png")
    ImageResource controllerInfoScreenBackground();

    @Source("resources/record-stop-button.png")
    ImageResource controllerRecordStopButton();

    @Source("resources/record-stop-button-d.png")
    ImageResource controllerRecordStopButtonDown();

    @Source("resources/record-stop-button-h.png")
    ImageResource controllerRecordStopButtonHover();

    @Source("resources/record-stop-button-p.png")
    ImageResource controllerRecordStopButtonPress();

    @Source("resources/report-button.png")
    ImageResource controllerReportButton();

    @Source("resources/report-button-d.png")
    ImageResource controllerReportButtonDown();

    @Source("resources/report-button-h.png")
    ImageResource controllerReportButtonHover();

    @Source("resources/report-button-p.png")
    ImageResource controllerReportButtonPress();

    @Source("resources/reset-button.png")
    ImageResource controllerResetButton();

    @Source("resources/reset-button-d.png")
    ImageResource controllerResetButtonDown();

    @Source("resources/reset-button-h.png")
    ImageResource controllerResetButtonHover();

    @Source("resources/reset-button-p.png")
    ImageResource controllerResetButtonPress();

    @Source("resources/save-button.png")
    ImageResource controllerSaveButton();

    @Source("resources/save-button-d.png")
    ImageResource controllerSaveButtonDown();

    @Source("resources/save-button-h.png")
    ImageResource controllerSaveButtonHover();

    @Source("resources/save-button-p.png")
    ImageResource controllerSaveButtonPress();

    @Source("resources/settings-button.png")
    ImageResource controllerSettingsButton();

    @Source("resources/settings-button-h.png")
    ImageResource controllerSettingsButtonHover();

    @Source("resources/settings-button-p.png")
    ImageResource controllerSettingsButtonPress();

    @Source("resources/zoom-all-button.png")
    ImageResource controllerZoomAllButton();

    @Source("resources/zoom-all-button-h.png")
    ImageResource controllerZoomAllButtonHover();

    @Source("resources/zoom-all-button-p.png")
    ImageResource controllerZoomAllButtonPress();

    @Source("resources/zoom-in-button.png")
    ImageResource controllerZoomInButton();

    @Source("resources/zoom-in-button-h.png")
    ImageResource controllerZoomInButtonHover();

    @Source("resources/zoom-in-button-p.png")
    ImageResource controllerZoomInButtonPress();

    @Source("resources/zoom-out-button.png")
    ImageResource controllerZoomOutButton();

    @Source("resources/zoom-out-button-h.png")
    ImageResource controllerZoomOutButtonHover();

    @Source("resources/zoom-out-button-p.png")
    ImageResource controllerZoomOutButtonPress();
  }

  private static class InfoScreen extends Div {
    private static DivElement appendDiv(Document document, Element parent,
        String className) {
      final DivElement elem = parent.getOwnerDocument().createDivElement();
      elem.setClassName(className);
      return parent.appendChild(elem);
    }

    private final DivElement totalElem, zoomElem;

    public InfoScreen(Controller controller, Css css) {
      super(controller.getContainer());

      final Element elem = getElement();
      final Document document = elem.getOwnerDocument();

      setStyleName(css.infoScreen());

      totalElem = appendDiv(document, elem, css.infoScreenTotal());
      zoomElem = appendDiv(document, elem, css.infoScreenZoom());
      appendDiv(document, elem, css.infoScreenTotalLabel()).setInnerText(
          "total");
      appendDiv(document, elem, css.infoScreenZoomLabel()).setInnerText("zoom");

      updateTotal(0);
      updateZoomRange(0, 0);
    }

    public void updateTotal(double time) {
      totalElem.setInnerText(TimeStampFormatter.formatSeconds(time, 2));
    }

    public void updateZoomRange(double start, double end) {
      zoomElem.setInnerHTML(TimeStampFormatter.formatSeconds(start, 2)
          + " &ndash; " + TimeStampFormatter.formatSeconds(end, 2));
    }
  }

  private final InfoScreen infoScreen;

  private MainTimeLine mainTimeline;

  private final DataModel model;

  private final Monitor monitor;

  private OverViewTimeLine overviewTimeline;

  private final Select pages;

  private final ToggleButton recordStopButton;

  public Controller(Container parent, DataModel model, Monitor monitor,
      Resources resources) {
    super(parent);
    this.model = model;
    this.monitor = monitor;

    final Container container = getContainer();

    final Css css = resources.controllerCss();

    setStyleName(css.base());

    recordStopButton = new ToggleButton(container, css);
    recordStopButton.setStyleName(css.control() + " " + css.recordStopButton()
        + " " + css.control());
    this.setIsRecording(true);
    recordStopButton.addClickListener(new ClickListener() {
      public void onClick(ClickEvent event) {
        setIsRecordingTitle(recordStopButton.isDown());
        Controller.this.monitor.setIsRecording(recordStopButton.isDown());
      }
    });

    final Button resetButton = new Button(container);
    resetButton.setStyleName(css.control() + " " + css.resetButton());
    resetButton.getElement().setAttribute("title", "Discard Data and Reset");
    resetButton.addClickListener(new ClickListener() {
      public void onClick(ClickEvent event) {
        doReset();
      }
    });

    final Button saveButton = new Button(container);
    saveButton.setStyleName(css.control() + " " + css.saveButton());
    saveButton.getElement().setAttribute("title", "Save Data to a File");
    saveButton.addClickListener(new ClickListener() {
      public void onClick(ClickEvent event) {
        Controller me = Controller.this;
        me.model.saveRecords(getVisitedUrls());
      }

      // TODO(jaimeyap): Revisit this since it is kinda yucky to be using a
      // view component as a model.
      private JSOArray<String> getVisitedUrls() {
        JSOArray<String> visitedUrls = JSOArray.create();
        int numberVisited = pages.getOptionCount();
        for (int i = 0; i < numberVisited; i++) {
          visitedUrls.push(getPageUrlForIndex(i));
        }
        return visitedUrls;
      }
    });

    infoScreen = new InfoScreen(this, css);

    final Button zoomOutButton = new Button(container);
    zoomOutButton.setStyleName(css.control() + " " + css.zoomOutButton());
    zoomOutButton.getElement().setAttribute("title", "Zoom Out");
    zoomOutButton.addClickListener(new ClickListener() {
      public void onClick(ClickEvent event) {
        overviewTimeline.zoom(-2);
      }
    });

    final Button zoomInButton = new Button(container);
    zoomInButton.setStyleName(css.control() + " " + css.zoomInButton());
    zoomInButton.getElement().setAttribute("title", "Zoom In");
    zoomInButton.addClickListener(new ClickListener() {
      public void onClick(ClickEvent event) {
        overviewTimeline.zoom(2);
      }
    });

    final Button zoomAllButton = new Button(container);
    zoomAllButton.setStyleName(css.control() + " " + css.zoomAllButton());
    zoomAllButton.getElement().setAttribute("title", "Zoom All");
    zoomAllButton.addClickListener(new ClickListener() {
      public void onClick(ClickEvent event) {
        overviewTimeline.zoomAll();
      }
    });

    // TODO(jaimeyap): Is this the best way to do the capability detection?
    if (hasSetProfilingOptionsApi()) {
      final Button settingsButton = new Button(container);
      settingsButton.setStyleName(css.control() + " " + css.settingsButton());
      settingsButton.getElement().setAttribute("title", "Set Profiling Options");
      final ProfilingOptionsPanel options = ProfilingOptionsPanel.create(
          getElement(), settingsButton.getAbsoluteLeft() + 10,
          settingsButton.getOffsetHeight(), model);
      settingsButton.addClickListener(new ClickListener() {
        public void onClick(ClickEvent event) {
          options.show();
        }
      });
    }

    pages = new Select(container);
    pages.setStyleName(css.control() + " " + css.pageSelect());
    pages.addChangeListener(new ChangeListener() {
      public void onChange(ChangeEvent event) {
        int selected = pages.getSelectedIndex();
        Controller.this.monitor.setStateForPageAtIndex(selected);
        // If we are not selecting the most resent ApplicationState, we should
        // disable the record/stop button
        if (selected != Controller.this.monitor.getNumberOfPagesViewed() - 1) {
          recordStopButton.getElement().setPropertyBoolean("disabled", true);
        } else {
          recordStopButton.getElement().setPropertyBoolean("disabled", false);
        }
      }
    });

    final Button reportButton = new Button(container);
    reportButton.setStyleName(css.control() + " " + css.reportButton());
    reportButton.getElement().setAttribute("title",
        "Display the Hintlet Report");
    reportButton.addClickListener(new ClickListener() {
      public void onClick(ClickEvent event) {
        Controller.this.monitor.showHintletReport();
      }
    });

    final Anchor helpButton = new Anchor(container);
    final Element helpButtonElem = helpButton.getElement();
    helpButton.setStyleName(css.helpButton());
    helpButton.setHref(Constants.HELP_URL);
    helpButtonElem.setAttribute("title", "Help");
    helpButtonElem.setAttribute("target", "_blank");
  }

  public void addPage(String pageUrl) {
    pages.addOption(pageUrl);
  }

  public void doReset() {
    // Truncate the overview graph.
    overviewTimeline.resetDisplayableBounds();

    // Nuke our Application states and reset everything.
    Controller.this.monitor.resetApplicationStates(
        mainTimeline.getModel().getMostRecentDomainValue(),
        mainTimeline.getModel().getMostRecentDomainValue()
            + Constants.DEFAULT_GRAPH_WINDOW_SIZE);
  }

  public DataModel getModel() {
    return model;
  }

  /**
   * Gets the url for the page at a specific index in our select box. This DOES
   * NOT DO BOUNDS CHECKING.
   * 
   * @param index
   * @return the String corresponding to the page URL
   */
  public String getPageUrlForIndex(int index) {
    OptionElement option = getOptionAtIndex((SelectElement) pages.getElement(),
        index);
    return option.getInnerText();
  }

  public void observe(MainTimeLine mainTimeline,
      OverViewTimeLine overviewTimeline) {
    assert this.overviewTimeline == null;
    assert this.mainTimeline == null;

    this.overviewTimeline = overviewTimeline;
    this.mainTimeline = mainTimeline;

    mainTimeline.getModel().addDomainObserver(this);
    mainTimeline.getModel().addWindowBoundsObserver(this);
  }

  public void onDomainChange(double newValue) {
    infoScreen.updateTotal(newValue);
  }

  public void onWindowBoundsChange(double left, double right) {
    infoScreen.updateZoomRange(left, right);
  }

  public void resetPageStates() {
    pages.clearOptions();
  }

  /**
   * Changes the UI state to display whether data is being recorded. This is
   * external API to be called by the Monitor when a click comes in from the
   * browser action, and the state needs to be reflected here.
   * 
   * @param isRecording <code>true</code> to specify that data is being
   *          recorded.
   */
  public void setIsRecording(boolean isRecording) {
    if (isRecording != recordStopButton.isDown()) {
      recordStopButton.toggle();
    }
    setIsRecordingTitle(isRecording);
  }

  public void setSelectedPage(int indexToSelect) {
    OptionElement option = getOptionAtIndex((SelectElement) pages.getElement(),
        indexToSelect);
    option.setSelected(true);
  }

  private native OptionElement getOptionAtIndex(SelectElement select,
      int indexToSelect) /*-{
    return select[indexToSelect];
  }-*/;

  private static native boolean hasSetProfilingOptionsApi() /*-{
    return !!chrome.devtools.setProfilingOptions;
  }-*/;

  private void setIsRecordingTitle(boolean isRecording) {
    recordStopButton.getElement().setAttribute("title",
        isRecording ? "Stop Recording Data" : "Record Data");
  }
}
