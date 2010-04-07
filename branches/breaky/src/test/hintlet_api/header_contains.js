// Test for hintlet.headerContains()

// Make a namespace for this rule using a closure
(function() {  // Begin closure

var headers;
var result;

headers = {"Content-Type":"text/html"};

result = hintlet.headerContains(headers, "Content-Type", "text/html");
if (result != true) {
  throw new Error("Expected to find Content-Type: text/html. "
                  + "Got result " + result);
}

result = hintlet.headerContains(headers, "Content-Type", "TEXT/HTML");
if (result != true) {
  throw new Error("Expected to find Content-Type: TEXT/HTML. "
                  + "Got result " + result);
}

result = hintlet.headerContains(headers, "Content-Type", "TEXT/SGML");
if (result != false) {
  throw new Error("Did not expected to find Content-Type: TEXT/SGML. "
                  + "Got result " + result);
}

result = hintlet.headerContains(headers, "bogus", "text/html");
if (result != false) {
  throw new Error("Did not expected to find bogus: text/html. "
                  + "Got result " + result);
}



}) ();
