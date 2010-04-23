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

import com.google.gwt.coreext.client.IterableFastStringMap;
import com.google.gwt.graphics.client.Color;
import com.google.speedtracer.client.model.EventRecordType;
import com.google.speedtracer.client.model.ServerEvent;

/**
 *
 */
public class ServerEventColors {
  private static IterableFastStringMap<Color> colorByTypeString;

  public static Color getColorFor(ServerEvent event) {
    assert event.getType() == EventRecordType.SERVER_EVENT : "event is not a ServerEvent";
    if (colorByTypeString == null) {
      final IterableFastStringMap<Color> map = new IterableFastStringMap<Color>();
      map.put("HTTP", Color.ORANGE);
      map.put("WEB_REQUEST", Color.BLUEVIOLET);
      map.put("VIEW_RENDER", Color.MIDNIGHT_BLUE);
      map.put("JDBC", Color.INDIAN_RED);
      map.put("VIEW_RESOLVER", Color.CYAN);
      map.put("TRANSACTION", Color.BLUE);
      map.put("CONTROLLER_METHOD", Color.LIGHTGREEN);
      map.put("MODEL_ATTRIBUTE", Color.DARKGREEN);
      map.put("ANNOTATED_METHOD", Color.PEACH);
      map.put("GRAILS_CONTROLLER_METHOD", Color.YELLOW);
      map.put("LIFECYCLE", Color.DARKBLUE);
      map.put("INIT_BINDER", Color.BROWN);
      map.put("SIMPLE", Color.LIMEGREEN);
      colorByTypeString = map;
    }

    final Color color = colorByTypeString.get(event.getServerEventData().getType());
    return (color == null) ? Color.LIGHTGREY : color;
  }

  private ServerEventColors() {
  }
}
