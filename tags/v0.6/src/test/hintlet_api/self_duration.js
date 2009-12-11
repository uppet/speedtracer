// Tests the _addSelfDuration() method in hintlet_main.js

// Make a namespace for this rule using a closure
(function() {  // Begin closure

// A simple test case with just one child
var data = {'children':[{'children':[],'data':{},'duration':10}], 
            'data':{}, 'duration':100};

hintlet._addSelfDuration(data);

dur = data.selfDuration;
if ( dur != 90) {
  throw new Error("Expected selfDuration in top level to be 90. Got: " + dur);
}
dur = data.children[0].selfDuration;
if (dur != 10) {
  throw new Error("Expected selfDuration in first child to be 10. Got: " 
                  + dur);
}

// A more deeply nested hierarcy
data = {'children':[{'children':[
                                  {'children':[],'data':{},'duration':5}, 
                                  {'children':[],'data':{},'duration':5}, 
                                ],'data':{},'duration':20}, 
                    {'children':[],'data':{},'duration':5}, 
                   ], 
        'data':{}, 'duration':100};

hintlet._addSelfDuration(data);

dur = data.selfDuration;
if ( dur != 75) {
  throw new Error("Expected selfDuration in top level to be 75. Got: " + dur);
}
dur = data.children[0].selfDuration;
if ( dur != 10) {
  throw new Error("Expected selfDuration in top level to be 10. Got: " + dur);
}
dur = data.children[0].children[0].selfDuration;
if ( dur != 5) {
  throw new Error("Expected selfDuration in top level to be 5. Got: " + dur);
}

// A more deeply nested hierarcy with a leaf that contains no duration.
data = {'children':[{'children':[
                                  {'children':[],'data':{},'duration':5}, 
                                  {'children':[],'data':{}}, 
                                ],'data':{},'duration':20}, 
                    {'children':[],'data':{},'duration':5}, 
                   ], 
        'data':{}, 'duration':100};

hintlet._addSelfDuration(data);

dur = data.selfDuration;
if ( dur != 75) {
  throw new Error("Expected selfDuration in top level to be 75. Got: " + dur);
}
dur = data.children[0].selfDuration;
if ( dur != 15) {
  throw new Error("Expected selfDuration in top level to be 15. Got: " + dur);
}
dur = data.children[0].children[0].selfDuration;
if ( dur != 5) {
  throw new Error("Expected selfDuration in top level to be 5. Got: " + dur);
}
if (data.children[0].children[1].hasOwnProperty("duration")) {
  throw new Error("Did not expect selfDuration property");
}

})();  // End closure
