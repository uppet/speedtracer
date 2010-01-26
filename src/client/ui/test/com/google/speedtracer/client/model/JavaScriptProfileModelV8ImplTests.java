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

import com.google.gwt.junit.client.GWTTestCase;
import com.google.speedtracer.client.Logging;
import com.google.speedtracer.client.model.V8SymbolTable.Symbol;

import java.util.List;

/**
 * Tests for parsing profile data from the v8 JavaScript engine.
 */
public class JavaScriptProfileModelV8ImplTests extends GWTTestCase {

  private static native JavaScriptProfileEvent makeV8ProfileEvent(
      String profileData) /*-{
    return {'type':15,'time':1,'sequence':1,'data':{'format':"v8",'profileData':profileData}};
  }-*/;

  @Override
  public String getModuleName() {
    return "com.google.speedtracer.Common";
  }

  public void testAddressParse() {
    
    JavaScriptProfileModelV8Impl impl = new JavaScriptProfileModelV8Impl();
    long address;
    address = impl.parseAddress("0x100",
        JavaScriptProfileModelV8Impl.ADDRESS_TAG_CODE);
    assertEquals(address, 0x100);
    address = impl.parseAddress("0x200",
        JavaScriptProfileModelV8Impl.ADDRESS_TAG_CODE);
    assertEquals(address, 0x200);
    // Note, the above parsing does not affect the current offset
    
    address = impl.parseAddress("+200",
        JavaScriptProfileModelV8Impl.ADDRESS_TAG_CODE);
    assertEquals(address, 0x200);
    address = impl.parseAddress("-100",
        JavaScriptProfileModelV8Impl.ADDRESS_TAG_CODE);
    assertEquals(address, 0x100);

    address = impl.parseAddress("0x100",
        JavaScriptProfileModelV8Impl.ADDRESS_TAG_CODE);
    assertEquals(address, 0x100);
    address = impl.parseAddress("0x200",
        JavaScriptProfileModelV8Impl.ADDRESS_TAG_CODE);
    assertEquals(address, 0x200);

    // Another tag shouldn't interfere with the address
    address = impl.parseAddress("+1000",
        JavaScriptProfileModelV8Impl.ADDRESS_TAG_CODE_MOVE);
    assertEquals(address, 0x1000);
  }

  public void testAlias() {
    JavaScriptProfileModelV8Impl impl = new JavaScriptProfileModelV8Impl();
    String ccrecs = "profiler,\"begin\",1\n" + "profiler,\"compression\",4\n"
        + "alias,cc,code-creation\n" + "alias,cm,code-move\n"
        + "alias,cd,code-delete\n" + "alias,t,tick\n" + "alias,r,repeat\n"
        + "alias,bi,Builtin\n" + "alias,cdb,CallDebugBreak\n"
        + "alias,cdbsi,CallDebugPrepareStepIn\n" + "alias,cic,CallIC\n"
        + "alias,ci,CallInitialize\n" + "alias,cmm,CallMegamorphic\n"
        + "alias,cm,CallMiss\n" + "alias,cn,CallNormal\n"
        + "alias,cpm,CallPreMonomorphic\n" + "alias,cb,Callback\n"
        + "alias,e,Eval\n" + "alias,f,Function\n" + "alias,klic,KeyedLoadIC\n"
        + "alias,ksic,KeyedStoreIC\n" + "alias,lc,LazyCompile\n"
        + "alias,lic,LoadIC\n" + "alias,re,RegExp\n" + "alias,sc,Script\n"
        + "alias,sic,StoreIC\n" + "alias,s,Stub\n";
    JavaScriptProfileEvent rawEvent = makeV8ProfileEvent(ccrecs);
    JavaScriptProfile profile = new JavaScriptProfile();
    impl.parseRawEvent(rawEvent, profile);
    assertEquals(0.0, profile.getTotalTime());
    assertNull(profile.getBottomUpProfile());
  }

  public void testCodeCreationSimple() {
    JavaScriptProfileModelV8Impl impl = new JavaScriptProfileModelV8Impl();
    String logRecs = "code-creation,LoadIC,5910913e,179,\"parentNode\"\n"
        + "code-creation,RegExp,3f830f,566,\"[^+&gt;] [^+&gt;]\"\n"
        + "code-creation,LazyCompile,59117070,2791,\"last_click http://www.reddit.com/static/reddit.js?v=437941e91e4684e9b4b00eca75a46dd9:62\"\"\n";
    JavaScriptProfileEvent rawEvent = makeV8ProfileEvent(logRecs);
    JavaScriptProfile profile = new JavaScriptProfile();
    impl.parseRawEvent(rawEvent, profile);
    assertEquals(0.0, profile.getTotalTime());
    Symbol found = impl.findSymbol(0x5910913e);
    assertNotNull(found);
  }

  public void testRepeatSimple() {
    JavaScriptProfileModelV8Impl impl = new JavaScriptProfileModelV8Impl();
    String logRecs = "code-creation,LoadIC,5910913e,179,\"parentNode\"\n"
        + "repeat,5,tick,5910913e,+1,0\n";
    JavaScriptProfileEvent rawEvent = makeV8ProfileEvent(logRecs);
    JavaScriptProfile profile = new JavaScriptProfile();
    impl.parseRawEvent(rawEvent, profile);
    JavaScriptProfileNode bottomUpProfile = profile.getBottomUpProfile();
    assertNotNull(bottomUpProfile);
    assertEquals(5.0, profile.getTotalTime(), .001);

    List<JavaScriptProfileNode> children = bottomUpProfile.getChildren();
    assertEquals(children.size(), 1);
    JavaScriptProfileNode child = children.get(0);
    assertEquals(child.getSymbolName(), "parentNode");
    assertEquals(5.0, child.getTime(), .001);
    assertEquals(5.0, child.getSelfTime(), .001);
  }

  public void testTickSimple() {
    JavaScriptProfileModelV8Impl impl = new JavaScriptProfileModelV8Impl();
    String logRecs = "code-creation,LoadIC,5910913e,179,\"parentNode\"\n"
        + "tick,5910913e,+1,0\n";
    JavaScriptProfileEvent rawEvent = makeV8ProfileEvent(logRecs);
    JavaScriptProfile profile = new JavaScriptProfile();
    impl.parseRawEvent(rawEvent, profile);
    JavaScriptProfileNode bottomUpProfile = profile.getBottomUpProfile();
    assertNotNull(bottomUpProfile);
    assertEquals(1.0, profile.getTotalTime(), .001);
    List<JavaScriptProfileNode> children = bottomUpProfile.getChildren();
    assertEquals(children.size(), 1);
    JavaScriptProfileNode child = children.get(0);
    assertEquals(child.getSymbolName(), "parentNode");
    assertEquals(1.0, child.getTime(), .001);
    assertEquals(1.0, child.getSelfTime(), .001);
  }

  @Override
  protected void gwtSetUp() throws Exception {
    Logging.createListenerLogger(null);
  }
}
