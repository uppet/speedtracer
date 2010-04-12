package com.google.speedtracer.client.breaky;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.speedtracer.client.breaky.JsonSchema.JsonSchemaResults;
import com.google.speedtracer.client.util.DataBag;
import com.google.speedtracer.client.util.JSOArray;
import com.google.speedtracer.client.util.JsIntegerMap;

//TODO: improve the hack that is the resolver hook
public class DumpValidator {
  @SuppressWarnings("unused")
  private final JavaScriptObject schemas = getSchemas();
  
  private JsIntegerMap<JsonSchema> idMap = JsIntegerMap.create();
  public DumpValidator() {
    fillIdMap();
    hookResolver();
  }
  
  /**
   * In our schema set, if a schema has a fully constrained type property, then
   * it is a concrete rather than an abstract type. The ID Map let's us quickly
   * validate based on the concrete type as objects come in.
   */
  private void fillIdMap() {
    JSOArray<String> schemaNames = listSchemas();
    for(int i = 0; i < schemaNames.size(); i++) {
      JsonSchema schema = getSchema(schemaNames.get(i));
      JavaScriptObject properties = schema.getProperties();
      
      if(DataBag.hasOwnProperty(properties, "type")) {
        JsonSchema dumpType = DataBag.getJSObjectProperty(properties, "type").cast();
        if((DataBag.hasOwnProperty(dumpType, "minimum") && 
            DataBag.hasOwnProperty(dumpType, "maximum")) &&
            dumpType.getMinimum() == dumpType.getMaximum()) {
          idMap.put(dumpType.getMinimum(), schema);
        }
      }
    }
  }
  
  /**
   * Validate a Speedtracer dump object
   * @param obj a speedtracer dump object to be validated
   * @return {@link JsonSchemaResults} object indicating that the entire object
   * is valid or containing the error that caused it to be invalid.
   */
  public JsonSchemaResults validate(JavaScriptObject obj) {
    JsonSchema concreteSchema = getSchema(obj);
    if(concreteSchema == null) {
      return JsonSchemaResults.create("", "No schema found for " + obj.toString());
    }
    
    JsonSchemaResults results = concreteSchema.validate(obj);
    if(!results.isValid()) {
      return results;
    }
    
    if(DataBag.hasOwnProperty(obj, "children")) {
      JSOArray<JavaScriptObject> children = DataBag.getJSObjectProperty(obj, "children").cast();
      for(int i = 0; i < children.size() && results.isValid(); i++) {
        results = this.validate(children.get(i));
      }
    }
    return results;
  }
  
  public native final JSOArray<String> listSchemas() /*-{
    var schemas = this.@com.google.speedtracer.client.breaky.DumpValidator::schemas;
    ret = [];
    for(schema in schemas) {
      ret.push(schema);
    }
    return ret;
  }-*/;
  
  public native final JsonSchema getSchema(String name) /*-{
    return this.@com.google.speedtracer.client.breaky.DumpValidator::schemas[name];
  }-*/;
  
  public final JsonSchema getSchema(int id) {
    return idMap.get(id);
  }
  
  public final JsonSchema getSchema(JavaScriptObject obj) {
    if(DataBag.hasOwnProperty(obj, "type")) {
      return getSchema(DataBag.getIntProperty(obj, "type"));
    } else {
      return null;
    }
  }
  
  private native final void hookResolver() /*-{
    var me = this;
    $wnd.JSONSchema.resolveReference = function(reference) {
      return me.@com.google.speedtracer.client.breaky.DumpValidator::schemas[reference];
    };
  }-*/;
  
  private static native final JavaScriptObject getSchemas() /*-{
    return {
      ///////////////// Browser Timeline Events /////////////////////////////
      
      //Base Definitions
     "TIMELINE_EVENT_BASE" : {
       "description" : "Base Browser Timeline Event Schema",
       "id" : "TIMELINE_EVENT_BASE",
       "type" : "object",
       "properties" : {
         "type" : {
           "description" : "Speedtracer Type ID",
           "type" : "integer", 
           "minimum" : 0,
           "maximum" : 19
         },
         "time"     : {"description" : "Milliseconds since start of session", "type" : "number", "minimum" : 0},
         "callerScriptName" : {
           "type" : "string",
           "description" : "The URL of the script that triggered this event",
           "requires" : "callerScriptLine",
           "optional" : true
          },
         "callerScriptLine" : {"type" : "integer",
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
         "data"     : {"description" : "A JSON dictionary of data", "type" : "object" },
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
         "duration" : {"description" : "Milliseconds this event took", "type" : "number", "minimum" : 0},
         "children" : {
           "description" : "Child Events",
           "type" : "array",
           "optional" : true,
         }
       }
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
         "additionalProperties" : false
       }
     },
     
     //Some Handy Databag references
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
         "timerId" : {"type" : "integer", "description" : "opaque ID identifying the timer"},
         "timeout" : {"type" : "integer", "descrption" : "timeout in milliseconds", "optional" : true},
         "singleShot" : {"type" : "boolean", "description" : "one time vs. repeating timer", "optional" : true}
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
               "enum" : [
                 "mousemove",
                 "mouseover",
                 "click",
                 "mouseout",
                 "load",
                 "unload",
                 "DOMContentLoaded",
                 "SpeedTracer Headless Event"
                ]
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
             "length" : {"type" : "integer"},
             "startLine" : {"type" : "integer"},
             "endLine" : {"type" : "integer"}
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
             "identifier" : {"type" : "integer", "description" : "Integer id of this resource" },
             "url" : {"type" : "string", "description" : "URL Requested"},
             "requestMethod" : {
               "type" : "string",
               "enum" : ["GET", "POST"],
               "description" : "Method used to retrieve the resource"
             },
             "isMainResource" : {"type" : "boolean", "description"  : "Is this the resource in the Browser's URL bar? (TODO: verify)" }
           },
           "additionalProperties" : false
         },
         "additionalProperties" : false
       }
     },
     "NETWORK_RESOURCE_RESPONSE" : {
       "description" : "The renderer has started receiving bits from the resource loader. Note that this is NOT a network level time, but rather the timing from the perspective of the UI thread in the renderer. THey usually align with network level timings, but if the UI thread is blocked doing work, this callback can be delayed",
       "id" : "NETWORK_RESOURCE_START",
       "type" : "object",
       "extends" : {"$ref" : "TIMELINE_EVENT_MARK"},
       "properties" : {
         "type" : {"type" : "integer", "minimum" : 13, "maximum" : 13},
         "data" : {
           "type" : "object",
           "properties" : {
             "identifier" : {"type" : "integer", "description" : "Integer id of this resource" },
             "expectedContentLength" : {
               "type" : "integer",
               "description" : "Size in bytes the browser expects the resource to be",
               "minimum" : -1
               },
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
             "identifier" : {"type" : "integer", "description" : "Integer id of this resource" },
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
       "description" : "A parent event for the duration of processing an external or inline script and its associated resource loads (TODO: edit me)",
       "id" : "RESOURCE_DATA_RECEIVED",
       "type" : "object",
       "extends" : {"$ref" : "TIMELINE_EVENT"},
       "properties" : {
         "type" : {"type" : "integer", "minimum" : 16, "maximum" : 16},
         "data" : {
           "type" : "object",
           "properties" : {
             "identifier" : {"type" : "integer", "description" : "Integer id of this resource" },
           },
           "additionalProperties" : false
         },
         "additionalProperties" : false
       }
     },
     "GC_EVENT" : {
       "description" : "A GC Event. TODO: this is a brand new event type",
       "id" : "GC_EVENT",
       "type" : "object",
       "extends" : {"$ref" : "TIMELINE_EVENT" },
       "properties" : {
         "type" : {"type" : "integer", "minimum" : 17, "maximum" : 17},
         "data" : {
           "type" : "object",
           "properties" : {
             "usedHeapSizeDelta" : {"type" : "integer", "description" : "TODO: delta from last GC event?"},
           },
           "additionalProperties" : false,
         },
         "additionalProperties" : false
       }
     },
     "MarkDOMContent" : {
       "description" : "TODO: this is a brand new event type",
       "id" : "GC_EVENT",
       "type" : "object",
       "extends" : {"$ref" : "TIMELINE_EVENT" },
       "properties" : {
         "type" : {"type" : "integer", "minimum" : 18, "maximum" : 18},
         "data" : {
           "type" : "object",
           "properties" : {
           },
           "additionalProperties" : false,
         },
         "additionalProperties" : false
       }
     },
     "MarkLoadEvent" : {
       "description" : "A GC Event. TODO: this is a brand new event type",
       "id" : "GC_EVENT",
       "type" : "object",
       "extends" : {"$ref" : "TIMELINE_EVENT" },
       "properties" : {
         "type" : {"type" : "integer", "minimum" : 19, "maximum" : 19},
         "data" : {
           "type" : "object",
           "properties" : {
             "usedHeapSizeDelta" : {"type" : "integer", "description" : "TODO: delta from last GC event?"},
           },
           "additionalProperties" : false,
         },
         "additionalProperties" : false
       }
     },
     
     /////////////// Speedtracer Events /////////////////////////////////
     "SPEEDTRACER_EVENT" : {
       "id" : "TIMELINE_EVENT",
       "type" : "object",
       "properties" : {
         "type" : {
           "description" : "Speedtracer Type ID",
           "type" : "integer", 
           "minimum" : 0x7FFFFFFD,
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
     "NETWORK_RESOURCE_UPDATE" : {
       "description" : "Intermittent update messages sent as the laoder learns more information about a given network resource",
       "id" : "TAB_CHANGED",
       "type" : "object",
       "extends" : {"$ref" : "SPEEDTRACER_EVENT"},
       "properties" : {
         "type" : {
           "type" : "integer",
           "minimum" : 0x7FFFFFFD ,
           "maximum" : 0x7FFFFFFD ,
           "description" : "MAX 32 BIT INTEGER - 2 (2147483645)"
         },
         //TODO: require this to be 0 once speedtracer updates
         "time" : {"type" : "number", "description" : "DEPRECATED. Previously synthesized (badly) out of the timingChanged information.", "optional" : true },
         "data" : {
           "type" : "object",
           "properties" : {
             "identifier" : {"type" : "integer", "description" : "Integer ID of the resource"},
                           
              //////////////// didRequestChange ///////////////////////
             "didRequestChange" : {"type" : "boolean", "description" : "Marks the WebKit UpdateResource Event", "enum" : [true], "optional" : true },
             "url" : {"type" : "string", "description" : "The URL of the resource", "requires" : "didRequestChange", "optional" : true},
             "documentURL" : {"type" : "string", "description" : "The URL of the document", "requires" : "didRequestChange", "optional" : true},
             "host" : {"type" : "string", "description" : "The network host of the resource", "requires" : "didRequestChange", "optional" : true},
             "path" : {"type" : "string", "descirption" : "URI to the resource from the origin", "requires" : "didRequestChange", "optional" : true},
             "lastPathComponent" : {"type" : "string", "description" : "Basename of the path", "requires" : "didRequestChange", "optional" : true},
             "requestHeaders" : { "type" : "object", "description" : "HTTP Headers from the request", "requires" : "didRequestChange", "optional" : true},
             "mainResource" : {"type" : "boolean", "description" : "Is this the resource in the Browser's URL bar? (TODO: verify)", "requires" : "didRequestChange", "optional" : true},
             "requestMethod" : {
               "type" : "string",
               "enum" : ["GET", "POST", ""],
               "description" : "Method used to retrieve the resource. Empty if cached",
               "requires" : "didRequestChange",
               "optional" : true
             },
             "requestFormData" : { "type" : "string", "description" : "The form data sent via POST", "requires" : "didRequestChange", "optional" : true},
             "cached" : {"type" : "boolean", "description" : "True if the request was received from cache", "requires" : "didRequestChange", "optional" : true},
             
             
             //////////////// didResponseChange ///////////////////////
             "didResponseChange" : {"type" : "boolean", "description" : "Marks that headers have been updated", "enum" : [true], "optional" : true},
             "mimeType" : {"type" : "string", "description" : "The MIME type of the resource", "requires" : "didResponseChange", "optional" : true},
             "suggestedFilename" : {"type" : "string", "description" : "TODO", "requires" : "didResponseChange", "optional" : true},    
             "expectedContentLength" : {"type" : "integer", "description" : "The expected content length of the resource in bytes", "requires" : "didResponseChange", "optional" : true},
             "statusCode" : {
               "type" : "integer",
               "description" : "HTTP Status Code",
               "minimum" : 0,
               "maximum" : 599,
               "requires" : "didResponseChange",
               "optional" : true
             },
             "responseHeaders" : {"type" : "object", "description" : "HTTP Headers from the response", "requires" : "didResponseChange", "optional" : true},
             
             //////////////// didTypeChange ///////////////////////
             "didTypeChange" : {"type"  : "boolean", "description" : "TODO", "enum" : [true], "optional" : true},
             "type" : {"type" : "integer", "description" : "TODO", "requires" : "didTypeChange", "optional" : true},
             
             //////////////// didLengthChange ///////////////////////
             "didLengthChange" : {"type" : "boolean", "description" : "Marks that the resourceSize has been updated", "enum" : [true], "optional" : true},
             "resourceSize" : {
               "type" : "integer",
               "description" : "The size of the uncompressed resource. Older versions reported contentLength instead",
               "requires" : "didLengthChange",
               "optional" : true
             },
             
             //////////////// didCompletionChange ///////////////////////
             "didCompletionChange" : {"type" : "boolean", "description" : "TODO", "enum" : [true], "optional" : true},
             "failed" : {"type" : "boolean", "description" : "TODO", "optional" : true, "requires" : "didCompletionChange" },
             "finished" : {"type" : "boolean", "description" : "TODO", "optional" : true, "requires" : "didCompletionChange" },
             
             //////////////// didTimingChange ///////////////////////
             "didTimingChange" : {"type" : "boolean", "description" : "TODO", "enum" : [true], "optional" : true},
             "startTime" : {"type" : "number", "description" : "TODO", "requires" : "didTimingChange", "optional" : true},
             "responseReceivedTime" : {"type" : "number", "description" : "TODO", "requires" : "didTimingChange", "optional" : true},
             "endTime" : {"type" : "number", "description" : "TODO", "requires" : "didTimingChange", "optional" : true},
             "loadEventTime" : {"type" : "number", "description" : "TODO", "requires" : "didTimingChange", "optional" : true},
             "domContentEventTime" : {"type" : "number", "description" : "TODO", "requires" : "didTimingChange", "optional" : true},
           },
           "additionalProperties" : false
         }
       },
       "additionalProperties" : false
     },
     "PROFILE_DATA" : {
       "description" : "Javascript Profile information from the Browser",
       "id" : "TAB_CHANGED",
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
    };
  }-*/;
    
}

