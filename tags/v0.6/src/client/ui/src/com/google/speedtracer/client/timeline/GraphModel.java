/*
 * Copyright 2008 Google Inc.
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

import com.google.speedtracer.client.MonitorConstants;

import java.util.ArrayList;
import java.util.List;

/**
 * The GraphModel contains the data, and meta data for each TimeLineGraph.
 * 
 */
public abstract class GraphModel {

  /**
   * A GraphModel that is optimized to work with an underlying {@link ModelData}
   * the has data points inserted at regular intervals. It does a heuristic
   * lookup for the closest index that should find it in near constant time.
   */
  public static class RegularGraphModel extends GraphModel {
    /**
     * The size of the regular interval between data points.
     */
    private final double intervalSize;

    protected RegularGraphModel(ModelData data, String xLabel, String xUnit,
        String yAxisLabel, String yUnit, double intervalSize) {
      super(data, xLabel, xUnit, yAxisLabel, yUnit);
      this.intervalSize = intervalSize;
    }

    /**
     * This implementation uses a heuristic to guess the correct index in a
     * regularly spaces data set. The guess will either be exactly at the index
     * value (if the domain values match exactly) or one index below
     * corresponding to the closest index without going over the domainVal.
     */
    @Override
    protected int findClosestIndex(double domainVal) {
      int indexRange = data.size() - 1;
      // Clamp to within the bounds.
      if (domainVal < getMinX()) {
        return -1;
      }

      double divisor = getMaxX() - getMinX();
      // fraction of total domain * index range
      int guess = (int) (((domainVal - getMinX()) / ((divisor == 0) ? 1
          : divisor)) * (double) indexRange);

      // Check for corner cases. We have the simplifying assumption
      // that if you request a value outside the total data bounds,
      // then we return 0;
      if (guess < 0) {
        return 0;
      }

      if (guess >= indexRange) {
        return indexRange;
      }

      double currDomainVal = data.get(guess).getX();

      // linear search to find most suitable spot for x index
      // If our heuristic was close
      if (currDomainVal < domainVal) {
        while (currDomainVal < domainVal && guess < indexRange) {
          currDomainVal = data.get(guess + 1).getX();
          guess++;
        }
        // We want to get the the closest domain value
        // If we start
        if (currDomainVal > domainVal) {
          guess--;
        }
      } else {
        while (currDomainVal > domainVal && guess > 0) {
          currDomainVal = data.get(guess - 1).getX();
          guess--;
        }
      }

      return guess;
    }

    /**
     * Returns the average between two indices.
     */
    @Override
    protected double interpolateRangeValue(int closestIndex, double domainVal,
        double sampleRange) {
      // If we are below the sampleRange, then punt to the
      // default implementation.
      if (sampleRange <= intervalSize) {
        return averageClosestIndices(closestIndex, domainVal, sampleRange);
      } else {
        return findMaxValueInRange(closestIndex, domainVal, sampleRange);
      }
    }
  }

  /**
   * A GraphModel that is optimized to work with a sparsely populated underlying
   * {@link ModelData}. It does an iterative binary search to find the closest
   * index.
   */
  public static class SparseGraphModel extends GraphModel {
    protected SparseGraphModel(ModelData data, String xLabel, String xUnit,
        String yAxisLabel, String yUnit) {
      super(data, xLabel, xUnit, yAxisLabel, yUnit);
    }

    @Override
    protected int findClosestIndex(double domainVal) {
      int result = -1;
      // Perform a binary search.
      int lower = 0;
      int upper = data.size() - 1;

      if (upper < 0) {
        return -1;
      }

      while (upper != lower) {
        int mid = Math.max(1, (upper - lower) / 2);
        int index = lower + mid;

        double found = data.get(index).getX();
        if (found > domainVal) {
          // move the pivot down
          upper = upper - mid;
        } else if (found < domainVal) {
          // move the pivot up
          lower = index;
        } else {
          // found == xValue
          lower = index;
          break;
        }
      }

      if (data.get(lower).getX() <= domainVal) {
        result = lower;
      }

      return result;
    }

    @Override
    protected double interpolateRangeValue(int closestIndex, double domainVal,
        double sampleRange) {
      return findMaxValueInRange(closestIndex, domainVal, sampleRange);
    }
  }

  /**
   * Static factory method. This creates a GraphModel instance of the correct
   * type depending on whether or not we want a sparse or regularly spaced
   * underlying Model.
   * 
   * @param data the underlying ModelData datastructure
   * @param xLabel the X Axis Label
   * @param xUnit the X Axis Unit
   * @param yAxisLabel the Y Axis Label
   * @param yUnit the Y Axis Unit
   * @param isSparse whether or not we want a sparse Model
   * @return the constructed GraphModel
   */
  public static GraphModel createGraphModel(ModelData data, String xLabel,
      String xUnit, String yAxisLabel, String yUnit, boolean isSparse) {
    if (isSparse) {
      return new SparseGraphModel(data, xLabel, xUnit, yAxisLabel, yUnit);
    } else {
      return new RegularGraphModel(data, xLabel, xUnit, yAxisLabel, yUnit,
          MonitorConstants.MIN_GRAPH_DATA_RESOLUTION);
    }
  }

  protected final ModelData data;
  private List<DomainObserver> domainObservers = new ArrayList<DomainObserver>();
  // this is ground zero for our data set
  private double minX;
  private final String xAxisLabel;
  private final String xAxisUnit;
  private final String yAxisLabel;

  private final String yAxisUnit;

  protected GraphModel(ModelData myData, String xLabel, String xUnit,
      String yLabel, String yUnit) {
    data = myData;
    xAxisLabel = xLabel;
    yAxisLabel = yLabel;
    xAxisUnit = xUnit;
    yAxisUnit = yUnit;
    // defaults to data[0].x
    int dataSize = myData.size();
    if (dataSize > 0) {
      minX = myData.get(0).getX();
    } else {
      // We are dealing with time.
      // Our initial data set is empty.
      minX = Double.MAX_VALUE;
    }
  }

  /**
   * Convenience Method. Appends data.
   * 
   * @param x
   * @param y
   */
  public void addData(double x, double y) {
    if (x < minX) {
      minX = x;
    }

    data.add(DataPoint.createDataPoint(x, y));

    for (int i = 0, n = domainObservers.size(); i < n; i++) {
      domainObservers.get(i).onDomainChange(x);
    }
  }

  public void addDomainObserver(DomainObserver domainObserver) {
    this.domainObservers.add(domainObserver);
  }

  /**
   * Erases all data and returns this GraphModel to the state it would be after
   * inoking the default Contructor.
   */
  public void clear() {
    data.clear();
    minX = Double.MAX_VALUE;
  }

  public ModelData getData() {
    return data;
  }

  public double getMaxEncounteredValue() {
    return data.getMaxEncounteredValue();
  }

  public double getMaxX() {
    if (data.size() == 0) {
      return getMinX();
    } else {
      return data.get(data.size() - 1).getX();
    }
  }

  public double getMinX() {
    return minX;
  }

  /**
   * Convenience method when we do not want to query with a sample range.
   * 
   * @param domainVal the X value which we want to look up
   * @return the Y value for the specified domain value
   */
  public double getRangeValue(double domainVal) {
    return getRangeValue(domainVal, 0);
  }

  /**
   * For a given X value, we must derive a Y Value to return. This function
   * simulates a continuous data set even though we have an underlying discrete
   * representation.
   * 
   * @param domainVal the X value which we want to look up
   * @param sampleRange the sampling precision which may be used for
   *          interpolating the returng Range value.
   * @return the Y value for the specified domain value
   */
  public double getRangeValue(double domainVal, double sampleRange) {
    int index = findClosestIndex(domainVal);
    if (index < 0) {
      return 0;
    }
    return interpolateRangeValue(index, domainVal, sampleRange);
  }

  public String getXAxisLabel() {
    return xAxisLabel;
  }

  public String getXAxisUnit() {
    return xAxisUnit;
  }

  public String getXAxisUnits() {
    return xAxisUnit;
  }

  public String getYAxisLabel() {
    return yAxisLabel;
  }

  public String getYAxisUnit() {
    return yAxisUnit;
  }

  public boolean isEmpty() {
    return (data.size() == 0) ? true : false;
  }

  public void removeDomainObserver(DomainObserver domainObserver) {
    this.domainObservers.remove(domainObserver);
  }

  /**
   * Just returns the average of the points surrounding our closestIndex.
   * 
   * @param closestIndex the index that is closest to our target domain values
   * @param targetDomainValue the domain value we are querying
   * @param sampleRange the range that we want to interpolate over
   * @return
   */
  protected final double averageClosestIndices(int closestIndex,
      double targetDomainValue, double sampleRange) {
    int endIndex;
    if (data.get(closestIndex).getX() > targetDomainValue) {
      endIndex = closestIndex - 1;
    } else {
      endIndex = closestIndex + 1;
    }

    if (endIndex < 0 || endIndex >= data.size()) {
      return data.get(closestIndex).getY();
    }

    double x0 = data.get(closestIndex).getX();
    double y0 = data.get(closestIndex).getY();

    double x1 = data.get(endIndex).getX();
    double y1 = data.get(endIndex).getY();

    double divisor = (x1 - x0);

    double fraction = Math.abs((targetDomainValue - x0)
        / ((divisor == 0) ? 1 : divisor));

    return ((y1 - y0) * fraction) + y0;
  }

  /**
   * Returns a closest match index in the underlying data set.
   * 
   * @param domainVal the target domain value
   * @return the index in the ModelData that closest matches the query domain
   *         value
   */
  protected abstract int findClosestIndex(double domainVal);

  /**
   * Finds the max value in the data set for a given range.
   * 
   * @param closestIndex the index that is closest to our target domain value,
   *          but whose X value is strictly not greater than out target
   * @param targetDomainValue the domain value we are querying
   * @param sampleRange the range that we want to interpolate over
   * @return
   */
  protected final double findMaxValueInRange(int closestIndex,
      double targetDomainValue, double sampleRange) {
    double endDomainVal = targetDomainValue + sampleRange - .0001;

    if (closestIndex < 0) {
      closestIndex = 0;
    }
    int index = closestIndex;
    // We round the first value <= the domainVal. Now, walk the data searching
    // for the datapoint with the largest Y value.
    DataPoint found;
    DataPoint max = null;
    while (index < data.size()
        && (found = data.get(index)).getX() <= endDomainVal) {
      if (found.getX() >= targetDomainValue) {
        if (max == null || found.getY() > max.getY()) {
          max = found;
        }
      }
      index++;
    }

    // If no values were found within the range, return the point originally
    // returned.
    if (max == null) {
      max = data.get(closestIndex);
    }
    return max.getY();
  }

  /**
   * Overrideable interpolation function. Allows concrete classes to specify how
   * to derive a range value given a set of computed mappings into the
   * underlying data structure.
   * 
   * @param closestIndex the index that is closest to our target domain values
   * @param targetDomainValue the domain value we are querying
   * @param sampleRange the range that we want to interpolate over
   * @return
   */
  protected abstract double interpolateRangeValue(int closestIndex,
      double targetDomainValue, double sampleRange);
}
