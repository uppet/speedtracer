// Test for hintlet.formatSeconds() and hintlet.formatMilliseconds()

// Make a namespace for this rule using a closure
(function() {  // Begin closure

var result;
result = hintlet.formatSeconds(1000.12345, 0);
if (result != "1s") {
  throw new Error("Expected 1s Got: " + result);
}

result = hintlet.formatSeconds(1234.5, 3);
if (result != "1.234s") {
  throw new Error("Expected 1.234s Got: " + result);
}

result = hintlet.formatMilliseconds(1000.12345, 0);
if (result != "1000ms") {
  throw new Error("Expected 1000ms Got: " + result);
}

result = hintlet.formatMilliseconds(1000.12345, 2);
if (result != "1000.12ms") {
  throw new Error("Expected 1000.12ms Got: " + result);
}

}) ();
