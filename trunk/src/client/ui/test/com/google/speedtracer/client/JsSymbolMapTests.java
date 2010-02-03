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
package com.google.speedtracer.client;

import com.google.gwt.junit.client.GWTTestCase;
import com.google.speedtracer.client.visualizations.model.JsSymbolMap;
import com.google.speedtracer.client.visualizations.model.JsSymbolMap.JsSymbol;

/**
 * Tests {@link JsSymbolMap}.
 */
public class JsSymbolMapTests extends GWTTestCase {

  String testGwtSymbolMapString = "# { 0 }"
      + '\n'
      + "# { 'speedtracer.use_mock_model' : 'nope' }"
      + '\n'
      + "# jsName, jsniIdent, className, memberName, sourceUri, sourceLine"
      + '\n'
      + "YAb,,boolean[],,Unknown,0"
      + '\n'
      + "Fi,,com.google.gwt.animation.client.Animation,,jar:file:/path/on/disk/ignored/gwt-user.jar!/com/google/gwt/animation/client/Animation.java,28"
      + '\n'
      + "Ti,com.google.gwt.animation.client.Animation::$run(Lcom/google/gwt/animation/client/Animation;ID)V,com.google.gwt.animation.client.Animation,$run,jar:file:/path/on/disk/ignored/gwt-user.jar!/com/google/gwt/animation/client/Animation.java,124"
      + '\n'
      + "sc,com.google.apu.demo.client.MultiColumnPanel::$manuallyAdjustColumns(Lcom/google/apu/demo/client/MultiColumnPanel;I)V,com.google.apu.demo.client.MultiColumnPanel,$manuallyAdjustColumns,file:/Users/jaimeyap/src/gitosis/apu-demo-app/src/com/google/apu/demo/client/MultiColumnPanel.java,46"
      + '\n';

  String testCompactGwtSymbolMapString = "# { 3 }"
      + '\n'
      + "# { 'user.agent' : 'safari' }"
      + '\n'
      + "# %%, package literal, package key"
      + '\n'
      + "# jsName, jsniSymbolType, package key, className, memberName, fileName, sourceLine"
      + '\n' + "%%,,0" + '\n' + "a,,0,<null>[],,,0" + '\n'
      + "%%,com.google.some.package,1" + '\n'
      + "v[],,1,Outer$Inner[],,Outer,44" + '\n'
      + "%%,com.google.another.package,2" + '\n' + "aC,,2,SomeClass,,%,55"
      + '\n' + "aM,m,2,SomeClass,aMethod,%,57" + '\n';

  @Override
  public String getModuleName() {
    return "com.google.speedtracer.Common";
  }

  class TestableSymbolMap extends JsSymbolMap {
    TestableSymbolMap(String sourceServer) {
      super(sourceServer);
    }
  }

  /**
   * Tests simply adding and looking up a symbol in a symbol map.
   */
  public void testAddSymbol() {
    String sourceServer = "http://notrealsourceserver";
    TestableSymbolMap symbolMap = new TestableSymbolMap(sourceServer);

    JsSymbol nullSymbol = symbolMap.lookup("IDontExist");
    assertTrue(nullSymbol == null);

    String resourceBase = "path/to/a/resource";
    String resourceName = "MyResource.java";
    int lineNumber = 14;
    String symbolName = "path.to.a.resource.MyResource.InnerClass::methodName";
    JsSymbol symbol = new JsSymbol(resourceBase, resourceName, lineNumber,
        symbolName);

    assertEquals(symbolMap.getSourceServer(), sourceServer + "/");

    String obfuscatedName = "APU";
    symbolMap.put(obfuscatedName, symbol);

    JsSymbol retrievedSymbol = symbolMap.lookup(obfuscatedName);
    assertEquals(resourceBase, retrievedSymbol.getResourceBase());
    assertEquals(resourceName, retrievedSymbol.getResourceName());
    assertEquals(lineNumber, retrievedSymbol.getLineNumber());
    assertEquals(symbolName, retrievedSymbol.getSymbolName());
  }

  /**
   * Tests initializing a JsSymbolMap from a GWT symbol map.
   */
  public void testParseCompactGwtSymbolMap() {
    String sourceServer = "http://notrealsourceserver";
    JsSymbolMap symbolMap = JsSymbolMap.parse(sourceServer,
        JsSymbolMap.COMPACT_GWT_SYMBOL_MAP, testCompactGwtSymbolMapString);
    assertEquals(4, symbolMap.getSymbolCount());

    // Tests derived from the testGwtSymbolMapString above.
    testGwtSymbol(symbolMap, "a", "<null>[]", "", "", "Unknown", 0);
    testGwtSymbol(symbolMap, "v[]", "com.google.some.package.Outer$Inner[]",
        "", "com/google/some/package/", "Outer.java", 44);
    testGwtSymbol(symbolMap, "aC", "com.google.another.package.SomeClass", "",
        "com/google/another/package/", "SomeClass.java", 55);
    testGwtSymbol(symbolMap, "aM", "com.google.another.package.SomeClass",
        "aMethod", "com/google/another/package/", "SomeClass.java", 57);
  }

  /**
   * Tests initializing a JsSymbolMap from a GWT symbol map.
   */
  public void testParseGwtSymbolMap() {
    String sourceServer = "http://notrealsourceserver";
    JsSymbolMap symbolMap = JsSymbolMap.parse(sourceServer,
        JsSymbolMap.GWT_SYMBOL_MAP, testGwtSymbolMapString);
    assertEquals(4, symbolMap.getSymbolCount());

    // Tests derived from the testGwtSymbolMapString above.
    testGwtSymbol(symbolMap, "YAb", "boolean[]", "", "", "Unknown", 0);
    testGwtSymbol(symbolMap, "Fi", "com.google.gwt.animation.client.Animation",
        "", "com/google/gwt/animation/client/", "Animation.java", 28);
    testGwtSymbol(symbolMap, "Ti", "com.google.gwt.animation.client.Animation",
        "$run", "com/google/gwt/animation/client/", "Animation.java", 124);
    testGwtSymbol(symbolMap, "sc",
        "com.google.apu.demo.client.MultiColumnPanel",
        "$manuallyAdjustColumns", "com/google/apu/demo/client/",
        "MultiColumnPanel.java", 46);
  }

  private void testGwtSymbol(JsSymbolMap symbolMap, String obfuscatedSymbol,
      String className, String memberName, String sourcePathBase,
      String sourceFileName, int lineNumber) {
    JsSymbol symbol = symbolMap.lookup(obfuscatedSymbol);
    assertTrue(symbol != null);

    assertEquals(sourcePathBase, symbol.getResourceBase());
    assertEquals(sourceFileName, symbol.getResourceName());
    assertEquals(lineNumber, symbol.getLineNumber());
    assertEquals(className + "::" + memberName, symbol.getSymbolName());
  }
}
