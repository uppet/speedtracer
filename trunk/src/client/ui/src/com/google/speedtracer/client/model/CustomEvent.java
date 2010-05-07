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
package com.google.speedtracer.client.model;

import com.google.gwt.coreext.client.DataBag;
import com.google.gwt.coreext.client.JsIntegerMap;
import com.google.gwt.coreext.client.JsStringIntegerMap;
import com.google.gwt.graphics.client.Color;
import com.google.speedtracer.client.visualizations.view.EventRecordColors;

/**
 * Data driven event that knows how to display itself.
 */
public class CustomEvent extends UiEvent {
  /**
   * Visitor for registering custom event types.
   */
  public static class TypeRegisteringVisitor implements LeafFirstTraversalVoid {
    public void visit(UiEvent event) {
      if (isCustomEvent(event.getType())) {
        event.<CustomEvent> cast().registerType();
      }
    }
  }
  
  public static final int TYPE = -2;

  // ID allocations start at -2.
  private static int customId = TYPE;

  private static JsIntegerMap<String> idNameMap = JsIntegerMap.create();

  private static JsStringIntegerMap nameIdMap = JsStringIntegerMap.create();

  public static String getCustomTypeName(int type) {
    return idNameMap.get(type);
  }

  public static final boolean isCustomEvent(int type) {
    return type <= TYPE;
  }

  private static int getCustomId(String typeName) {
    if (nameIdMap.hasKey(typeName)) {
      return nameIdMap.get(typeName);
    } else {
      int type = nextId();
      nameIdMap.put(typeName, type);
      idNameMap.put(type, typeName);
      return type;
    }
  }

  private static int nextId() {
    return customId--;
  }

  protected CustomEvent() {
  }

  public final String getColorString() {
    return DataBag.getStringProperty(this, "color");
  }

  public final DataBag getCustomData() {
    return getData();
  }

  public final String getTypeName() {
    return DataBag.getStringProperty(this, "typeName");
  }

  public final void registerType() {
    int type = getCustomId(getTypeName());
    setType(type);
    EventRecordColors.registerColor(type, new Color(getColorString()));
  }

  private native void setType(int type) /*-{
    this.type = type;
  }-*/;
}
