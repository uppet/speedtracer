/*
 * Copyright 2012 Google Inc.
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
package com.google.gwt.chrome.crx.linker;

import com.google.gwt.core.ext.LinkerContext;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.linker.AbstractLinker;
import com.google.gwt.core.ext.linker.ArtifactSet;
import com.google.gwt.core.ext.linker.CompilationResult;
import com.google.gwt.core.ext.linker.LinkerOrder;
import com.google.gwt.core.ext.linker.SelectionProperty;
import com.google.gwt.core.ext.linker.LinkerOrder.Order;
import com.google.gwt.dev.About;

import java.util.Collection;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.Map.Entry;

/**
 * A Linker for producing a single JavaScript file from a GWT module. The use of
 * this Linker requires that the module has exactly one distinct compilation
 * result.
 */
@LinkerOrder(Order.PRIMARY)
public class SingleScriptLinker extends AbstractLinker {
  @Override
  public String getDescription() {
    return "Single Script";
  }

  @Override
  public ArtifactSet link(TreeLogger logger, LinkerContext context,
      ArtifactSet artifacts) throws UnableToCompleteException {
    final CompilationResult compilation = findCompilation(logger, artifacts);
    ArtifactSet toReturn = new ArtifactSet(artifacts);
    toReturn.add(emitString(logger, generateJsContents(logger, compilation),
        context.getModuleName() + ".js"));
    return toReturn;
  }

  private static String generateJsContents(TreeLogger logger,
      CompilationResult js) throws UnableToCompleteException {
    String compiledJs = js.getJavaScript()[0];
    final StringBuffer buffer = new StringBuffer();    
    buffer.append("var $gwt_version = \"" + About.GWT_VERSION_NUM + "\";\n");
    buffer.append("var $wnd = window;\n");
    buffer.append("var $doc = $wnd.document;\n");
    buffer.append("var $moduleName, $moduleBase;\n");    
    buffer.append("var $stats;\n").append(compiledJs).append("gwtOnLoad();\n");
    return buffer.toString();
  }
  
  private static CompilationResult findCompilation(TreeLogger logger,
      ArtifactSet artifacts) throws UnableToCompleteException {
    final SortedSet<CompilationResult> compilations = artifacts.find(CompilationResult.class);
    if (compilations.size() != 1) {
      logger.log(TreeLogger.ERROR, "Found " + compilations.size()
          + " permutations compiled in "
          + ExtensionLinker.class.getSimpleName()
          + ".  Use only a single permutation per module with this linker.");
      logPermutations(logger, compilations);
      throw new UnableToCompleteException();
    }
    return compilations.first();
  }

  private static void logPermutations(TreeLogger logger,
      Collection<CompilationResult> compilations) {
    int count = 0;
    for (CompilationResult compilationResult : compilations) {
      SortedSet<SortedMap<SelectionProperty, String>> propertyMap = compilationResult.getPropertyMap();
      StringBuilder builder = new StringBuilder();
      for (SortedMap<SelectionProperty, String> propertySubMap : propertyMap) {
        builder.append("{");
        for (Entry<SelectionProperty, String> entry : propertySubMap.entrySet()) {

          SelectionProperty selectionProperty = entry.getKey();
          if (!selectionProperty.isDerived()) {
            builder.append(selectionProperty.getName() + ":" + entry.getValue()
                + " ");
          }
        }
        builder.append("}");
      }
      logger.log(TreeLogger.ERROR, "Permutation " + count + ": "
          + builder.toString());
      count++;
    }
  }
}