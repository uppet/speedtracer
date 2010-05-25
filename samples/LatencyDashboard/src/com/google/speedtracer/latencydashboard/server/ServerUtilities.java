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
package com.google.speedtracer.latencydashboard.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;

/**
 * Utility class for server side code.
 */
public class ServerUtilities {

  /**
   * Read an input stream as UTF8 bytes and converts to a String.
   * 
   * Paraphrased from Apache Commons IOUtils class.
   * 
   * @param input stream to read from.
   * @return string from reader.
   * @throws IOException
   */

  public static String streamToString(InputStream input) throws IOException {
    return streamToString(input, "UTF8");
  }

  /**
   * Read an input stream as a String.
   * 
   * Paraphrased from Apache Commons IOUtils class.
   * 
   * @param input stream to read from.
   * @param encoding the name of the character encoding. For example, "UTF8"
   * @return string from reader.
   * @throws IOException
   */
  public static String streamToString(InputStream input, String encoding)
      throws IOException {
    InputStreamReader reader = new InputStreamReader(input, encoding);
    StringWriter writer = new StringWriter();
    char[] buffer = new char[16 * 1024];
    int result = 0;
    while (-1 != (result = reader.read(buffer))) {
      writer.write(buffer, 0, result);
    }
    return writer.toString();
  }

  private ServerUtilities() {
    // Utility class. Do not instantiate.
  }
}
