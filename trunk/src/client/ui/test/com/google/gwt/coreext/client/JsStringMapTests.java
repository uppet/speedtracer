/*
 * + * Copyright 2010 Google Inc.
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
package com.google.gwt.coreext.client;

import com.google.gwt.core.client.JsArrayBoolean;
import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.junit.client.GWTTestCase;

import java.util.HashMap;
import java.util.Map;

/**
 * Tests {@link JsStringMap} and {@link JsStringBooleanMap}.
 */
public class JsStringMapTests extends GWTTestCase {

  private static void assertContainsSet(JsArrayString values, String... expects) {
    final HashMap<String, Boolean> keys = new HashMap<String, Boolean>();
    for (int i = 0, n = expects.length; i < n; ++i) {
      keys.put(expects[i], Boolean.FALSE);
    }

    for (int i = 0, n = values.length(); i < n; ++i) {
      final String item = values.get(i);
      assertTrue(keys.containsKey(item));
      assertFalse(keys.get(item).booleanValue());
      keys.put(values.get(i), Boolean.TRUE);
    }

    for (Map.Entry<String, Boolean> entry : keys.entrySet()) {
      assertTrue(entry.getValue().booleanValue());
    }
  }

  private static <T> void assertContainsValues(JSOArray<T> values, T... expects) {
    assertEquals(expects.length, values.size());

    final Map<T, Integer> countByValue = new HashMap<T, Integer>();

    for (int i = 0, n = expects.length; i < n; ++i) {
      final T value = expects[i];
      if (countByValue.containsKey(value)) {
        countByValue.put(value, countByValue.get(value) + 1);
      } else {
        countByValue.put(value, 1);
      }
    }

    for (int i = 0, n = values.size(); i < n; ++i) {
      final T value = values.get(i);
      assertTrue(countByValue.containsKey(value));
      countByValue.put(value, countByValue.get(value) - 1);
    }

    for (Map.Entry<T, Integer> entry : countByValue.entrySet()) {
      assertEquals(0, entry.getValue().intValue());
    }
  }

  private static void assertContainsValues(JsArrayBoolean values,
      boolean... expects) {
    assertEquals(expects.length, values.length());

    int numberOfFalseExpected = 0;
    int numberOfTrueExpected = 0;
    for (int i = 0, n = expects.length; i < n; ++i) {
      if (expects[i]) {
        numberOfTrueExpected++;
      } else {
        numberOfFalseExpected++;
      }
    }

    int numberOfFalseFound = 0;
    int numberOfTrueFound = 0;
    for (int i = 0, n = values.length(); i < n; ++i) {
      if (values.get(i)) {
        numberOfTrueFound++;
      } else {
        numberOfFalseFound++;
      }
    }

    assertEquals(numberOfTrueExpected, numberOfTrueFound);
    assertEquals(numberOfFalseExpected, numberOfFalseFound);
  }

  @Override
  public String getModuleName() {
    return "com.google.gwt.coreext.CoreExt";
  }

  /**
   * Tests {@link JsStringBooleanMap}.
   */
  public void testBooleanMap() {
    final JsStringBooleanMap map = JsStringBooleanMap.create();

    map.put("larry", true);
    map.put("moe", false);
    map.put("curly", true);
    map.put("divine", false);

    assertTrue(map.get("larry"));
    assertFalse(map.get("moe"));
    assertTrue(map.get("curly"));
    assertFalse(map.get("divine"));

    assertTrue(map.hasKey("larry"));
    assertTrue(map.hasKey("moe"));
    assertTrue(map.hasKey("curly"));
    assertTrue(map.hasKey("divine"));
    assertFalse(map.hasKey("smarty"));

    assertContainsValues(map.getValues(), true, false, true, false);
    assertContainsSet(map.getKeys(), "larry", "moe", "curly", "divine");

    map.erase("moe");
    assertFalse(map.hasKey("moe"));
    assertContainsSet(map.getKeys(), "larry", "curly", "divine");
  }

  /**
   * Tests {@link JsStringMap}.
   */
  public void testObjectMap() {
    final JsStringMap<Object> map = JsStringMap.create();

    final Object a = new Object();
    final Object b = new Object();

    map.put("a", a);
    map.put("b", b);

    assertSame(a, map.get("a"));
    assertSame(b, map.get("b"));

    assertTrue(map.hasKey("a"));
    assertTrue(map.hasKey("b"));
    assertFalse(map.hasKey("c"));

    assertContainsValues(map.getValues(), a, b);
    assertContainsSet(map.getKeys(), "a", "b");

    map.erase("a");
    assertFalse(map.hasKey("a"));
    assertContainsSet(map.getKeys(), "b");
  }
}
