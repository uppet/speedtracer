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
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.SpanElement;
import com.google.gwt.dom.client.UListElement;
import com.google.gwt.events.client.Event;
import com.google.gwt.events.client.EventListener;
import com.google.gwt.events.client.EventListenerRemover;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.topspin.ui.client.ClickEvent;
import com.google.gwt.topspin.ui.client.Container;
import com.google.gwt.topspin.ui.client.DefaultContainerImpl;
import com.google.gwt.topspin.ui.client.Widget;
import com.google.speedtracer.client.util.Command;
import com.google.speedtracer.client.util.dom.DocumentExt;

import java.util.ArrayList;
import java.util.List;

/**
 * A standard hierarchical tree widget. The tree contains a hierarchy of
 * {@link Tree.Item tree items} that the user can open, close, and select.
 * 
 * TODO(jaimeyap): There's still a lot of missing functionality here, most
 * notably keyboard support.
 * 
 * Although Speed Tracer's version now has proper tree styles and selection
 * support.
 */
public class Tree extends Widget {

  /**
   * Styles.
   */
  public interface Css extends CssResource {
    String expansionControl();

    String itemList();

    String leaf();

    String minus();

    String nodeAnnotation();

    String plus();

    String tree();

    String treeItem();

    String treeItemContent();

    String treeItemContentSelected();

    String treeItemLabel();
  }
  /**
   * Listener interface for receiving expansion/collapses of Items.
   */
  public interface ExpansionChangeListener {
    void onExpansionChange(Item changedItem);
  }

  /**
   * A widget representing a single item within the tree.
   * 
   * TODO(jaimeyap): Item should not have the setText() or setHtml() methods.
   * Instead, we should create an 'html item' and a 'container item' that can
   * handle widgets. Having these two things conflated in the original GWT Tree
   * is an unnecessary source of complexity.
   * 
   * TODO(jaimeyap): This class used to be a member class but due to a GWT
   * compiler bug (@see
   * "http://code.google.com/p/google-web-toolkit/issues/detail?id=3408") we've
   * changed it to a static member class where we explicitly pass the reference
   * to the enclosing Tree instance in the constructor.
   */
  public static class Item extends Widget implements EventListener {

    private final Tree owner;
    private DivElement annotationIcon;
    private List<Item> childList = new ArrayList<Item>();
    private UListElement childListElement;
    private DivElement contentElem;
    private Container defaultContainer;
    private DivElement expandIcon;
    private SpanElement itemLabel;
    // This is a placeholder reference for arbitrary data we want to associate
    // with a tree item. Useful for selection change events
    private Object itemTarget;
    // Keeps track of how deep we are in the tree.
    // Would like to make it final, but due to constructor chaining, we lose
    // reference to parent before getting the the last chained constructor.
    private int nodeDepth;
    private Item parent;

    /**
     * Creates a new Item as a child of an existing item, providing a Container
     * for the item.
     * 
     * @param parent existing item in the tree
     * @param parentContainer An alternative container to use for holding this
     *          Item (it will be attached to the parent element.)
     */
    public Item(Item parent, Container parentContainer) {
      this(parent.owner, parentContainer);
      this.parent = parent;
      parent.childList.add(this);
      // Indicate that the parent has children
      if (!parent.isOpen()) {
        parent.setOpen(true);
      }
      this.nodeDepth = parent.nodeDepth + 1;
    }

    /**
     * Creates a new Item as a child of an existing item.
     * 
     * @param parent existing item in the tree
     */
    public Item(Item parent) {
      this(parent, parent.ensureContainer());
    }

    /**
     * Creates a new Item at the root of a {@link Tree}.
     * 
     * @param tree the item's tree
     */
    public Item(Tree tree) {
      this(tree, tree.defaultContainer);
      tree.setSelection(this);
      this.owner.childList.add(this);
      this.nodeDepth = 0;
    }

    /**
     * Common constructor used to create a new Item in a {@link Tree}.
     * 
     * @param tree the item's tree
     * @param container
     */
    private Item(Tree tree, Container container) {
      super(container.getDocument().createLIElement(), container);
      this.owner = tree;
      final Tree.Css css = tree.getResources().treeCss();
      getElement().setClassName(css.treeItem());
      contentElem = container.getDocument().createDivElement();
      contentElem.setClassName(css.treeItemContent());
      itemLabel = container.getDocument().createSpanElement();
      itemLabel.setClassName(css.treeItemLabel());
      expandIcon = container.getDocument().createDivElement();
      expandIcon.setClassName(css.expansionControl() + " " + css.leaf());
      contentElem.appendChild(expandIcon);
      contentElem.appendChild(itemLabel);
      getElement().appendChild(contentElem);
      sinkEvents();
    }

    /**
     * Returns the node's content Element.
     */
    public Element getContentElement() {
      return contentElem;
    }

    /**
     * Returns the node's label Element to allow people to stick arbitrary
     * things to a Tree Item.
     */
    public Element getItemLabelElement() {
      return itemLabel;
    }

    /**
     * Returns application specific context associated with this item.
     * 
     * @return application specific context associated with this item.
     */
    public Object getItemTarget() {
      return itemTarget;
    }

    /**
     * Gets the depth of this node in the tree.
     * 
     * @return how deep this node is in the tree.
     */
    public final int getNodeDepth() {
      return nodeDepth;
    }

    public Tree getOwningTree() {
      return owner;
    }

    /**
     * Gets this item's parent item.
     * 
     * @return the item's parent, or <code>null</code> if it is a root item
     */
    public Item getParent() {
      return parent;
    }

    /**
     * Handles click events in the tree. Decides between a node selection and
     * node expansion/contraction.
     */
    public void handleEvent(Event event) {
      if (isSelectionEvent(event)) {
        if (event.getShiftKey()) {
          // It is a multi select
          // TODO (jaimeyap): Properly handle a range multiselect.
          // Currently we can only handle single selections and ctrl selections.
        } else {
          if (event.getCtrlKey() || event.getMetaKey()) {
            // It is a crtl selection which means we add one more node to the
            // selection list.
            owner.addSelection(Item.this);
          } else {
            // it is a single select
            owner.setSelection(Item.this);
          }
        }
      } else {
        setOpen(!isOpen());
      }
      event.cancelBubble(true);
    }

    /**
     * Gets whether this item is currently open or closed.
     * 
     * @return <code>true</code> if the item is open
     */
    public boolean isOpen() {
      if (childListElement == null) {
        return false;
      }
      return !"none".equals(childListElement.getStyle().getProperty("display"));
    }

    public void setExpandIconVisible(boolean visible) {
      if (visible) {
        this.expandIcon.getStyle().setProperty("display", "inline-block");
      } else {
        this.expandIcon.getStyle().setProperty("display", "none");
      }
    }

    /**
     * Associates an application specific context with this item.
     * 
     * @param itemTarget application specific context.
     */
    public void setItemTarget(Object itemTarget) {
      this.itemTarget = itemTarget;
    }

    /**
     * Sets whether this item is open or closed by toggling the plus/minus icon
     * and by setting the display on the childList. This occurs only if the
     * childList is non-null.
     * 
     * @param open <code>true</code> to open the item, <code>false</code> to
     *          close it
     */
    public void setOpen(boolean open) {
      if (childListElement != null) {
        setExpansionIcon(open);

        if (open) {
          childListElement.getStyle().setProperty("display", "");
        } else {
          childListElement.getStyle().setProperty("display", "none");
        }

        owner.fireExpansionChangeEvent(this);
      }
    }

    /**
     * Sets the item's text contents.
     * 
     * @param text the text contents
     */
    public void setText(String text) {
      itemLabel.setInnerText(text);
    }

    protected void annotate() {
      if (annotationIcon == null) {
        annotationIcon = DocumentExt.get().createDivWithClassName(
            owner.getResources().treeCss().nodeAnnotation());
        contentElem.appendChild(annotationIcon);
      }
    }

    protected List<Item> getChildList() {
      return childList;
    }

    protected UListElement getChildListElement() {
      return childListElement;
    }

    protected boolean isSelectionEvent(Event event) {
      return itemLabel.isOrHasChild(event.getTarget());
    }

    /**
     * Simply changes the Icon to the plus or the minus. This DOES NOT change
     * the nodes open state as queried by <code>isOpen()</code>.
     * 
     * @param open
     */
    protected void setExpansionIcon(boolean open) {
      final Css css = getOwningTree().getResources().treeCss();
      if (open) {
        expandIcon.setClassName(css.expansionControl() + " " + css.minus());
      } else {
        expandIcon.setClassName(css.expansionControl() + " " + css.plus());
      }
    }

    private void ensureChildList() {
      if (childListElement == null) {
        childListElement = getElement().getOwnerDocument().createULElement();
        childListElement.setClassName(getOwningTree().getResources().treeCss().itemList());
        childListElement.getStyle().setProperty("display", "none");
        getElement().appendChild(childListElement);
      }
    }

    private Container ensureContainer() {
      if (defaultContainer == null) {
        ensureChildList();
        defaultContainer = new DefaultContainerImpl(childListElement);
      }
      return defaultContainer;
    }

    private void setSelection(boolean selected) {
      final Css css = getOwningTree().getResources().treeCss();
      contentElem.setClassName(selected ? css.treeItemContentSelected()
          : css.treeItemContent());
    }

    private void sinkEvents() {
      owner.addRemover(Event.addEventListener(ClickEvent.NAME, expandIcon, this));
      owner.addRemover(Event.addEventListener(ClickEvent.NAME, itemLabel, this));
    }
  }

  /**
   * Externalized Resource interface.
   */
  public interface Resources extends ClientBundle {
    @Source("resources/info-bubble.png")
    ImageResource infoBubble();

    @Source("resources/tree_leaf.png")
    ImageResource itemLeaf();

    @Source("resources/tree_minus.png")
    ImageResource itemMinus();

    @Source("resources/tree_plus.png")
    ImageResource itemPlus();

    @Source("resources/Tree.css")
    Tree.Css treeCss();
  }

  /**
   * Listener interface for receiving Selection changes.
   */
  public interface SelectionChangeListener {
    void onSelectionChange(ArrayList<Item> selected);
  }

  private final List<Item> childList = new ArrayList<Item>();

  // We keep a list of selected items to support multi-select.
  private final List<Item> currentSelections;

  private final Container defaultContainer;

  private boolean disableSelection = false;

  private final List<ExpansionChangeListener> expansionListeners = new ArrayList<ExpansionChangeListener>();

  private Command.Method fireExpansionChangeEvent = null;

  private Command.Method fireSelectionChangeEvent = null;

  private final List<EventListenerRemover> removeHandles = new ArrayList<EventListenerRemover>();

  private EventListenerRemover remover;

  private final Tree.Resources resources;

  private final List<SelectionChangeListener> selectionListeners = new ArrayList<SelectionChangeListener>();

  /**
   * Creates a new Tree widget in the given container.
   * 
   * @param container the container in which the widget will be created
   */
  public Tree(Container container, Tree.Resources resources) {
    super(container.getDocument().createULElement(), container);
    this.currentSelections = new ArrayList<Item>();
    this.defaultContainer = new DefaultContainerImpl(getElement());
    getElement().setClassName(resources.treeCss().tree());
    this.resources = resources;
  }

  public void addExpansionChangeListener(ExpansionChangeListener listener) {
    expansionListeners.add(listener);
  }

  /**
   * Adds an {@link Item} to the list of currently selected nodes.
   * 
   * @param toSelect the {@link Item} to select
   */
  public void addSelection(Item toSelect) {
    currentSelections.add(toSelect);
    toSelect.setSelection(true);
    fireSelectionChangeEvent();
  }

  public void addSelectionChangeListener(SelectionChangeListener listener) {
    selectionListeners.add(listener);
  }

  /**
   * Remove the contents of the tree and any registered listeners.
   */
  public void clear() {
    getRemover().remove();
    getElement().setInnerHTML("");
  }

  /**
   * Closes all nodes in the tree.
   */
  public void collapseAll() {
    for (int i = 0, n = childList.size(); i < n; i++) {
      Item item = childList.get(i);
      setOpenRecursive(item, false);
    }
  }

  public void disableSelection(boolean disable) {
    this.disableSelection = disable;
  }

  /**
   * Opens all nodes in the tree.
   */
  public void expandAll() {
    for (int i = 0, n = childList.size(); i < n; i++) {
      Item item = childList.get(i);
      setOpenRecursive(item, true);
    }
  }

  public EventListenerRemover getRemover() {
    if (remover == null) {
      remover = new EventListenerRemover() {
        public void remove() {
          for (int i = 0, n = removeHandles.size(); i < n; i++) {
            removeHandles.get(i).remove();
          }
          removeHandles.clear();
        }
      };
    }

    return remover;
  }

  /**
   * Sets the current selection to the specified {@link Item}. If one or more
   * nodes were selected, this method unselects them.
   * 
   * @param selected the {@link Item} to select.
   */
  public void setSelection(Item selected) {
    if (!disableSelection) {
      for (int i = 0, n = currentSelections.size(); i < n; i++) {
        Item item = currentSelections.get(0);
        item.setSelection(false);
        currentSelections.remove(0);
      }

      addSelection(selected);
    }
  }

  // TODO(knorton): Replace with common utility class, EventCleanup.
  protected void addRemover(EventListenerRemover remover) {
    removeHandles.add(remover);
  }

  protected void fireExpansionChangeEvent(final Item changedItem) {
    // We queue a single task to do this as soon as possible.
    // This method can get called recursively, so we dont want to fire a
    // bajillion expansion change events.
    if (fireExpansionChangeEvent != null) {
      return;
    }

    fireExpansionChangeEvent = new Command.Method() {
      public void execute() {
        for (int i = 0, n = expansionListeners.size(); i < n; i++) {
          expansionListeners.get(i).onExpansionChange(changedItem);
        }
        // null the task once it has run.
        fireExpansionChangeEvent = null;
      }
    };

    Command.defer(fireExpansionChangeEvent);
  }

  protected Tree.Resources getResources() {
    return resources;
  }

  private void fireSelectionChangeEvent() {
    if (fireSelectionChangeEvent != null) {
      return;
    }

    // Fire selection change event sometime later.
    fireSelectionChangeEvent = new Command.Method() {
      public void execute() {
        for (int i = 0, n = selectionListeners.size(); i < n; i++) {
          selectionListeners.get(i).onSelectionChange(
              (ArrayList<Item>) currentSelections);
          fireSelectionChangeEvent = null;
        }
      }
    };

    Command.defer(fireSelectionChangeEvent);
  }

  private void setOpenRecursive(Item item, boolean value) {
    item.setOpen(value);
    for (int i = 0, n = item.getChildList().size(); i < n; i++) {
      Item childItem = item.getChildList().get(i);
      setOpenRecursive(childItem, true);
    }
  }
}
