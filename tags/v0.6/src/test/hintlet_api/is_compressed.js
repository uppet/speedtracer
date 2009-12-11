// Test for hintlet.isCompressed()

// Make a namespace for this rule using a closure
(function() {  // Begin closure

var data;
var result;


data = {'data':{'headers':{"Content-Type":"text/html",
                           "Content-Encoding":"gzip"
                          }
               }
       };
result = hintlet.isCompressed(data);
if (result != true) {
  throw new Error("Expected record to be shown as compressed.");
}

data = {'data':{'headers':{"Content-Type":"text/html",
                           "Content-Encoding":"bogus"
                          }
               }
       };
result = hintlet.isCompressed(data);
if (result != false) {
  throw new Error("Expected record w/ bogus encoding to be shown "
                  + "as uncompressed.");
}

data = {'data':{'headers':{"Content-Type":"text/html",
                          }
               }
       };
result = hintlet.isCompressed(data);
if (result != false) {
  throw new Error("Expected record w/o content-encoding to be shown "
                  + "as uncompressed.");
}


}) ();
