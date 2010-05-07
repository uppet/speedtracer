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

import com.google.gwt.coreext.client.JsIntegerMap;
import com.google.gwt.graphics.client.Color;
import com.google.speedtracer.client.model.DomEvent;
import com.google.speedtracer.client.model.EvalScript;
import com.google.speedtracer.client.model.GarbageCollectionEvent;
import com.google.speedtracer.client.model.JavaScriptExecutionEvent;
import com.google.speedtracer.client.model.LayoutEvent;
import com.google.speedtracer.client.model.LogEvent;
import com.google.speedtracer.client.model.PaintEvent;
import com.google.speedtracer.client.model.ParseHtmlEvent;
import com.google.speedtracer.client.model.RecalcStyleEvent;
import com.google.speedtracer.client.model.ResourceDataReceivedEvent;
import com.google.speedtracer.client.model.TimerFiredEvent;
import com.google.speedtracer.client.model.XhrReadyStateChangeEvent;

/**
 * Simple constants class to hold our EventRecord Type-> Color mappings for
 * charts and color coding.
 */
public class EventRecordColors {

  private static final JsIntegerMap<Color> colorMap = JsIntegerMap.<Color> create();
  private static final Color OTHER_COLOR = Color.LIGHTGREY;

  static {
    registerColor(DomEvent.TYPE, Color.ORANGE);
    registerColor(LayoutEvent.TYPE, Color.BLUEVIOLET);
    registerColor(PaintEvent.TYPE, Color.MIDNIGHT_BLUE);
    registerColor(ParseHtmlEvent.TYPE, Color.INDIAN_RED);
    registerColor(LogEvent.TYPE, Color.CYAN);
    registerColor(TimerFiredEvent.TYPE, Color.BLUE);
    registerColor(XhrReadyStateChangeEvent.TYPE, Color.LIGHTGREEN);
    registerColor(RecalcStyleEvent.TYPE, Color.DARKGREEN);
    registerColor(EvalScript.TYPE, Color.PEACH);
    registerColor(JavaScriptExecutionEvent.TYPE, Color.YELLOW);
    registerColor(ResourceDataReceivedEvent.TYPE, Color.DARKBLUE);
    registerColor(GarbageCollectionEvent.TYPE, Color.BROWN);

    // TODO(jaimeyap): Make use of these colors later on.
    // registerColor(MouseHoverStyleEvent.TYPE, Color.LIMEGREEN);
    // registerColor(MouseHoverStyleEvent.TYPE, Color.LIMEGREEN);
    // registerColor(DomEventDispatch.TYPE, Color.YELLOW);
    // registerColor(DomBindingEvent.TYPE, Color.PALE_GREEN);
    // registerColor(JavaScriptCompileEvent.TYPE, Color.CYAN);
  }

  public static Color getColorForType(int type) {
    if (colorMap.hasKey(type)) {
      return colorMap.get(type);
    } else {
      return OTHER_COLOR;
    }
  }
  
  public static void registerColor(int type, Color color) {
    colorMap.put(type, color);
  }
}
