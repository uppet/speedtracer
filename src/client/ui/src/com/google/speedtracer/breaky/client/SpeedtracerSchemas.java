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
package com.google.speedtracer.breaky.client;

import com.google.gwt.core.client.JavaScriptObject;

/**
 * A simple container class for the Speed Tracer schemas
 *
 *  TODO(conroy): make the schema definitions the source of truth for
 * generate-event-record.py, etc..
 */
public class SpeedtracerSchemas {
  /**
   * Native method to return a JSON Dictionary of all the speedtracer schema
   * types.
   *
   * @return
   */
  public static final native JavaScriptObject getSchemas() /*-{
    return {
      // Base Definitions
     "TIMELINE_EVENT_BASE" : {
       "description" : "Base Browser Timeline Event Schema",
       "id" : "TIMELINE_EVENT_BASE",
       "type" : "object",
       "properties" : {
         "type" : {
           "description" : "Speedtracer Type ID",
           "type" : "integer", 
           "minimum" : 0,
           "maximum" : 20
         },
         "time"     : {
           "description" : "Milliseconds since start of session",
           "type" : "number",
           "minimum" : 0
         },
         "callerScriptName" : {
           "type" : "string",
           "description" : "The URL of the script that triggered this event",
           "requires" : "callerScriptLine",
           "optional" : true
          },
         "callerScriptLine" : {
           "type" : "integer",
           "description" : "The particular line of the calling script",
           "requires" : "callerScriptName",
           "optional" : true
         },
         "callerFunctionName" : {
            "type" : "string",
            "description" : "The name of the calling function in the script",
            "requires" : "callerScriptName",
            "optional" : true
         },
         "stackTrace" : {
            "type" : "array",
            "description" : "Array of JavaScript stack frame objects.",
            "optional" : true,
            "items" : {
              "title" : "Stack frame object",
              "type" : "object",
                "properties" : {
                  "url" : {
                    "type" : "string",
                  },
                  "functionName" : {
                    "type" : "string",
                  },
                  "lineNumber" : {
                    "type" : "integer",
                  },
                  "columnNumber" : {
                    "type" : "integer",
                    "optional" : true,
                  }
              },
             "minItems" : 1,
            }            
         },
         "usedHeapSize" : {
           "type" : "integer",
           "description" : "Size in bytes used in the heap",
           "requires" : "totalHeapSize",
           "optional" : true,
         },
         "totalHeapSize" : {
           "type" : "integer",
           "description" : "Total size in bytes of the heap",
           "requires" : "usedHeapSize",
           "optional" : true
         },
         "sequence" : {
           "description" : "Sequence number of this event",
           "type" : "integer",
           "minimum" : 0,
           "optional" : true
         }
       }
     },
     "TIMELINE_EVENT" : {
       "id" : "TIMELINE_EVENT",
       "description" : "A normal timeline event with duration",
       "type" : "object",
       "extends" : {"$ref" : "TIMELINE_EVENT_BASE"},
       "properties" : {
         "duration" : {
           "description" : "Milliseconds this event took",
           "type" : "number",
           "minimum" : 0
         },
         "children" : {
           "description" : "Child Events.",
           "type" : "array",
           "optional" : true,
         },
         "data" : {
           "description" : "A JSON dictionary of data",
           "type" : "object"
         }
       },
       "additionalProperties" : false
     },
     "TIMELINE_EVENT_MARK" : {
       "id" : "TIMELINE_EVENT_MARK",
       "description" : "A timeline event with no duration",
       "type" : "object",
       "extends" : {"$ref" : "TIMELINE_EVENT_BASE"},
       "properties" : {
         "children" : {
           "description" : "Child Events",
           "type" : "array",
           "optional" : true
         },
         // Apparently, the data field is optional for some MARK events
         "data" : {
           "description" : "A JSON dictionary of data",
           "type" : "object",
           "optional" : true
         }
       },
       "additionalProperties" : false
     },

     // Some Handy Databag references
     "EMPTY_DATA" : {
       "id" : "EMPTY_DATA",
       "description" : "An empty databag!",
       "type" : "object",
       "properties" : {
       },
       "additionalProperties" : false
     },
     "TIMER_DATA" : {
       "id" : "TIMER_DATA",
       "description" : "A timer event databag",
       "type" : "object",
       "properties" : {
         "timerId" : {
           "type" : "integer",
           "description" : "opaque ID identifying the timer"
         },
         "timeout" : {
           "type" : "integer",
           "descrption" : "timeout in milliseconds",
           "optional" : true
         },
         "singleShot" : {
           "type" : "boolean",
           "description" : "one time vs. repeating timer",
           "optional" : true
         }
       },
       "additionalProperties" : false
     },

     // Concrete Events
     "DOM_EVENT" : { 
       "description" : "DOM Event",
       "id" : "DOM_EVENT",
       "type" : "object",
       "extends" : {"$ref" : "TIMELINE_EVENT"},
       "properties" : {
         "type" : {"type" : "integer", "minimum" : 0, "maximum": 0},
         "data" : {
           "type" : "object",
           "properties" : {
             "type" : {
               "type" : "string",
               "description" : "String representation of a DOM-Level-3-Event"
             }
           },
           "additionalProperties" : false
         }
       },
       "additionalProperties" : false
     },
     "LAYOUT" : { 
       "description" : "Layout or reflow of the document",
       "id" : "LAYOUT",
       "type" : "object",
       "extends" : {"$ref" : "TIMELINE_EVENT"},
       "properties" : {
         "type" : {"type" : "integer", "minimum" : 1, "maximum" : 1},
         "data" : {"$ref" : "EMPTY_DATA" }
       },
       "additionalProperties" : false
     }, 
     "RECALC_STYLE" : {
       "description" : "The renderer recalculated CSS styles. Style rules were rematched against the appropriate DOM elements",
       "id" : "PARSE_HTML",
       "type" : "object",
       "extends" : {"$ref" : "TIMELINE_EVENT"},
       "properties" : {
         "type" : {"type" : "integer", "minimum" : 2, "maximum" : 2},
         "data" : {"$ref" : "EMPTY_DATA" }
       },
       "additionalProperties" : false
     },
     "PAINT" : { 
       "description" : "Layout or reflow of the document",
       "id" : "PAINT",
       "type" : "object",
       "extends" : {"$ref" : "TIMELINE_EVENT"},
       "properties" : {
         "type" : {"type" : "integer", "minimum" : 3, "maximum" : 3},
         "data" : {
           "type" : "object",
           "properties" : {
             "x"      : {"type" : "integer", "description" : "x-offset of area painted in pixels"},
             "y"      : {"type" : "integer", "description" : "y-offset of area painted in pixels"},
             "width"  : {"type" : "integer",  "description" : "width of area painted in pixels"},
             "height" : {"type" : "integer",  "description" : "width of area painted in pixels"}
           },
           "additionalProperties" : false
         }
       },
       "additionalProperties" : false
     },
     "PARSE_HTML" : {
       "description" : "The HTML tokenizer processed some of the document",
       "id" : "PARSE_HTML",
       "type" : "object",
       "extends" : {"$ref" : "TIMELINE_EVENT"},
       "properties" : {
         "type" : {"type" : "integer", "minimum" : 4, "maximum" : 4},
         "data" : {
           "type" : "object",
           "properties" : {
             "length" : {"type" : "integer", "description" : "Length in bytes of the parsed section."},
             "startLine" : {"type" : "integer", "description" : "The first line of the parsed section."},
             "endLine" : {"type" : "integer", "descirption" : "The last line of the parsed section."}
           },
           "additionalProperties" : "false"
         }
       },
       "additionalProperties" : false
     },
     "TIMER_INSTALLED" : {
       "description" : "A timer was created through either a call to setTimeout() or setInterval(). This event should always be 0 duration",
       "id" : "TIMER_INSTALLED",
       "type" : "object",
       "extends" : {"$ref" : "TIMELINE_EVENT_MARK"},
       "properties" : {
         "type" : {"type" : "integer", "minimum" : 5, "maximum" : 5},
         "data" : {"$ref" : "TIMER_DATA" }
       },
       "additionalProperties" : false
     },
     "TIMER_CLEARED" : {
       "description" : "A timer was cancelled. This event should always be 0 duration",
       "id" : "TIMER_CLEARED",
       "type" : "object",
       "extends" : {"$ref" : "TIMELINE_EVENT_MARK"},
       "properties" : {
         "type" : {"type" : "integer", "minimum" : 6, "maximum" : 6},
         "data" : {"$ref" : "TIMER_DATA" }
       },
       "additionalProperties" : false
     },
     "TIMER_FIRED" : {
       "description" : "Event corresponding to a timer fire.",
       "id" : "TIMER_FIRED",
       "type" : "object",
       "extends" : {"$ref" : "TIMELINE_EVENT"},
       "properties" : {
         "type" : {"type" : "integer", "minimum" : 7, "maximum" : 7},
         "data" : {"$ref" : "TIMER_DATA" }
       },
       "additionalProperties" : false
     },
     "XHR_READY_STATE_CHANGE" : {
       "description" : "An XMLHttpRequest readystatechange event handler",
       "id" : "XHR_READY_STATE_CHANGE",
       "type" : "object",
       "extends" : {"$ref" : "TIMELINE_EVENT"},
       "properties" : {
         "type" : {"type" : "integer", "minimum" : 8, "maximum" : 8},
         "data" : {
           "type" : "object",
           "properties" : {
             "url" : {"type" : "string", "description" : "URL Requested" },
             "readyState" : {"type" : "integer", "minimum" : 1, "maximum" : 4}
           },
           "additionalProperties" : false
         }
       },
       "additionalProperties" : false
     },
     "XHR_LOAD" : {
       "description" : "XMLHttpRequest load event handler",
       "id" : "XHR_LOAD",
       "type" : "object",
       "extends" : {"$ref" : "TIMELINE_EVENT"},
       "properties" : {
         "type" : {"type" : "integer", "minimum" : 9, "maximum" : 9},
         "data" : {
           "type" : "object",
           "properties" : {
             "url" : {"type" : "string", "description" : "URL Requested"}
           },
           "additionalProperties" : false
         }
       },
       "additionalProperties" : false
     },
     "EVAL_SCRIPT" : {
       "description" : "A <script> tag has been encountered evaluated/compiled and run. In the case of an external script, this includes the time to download the script",
       "id" : "EVAL_SCRIPT",
       "type" : "object",
       "extends" : {"$ref" : "TIMELINE_EVENT"},
       "properties" : {
         "type" : {"type" : "integer", "minimum" : 10, "maximum" : 10},
         "data" : {
           "type" : "object",
           "properties" : {
             "url" : {"type" : "string", "description" : "URL of the tag's source"},
             "lineNumber" : {
               "type" : "integer", 
               "description" : "Integer start line of script contents within the document",
               "minimum" : 0
             },
             "additionalProperties" : false
           }
         },
         "additionalProperties" : false
       }
     },
     "LOG_MESSAGE" : {
       "description" : "A call to console.markTimeline() from within javascript on the monitored page",
       "id" : "LOG_MESSAGE",
       "type" : "object",
       "extends" : {"$ref" : "TIMELINE_EVENT_MARK"},
       "properties" : {
         "type" : {"type" : "integer", "minimum" : 11, "maximum" : 11},
         "duration" : {"type" : "integer", "optional" : true},
         "data" : {
           "type" : "object",
           "properties" : {
             "message" : {"type" : "string", "description" : "Contents of message"}
           },
           "additonalProperties" : false
         },
         "additionalProperties" : false
       }
     },
     "NETWORK_RESOURCE_START" : {
       "description" : "A network request is enqueued",
       "id" : "NETWORK_RESOURCE_START",
       "type" : "object",
       "extends" : {"$ref" : "TIMELINE_EVENT_MARK"},
       "properties" : {
         "type" : {"type" : "integer", "minimum" : 12, "maximum" : 12},
         "duration" : {"type" : "integer", "optional" : true},
         "data" : {
           "type" : "object",
           "properties" : {
             "requestId" : {"type" : "string", "description" : "String id of this resource" },
             "url" : {"type" : "string", "description" : "URL Requested"},
             "requestMethod" : {
               "type" : "string",
               "description" : "Method used to retrieve the resource. (e.g. GET/POST)"
             }
           },
           "additionalProperties" : false
         },
         "additionalProperties" : false
       }
     },
     "NETWORK_RESOURCE_RESPONSE" : {
       "description" : "The renderer has started receiving bits from the resource loader (UI thread time).",
       "id" : "NETWORK_RESOURCE_START",
       "type" : "object",
       "extends" : {"$ref" : "TIMELINE_EVENT"},
       "properties" : {
         "type" : {"type" : "integer", "minimum" : 13, "maximum" : 13},
         "data" : {
           "type" : "object",
           "properties" : {
             "requestId" : {"type" : "string", "description" : "String id of this resource" },
             "mimeType" : {"type" : "string", "description" : "The MIME type of the resource" },
             "statusCode" : {
               "type" : "integer",
               "description" : "Integer HTTP response code",
               "minimum" : 0,
               "maximum" : 599
              }
           },
           "additionalProperties" : false
         },
         "additionalProperties" : false
       }
     },
     "NETWORK_RESOURCE_FINISH" : {
       "description" : "A resource load is successful and complete",
       "id" : "NETWORK_RESOURCE_FINISH",
       "type" : "object",
       "extends" : {"$ref" : "TIMELINE_EVENT_MARK"},
       "properties" : {
         "type" : {"type" : "integer", "minimum" : 14, "maximum" : 14},
         "data" : {
           "type" : "object",
           "properties" : {
             "requestId" : {"type" : "string", "description" : "String id of this resource" },
             "networkTime" : {"type" : "number", "description" : "Network level timing for end.", "optional" : true },
             "didFail" : {"type" : "boolean", "description" : "whether the resource request failed" }
           },
           "additionalProperties" : false
         },
         "additionalProperties" : false
       }
     },
     "JAVASCRIPT_CALLBACK" : {
       "description" : "TIme spent running JavaScript during an event dispatch",
       "id" : "JAVASCRIPT_CALLBACK",
       "type" : "object",
       "extends" : {"$ref" : "TIMELINE_EVENT"},
       "properties" : {
         "type" : {"type" : "integer", "minimum" : 15, "maximum" : 15},
         "data" : {
           "type" : "object",
           "properties" : {
             "scriptName" : {"type" : "string", "description" : "Name of the script that contained this function" },
             "scriptLine" : {
               "type" : "integer",
               "description" : "Line in the script where the function resides",
               "minimium" : 0
             }
           },
           "additionalProperties" : false
         },
         "additionalProperties" : false
       }
     },
     "RESOURCE_DATA_RECEIVED" : {
       "description" : "This is a parent event for processing data for any resource loaded from the resource loader (e.g. HTML pages, images, external scripts).",
       "id" : "RESOURCE_DATA_RECEIVED",
       "type" : "object",
       "extends" : {"$ref" : "TIMELINE_EVENT"},
       "properties" : {
         "type" : {"type" : "integer", "minimum" : 16, "maximum" : 16},
         "data" : {
           "type" : "object",
           "properties" : {
             "requestId" : {"type" : "string", "description" : "Integer id of this resource" },
           },
           "additionalProperties" : false
         },
         "additionalProperties" : false
       }
     },
     "GC_EVENT" : {
       "description" : "A GC Event. NOTE: this is a brand new event type",
       "id" : "GC_EVENT",
       "type" : "object",
       "extends" : {"$ref" : "TIMELINE_EVENT" },
       "properties" : {
         "type" : {"type" : "integer", "minimum" : 17, "maximum" : 17},
         "data" : {
           "type" : "object",
           "properties" : {
             "usedHeapSizeDelta" : {"type" : "integer", "description" : "Delta from last GC event?"},
           },
           "additionalProperties" : false,
         },
         "additionalProperties" : false
       }
     },
     "MARK_DOM_CONTENT" : {
       "description" : "Mark that the main resource DOM document finished parsing",
       "id" : "MARK_DOM_CONTENT",
       "type" : "object",
       "extends" : {"$ref" : "TIMELINE_EVENT_MARK" },
       "properties" : {
         "type" : {"type" : "integer", "minimum" : 18, "maximum" : 18},
         "data" : {
           "type" : "object",
           "properties" : {
           },
           "additionalProperties" : false,
           "optional" : true,
         },
         "additionalProperties" : false
       }
     },
     "MARK_LOAD_EVENT" : {
       "description" : "Mark that the main resource finished loading",
       "id" : "MARK_LOAD_EVENT",
       "type" : "object",
       "extends" : {"$ref" : "TIMELINE_EVENT_MARK" },
       "properties" : {
         "type" : {"type" : "integer", "minimum" : 19, "maximum" : 19},
         "data" : {
           "type" : "object",
           "properties" : {
           },
           "additionalProperties" : false,
           "optional" : true
         },
         "additionalProperties" : false
       }
     },
     "SCHEDULE_RESOURCE_REQUEST" : {
       "description" : "A resource request was scheduled to be added to the network queue.",
       "id" : "SCHEDULE_RESOURCE_REQUEST",
       "type" : "object",
       "extends" : {"$ref" : "TIMELINE_EVENT_MARK" },
       "properties" : {
         "type" : {"type" : "integer", "minimum" : 20, "maximum" : 20},
         "data" : {
           "type" : "object",
           "properties" : {
             "url" : {"type" : "string", "description" : "URL Requested"},
           },
           "additionalProperties" : false,
           "optional" : true
         },
         "additionalProperties" : false
       }
     },

     // Speedtracer Events
     "SPEEDTRACER_EVENT" : {
       "id" : "SPEEDTRACER_EVENT",
       "type" : "object",
       "properties" : {
         "type" : {
           "description" : "Speedtracer Type ID",
           "type" : "integer", 
           "minimum" : 0x7FFFFFF8,
           "maximum" : 0x7FFFFFFE,
         },
         "data"     : {"description" : "A JSON dictionary of data", "type" : "object" },
         "sequence" : {
           "description" : "Sequence number of this event",
           "type" : "integer",
           "minimum" : 0,
           "optional" : true
         }
       }
     },
     "TAB_CHANGED" : {
       "description" : "There was a change in the location bar or title of the tab. Can use to detect a page transition",
       "id" : "TAB_CHANGED",
       "type" : "object",
       "extends" : {"$ref" : "SPEEDTRACER_EVENT"},
       "properties" : {
         "type" : {
           "type" : "integer",
           "minimum" : 0x7FFFFFFE ,
           "maximum" : 0x7FFFFFFE ,
           "description" : "MAX 32 BIT INTEGER - 1 (2147483646)"
         },
         "time"     : {"description" : "Milliseconds since start of session", "type" : "number", "minimum" : 0},
         "data" : {
           "type" : "object",
           "properties" : {
             "url" : {"type" : "string", "description" : "new URL of the monitored TAB"}
           },
           "additionalProperties" : false
         }
       },
       "additionalProperties" : false
     },
     "RESPONSE_DATA" : {
       "description" : "Response information for this request.",       
       "id" : "RESPONSE_DATA",
       "type" : "object",
       "properties" : {
         "url" : {
           "type" : "string",
           "description" : "The URL of the resource",           
           "optional" : true
         },
         "connectionId" : {
           "type" : "integer",
           "description" : "ID for the network connection",           
           "minimum" : 0,
           "optional" : true
         },
         "connectionReused" : {
           "type" : "boolean",
           "description" : "Socket reused from a previous connection",           
           "optional" : true
         },
         "fromDiskCache" : {
           "type" : "boolean",
           "description" : "True if the request was received from cache",           
           "optional" : true
         },
         "status" : {
           "type" : "integer",
           "description" : "Integer HTTP response code",
           "minimum" : 0,
           "maximum" : 599,
           "optional" : true
         },
         "headers" : {"type" : "object", "description" : "Request Headers", "optional" : true},
         "timing" : {
           "type" : "object",
           "description" : "Detailed resource loader network timing info",
           "optional" : true,
           "properties" : {
             "requestTime" : {
               "type" : "number",
               "description" : "Time the network transfer started (the request became unblocked)",
               "minimum" : 0
             },
             "proxyStart" : {
               "type" : "number",
               "description" : "Start of proxy processing in milliseconds (-1 indicates no proxy)",
               "minimum" : -1
             },
             "proxyEnd" : {
               "type" : "number",
               "description" : "End of proxy processing in milliseconds (-1 indicates no proxy)",
               "minimum" : -1
             },
             "dnsStart" : {
               "type" : "number",
               "description" : "Start of DNS resolution in milliseconds (negative value indicates re-used socket)"
              },
              "dnsEnd" : {
                "type" : "number",
                "description" : "End of DNS resolution in milliseconds (negative value indicates re-used socket)"
              },
              "connectStart" : {
                "type" : "number",
                "description" : "Start of TCP connection establishment in milliseconds (includes DNS duration, -1 indicates re-used socket)",
                "minimum" : -1                   
              },
              "connectEnd" : {
                "type" : "number",
                "description" : "End of TCP connection establishment in milliseconds (includes DNS duration, -1 indicates re-used socket)",
                "minimum" : -1                   
              },                 
              "sendStart" : {
                "type" : "number",
                "description" : "Start sending request to server in milliseconds",
                "minimum" : 0
              },
              "sendEnd" : {
                "type" : "number",
                "description" : "End sending request to server in milliseconds",
                "minimum" : 0
              },
              "sslStart" : {
                "type" : "number",
                 "description" : "Start of the SSL handshake in milliseconds (negative value indicates no handshake)"
              },
              "sslEnd" : {
                 "type" : "number",
                 "description" : "End of the SSL handshake in milliseconds (negative value indicates no handshake)"
              },
              "receiveHeadersEnd" : {
                 "type" : "number",
                 "description" : "Time by which HTTP headers have been received in milliseconds",
                 "minimum" : 0
             }
           }
         }
       }
     },
     "NETWORK_REQUEST_WILL_BE_SENT" : {
       "description" : "Network message indicating a request for a resource is about to go out.",
       "id" : "NETWORK_REQUEST_WILL_BE_SENT",
       "type" : "object",
       "extends" : {"$ref" : "SPEEDTRACER_EVENT"},
       "properties" : {
         "type" : {
           "type" : "integer",
           "minimum" : 0x7FFFFFFA ,
           "maximum" : 0x7FFFFFFA ,
           "description" : "MAX 32 BIT INTEGER - 5"
         },
         "time" : {"type" : "number", "description" : "Synthesized timingChanged information.", "optional" : true },
         "data" : {
           "type" : "object",
           "properties" : {                          
             "requestId" : {"type" : "string", "description" : "String ID of the resource"},
             "request" : {
               "type" : "object",
               "description" : "Request information. Basically just headers",
               "properties" : {
                 "httpHeaderFields" : {"type" : "object", "description" : "Request Headers", "optional" : true}
               }
             },
             "redirectResponse" : {
               "extends" : {"$ref" : "RESPONSE_DATA"},
               "optional" : true
             }
           }
         }
       }
     },
     "NETWORK_RESPONSE_RECEIVED" : {
       "description" : "Network message indicating that we received a response from the server for a resource.",
       "id" : "NETWORK_RESPONSE_RECEIVED",
       "type" : "object",
       "extends" : {"$ref" : "SPEEDTRACER_EVENT"},
       "properties" : {
         "type" : {
           "type" : "integer",
           "minimum" : 0x7FFFFFF9 ,
           "maximum" : 0x7FFFFFF9 ,
           "description" : "MAX 32 BIT INTEGER - 6"
         },
         "time" : {"type" : "number", "description" : "Synthesized timingChanged information.", "optional" : true },
         "data" : {
           "type" : "object",
           "properties" : {
             "response" : {
               "extends" : {"$ref" : "RESPONSE_DATA"},
             }                       
           }
         }
       }
     },
     "NETWORK_DATA_RECEIVED" : {
       "description" : "Network message indicating that we received a response from the server for a resource.",
       "id" : "NETWORK_DATA_RECEIVED",
       "type" : "object",
       "extends" : {"$ref" : "SPEEDTRACER_EVENT"},
       "properties" : {
         "type" : {
           "type" : "integer",
           "minimum" : 0x7FFFFFF8 ,
           "maximum" : 0x7FFFFFF8 ,
           "description" : "MAX 32 BIT INTEGER - 7"
         },
         "time" : {"type" : "number", "description" : "Synthesized timingChanged information.", "optional" : true },
         "data" : {
           "type" : "object",
           "properties" : {
             "dataLength" : {"type" : "integer", "description" : "Size of payload in bytes"}
           }
         }
       }
     },
     "PROFILE_DATA" : {
       "description" : "Javascript Profile information from the Browser",
       "id" : "PROFILE_DATA",
       "type" : "object",
       "extends" : {"$ref" : "SPEEDTRACER_EVENT"},
       "properties" : {
         "type" : {
           "type" : "integer",
           "minimum" : 0x7FFFFFFC ,
           "maximum" : 0x7FFFFFFC ,
           "description" : "MAX 32 BIT INTEGER - 3 (2147483644)"
         },
         "data" : {
           "type" : "object",
           "properties" : {
             "format"      : {"type" : "string", "description" : "The type of javascript profile data (e.g. 'v8')"},
             "profileData" : {"type" : "string", "description" : "The profile Data"},
             "isOrphaned"  : {"type" : "boolean", "description" : "Does this record belong to a Timeline Event?"}
           },
           "additionalProperties" : false
         }
       },
       "additionalProperties" : false
     },
    // CUSTOM EVENTS
    "CUSTOM_EVENT" : {
      "description" : "A data-driven, self-describing event.",
      "id" : "CUSTOM_EVENT",
      "type" : "object",
      "properties" : {        
        // Custom Events have their types calculated on the fly from the
        // typeName, however, these types must be negative numbers.
        "type" : {
           "type" : "integer",
           "minimum" : -2147483648 ,
           "maximum" : -2 ,
           "description" : "A value between -2 and -2147483648"
        },
        "time" : {
           "description" : "Milliseconds since start of session",
           "type" : "number", "minimum" : 0
        },
        "duration" : {
           "description" : "Milliseconds this event took",
           "type" : "number",
           "minimum" : 0,
           "optional" : true
         },
        "typeName" : {
          "type" : "string",
          "description" : "The display name for this custom event"
        },
        "color" : {
          "type" : "string",
          "description" : "The CSS color to use for displaying this event"
        },
        "children" : {
          "description" : "Child Events.",
          "type" : "array",
          "optional" : true,
        },
        "data" : {
          "description" : "String map of key/value pairs",
          "type" : "object"
          },
        "sequence" : {
          "description" : "Sequence number of this event",
          "type" : "integer",
          "minimum" : 0,
          "optional" : true
        }
      },
      "additionalProperties" : false
    }
    };
  }-*/;

}
