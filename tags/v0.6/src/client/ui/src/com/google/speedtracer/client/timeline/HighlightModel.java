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
package com.google.speedtracer.client.timeline;

import java.util.Iterator;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Map.Entry;

/**
 * The HighlightModel contains points on the graph to be highlighted.
 */
public class HighlightModel {

  /**
   * Represents an element in the model to external callers. The data is not
   * actually stored in this format.
   */
  public static class HighlightEntry implements Entry<Double, Integer> {
    private Double xValue;
    private Integer yValue;

    public HighlightEntry(Double x, Integer y) {
      this.xValue = x;
      this.yValue = y;
    }

    public Double getKey() {
      return new Double(xValue);
    }

    public Integer getValue() {
      return yValue;
    }

    public Integer setValue(Integer value) {
      yValue = value;
      return yValue;
    }
  }

  /**
   * This iterator finds the highest value for each specified delta.
   */
  private class HighlightIterator implements Iterator<HighlightEntry> {
    private final double delta;
    private Entry<Double, Integer> next;
    private final Iterator<Entry<Double, Integer>> subMapIterator;

    public HighlightIterator(double start, double end, double delta) {
      SortedMap<Double, Integer> subMap = values.subMap(start, end);
      if (subMap.size() > 0) {
        subMapIterator = subMap.entrySet().iterator();
        next = subMapIterator.hasNext() ? subMapIterator.next() : null;
      } else {
        subMapIterator = null;
      }
      this.delta = delta;
    }

    public boolean hasNext() {
      return (subMapIterator != null && subMapIterator.hasNext())
          || next != null;
    }

    /**
     * Looks through the values in the model, and returns the highest value
     * encountered over the delta starting at the next data point.
     */
    public HighlightEntry next() {
      double firstX = next.getKey();
      int maxValue = 0;
      while (next != null && next.getKey() - firstX < delta) {
        maxValue = Math.max(maxValue, next.getValue());
        if (!subMapIterator.hasNext()) {
          next = null;
          break;
        }
        next = subMapIterator.next();
      }
      return new HighlightEntry(firstX, maxValue);
    }

    public void remove() {
      throw new RuntimeException("remove() not implemented.");
    }
  }

  public static final Integer HIGHLIGHT_CRITICAL = Integer.valueOf(3);
  public static final Integer HIGHLIGHT_INFO = Integer.valueOf(1);
  public static final Integer HIGHLIGHT_NONE = Integer.valueOf(0);
  public static final Integer HIGHLIGHT_WARNING = Integer.valueOf(2);

  /**
   * Factory method.
   */
  public static HighlightModel create() {
    return new HighlightModel();
  }

  TreeMap<Double, Integer> values = new TreeMap<Double, Integer>();

  protected HighlightModel() {
  }

  /**
   * Add a data point to the model. If the same data point is added more than
   * once, the largest value is saved.
   * 
   * @param x X Position
   * @param value One of the HighlightModel.HIGHLIGHT_XXX values.
   */
  public void addData(double x, int value) {
    // Save just the max value if there is a collision.
    Integer result = values.get(Double.valueOf(x));
    if (result != null) {
      values.put(x, Math.max(value, result));
    } else {
      values.put(x, value);
    }
  }

  /**
   * Removes all data from the model.
   */
  public void clear() {
    values.clear();
  }

  /**
   * Returns the maximum x value in the model.
   * 
   * @return the maximum x value in the model.
   * @throws java.util.NoSuchElementException if the model is empty.
   */
  public double getMaxX() {
    return values.lastKey();
  };

  /**
   * Returns an iterator that returns aggregated values based on the delta
   * (finds the max value within each delta).
   * 
   * @param start start x value
   * @param end end x value
   * @param delta distance to aggregate on each result return.
   */
  public Iterator<HighlightEntry> getRangeValues(final double start,
      final double end, final double delta) {
    return new HighlightIterator(start, end, delta);
  }

  /**
   * This value tells you the number of unique points added with the add()
   * method, but does not necessarily tell you the number of values returned
   * from the getRangeValues() iterator, which performs some aggregation.
   * 
   * @return the number of points added with the add() method.
   */
  public int size() {
    return values.size();
  }
}
