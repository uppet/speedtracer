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
package com.google.speedtracer.client.util;

/**
 * Utility for formatting <code>double</code> based timestamps.
 * 
 * Not trying to be a full feature complete Time formatter. Just a utility class
 * for common formatting.
 */
public class TimeStampFormatter {

  /**
   * The standard (and default) way to display units for millisecond times.
   */
  public static final String UNIT_MILLISECONDS = "ms";

  /**
   * The standard (and default) way to display units for second times.
   * 
   */
  public static final String UNIT_SECONDS = "s";

  /**
   * Format a time returning milliseconds for times less than 2 seconds and
   * seconds to two decimal places for times greater than 2 seconds.
   * 
   * @param time the time stamp
   * @return a string representation of the time, with units
   */
  public static String format(double time) {
    return format(time, 2, 2000);
  }

  /**
   * Format a time returning milliseconds for times less than 2 seconds and
   * seconds to <code>decimalPlacesForSeconds</code> decimal places for times
   * greater than 2 seconds.
   * 
   * @param time the time stamp
   * @param decimalPlacesForSeconds the number of decimal places for seconds
   *          formatted times
   * @return a string representation of the time, with units
   */
  public static String format(double time, int decimalPlacesForSeconds) {
    return format(time, decimalPlacesForSeconds, 2000);
  }

  /**
   * Format a time returning milliseconds for times less than
   * <code>maxMillisecons</code> seconds and seconds to
   * <code>decimalPlacesForSeconds</code> decimal places for times greater than
   * <code>maxMilliseconds</code> seconds.
   * 
   * @param time the time stamp
   * @param decimalPlacesForSeconds the number of decimal places for seconds
   *          formatted times
   * @param maxMilliseconds the maximum time which will be formatted as
   *          milliseconds
   * @return a string representation of the time, with units
   */
  public static String format(double time, int decimalPlacesForSeconds,
      double maxMilliseconds) {
    return (time < maxMilliseconds) ? formatMilliseconds(time) : formatSeconds(
        time, decimalPlacesForSeconds);
  }

  /**
   * Formats time as milliseconds with units.
   * 
   * @param time the time stamp
   * @return a string representation of the time
   */
  public static String formatMilliseconds(double time) {
    return (int) time + UNIT_MILLISECONDS;
  }

  /**
   * Formats time as milliseconds with units.
   * 
   * @param time the time stamp
   * @param decimalPlaces the number of decimal places you want to have.
   * @return a string representation of the time
   */
  public static String formatMilliseconds(double time, int decimalPlaces) {
    return formatToFixedDecimalPoint(time, decimalPlaces) + UNIT_MILLISECONDS;
  }

  /**
   * Formats time as seconds.
   * 
   * @param time the time stamp
   * @param decimalPlaces the number of decimal places to display
   * @return a string representation of second time
   */
  public static String formatSeconds(double time, int decimalPlaces) {
    return formatSeconds(time, decimalPlaces, UNIT_SECONDS);
  }

  /**
   * Formats time as seconds.
   * 
   * @param time the time stamp
   * @param decimalPlaces the number of decimal places to display
   * @param units the string to use as units (i.e. " sec")
   * @return a string representation of second time
   */
  public static String formatSeconds(double time, int decimalPlaces,
      String units) {
    return formatToFixedDecimalPoint(time / 1000.0, decimalPlaces) + units;
  }

  /**
   * Formats a <code>double</code> to a fixed number of decimal places.
   * 
   * @param value the value to be formatted
   * @param decimalPlaces the number of decimal places to show
   * @return a string representation of the value
   */
  public static native String formatToFixedDecimalPoint(double value,
      int decimalPlaces) /*-{
    return Number(value).toFixed(decimalPlaces);
  }-*/;
}
