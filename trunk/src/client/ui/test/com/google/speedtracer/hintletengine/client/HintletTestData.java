/*
 * Copyright 2011 Google Inc.
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
package com.google.speedtracer.hintletengine.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.coreext.client.JSON;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.TextResource;

import java.util.ArrayList;
import java.util.List;

/**
 * Loads hintlet test data
 */
public class HintletTestData {

  /**
   * Pull in the test data from external text files.
   */
  public interface MockResources extends ClientBundle {
    @Source("resources/cacheUtilsTestData.in")
    TextResource cacheUtilsTestData();
    
    @Source("resources/frequent_layout.in")
    TextResource frequentLayoutInput();
    
    @Source("resources/frequent_layout.out")
    TextResource frequentLayoutOutput();
    
    @Source("resources/long_duration.in")
    TextResource longDurationInput();
    
    @Source("resources/long_duration.out")
    TextResource longDurationOutput();
    
    @Source("resources/total_bytes.in")
    TextResource totalBytesInput();
    
    @Source("resources/total_bytes.out")
    TextResource totalBytesOutput();
  }

  private static MockResources mockResources;

  private static HintletTestData.MockResources getMockResources(){
    if (mockResources == null) {
      mockResources = GWT.create(MockResources.class);
    }
    return mockResources;
  }

  /**
   * parse the JSON object from the given String
   */
  private static List<JavaScriptObject> getJsonObjects(String text) {
    List<String> jsonStrings = getJsonStrings(text);
    List<JavaScriptObject> jsonObjects = new ArrayList<JavaScriptObject>();
    for (String json : jsonStrings) {
      jsonObjects.add(JSON.parse(json));
    }
    return jsonObjects;
  }
  
  /**
   * Find the Strings representing the JSON objects for
   * the given input
   * 
   * <p>
   * Format:
   * <p>
   * <ul>
   * <li>Every line starting with a # is a comment</li>
   * <li>Blank lines are returned</li>
   * <li>All other lines should contain a single JSON string</li>
   * </ul>
   * 
   * @param input a String representing an input file
   * @return
   */
  private static List<String> getJsonStrings(String input) {
    String[] lines = input.split("\n");
    List<String> jsonStrings = new ArrayList<String>();
    for (String line : lines) {
      if (line.length() == 0) {
        continue;
      }
      if (line.charAt(0) == '#') {
        continue;
      }
      jsonStrings.add(line);
    }
    return jsonStrings;
  }
  
  public static List<JavaScriptObject> getCacheUtilsTestData() {
    String text = getMockResources().cacheUtilsTestData().getText();
    return getJsonObjects(text);
  }
  
  public static List<JavaScriptObject> getFrequentLayoutInput() {
    String text = getMockResources().frequentLayoutInput().getText();
    return getJsonObjects(text);
  }
  
  public static List<JavaScriptObject> getFrequentLayoutOutput() {
    String text = getMockResources().frequentLayoutOutput().getText();
    return getJsonObjects(text);
  }

  public static List<JavaScriptObject> getLongDurationInput() {
    String text = getMockResources().longDurationInput().getText();
    return getJsonObjects(text);
  }
  
  public static List<JavaScriptObject> getLongDurationOutput() {
    String text = getMockResources().longDurationOutput().getText();
    return getJsonObjects(text);
  }
  
  public static List<JavaScriptObject> getTotalBytesInput() {
    String text = getMockResources().totalBytesInput().getText();
    return getJsonObjects(text);
  }
  
  public static List<JavaScriptObject> getTotalBytesOutput() {
    String text = getMockResources().totalBytesOutput().getText();
    return getJsonObjects(text);
  }
}
