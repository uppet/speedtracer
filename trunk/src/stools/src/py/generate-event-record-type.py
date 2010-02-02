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
events = [

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
    'help_text' : 'A new request for a network resource completed.'
  },
# END INSPECTOR TIMELINE RECORDS


# THE FOLLOWING HAS NOT YET LANDED.
  {
    'value' : 15,
    'constant' : 'PROFILE_DATA', 
    'desc' : 'JavaScript CPU profile data',
    'help_text' : 'Contains raw data from the JavaScript engine profiler.',
  },
# END NOT LANDED


# USED INTERNALLY TO TRACK PAGE TRANSITIONS. SPEED TRACER GENERATED.
  {
    'value' : 16,
    'constant' : 'TAB_CHANGED',
    'desc' : 'Tab Changed',
    'help_text' : 'Something about the Tab where the page viewed changed.  '
                  + 'Usually this is the title string or the location of '
                  + 'the page.',
  },
# END PAGE TRANSITIONS


# THE FOLLOWING HAS NOT YET LANDED
  {
    'value' : 17,
    'constant' : 'AGGREGATED_EVENTS',
    'desc' : 'AGGREGATED Events',
    'help_text' : 'This event represents many short events that have been '
                  + 'aggregated to help reduce the total amount of data '
                  + 'displayed.',
  },
  { 
    'value' : 18,
    'constant' : 'DOM_BINDING_EVENT', 
    'desc' : 'Dom Bindings',
    'help_text' : 'A DOM property or function was accessed, such as '
                  + 'HTMLElement.insertBefore()',
  },

  { 
    'value' : 19,
    'constant' : 'JAVASCRIPT_COMPILE_EVENT', 
    'desc' : 'JavaScript Compile',
    'help_text' : 'A block of JavaScript was parsed and compiled by the '
                  + 'JavaScript interpreter.',
  },
  { 
    'value' : 20,
    'constant' : 'WINDOW_EVENT', 
    'desc' : 'Window',
    'help_text' : 'A top level Window event fired, such as load or '
                  + 'unload.',
  },
# END NOT LANDED


# THE FOLLOWING ARE SPEED TRACER TIMELINE RECORDS FOR TRACKING NETWORK
# RESOURCES.
  {
    'value' : 21,
    'constant' : 'NETWORK_RESOURCE_ERROR',
    'desc' : 'Network Resource Error',
    'help_text' : 'A network resource load ended in error.',
  },
  {
    'value' : 22,
    'constant' : 'NETWORK_RESOURCE_FINISH',
    'desc' : 'Network Resource Finished',
    'help_text' : 'A network resource loaded sucessfully.',
  },
  {
    'value' : 23, 
    'constant' : 'NETWORK_RESOURCE_RESPONSE',
    'desc' : 'Network Resource Response',
    'help_text' : 'A network resource load began to recieve data from the server.',
  },
  { 
    'value' : 24, 
    'constant' : 'NETWORK_RESOURCE_START',
    'desc' : 'Network Resource Start',
    'help_text' : 'A new request for a network resource started.'
  },
# END SPEEDTRACER RECORDS

 
# THE FOLLOWING HAS NOT YET LANDED
  {
    'value' : 25,
    'constant' : 'GARBAGE_COLLECT_EVENT',
    'desc' : 'Garbage Collection',
    'help_text' : 'The JavaScript engine ran garbage collection.',
  },
  {
    'value' : 26,
    'constant' : 'MOUSE_HOVER_STYLE_EVENT',
    'desc' : 'Mouse Hover for Style',
    'help_text' : 'The UI changed the currently displayed style for an '
                  + 'Element based on a CSS hover rule.',
  },
  {
    'value' : 27,
    'constant' : 'DOM_EVENT_DISPATCH', 
    'desc' : 'Dom Dispatch',
    'help_text' : 'A DOM event dispatch ran.  The event may be in the capture or bubble phase.',
  },
# END NOT LANDED.

]

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
""")
  # Dump the integer constants
  max_key_value = 0
  for event in events:
    javaSrc.write("  public static final int " + event['constant'] 
      + " = " + str(event['value']) + ";\n")
    if (event['value'] > max_key_value):
      max_key_value = event['value']
  javaSrc.write("\n")
  javaSrc.write("  // The highest value key represented by the constants above\n")
  javaSrc.write("  public static final int MAX_KEY = " + str(max_key_value) + ";\n")
  javaSrc.write("\n")
  javaSrc.write("  private static final String[] typeStrings = {\n")
  count = 0
  for event in events:
    javaSrc.write('    "' + event['desc'] + '"')
    commaString = ','
    if (count == max_key_value):
       commaString = " "
    count = count + 1;
    javaSrc.write("%s%*s // %d %s\n" 
      % (commaString, 32-len(event['desc']), " ", event['value'], 
         event['constant']));
  javaSrc.write("""  };

""")
  javaSrc.write("  private static final String[] helpStrings = {\n")
  count = 0
  for event in events:
    javaSrc.write('    // %d %s\n' % (event['value'], event['constant']))
    javaSrc.write('    "' + event['help_text'].replace("\"", "'") + '"')
    commaString = ','
    if (count == max_key_value):
       commaString = " "
    javaSrc.write("%s\n" % (commaString))
    count = count + 1;
  javaSrc.write("""  };

  public static String typeToDetailedTypeString(UiEvent e) {
    switch (e.getType()) {
      case DomEvent.TYPE:
        return "DOM (" + ((DomEvent) e).getDomEventType() + ")";
      case LogEvent.TYPE:
        String logMessage = ((LogEvent) e).getMessage();
        logMessage = (logMessage.length() > 20) ? logMessage.substring(0, 8)
            + "..." + logMessage.substring(12, 20) : logMessage;
        return "Log: " + logMessage;
      case DomEventDispatch.TYPE:
        return "Dom Dispatch (" + ((DomEventDispatch) e).getPhase() + ")";
      case DomBindingEvent.TYPE:
        DomBindingEvent domBindingEvent = e.cast();
        return (domBindingEvent.isGetter() ? "get " : "set ")
            + domBindingEvent.getName();
      case TimerFiredEvent.TYPE:
        TimerFiredEvent timerEvent = e.cast();
        return "Timer Fire (" + timerEvent.getTimerId() + ")";
      default:
        return EventRecordType.typeToString(e.getType());
    }
  }

  public static String typeToHelpString(int type) {
    if (type < 0 || type >= helpStrings.length) {
      return "(Unknown Event Type: " + type + ")";
    }
    return helpStrings[type];  
  }

  public static String typeToString(int type) {
    if (type < 0 || type >= typeStrings.length) {
      return "(Unknown Event Type: " + type + ")";
    }
    return typeStrings[type];
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
  for event in events:
    javaScriptSrc.write("  '" + event['constant'] + "' : " 
      + str(event['value']) + ",\n");
  javaScriptSrc.write("};\n");
  javaScriptSrc.write("""
hintlet.typeList = [
""");
  for event in events:
    javaScriptSrc.write(("  '%s', %*s // %d\n" % 
      (event['constant'], 32-len(event['constant']), " ", event['value'])))
  javaScriptSrc.write("];\n");
  javaScriptSrc.close()
  # end writeJavaScriptSource


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
