// tests for hintlet.typeToString() and hintlet.stringToType()

// Make a namespace for this rule using a closure
(function() {  // Begin closure

// Function to perform the tests for all known values.
function check_type(property, value) {
  var  result = hintlet.stringToType(property);
  if (result != value) {
    throw new Error("hintlet.type." + property + ": Expected: " + value 
                    + ", Got: " + result);
  }
  result = hintlet.typeToString(value);
  if (result != property) {
    throw new Error("typeToString(): " + value + ": Expected: " + property 
                    + ", Got: " + result);
  }
}

// Check of the hintlet.types array.  These values should not change 
// over time, as they will break existing hintlet rules.
check_type('DOM_EVENT', 0);
check_type('LAYOUT_EVENT', 1);
check_type('RECALC_STYLE_EVENT', 2);
check_type('PAINT_EVENT', 3);
check_type('PARSE_HTML_EVENT', 4);
check_type('TIMER_INSTALLED', 5);
check_type('TIMER_CLEARED', 6);
check_type('TIMER_FIRED', 7);
check_type('XHR_READY_STATE_CHANGE', 8);
check_type('XHR_LOAD', 9);
check_type('EVAL_SCRIPT_EVENT', 10);
check_type('LOG_MESSAGE_EVENT', 11);
check_type('RESOURCE_SEND_REQUEST', 12);
check_type('RESOURCE_RECEIVE_RESPONSE', 13);
check_type('RESOURCE_FINISH', 14);

check_type('TAB_CHANGED', 16);
check_type('AGGREGATED_EVENTS' ,17);

check_type('NETWORK_RESOURCE_ERROR', 21);
check_type('NETWORK_RESOURCE_FINISH', 22);
check_type('NETWORK_RESOURCE_RESPONSE', 23);
check_type('NETWORK_RESOURCE_START', 24);

/*
check_type('DOM_EVENT_DISPATCH', 1);
check_type('JAVASCRIPT_COMPILE_EVENT' ,16);
check_type('WINDOW_EVENT' ,17);
check_type('DOM_BINDING_EVENT' ,18);
check_type('GARBAGE_COLLECT_EVENT' ,22);
check_type('MOUSE_HOVER_STYLE_EVENT' ,23);
*/

var expectedNumTypes = 28;
if (hintlet.typeList.length > expectedNumTypes) {
  throw new Error("There are more types: " + hintlet.typeList.length
                  + " than expected: " + expectedNumTypes + ".  Update this test.");
}

})();  // End closure

