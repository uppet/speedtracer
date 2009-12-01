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
package com.google.speedtracer.tools;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

/**
 * TODO:
 */
public class GenerateMockModelsFromEventLogs {

  private static class CodeGenerator {
    private static void emitEventRecords(String dataName, List<String> events,
        StringBuffer buffer) {
      buffer.append("private static final String[] ");
      buffer.append(dataName);
      buffer.append(" = {\n");

      for (int i = 0; i < events.size(); i++) {
        buffer.append("\"");
        buffer.append(events.get(i));
        buffer.append("\",\n");
      }
      buffer.append("};\n");
    }

    private static void emitSimulateMethod(String methodName,
        List<String> events, StringBuffer buffer) {
      String dataSetName = getDataSetName(methodName);
      emitEventRecords(dataSetName, events, buffer);
      buffer.append("public static void ");
      buffer.append(methodName);
      buffer.append("(MockDataModel mockModel) {\n");
      buffer.append("Generator generator = new Generator(mockModel);\n");
      buffer.append("generator.run(");
      buffer.append(dataSetName);
      buffer.append(");");
      buffer.append("\n}\n");
    }

    private static void generate(String methodName, List<String> events,
        StringBuffer buffer) {
      emitSimulateMethod(methodName, events, buffer);
    }

    private static String getDataSetName(String methodName) {
      return methodName + "Data";
    }
  }

  private static class Template {
    private static final String BEGIN_MARKER = "/* BEGIN GENERATED CODE - DO NOT EDIT */";
    private static final String END_MARKER = "/* END GENERATED CODE - DO NOT EDIT */";
    private final File fileToReplace;
    private String headContent, tailContent;

    Template(File file) throws Exception {
      fileToReplace = file;
      parse(file);
    }

    private void parse(File file) throws Exception {
      final String contents = getFileContents(file);
      final int bx = contents.indexOf(BEGIN_MARKER);
      if (bx < 0) {
        throw new Exception("Template (" + file + ") is missing marker \""
            + BEGIN_MARKER + "\"");
      }
      final int ex = contents.lastIndexOf(END_MARKER);
      if (ex < 0) {
        throw new Exception("Template (" + file + ") is missing marker \""
            + END_MARKER + "\"");
      }

      headContent = contents.substring(0, bx);
      tailContent = contents.substring(ex + END_MARKER.length());
    }

    void merge(StringBuffer buffer) {
      buffer.insert(0, "\n");
      buffer.insert(0, BEGIN_MARKER);
      buffer.insert(0, headContent);
      buffer.append(END_MARKER);
      buffer.append("\n");
      buffer.append(tailContent);
    }

    void mergeAndWrite(StringBuffer buffer) throws IOException {
      merge(buffer);
      final OutputStream stream = new FileOutputStream(fileToReplace);
      try {
        stream.write(buffer.toString().getBytes());
      } finally {
        stream.close();
      }
    }
  }

  private static final String[] DATA_FILES = new String[] {
      "data/digg.com"};

  public static void main(String[] args) {
    if (args.length != 1) {
      System.err.println("usage: "
          + GenerateMockModelsFromEventLogs.class.getSimpleName()
          + " output_file");
      System.exit(-1);
    }

    final File templateFile = new File(args[0]);
    try {
      final Template template = new Template(templateFile);
      final StringBuffer buffer = new StringBuffer();
      for (int i = 0, n = DATA_FILES.length; i < n; ++i) {
        final InputStream stream = GenerateMockModelsFromEventLogs.class.getResourceAsStream(DATA_FILES[i]);
        if (stream == null) {
          System.err.println("Unable to load data file: (" + DATA_FILES[i]
              + ")");
          System.exit(-1);
        }

        try {
          CodeGenerator.generate(
              createMethodNameFromFilename(DATA_FILES[i]),
              new GenerateMockModelsFromEventLogs().parse(new InputStreamReader(
                  stream)), buffer);
        } catch (IOException e) {
          System.err.println("Unable to load data file: (" + DATA_FILES[i]
              + ")");
          System.exit(-1);
        }
      }
      template.mergeAndWrite(buffer);
      System.out.println("Wrote file: " + templateFile.getAbsolutePath());
      System.out.println("You will probably have to refesh eclipse to see the changes.");
    } catch (Exception e) {
      System.err.println(e.getMessage());
      System.exit(-1);
    }
  }

  private static String createMethodNameFromFilename(String filename) {
    final String name = new File(filename).getName();
    final StringBuffer buffer = new StringBuffer("simulate");
    boolean upperCaseNextChar = true;
    for (int i = 0, n = name.length(); i < n; ++i) {
      final char c = name.charAt(i);
      if (c == '.') {
        buffer.append("Dot");
        upperCaseNextChar = true;
      } else if (upperCaseNextChar) {
        buffer.append(Character.toUpperCase(name.charAt(i)));
        upperCaseNextChar = false;
      } else {
        buffer.append(name.charAt(i));
      }
    }
    return buffer.toString();
  }

  private static String getFileContents(File file) throws IOException {
    final int length = (int) file.length();
    final byte[] buffer = new byte[length];
    final DataInputStream stream = new DataInputStream(
        new FileInputStream(file));
    try {
      stream.readFully(buffer);
      return new String(buffer);
    } finally {
      stream.close();
    }
  }

  public GenerateMockModelsFromEventLogs() {
  }

  public List<String> parse(Reader reader) throws IOException {
    final List<String> events = new ArrayList<String>();
    BufferedReader br = new BufferedReader(reader);
    int openParens = 0;
    StringBuffer eventRecord = new StringBuffer();
    int c;
    while ((c = br.read()) > 0) {
      eventRecord.append((char) c);
      if (c == '{') {
        openParens++;
      }

      if (c == '}') {
        openParens--;
        if (openParens == 0) {
          // Remove newLines, and replace double quotes with single quotes.
          // Also, if we have escaped quotes, get rid of them.
          
          String eventString = eventRecord.toString();
          // remove escaped quotes
          eventString = eventString.replace("\\\"", "");
          // remove new lines
          eventString = eventString.replaceAll("\n", "");
          eventString = eventString.replaceAll("\r", "");
          // set all double quotes to escaped double quotes
          eventString = eventString.replace("\"", "\\\"");
          // remove all white space
          eventString = eventString.replaceAll(" ", "");

          events.add(eventString);
          eventRecord = new StringBuffer();
        }
      }
    }
    return events;
  }
}
