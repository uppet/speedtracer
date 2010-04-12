# Copyright 2009 Google Inc.
# 
# Licensed under the Apache License, Version 2.0 (the "License"); you may not
# use this file except in compliance with the License. You may obtain a copy of
# the License at
# 
# http://www.apache.org/licenses/LICENSE-2.0
# 
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
# WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
# License for the specific language governing permissions and limitations under
# the License.

# Creates header files for Java, JavaScript, and C++ for Speed Tracer 
# data records

import os
from optparse import OptionParser

####
# *EDIT* : The following values are used to generate record type 
#          definitions in Java, JavaScript, and C++ code.
#
# Add the events to the list in value order.  The generated code will 
# break if there are more than one entry with the same value.
#
webkitEvents = [

# 0-14 Correspond to record types landed as webkit inspector timeline records.
  {
    'value' : 0,
    'constant' : 'DOM_EVENT', 
    'desc' : 'Dom Event',
    'help_text' : 'A top level DOM event fired, such as mousemove or DOMContentLoaded fired.',
  },
  {
    'value' : 1,
    'constant' : 'LAYOUT_EVENT',
    'desc' : 'Layout',
    'help_text' : 'The browser\'s rendering engine performed layout calculations.',
  },
  { 
    'value' : 2,
    'constant' : 'RECALC_STYLE_EVENT',
    'desc' : 'Style Recalculation',
    'help_text' : 'The renderer recalculated CSS styles.',
  },
  { 
    'value' : 3,
    'constant' : 'PAINT_EVENT',
    'desc' : 'Paint',
    'help_text' : 'The browser\'s rendering engine updated the screen.',
  },
  { 
    'value' : 4,
    'constant' : 'PARSE_HTML_EVENT',
    'desc' : 'Parse HTML',
    'help_text' : 'A block of HTML was parsed.',
  },
  {
    'value' : 5,
    'constant' : 'TIMER_INSTALLED',
    'desc' : 'Timer Installed',
    'help_text' : 'A new JavaScript timer was created.',
  },
  {
    'value' : 6,
    'constant' : 'TIMER_CLEARED',
    'desc' : 'Timer Cleared',
    'help_text' : 'A JavaScript timer was cancelled.',
  },
  { 
    'value' : 7,
    'constant':'TIMER_FIRED',
    'desc':'Timer Fire',
    'help_text' : 'A block of JavaScript was executed due to a JavaScript '
                  + 'timer firing.',
  },
  { 
    'value' : 8,
    'constant' : 'XHR_READY_STATE_CHANGE',
    'desc' : 'XMLHttpRequest',
    'help_text' : 'The handler for an XMLHttpRequest ran.  Check the state '
                  + 'field to see if this is an intermediate state or the '
                  + 'last call for the request.',
  },
  { 
    'value' : 9,
    'constant' : 'XHR_LOAD',
    'desc' : 'XHR Load',
    'help_text' : 'The onload handler for an XMLHttpRequest ran.',
  },
  { 
    'value' : 10,
    'constant' : 'EVAL_SCRIPT_EVENT',
    'desc' : 'Script Evaluation',
    'help_text' : 'A block of JavaScript was parsed/compiled and executed. This only includes script encountered via an HTML <script> tag.',
  },
  {
    'value' : 11,
    'constant' : 'LOG_MESSAGE_EVENT',
    'desc' : 'Log Message',
    'help_text' : 'A log message written using console.markTimeline.',
  },
  {
    'value' : 12,
    'constant' : 'RESOURCE_SEND_REQUEST',
    'desc' : 'Resource Request',
    'help_text' : 'A network request was queued up to send.',
  },
  {
    'value' : 13, 
    'constant' : 'RESOURCE_RECEIVE_RESPONSE',
    'desc' : 'Resource Response',
    'help_text' : 'A network resource load began to recieve data from the server.',
  },
  { 
    'value' : 14, 
    'constant' : 'RESOURCE_FINISH',
    'desc' : 'Resource Finish',
    'help_text' : 'A new request for a network resource completed.',
  },
  { 
    'value' : 15, 
    'constant' : 'JAVASCRIPT_EXECUTION',
    'desc' : 'JavaScript Callback',
    'help_text' : 'JavaScript was run in an event dispatch.',
  },
  { 
    'value' : 16, 
    'constant' : 'RESOURCE_DATA_RECEIVED',
    'desc' : 'Resource Data Received',
    'help_text' : 'Processing a file received by the resource loader.',
  },
  { 
    'value' : 17, 
    'constant' : 'GCEvent',
    'desc' : 'Garbage Collection',
    'help_text' : 'The JavaScript engine ran its garbage collector to reclaim memory.',
  }
]
# END INSPECTOR TIMELINE RECORDS.


# THE FOLLOWING ARE SPEED TRACER SPECIFIC.

# WE COUNT DOWN FROM THE MAX JAVA 32 BIT INTEGER.
maxInt = 2147483647

speedTracerEvents = [
  {
    # NOTE: AGGREGATED_EVENTS is no longer being used but may be added back
    # in the future, so we have kept the type enum for it. However, it is
    # currently ignored in the frontend.
    'value' : maxInt,
    'constant' : 'AGGREGATED_EVENTS',
    'desc' : 'AGGREGATED Events',
    'help_text' : 'This event represents many short events that have been '
                  + 'aggregated to help reduce the total amount of data '
                  + 'displayed.',
  },
  {
    'value' : (maxInt - 1),
    'constant' : 'TAB_CHANGED',
    'desc' : 'Tab Changed',
    'help_text' : 'Something about the Tab where the page viewed changed.  '
                  + 'Usually this is the title string or the location of '
                  + 'the page.',
  },
  {
    'value' : (maxInt - 2),
    'constant' : 'RESOURCE_UPDATED',
    'desc' : 'Resource Updated',
    'help_text' : 'Details about a Network Resource were updated.',
  },
# THE FOLLOWING MIGHT CHANGE.
  {
    'value' : (maxInt - 3),
    'constant' : 'PROFILE_DATA',
    'desc' : 'JavaScript CPU profile data',
    'help_text' : 'Contains raw data from the JavaScript engine profiler.',
  }
# END EXPERIMENTAL TYPES.
]
# END SPEEDTRACER RECORDS


# End of *EDIT*
####

regen_string = """/*
 * DO NOT EDIT - Automatically generated file.  Make modifications
 *   to speedtracer/stools/src/py/generate-event-record-type.py and regenerate
 *   by running speedtracer/stools/generate-event-record-type.
 */
"""

header_string = """/*
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
"""

def writeJavaSource(path):
  """ Creates a file that maps event types to strings and creates a 
      set of constants for use in Java.
  """
  javaSrc = open(path, "w");
  javaSrc.write(header_string + "\n" + 
"""package com.google.speedtracer.client.model;

""" + regen_string + """
/**
 * Primitive integer values for the Types of EventRecords to let us 
 * switch on an int field which faster than an if ladder or string hash.
 */

public class EventRecordType {
  // Webkit Timeline Types
""")
  # Dump the integer constants for webkit
  for event in webkitEvents:
    javaSrc.write("  public static final int " + event['constant'] 
      + " = %d;\n" % event['value'])

  javaSrc.write("\n  // Speed Tracer Types\n")
  for event in speedTracerEvents:
    javaSrc.write("  public static final int " + event['constant'] 
      + " = 0x%08X;\n" % event['value'])

  javaSrc.write("\n")
  javaSrc.write("  private static final String[] webkitTypeStrings = {\n")
  for event in webkitEvents:
    javaSrc.write('    "' + event['desc'] + '"')
    javaSrc.write(",%*s // %d %s\n" 
      % (32-len(event['desc']), " ", event['value'], 
         event['constant']));
  javaSrc.write("""  };

""")
  javaSrc.write("  private static final String[] speedTracerTypeStrings = {\n")
  for event in speedTracerEvents:
    javaSrc.write('    "' + event['desc'] + '"')
    javaSrc.write(",%*s // 0x%08X %s\n" 
      % (32-len(event['desc']), " ", event['value'], 
         event['constant']));
  javaSrc.write("""  };

""")
  javaSrc.write("  private static final String[] webkitHelpStrings = {\n")
  for event in webkitEvents:
    javaSrc.write('    // %d %s\n' % (event['value'], event['constant']))
    javaSrc.write('    "' + event['help_text'].replace("\"", "'") + '"')
    javaSrc.write(",\n")
  javaSrc.write("""  };

""")
  javaSrc.write("  private static final String[] speedTracerHelpStrings = {\n")
  for event in speedTracerEvents:
    javaSrc.write('    // 0x%08X %s\n' % (event['value'], event['constant']))
    javaSrc.write('    "' + event['help_text'].replace("\"", "'") + '"')
    javaSrc.write(",\n")
  javaSrc.write("""  };

  public static String typeToDetailedTypeString(UiEvent e) {
    switch (e.getType()) {
      case DomEvent.TYPE:
        return "DOM (" + ((DomEvent) e).getDomEventType() + ")";
      case LogEvent.TYPE:
        String logMessage = ((LogEvent) e).getMessage();
        int logLength = logMessage.length();
        logMessage = (logLength > 20) ? logMessage.substring(0, 8) + "..."
            + logMessage.substring(logLength - 8, logLength) : logMessage;
        return "Log: " + logMessage;
      case TimerFiredEvent.TYPE:
        TimerFiredEvent timerEvent = e.cast();
        return "Timer Fire (" + timerEvent.getTimerId() + ")";
      default:
        return EventRecordType.typeToString(e.getType());
    }
  }

  public static String typeToHelpString(int type) {
    if (type < 0 || type >= webkitHelpStrings.length) {
      // Normalize to speed tracer range types.
      int speedTracerType = Integer.MAX_VALUE - type;
      if (speedTracerType < 0 || type >= speedTracerHelpStrings.length) {
        return "(Unknown Event Type: " + type + ")";
      }
      
      return speedTracerHelpStrings[speedTracerType];
    }
    return webkitHelpStrings[type];
  }

  public static String typeToString(int type) {
    if (type < 0 || type >= webkitTypeStrings.length) {
      // Normalize to speed tracer range types.
      int speedTracerType = Integer.MAX_VALUE - type;
      if (speedTracerType < 0 || type >= speedTracerTypeStrings.length) {
        return "(Unknown Event Type: " + type + ")";
      }
      
      return speedTracerTypeStrings[speedTracerType];
    }
    return webkitTypeStrings[type];
  }
}
""")
  javaSrc.close()
 # end writeJavaSource


def writeJavaScriptSource(path):
  """ Creates a file that maps event types to strings and creates a 
      set of constants for use in JavaScript.
  """
  javaScriptSrc = open(path, "w")
  javaScriptSrc.write(header_string + "\n" + regen_string);
  javaScriptSrc.write("hintlet.types = {\n");
  for event in webkitEvents:
    javaScriptSrc.write("  '" + event['constant'] + "' : " 
      + str(event['value']) + ",\n");
  for event in speedTracerEvents:
    javaScriptSrc.write("  '" + event['constant'] + "' : " 
      + str(event['value']) + ",\n");
  javaScriptSrc.write("};\n");
  javaScriptSrc.write("""
hintlet.webkitTypeList = [
""");
  for event in webkitEvents:
    javaScriptSrc.write(("  '%s', %*s // %d\n" % 
      (event['constant'], 32-len(event['constant']), " ", event['value'])))
  javaScriptSrc.write("];\n");
  javaScriptSrc.write("""
hintlet.speedTracerTypeList = [
""");
  for event in speedTracerEvents:
    javaScriptSrc.write(("  '%s', %*s // 0x%08X\n" % 
      (event['constant'], 32-len(event['constant']), " ", event['value'])))
  javaScriptSrc.write("];\n");
  javaScriptSrc.close()
  # end writeJavaScriptSource


# TODO(jaimeyap) : Update this when we revive plugin.
def writeCPlusPlusSource(path):
  """ Creates a file that maps event types to strings and creates a 
      set of constants for use in Java.
  """
  cppSrc = open(path, "w")
  cppSrc.write(header_string + "\n" + regen_string)
  cppSrc.write("""
#ifndef SPEEDTRACER_RECORD_TYPE_H__
#define SPEEDTRACER_RECORD_TYPE_H__

namespace speedtracer {

typedef enum {
""")
  for event in events:
    cppSrc.write(("  %s %*s= %d,\n" % 
      (event['constant'], 32-len(event['constant']), " ", event['value'])))
  cppSrc.write("""} EventRecordType;

}  // end namespace speedtracer

#endif // SPEEDTRACER_RECORD_TYPE_H__
""")
  cppSrc.close()
  # end writeCPlusPlusSource


def Main(basedir):
  """The main."""
  java_path=basedir + "/client/ui/src/com/google/speedtracer/client/model/EventRecordType.java";
  # check for the existence of the directory
  if (os.path.isdir(os.path.dirname(java_path)) == False):
    raise Exception("Path to java src: " + os.path.dirname(java_path) 
    + " Does not exist.")
  writeJavaSource(java_path);
  print("Wrote " + java_path);

  javascript_path = basedir + "/client/ui/src/com/google/speedtracer/hintletengine/public/hintlet_record_type.js"
  if (os.path.isdir(os.path.dirname(javascript_path)) == False):
    raise Exception("Path to javascript src: " 
    + os.path.dirname(javascript_path) + " Does not exist.")
  writeJavaScriptSource(javascript_path);
  print("Wrote " + javascript_path);

usage = "usage: %prog [--basedir=<path>]"
parser = OptionParser(usage=usage)
parser.add_option("--basedir", dest="basedir",
                  help="path to root of project tree")
# Default: assumes you are running from the parent of the project directory
parser.set_defaults(basedir="./src");

(options, args) = parser.parse_args()

if __name__ == "__main__":
  Main(options.basedir)
