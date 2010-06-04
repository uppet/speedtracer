// Test for hintlet.getResourceType()

// Make a namespace for this rule using a closure
(function() {  // Begin closure

var data;
var result;

// No Content-Type defined
data = {'data':{'headers':{}}};
result = hintlet.getResourceType(data);
if (result != hintlet.RESOURCE_TYPE_OTHER) {
  throw new Error("Expected hintlet.RESOURCE_TYPE_OTHER from undefined "
                  + "Content-Type header.  Got: " + result);
}

// Creates a dummy record with the Content-Type header set as specified
function test_content_type(mimeType, resourceType) {
  var data = {'data':{'headers':{'Content-Type':mimeType},
                                 'url':'http://foo'},
              'type':6};
  result = hintlet.getResourceType(data);
  if (result != resourceType) {
    throw new Error("Expected " + resourceType + " from " + mimeType
                    + "Content-Type header.  Got: " + result);
  }
}

test_content_type("text/plain", hintlet.RESOURCE_TYPE_DOCUMENT);
test_content_type("text/html", hintlet.RESOURCE_TYPE_DOCUMENT);
test_content_type("text/xml", hintlet.RESOURCE_TYPE_DOCUMENT);
test_content_type("application/xml", hintlet.RESOURCE_TYPE_DOCUMENT);
test_content_type("application/json", hintlet.RESOURCE_TYPE_DOCUMENT);

test_content_type ("text/css", hintlet.RESOURCE_TYPE_STYLESHEET);

test_content_type ("text/javascript", hintlet.RESOURCE_TYPE_SCRIPT);

test_content_type ("image/vnd.microsoft.icon", hintlet.RESOURCE_TYPE_FAVICON);

test_content_type ("image/jpeg", hintlet.RESOURCE_TYPE_IMAGE);
test_content_type ("image/gif", hintlet.RESOURCE_TYPE_IMAGE);
test_content_type ("image/png", hintlet.RESOURCE_TYPE_IMAGE);

// Try to compare url of favicon with a regular image mime type
var data = {'data':{'headers':{'Content-Type':'image/png'},
                                 'url':'http://example.com/favicon.ico'},
              'type':6};
result = hintlet.getResourceType(data);
if (result != hintlet.RESOURCE_TYPE_FAVICON) {
  throw new Error("Expected FAVICON from URL favicon.ico Got: " + result);
                  
}

}) ();