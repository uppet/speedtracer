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

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.InputElement;
import com.google.gwt.dom.client.Style.Position;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.events.client.EventListenerRemover;
import com.google.gwt.topspin.ui.client.ClickEvent;
import com.google.gwt.topspin.ui.client.ClickListener;
import com.google.gwt.topspin.ui.client.Container;
import com.google.gwt.topspin.ui.client.DefaultContainerImpl;
import com.google.gwt.topspin.ui.client.Window;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.speedtracer.client.MonitorResources;
import com.google.speedtracer.client.SourceViewer;
import com.google.speedtracer.client.SymbolServerController;
import com.google.speedtracer.client.SymbolServerService;
import com.google.speedtracer.client.SourceViewer.SourcePresenter;
import com.google.speedtracer.client.SourceViewer.SourceViewerLoadedCallback;
import com.google.speedtracer.client.model.DataModel;
import com.google.speedtracer.client.model.EventVisitor;
import com.google.speedtracer.client.model.EventVisitorTraverser;
import com.google.speedtracer.client.model.JavaScriptProfile;
import com.google.speedtracer.client.model.JavaScriptProfileModel;
import com.google.speedtracer.client.model.LogEvent;
import com.google.speedtracer.client.model.UiEvent;
import com.google.speedtracer.client.model.EventVisitor.PostOrderVisitor;
import com.google.speedtracer.client.util.JSOArray;
import com.google.speedtracer.client.util.Url;
import com.google.speedtracer.client.view.AutoHideDiv;
import com.google.speedtracer.client.view.HotKeyPanel;
import com.google.speedtracer.client.visualizations.view.JavaScriptProfileRenderer.SourceClickCallback;

/**
 * Offers a UI to allow merging together profiles from different events.
 */
public class MergeProfilesPanel extends HotKeyPanel implements SourcePresenter {
  class MySourceViewerLoadedCallback implements SourceViewerLoadedCallback {
    private String resourceUrl;
    private int lineNumber;

    public MySourceViewerLoadedCallback(String resourceUrl, int lineNumber) {
      this.resourceUrl = resourceUrl;
      this.lineNumber = lineNumber;
    }

    public void onSourceFetchFail(int statusCode, SourceViewer viewer) {
      if (errorDiv == null) {
        errorDiv = new ErrorDiv();
      }
      errorDiv.getElement().getStyle().setPosition(Position.ABSOLUTE);
      errorDiv.getElement().getStyle().setTop(0, Unit.PX);
      errorDiv.getElement().getStyle().setLeft(51, Unit.PX);
      errorDiv.getElement().getStyle().setRight(0, Unit.PX);
      errorDiv.getElement().getStyle().setBackgroundColor("#fbe78c");
      errorDiv.getElement().getStyle().setColor("#000");
      errorDiv.getElement().getStyle().setProperty("border", "1px black solid");
      errorDiv.setText("XHR fetch of " + resourceUrl + "failed with status: "
          + statusCode);
      errorDiv.show();
    }

    public void onSourceViewerLoaded(SourceViewer viewer) {
      // Position the source viewer so that it fills half the
      // details view. Below the table header, and flush with the
      // bottom of the window.
      viewer.getElement().getStyle().setTop(1, Unit.PX);
      // Half the width with a little space for border of the
      // table.
      viewer.getElement().getStyle().setLeft(51, Unit.PCT);
      viewer.getElement().getStyle().setRight(0, Unit.PCT);
      viewer.show();
      viewer.highlightLine(lineNumber);
      viewer.scrollHighlightedLineIntoView();
    }
  }

  interface MyUiBinder extends UiBinder<DivElement, MergeProfilesPanel> {
  }

  private class ErrorDiv extends AutoHideDiv {

    public ErrorDiv() {
      super(new DefaultContainerImpl(elem), 3000);
    }
  }

  private class MatchLogVisitor implements PostOrderVisitor {
    private boolean found = false;
    private final String regexp;
    private int sequence;

    public MatchLogVisitor(String regexp) {
      this.regexp = regexp;
    }

    public void postProcess() {
      // Reset to starting state
      found = false;
    }

    public void setSequence(int sequence) {
      this.sequence = sequence;
    }

    public void visitUiEvent(UiEvent e) {
      if (found) {
        return;
      }
      if (e.getType() == LogEvent.TYPE) {
        LogEvent event = (LogEvent) e;
        String message = event.getMessage();
        if (message.matches(regexp)) {
          found = true;
          matchingProfiles.push(dataModel.getJavaScriptProfileModel().getProfileForEvent(
              sequence));
        }
      }
    }
  }

  private class ProfileClickListener implements ClickListener {
    private final int profileType;
    private final JavaScriptProfileRenderer renderer;

    private ProfileClickListener(JavaScriptProfileRenderer renderer,
        int profileType) {
      this.profileType = profileType;
      this.renderer = renderer;
    }

    public void onClick(ClickEvent clickEvent) {
      renderer.show(profileType);
    }
  }

  private static MyUiBinder uiBinder = GWT.create(MyUiBinder.class);

  @UiField
  DivElement closeButton;

  @UiField
  InputElement regexpInput;

  @UiField
  InputElement searchButton;

  @UiField
  DivElement resultsDiv;
  private final MonitorResources.Resources resources;
  private final DataModel dataModel;
  private final Element elem;
  private boolean inSearch = false;
  private EventListenerRemover rendererRemover;
  private JSOArray<JavaScriptProfile> matchingProfiles;

  private ErrorDiv errorDiv;

  public MergeProfilesPanel(DataModel dataModel,
      MonitorResources.Resources resources) {
    this.dataModel = dataModel;
    this.resources = resources;
    this.elem = uiBinder.createAndBindUi(this);

    // Wire up the close button.
    ClickEvent.addClickListener(closeButton, closeButton, new ClickListener() {

      public void onClick(ClickEvent event) {
        MergeProfilesPanel.this.hide();
      }
    });

    // Wire up the search button.
    ClickEvent.addClickListener(searchButton, searchButton,
        new ClickListener() {
          public void onClick(ClickEvent event) {
            if (null == regexpInput.getValue()) {
              return;
            }
            search(regexpInput.getValue(),
                MergeProfilesPanel.this.dataModel.getJavaScriptProfileModel());
          }
        });
  }

  public void showSource(String resourceUrl, int lineNumber, int colNumber) {
    SourceViewer.create(elem, resourceUrl, resources,
        new MySourceViewerLoadedCallback(resourceUrl, lineNumber));
  }

  @Override
  protected Element createContentElement(Document document) {
    return elem;
  }

  @Override
  protected void populateContent(Element contentElement) {
    regexpInput.setInnerText("");
    resultsDiv.setInnerText("");
  }

  private SymbolServerController getSymbolServerController() {
    String url = dataModel.getTabDescription().getUrl();
    return SymbolServerService.getSymbolServerController(new Url(url));
  }

  /**
   * Runs after the search completes to create a profile that combine all the
   * found profiles into one, then displays a renderer to display the resulting
   * merge profile.
   */
  private void mergeProfiles() {
    JavaScriptProfile profile = new JavaScriptProfile();
    for (int i = 0, length = matchingProfiles.size(); i < length; ++i) {
      profile.merge(matchingProfiles.get(i));
    }
    Container resultsContainer = new DefaultContainerImpl(resultsDiv);
    ScopeBar bar = new ScopeBar(resultsContainer, resources);

    JavaScriptProfileRenderer renderer = new JavaScriptProfileRenderer(
        resultsContainer, resources, getSymbolServerController(), this,
        profile, new SourceClickCallback() {

          public void onSourceClick(final String resourceUrl,
              final int lineNumber) {
            SourceViewer.create(elem, resourceUrl, resources,
                new MySourceViewerLoadedCallback(resourceUrl, lineNumber));
          }
        }, null);

    if (rendererRemover != null) {
      rendererRemover.remove();
    }
    rendererRemover = renderer.getRemover();
    Element flatProfile = bar.add("Flat", new ProfileClickListener(renderer,
        JavaScriptProfile.PROFILE_TYPE_FLAT));
    bar.add("Top Down", new ProfileClickListener(renderer,
        JavaScriptProfile.PROFILE_TYPE_TOP_DOWN));
    bar.add("Bottom Up", new ProfileClickListener(renderer,
        JavaScriptProfile.PROFILE_TYPE_BOTTOM_UP));
    bar.setSelected(flatProfile, true);
  }

  private void search(final String regexp, JavaScriptProfileModel profileModel) {
    if (inSearch) {
      Window.alert("A search is already running.");
      return;
    }
    inSearch = true;

    // TODO(zundel): This is kind of ghetto - put up a spinner or something -
    // this takes a while.
    resultsDiv.setInnerHTML("Searching...");
    matchingProfiles = JSOArray.createArray().cast();

    profileModel.visitEventsWithProfiles(new EventVisitor() {
      private int eventsFound = 0;
      private int logsFound = 0;
      private MatchLogVisitor matchLogVisitor = new MatchLogVisitor(regexp);
      private PostOrderVisitor visitors[] = { matchLogVisitor };

      public void postProcess() {
        inSearch = false;
        resultsDiv.setInnerHTML("<div>Found " + eventsFound + " events, "
            + logsFound + " log entries, and " + matchingProfiles.size()
            + " matching logs.</div><br/>");
        mergeProfiles();
      }

      public void visitUiEvent(UiEvent e) {
        if (e.hasJavaScriptProfile()) {
          eventsFound++;
          if (e.hasUserLogs()) {
            logsFound++;
            matchLogVisitor.setSequence(e.getSequence());
            EventVisitorTraverser.traversePostOrder(e, visitors);
          }
        }
        matchLogVisitor.postProcess();
      }
    });
  }
}
