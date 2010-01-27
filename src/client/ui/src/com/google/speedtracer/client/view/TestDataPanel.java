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
package com.google.speedtracer.client.view;

import com.google.gwt.topspin.ui.client.Container;

/**
 * Panel for displaying options when in mock data mode. The implementation of
 * the class is swapped in by the use_mock_data property-provider. In release
 * permutations, this class is essentially a no-op.
 */
public class TestDataPanel {

  /**
   * Adds the appropriate UI to the controller when running with mock data,
   * otherwise a no-op.
   * 
   * @param controllerContainer
   */
  public void addButtonToController(Controller.Resources resources,
      Controller controller, Container controllerContainer) {
  }

}
