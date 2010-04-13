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

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.SpanElement;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.events.client.Event;
import com.google.gwt.topspin.ui.client.Container;
import com.google.gwt.topspin.ui.client.InsertingContainerImpl;
import com.google.speedtracer.client.model.EventRecordType;
import com.google.speedtracer.client.model.LogEvent;
import com.google.speedtracer.client.model.UiEvent;
import com.google.speedtracer.client.util.JSOArray;
import com.google.speedtracer.client.util.TimeStampFormatter;
import com.google.speedtracer.client.visualizations.view.EventTraceBreakdown.Renderer;

import java.util.ArrayList;
import java.util.List;

/**
 * A tree for displaying UIEventRecords. It provides utility implementations to
 * allow Items to be lazily constructed if they belong to parents that have
 * never been expanded.
 * 
 * TODO(jaimeyap): Implement incremental rendering of this tree. Despite the
 * best efforts of filtering... sometimes you just have lots of interesting data
 * to show. So we don't hang the browser, we should probably incrementally
 * render this tree using timers and a continuation style approach to
 * constructing it.
 * 
 * It also extends the basic tree structure to support a
 * {@link EventTraceBreakdown} off the the side.
 */
public class LazyEventTree extends Tree {
  /**
   * A Lazy version of a Tree Item. It has the notion of "dirty Children",
   * meaning it has not constructed the DOM structure for its children yet. It
   * also has the facility for filling itself in.
   */
  private static class LazyItem extends Tree.Item {
    private static void addLabelForEvent(Element itemElem, UiEvent event) {
      itemElem.setInnerText(EventRecordType.typeToDetailedTypeString(event));
      final SpanElement timesElem = itemElem.appendChild(itemElem.getOwnerDocument().createSpanElement());
      timesElem.getStyle().setProperty("cssText", timeLabelStyle);
      if (event.getType() == EventRecordType.AGGREGATED_EVENTS) {
        timesElem.setInnerText(" "
            + TimeStampFormatter.formatMilliseconds(event.getSelfTime(), 1));
      } else {
        timesElem.setInnerText(" "
            + TimeStampFormatter.formatMilliseconds(event.getDuration(), 1)
            + " (self "
            + TimeStampFormatter.formatMilliseconds(event.getSelfTime(), 1)
            + ")");
      }
    }

    private final Renderer renderer;
    private boolean dirtyChildren = true;
    private final UiEvent uiEvent;

    /**
     * Add an item beneath an existing node providing a Container for the item.
     * 
     * This gets called when we are expanding coalesced nodes. The
     * {@link SiblingCoalescer} has a place holder element in the tree. It only
     * expands up to <code>MAX_NODE_EXPANSION_COUNT</code> at a time, so we have
     * to be able to insert nodes in the tree in front of the coalescer
     * placeholder. Hence the special <code>parentContainer</code>.
     * 
     * @param parent The parent node in the tree.
     * @param parentContainer An alternative container to use for holding this
     *          Item (it will be attached to the parent node.)
     * @param resources Static resources
     * @param uiEvent Event to display in this node
     */
    private LazyItem(LazyItem parent, Container parentContainer, UiEvent uiEvent) {
      super(parent, parentContainer);
      this.uiEvent = uiEvent;
      setItemTarget(uiEvent);
      final LazyEventTree tree = parent.getOwningTree();
      addLabelForEvent(getItemLabelElement(), uiEvent);
      renderer = tree.breakdownGraph.createRenderer(uiEvent,
          getNodeDepth());
      getContentElement().appendChild(renderer.getElement());
    }

    /**
     * Add an item beneath an existing node.
     * 
     * @param parent The parent node in the tree.
     * @param resources Static resources
     * @param uiEvent Event to display in this node
     */
    private LazyItem(LazyItem parent, UiEvent uiEvent) {
      super(parent);
      this.uiEvent = uiEvent;
      setItemTarget(uiEvent);
      final LazyEventTree tree = parent.getOwningTree();
      addLabelForEvent(getItemLabelElement(), uiEvent);
      renderer = tree.breakdownGraph.createRenderer(uiEvent,
          getNodeDepth());
      getContentElement().appendChild(renderer.getElement());
    }

    /**
     * Add an item at the root.
     * 
     * @param resources Static resources
     * @param uiEvent Event to display in this node.
     * @param backRef Reference to the tree object.
     */
    private LazyItem(UiEvent uiEvent, LazyEventTree backRef) {
      super(backRef);
      this.uiEvent = uiEvent;
      setItemTarget(uiEvent);
      addLabelForEvent(getItemLabelElement(), uiEvent);
      renderer = backRef.breakdownGraph.createRenderer(uiEvent,
          getNodeDepth());
      getContentElement().appendChild(renderer.getElement());
    }

    public void expand() {
      maybeExpandNode(this, true);
    }

    public LazyEventTree getOwningTree() {
      return (LazyEventTree) super.getOwningTree();
    }

    public UiEvent getUiEvent() {
      return uiEvent;
    }

    @Override
    public void handleEvent(Event event) {
      // if we have clicked on the expansion control and we havn't yet generated
      // the DOM for our children.
      if (!isSelectionEvent(event) && hasDirtyChildren()) {
        // Build the children.
        expand();
        event.cancelBubble(true);
      } else {
        super.handleEvent(event);
      }
    }

    /**
     * Whether or not the children have been constructed.
     * 
     * @return boolean indicating if the children have been constructed.
     */
    public boolean hasDirtyChildren() {
      return dirtyChildren;
    }

    private void setDirtyChildren(boolean b) {
      this.dirtyChildren = b;
    }
  }

  /**
   * Specify a dependency on the resources used in EventPhaseBreakdown.
   */
  public interface Resources extends EventTraceBreakdown.Resources,
      Tree.Resources {
  }

  /**
   * Class that allows for coalescing of Sibling nodes in the tree, and for
   * their subsequent expansion.
   */
  private class SiblingCoalescer extends Tree.Item {
    private final List<UiEvent> coalescedItems;
    private final LazyItem parent;
    private final Container parentContainer;

    public SiblingCoalescer(LazyItem parent) {
      super(parent);
      setText("Hiding short events.");
      this.parent = parent;

      // We want to shove children before the Coalescer, which serves as a place
      // holder.
      this.parentContainer = new InsertingContainerImpl(
          parent.getChildListElement(), getElement());
      this.coalescedItems = new ArrayList<UiEvent>();

      // Give it some style so we know this is different
      if (parent.getUiEvent().getDuration() > TREE_ITEM_COALESCING_THRESHOLD) {
        // Grey it out to begin with
        getElement().getStyle().setProperty("opacity", "0.3");
      }
      getElement().setTitle("Click to reveal hidden events.");
      // We change the icon to closed.
      setExpansionIcon(false);
    }

    public void coalesce(UiEvent event) {
      coalescedItems.add(event);
    }

    public void expand() {
      for (int i = 0, n = coalescedItems.size(); i < n
          && i < MAX_NODE_EXPANSION_COUNT; i++) {
        UiEvent event = coalescedItems.get(0);
        // This is the magic constructor that inserts the LazyItem before the
        // placeholder.
        LazyItem formerlyHidden = new LazyItem(parent, parentContainer, event);

        deemphasizeNodeIfParentIsNotDeemphasized(parent, formerlyHidden);

        // If its got kids, we obviously are not going to be in an opened state.
        // Change the icon.
        if (event.getChildren().size() > 0) {
          formerlyHidden.setExpansionIcon(false);
        }

        coalescedItems.remove(0);
      }

      if (coalescedItems.size() == 0) {
        // get rid of the current placeHolder.
        destroy();
      }

      // Reflow the page by firing expansion change event.
      fireExpansionChangeEvent(parent);
    }

    /**
     * If we have only coalesced one node, the caller may choose to expand us
     * since we aren't really gaining anything by remaining hidden.
     */
    public void expandIfOnlyContainsOneChild() {
      if (coalescedItems.size() == 1) {
        expand();
      }
    }

    @Override
    public void handleEvent(Event event) {
      expand();
    }
  }

  // We cap the number of nodes we expand in a single expansion so we don't
  // accidentally reveal thousands/millions of nodes and hang the browser.
  private static final int MAX_NODE_EXPANSION_COUNT = 50;

  // TODO(knorton): Move this to a CssResource. This requires that subclasses
  // of Tree be able to have more specific Tree.Resource types. That's
  // non-trivial at the moment.
  private static final String timeLabelStyle = "color:#888;white-space:nowrap;";

  // The threshold by which we determine if a node should be hidden/coalesced
  private static final double TREE_ITEM_COALESCING_THRESHOLD = 0.4;

  // The threshold by which we determine if a node should be auto expanded
  private static final double TREE_ITEM_EXPANSION_THRESHOLD = 3;

  /**
   * This method expands the current node and all of its children if they have
   * durations greater than <code>TREE_ITEM_EXPANSION_THRESHOLD</code>.
   * 
   * If <code>force</code> is <code>true</code>, then we definitely expand the
   * current node. Force is NOT passed down recursively.
   * 
   * If the duration of an event is below the
   * <code>TREE_ITEM_COALESCING_THRESHOLD</code>, we hide it by coalescing it
   * with other events below the threshold that are adjacent.
   * 
   * @param node
   */
  public static void maybeExpandNode(LazyItem node, boolean force) {
    UiEvent event = node.getUiEvent();
    boolean whiteListed = isWhiteListed(event);
    JSOArray<UiEvent> children = event.getChildren();
    // If we are not whitelisted, not forcing an expansion, and the duration
    // does not meet the threshold. Leave it unexpanded (set the expansion icon
    // to plus).
    if (!whiteListed && event.getDuration() < TREE_ITEM_EXPANSION_THRESHOLD
        && !force) {
      // Node MUST have already passed the Coalescing threshold
      if (children.size() > 0) {
        // Change the icon to closed. We let the click handler expand it.
        node.setDirtyChildren(true);
        node.setExpansionIcon(false);
      }
      return;
    } else {
      final LazyEventTree tree = node.getOwningTree();
      SiblingCoalescer coalescer = null;
      for (int i = 0, n = children.size(); i < n; i++) {
        UiEvent childUiEvent = children.get(i);
        boolean childWhiteListed = isWhiteListed(childUiEvent);
        // We coalesce zero duration events always EXCEPT:
        // 1. If it is white listed.
        // 2. If there is only one node in the child list (n==1).
        // 3. If we are on the last node and starting a new coalescer (n==i+1)
        // and (coalescer == null)
        if (childUiEvent.getDuration() <= TREE_ITEM_COALESCING_THRESHOLD
            && !childWhiteListed) {
          if (coalescer == null) {
            // If we are on the last child and attempting to start a new
            // coalescer.
            if ((n - i) == 1) {
              // No sense in coalescing only one node.
              LazyItem childNode = createNodeAndAddToTree(node, childUiEvent,
                  tree);
              // We still want to de-emphasize things below the threshold
              deemphasizeNodeIfParentIsNotDeemphasized(node, childNode);
              // We are done with the loop
              break;
            }

            // Create a new Coalescer
            coalescer = tree.createSiblingCoalescer(node);
          }
          // Proceed with coalescing the child
          coalescer.coalesce(childUiEvent);
        } else {
          if (coalescer != null) {
            // If we have only coalesced one node, go ahead and expand it
            coalescer.expandIfOnlyContainsOneChild();
            // null out the coalescer so we start over after this child
            coalescer = null;
          }

          // We are ok to add a node to the tree that is not hidden.
          createNodeAndAddToTree(node, childUiEvent, tree);
        }
      }

      // We are guaranteed to have coerced the child list <ul> element into
      // existence by now. Go ahead and add guide lines for it.
      if (children.size() > 0) {
        tree.breakdownGraph.createBarGraphGuides(node.getChildListElement(),
            node.getUiEvent(), node.getNodeDepth());
      }

      node.setDirtyChildren(false);
    }
  }

  /**
   * Helper for maybeExpandNode that is part of the mutual recursion.
   * 
   * @param parent the node to append the new child node to
   * @param childUiEvent the UiEvent for the new child node
   * @param tree the tree that all these nodes belong to
   * @return the newly created child node
   */
  private static LazyItem createNodeAndAddToTree(LazyItem parent,
      UiEvent childUiEvent, LazyEventTree tree) {
    // Add a new node and maybe expand it
    LazyItem child = new LazyItem(parent, childUiEvent);

    if (childUiEvent.getType() == LogEvent.TYPE) {
      // annotate if we are a log message
      child.annotate();
    }

    maybeExpandNode(child, false);

    renderBarGraph(child);

    return child;
  }

  private static void deemphasizeNodeIfParentIsNotDeemphasized(LazyItem parent,
      LazyItem nodeToDeemphasize) {
    // We style it to de-emphasize it, only if the parent hasn't been
    // hidden. We assume if the parent is above the coalescing threshold
    // that it is not hidden. Also if the parent is the Tree Root node (checking
    // via object identity), then we can assume that no matter what the parent
    // node is not hidden.
    final LazyEventTree ownerTree = parent.getOwningTree();
    if (parent.getUiEvent().getDuration() > TREE_ITEM_COALESCING_THRESHOLD
        || parent == ownerTree.rootNode) {
      nodeToDeemphasize.getElement().getStyle().setProperty("opacity", "0.3");
    }
  }

  /**
   * Events that cannot be coalesced/filtered out.
   * 
   * @param event the event we want to test for white listing
   * @return whether or not we want to white list it
   */
  private static boolean isWhiteListed(UiEvent event) {
    // currently the only criteria is having a log message.
    return event.hasUserLogs();
  }

  private static void renderBarGraph(LazyItem item) {
    // Do initial rendering of the bar graph.
    if (item.isOpen()) {
      item.renderer.getElement().getStyle().setProperty("border",
          "1px solid #ccc");
      item.renderer.renderOnlySelf();
    } else {
      item.renderer.renderSelfAndChildren();
      item.renderer.getElement().getStyle().setProperty("border", "none");
    }
  };

  private final EventTraceBreakdown breakdownGraph;

  private final LazyItem rootNode;

  /**
   * Constructor.
   * 
   * @param container the parent Container
   * @param treeRoot the root UiEvent of the tree
   * @param resources our ImmutableResourceBundle resources
   */
  public LazyEventTree(Container container, UiEvent treeRoot,
      EventTraceBreakdown breakdownGraph, LazyEventTree.Resources resources) {
    super(container, resources);
    EventTraceBreakdown.Css css = resources.eventTraceBreakdownCss();
    getElement().getStyle().setPaddingLeft(
        css.widgetWidth() + css.listMargin() + css.sideMargins(), Unit.PX);

    addExpansionChangeListener(new ExpansionChangeListener() {
      public void onExpansionChange(Item changedItem) {
        LazyItem item = (LazyItem) changedItem;
        renderBarGraph(item);
      }
    });

    this.breakdownGraph = breakdownGraph;

    // We want to stick a render of the masterBar ABOVE the tree.
    // Which means inserting it before the tree's <ul>.
    getElement().getParentElement().insertBefore(
        breakdownGraph.cloneRenderedCanvasElement(), getElement());

    // Builds up the tree with treeRoot as the root UiEvent.
    rootNode = new LazyItem(treeRoot, this);
    // Kick start things by maybe expanding it
    maybeExpandNode(rootNode, false);
  }

  /**
   * SiblingCoalescer is non-static. So we need to expose a create method to
   * give it an enclosing instance.
   * 
   * @param parent the {@link Lazyitem} we want to attach the coalescer to
   * @return the SiblingCoalescer
   */
  private SiblingCoalescer createSiblingCoalescer(LazyItem parent) {
    return new SiblingCoalescer(parent);
  }
}
