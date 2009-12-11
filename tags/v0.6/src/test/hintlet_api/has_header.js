// Test for hintlet.hasHeader

// Make a namespace for this rule using a closure
(function() {  // Begin closure

var headers;
var result;

// Check for the Content-Type header
headers = {"Content-Type":"text/html"};
result = hintlet.hasHeader(headers, "Content-Type");
if (result != "text/html") {
  throw new Error("Expected to find text/html from Content-Type");
}

// Comparison should be case insensitive
result = hintlet.hasHeader(headers, "content-type");
if (result != "text/html") {
  throw new Error("Expected to find text/html from content-type");
}

// Test for a non-existent header
result = hintlet.hasHeader(headers, "bogus");
if (result !== undefined) {
  throw new Error("Expected to find undefined from bogus");
}

}) ();
