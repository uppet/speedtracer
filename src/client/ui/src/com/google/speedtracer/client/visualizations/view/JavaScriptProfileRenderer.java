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

import com.google.gwt.dom.client.AnchorElement;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.SpanElement;
import com.google.gwt.dom.client.TableCellElement;
import com.google.gwt.dom.client.TableRowElement;
import com.google.gwt.events.client.Event;
import com.google.gwt.graphics.client.Color;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.topspin.ui.client.Anchor;
import com.google.gwt.topspin.ui.client.ClickEvent;
import com.google.gwt.topspin.ui.client.ClickListener;
import com.google.gwt.topspin.ui.client.Container;
import com.google.gwt.topspin.ui.client.DefaultContainerImpl;
import com.google.gwt.topspin.ui.client.Div;
import com.google.gwt.topspin.ui.client.Table;
import com.google.speedtracer.client.ClientConfig;
import com.google.speedtracer.client.Logging;
import com.google.speedtracer.client.SymbolServerController;
import com.google.speedtracer.client.SourceViewer.SourcePresenter;
import com.google.speedtracer.client.SymbolServerController.Resymbolizeable;
import com.google.speedtracer.client.model.JavaScriptProfile;
import com.google.speedtracer.client.model.JavaScriptProfileModel;
import com.google.speedtracer.client.model.JavaScriptProfileNode;
import com.google.speedtracer.client.model.JsSymbol;
import com.google.speedtracer.client.util.Command;
import com.google.speedtracer.client.util.TimeStampFormatter;
import com.google.speedtracer.client.util.dom.ManagesEventListeners;

import java.util.Collections;
import java.util.List;

/**
 * Presents a UI for a JavaScript profile.
 */
public class JavaScriptProfileRenderer {
  /**
   * Css.
   */
  public interface Css extends CssResource {
    String resymbolizedLink();

    String treeItemBottomDiv();

    String treeItemTopDiv();
  }

  /**
   * Callback invoked when the screen dimensions of the profile changes.
   */
  public interface ResizeCallback {
    void onResize();
  }

  /**
   * Resources.
   */
  public interface Resources extends Tree.Resources {
    @Source("resources/JavaScriptProfileRenderer.css")
    Css javaScriptProfileRendererCss();
  }

  private class FlatChildRowRenderer implements Resymbolizeable {
    private final JavaScriptProfileNode profileNode;
    private final TableRowElement row;
    private TableCellElement symbolNameCell;

    FlatChildRowRenderer(JavaScriptProfileNode profileNode, TableRowElement row) {
      this.profileNode = profileNode;
      this.row = row;
    }

    public void reSymbolize(final String sourceServer,
        final String sourceViewerServer, final JsSymbol sourceSymbol,
        final SourcePresenter sourcePresenter) {
      AnchorElement resymbolizedSymbol = symbolNameCell.getOwnerDocument().createAnchorElement();
      resymbolizedSymbol.setClassName(css.resymbolizedLink());
      resymbolizedSymbol.setInnerText(sourceSymbol.getSymbolName());
      resymbolizedSymbol.setHref("javascript:;");

      symbolNameCell.appendChild(resymbolizedSymbol);
      listenerManager.manageEventListener(ClickEvent.addClickListener(
          resymbolizedSymbol, resymbolizedSymbol, new ClickListener() {
            public void onClick(ClickEvent event) {
              sourcePresenter.showSource(sourceServer
                  + sourceSymbol.getResourceUrl().getPath(),
                  sourceViewerServer, sourceSymbol.getLineNumber(), 0,
                  sourceSymbol.getAbsoluteFilePath());
            }
          }));

      if (resizeCallback != null) {
        resizeCallback.onResize();
      }
    }

    void render() {
      final JsSymbol childSymbol = profileNode.getSymbol();
      symbolNameCell = row.insertCell(-1);
      symbolNameCell.setInnerText(formatSymbolName(childSymbol));
      final TableCellElement resourceCell = row.insertCell(-1);
      renderResourceLocation(resourceCell, childSymbol);
      row.insertCell(-1).setInnerHTML(
          "<b>" + formatSelfTime(profileNode) + "%</b></td>");
      row.insertCell(-1).setInnerHTML(
          "<b>" + formatTime(profileNode) + "%</td>");

      Color rowColor = rowEvenOdd ? Color.WHITE : Color.CHROME_BLUE;
      row.getStyle().setBackgroundColor(rowColor.toString());
      rowEvenOdd = !rowEvenOdd;
    }
  }

  private class ProfileTree extends Tree {
    private class ProfileItem extends Tree.Item implements Resymbolizeable {

      private DivElement bottomDiv;

      /**
       * New child item.
       */
      public ProfileItem(ProfileItem parent, JavaScriptProfileNode profileNode) {
        super(parent);
        setItemTarget(profileNode);
        initItem(profileNode);
      }

      /**
       * New root item.
       */
      public ProfileItem(Tree tree, JavaScriptProfileNode profileNode) {
        super(tree);
        setItemTarget(profileNode);
        initItem(profileNode);
      }

      @Override
      public void handleEvent(Event event) {
        super.handleEvent(event);
        if (resizeCallback != null) {
          resizeCallback.onResize();
        }
      }

      public void reSymbolize(final String sourceServer,
          final String sourceViewerServer, final JsSymbol sourceSymbol,
          final SourcePresenter sourcePresenter) {
        AnchorElement resymbolizedSymbol = bottomDiv.getOwnerDocument().createAnchorElement();
        resymbolizedSymbol.setClassName(css.resymbolizedLink());
        resymbolizedSymbol.setInnerText(sourceSymbol.getSymbolName());
        resymbolizedSymbol.setHref("javascript:;");

        bottomDiv.appendChild(resymbolizedSymbol);
        listenerManager.manageEventListener(ClickEvent.addClickListener(
            resymbolizedSymbol, resymbolizedSymbol, new ClickListener() {
              public void onClick(ClickEvent event) {
                sourcePresenter.showSource(sourceServer
                    + sourceSymbol.getResourceUrl().getPath(),
                    sourceViewerServer, sourceSymbol.getLineNumber(), 0,
                    sourceSymbol.getAbsoluteFilePath());
              }
            }));

        if (resizeCallback != null) {
          resizeCallback.onResize();
        }
      }

      private void initItem(JavaScriptProfileNode profileNode) {
        Container container = new DefaultContainerImpl(
            this.getItemLabelElement());
        DivElement topDiv = container.getDocument().createDivElement();
        topDiv.setClassName(css.treeItemTopDiv());
        this.getItemLabelElement().appendChild(topDiv);
        // The bottom div is reserved for the resymbolized link.
        bottomDiv = container.getDocument().createDivElement();
        bottomDiv.setClassName(css.treeItemBottomDiv());
        this.getItemLabelElement().appendChild(bottomDiv);

        final JsSymbol symbol = profileNode.getSymbol();
        SpanElement symbolNameElement = container.getDocument().createSpanElement();
        symbolNameElement.setInnerText(formatSymbolName(symbol));
        topDiv.appendChild(symbolNameElement);
        renderResourceLocation(topDiv, symbol);
        SpanElement timeValue = container.getDocument().createSpanElement();
        topDiv.appendChild(timeValue);
        timeValue.setInnerHTML(" <b>self: " + formatSelfTime(profileNode)
            + "%</b> (" + formatTime(profileNode) + "%)");
      }
    }

    /**
     * Creates a node that is not expanded, and children are to be created
     * lazily.
     */
    private class UnexpandedProfileItem extends ProfileItem {
      private boolean dirtyChildren;

      public UnexpandedProfileItem(final ProfileItem parent,
          final JavaScriptProfileNode profileNode) {
        super(parent, profileNode);
        List<JavaScriptProfileNode> children = profileNode.getChildren();
        if (children == null || children.size() == 0) {
          dirtyChildren = false;
        } else {
          this.setExpandIconVisible(true);
          this.setExpansionIcon(false);
          dirtyChildren = true;
        }
      }

      @Override
      public void handleEvent(Event event) {
        // if we have clicked on the expansion control and we havn't yet
        // generated
        // the DOM for our children.
        if (!isSelectionEvent(event) && dirtyChildren) {
          // Build the children.
          expand();
          event.cancelBubble(true);
        } else {
          super.handleEvent(event);
        }
        if (resizeCallback != null) {
          resizeCallback.onResize();
        }
      }

      private void expand() {
        JavaScriptProfileNode profileNode = (JavaScriptProfileNode) getItemTarget();
        addChildrenRecursive(this, resources, profileNode, 1);
        this.setExpansionIcon(true);
      }
    }

    public ProfileTree(Container container, Resources resources,
        JavaScriptProfileNode profileRoot) {
      super(container, resources);
      List<JavaScriptProfileNode> children = profileRoot.getChildren();
      Collections.sort(children, JavaScriptProfileModel.nodeTimeComparator);
      for (int i = 0, length = children.size(); i < length; ++i) {
        final JavaScriptProfileNode profileChild = children.get(i);
        // add root nodes
        final ProfileItem item = new ProfileItem(this, profileChild);
        // Add resymbolized data to frame/profile if it is available.
        Command.defer(new Command.Method() {
          public void execute() {
            if (ssController != null) {
              JsSymbol jsSymbol = profileChild.getSymbol();
              ssController.attemptResymbolization(
                  jsSymbol.getResourceUrl().getUrl(), jsSymbol.getSymbolName(),
                  item, sourcePresenter);
            }
          }
        });
        addChildrenRecursive(item, resources, children.get(i), 1);
      }
    }

    /**
     * Displays a hierarchical profile, top-down or bottom-up.
     */
    private void addChildrenRecursive(ProfileItem item,
        Tree.Resources resources, JavaScriptProfileNode profileParent, int depth) {

      List<JavaScriptProfileNode> children = profileParent.getChildren();

      Collections.sort(children, JavaScriptProfileModel.nodeTimeComparator);
      for (int i = 0, length = children.size(); i < length; ++i) {
        final JavaScriptProfileNode profileChild = children.get(i);
        if (depth < 4 || profileChild.hasTwoOrMoreChildren() == false) {
          final ProfileItem childItem = new ProfileItem(item, profileChild);
          // Add resymbolized data to frame/profile if it is available.
          Command.defer(new Command.Method() {
            public void execute() {
              if (ssController != null) {
                final JsSymbol childSymbol = profileChild.getSymbol();
                ssController.attemptResymbolization(
                    childSymbol.getResourceUrl().getUrl(),
                    childSymbol.getSymbolName(), childItem, sourcePresenter);
              }
            }
          });
          addChildrenRecursive(childItem, resources, children.get(i), depth + 1);
        } else {
          new UnexpandedProfileItem(item, profileChild);
        }
      }
    }
  }

  // Constant for limiting the number of symbols we show initially for the flat
  // profile.
  private static final int FLAT_PROFILE_PAGE_SIZE = 15;

  // Flips between true and false depending on when a flat profile row gets
  // rendered.
  private static boolean rowEvenOdd = true;

  private final Tree.Resources resources;

  private final Div profileDiv;

  private final JavaScriptProfile profile;

  private final ResizeCallback resizeCallback;

  private final SourceSymbolClickListener sourceClickCallback;

  private final SourcePresenter sourcePresenter;

  private final Css css;

  private final SymbolServerController ssController;

  private final ManagesEventListeners listenerManager;

  public JavaScriptProfileRenderer(Container container, Resources resources,
      ManagesEventListeners listenerManager,
      SymbolServerController ssController, SourcePresenter sourcePresenter,
      JavaScriptProfile profile, SourceSymbolClickListener sourceClickCallback,
      ResizeCallback resizeCallback) {
    this.profileDiv = new Div(container);
    this.resources = resources;
    this.profile = profile;
    this.sourceClickCallback = sourceClickCallback;
    this.resizeCallback = resizeCallback;
    this.ssController = ssController;
    this.sourcePresenter = sourcePresenter;
    this.css = resources.javaScriptProfileRendererCss();
    this.listenerManager = listenerManager;
  }

  /**
   * Display the specified profile.
   * 
   * @param profileType one of JavaScriptProfile.PROFILE_TYPE_XXX values
   */
  public void show(int profileType) {
    JavaScriptProfileNode profileRoot = profile.getProfile(profileType);
    if (profileRoot == null) {
      profileDiv.setHtml("Profile is empty.");
      return;
    }
    profileDiv.setHtml("");
    Container container = new DefaultContainerImpl(profileDiv.getElement());
    // show VM states (Garbage Collect, etc)
    StringBuilder result = new StringBuilder();
    result.append("<h3>VM States</h3>");
    result.append("<br/>");
    profile.getVmStateHtml(result);
    result.append("<br/>");
    DivElement vmStatesDiv = container.getDocument().createDivElement();
    vmStatesDiv.setInnerHTML(result.toString());
    profileDiv.getElement().appendChild(vmStatesDiv);

    profileDiv.setHtml(result.toString());
    switch (profileType) {
      case JavaScriptProfile.PROFILE_TYPE_FLAT:
        dumpNodeChildrenFlat(container, profileRoot);
        break;
      case JavaScriptProfile.PROFILE_TYPE_BOTTOM_UP:
      case JavaScriptProfile.PROFILE_TYPE_TOP_DOWN:
        new ProfileTree(container, resources, profileRoot);
        break;
      default:
        if (ClientConfig.isDebugMode()) {
          Logging.getLogger().logText("Unknown Profile type: " + profileType);
        }
        assert false : "Unknown profile type: " + profileType;
    }
  }

  private void addFlatChild(final JavaScriptProfileNode child,
      Table profileTable) {

    TableRowElement row = profileTable.appendRow();
    final FlatChildRowRenderer childRenderer = new FlatChildRowRenderer(child,
        row);
    childRenderer.render();

    // Add resymbolized data to frame/profile if it is available.
    Command.defer(new Command.Method() {
      public void execute() {
        if (ssController != null) {
          final JsSymbol childSymbol = child.getSymbol();
          ssController.attemptResymbolization(
              childSymbol.getResourceUrl().getUrl(),
              childSymbol.getSymbolName(), childRenderer, sourcePresenter);
        }
      }
    });
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

    for (int length = children.size(); rowIndex < length; ++rowIndex) {
      JavaScriptProfileNode child = children.get(rowIndex);
      // Truncate the display by default to show only nodes where self time
      // occurred.
      if (child.getSelfTime() <= 0 && (length - rowIndex > 4)
          || rowIndex > FLAT_PROFILE_PAGE_SIZE) {
        break;
      }
      addFlatChild(child, profileTable);
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
      listenerManager.manageEventListener(anchor.addClickListener(new ClickListener() {

        public void onClick(ClickEvent event) {
          profileTable.deleteRow(moreRowIndex + 1);
          for (int i = moreRowIndex; i < children.size(); ++i) {
            JavaScriptProfileNode child = children.get(i);
            addFlatChild(child, profileTable);
          }
          if (resizeCallback != null) {
            resizeCallback.onResize();
          }
        }
      }));
    }
  }

  /**
   * Return a string with the self time formatted as a decimal percentage.
   */
  private String formatSelfTime(JavaScriptProfileNode profileNode) {
    int relativeSelfTime = (int) (profile.getTotalTime() > 0
        ? (profileNode.getSelfTime() / profile.getTotalTime()) * 100 : 0);
    return TimeStampFormatter.formatToFixedDecimalPoint(relativeSelfTime, 1);
  }

  /**
   * Return the symbol name formatted as a human readable string. If the symbol
   * is blank, the string [unknown] is substituted.
   */
  private String formatSymbolName(JsSymbol jsSymbol) {
    return "".equals(jsSymbol.getSymbolName()) ? "[unknown] "
        : jsSymbol.getSymbolName() + "() ";
  }

  /**
   * Return a string with the time formatted as a decimal percentage.
   */
  private String formatTime(JavaScriptProfileNode profileNode) {
    int relativeTime = (int) (profile.getTotalTime() > 0
        ? (profileNode.getTime() / profile.getTotalTime()) * 100 : 0);
    return TimeStampFormatter.formatToFixedDecimalPoint(relativeTime, 1);
  }

  /**
   * Create a resource location as an anchor in the parent element.
   */
  private void renderResourceLocation(Element parent, final JsSymbol jsSymbol) {
    final Anchor anchor = new Anchor(new DefaultContainerImpl(parent));

    String resourceLocation = jsSymbol.getResourceUrl().getLastPathComponent();
    if (!jsSymbol.isNativeSymbol()) {
      resourceLocation = "".equals(resourceLocation) ? "" : resourceLocation
          + ":" + jsSymbol.getLineNumber();
      anchor.setHref("javascript:;");
      listenerManager.manageEventListener(anchor.addClickListener(new ClickListener() {
        public void onClick(ClickEvent event) {
          String resourceUrl = jsSymbol.getResourceUrl().getUrl();
          if (ClientConfig.isDebugMode()) {
            Logging.getLogger().logText(
                "opening resource " + resourceUrl + " line: "
                    + jsSymbol.getLineNumber());
          }
          sourceClickCallback.onSymbolClicked(resourceUrl, null,
              jsSymbol.getLineNumber(), 0, null);
        }
      }));
    } else {
      resourceLocation = "native " + resourceLocation;
    }
    anchor.setText(resourceLocation);
  }
}