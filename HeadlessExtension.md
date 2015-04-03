

# Introduction #

The Speed Tracer Headless Extension is a version of the Speed Tracer extension that has all the user interface removed.  Instead, it supports a JavaScript API that the application can interact with to create and retrieve trace data into the page, or send a trace to a remote server.  The extension can also be controlled without writing any JavaScript by adding special parameters to any URL's query string.

## Security Implications ##

The Speed Tracer Headless extension is experimental software.  Due to security concerns, limit its use to testing machines.  Do not install the extension and use it in an installation where you perform regular browsing.  No attempt has been made to limit the access of unauthorized sites to the trace information that can be gathered by the extension and its APIs.

## Use Cases ##

  * Measuring start-up latency of a page
  * Measuring latency of a particular operation
  * Capturing Speed Tracer data as a part of an automated test

# Getting Started #

The Speed Tracer Headless Extension is not distributed from the Chrome Extension Gallery so you will need to build and install it yourself.

## Building the Headless Extension ##

Compile the Speed Tracer project as you normally would.  In the target directory, you will find a directory  named `speedtracerheadless`.

## Installing the Headless Extension ##

Using the Chrome Extension page `chrome://extensions`, install the headless extension from the speedtracerheadless directory by clicking the _Load unpacked extension..._ button.  Once installed, there are no further configuration settings for the Headless Extension.

# Configuring an Application for the Headless Extension #

For measuring page startup latency, there is only one change you need to make to your application.  Add the following script tag to the head part of your main document:

```
   <script language="javascript" 
           src="chrome-extension://jolleknjmmglebfldiogepklbacoohni/headless_api.js">
```

> Note: if you choose to make a `.crx`, you will need to replace the long string of alphabetic characters with the correct ID: for the extension you just compiled.  You can find the value of the `ID:` field of the extension in the `chrome://extensions` status page. There is also a setting of the public key field for the manifest in HeadlessBackgroundPage.java that will need updating.  Keep track of the generated .pem file from the first build so you can rebuild with the same key and extension id.

It is safe to add this script tag into any application.  The user will see no changes in behavior if a user is not using the Chrome browser or does not have the Speed Tracer headless extension installed.

# Retrieving data from the Extension #

To retrieve the dump data, you have a choice of either adding calls to an API explicitly, or using the Headless Extension's query string processing capability to start monitoring and automatically forwarding the dump to a remote server after a timeout.

## Using the query string to activate the extension ##

The headless extension installs a content script on each page that looks for special parameters passed on the URL query string.

  * monitor
> > Turns on monitoring of this tab and reloads the current page.  The current URL minus the 'monitor' argument is used to reload the page.
  * header(name:value)
> > Sets a custom name/value pair for a header to send in the xhr. Multiple headers can be set by using multiple instances (e.g. SpeedTracer=header(foo:bar),header(blah:asdf)
  * xhr(url)
> > Sets where the JSON formatted data should be sent via XHR.
  * timeout(value)
> > Sets the timeout to send the dump to.  The value specified is in milliseconds.

Consider the following URL:

```
  http://example.com/mypage.html?args=123&SpeedTracer=monitor,header(foo:bar),xhr(http://nowhere.com),timeout(5000)
```

The `SpeedTracer=` parameters would be parsed as arguments to the headless extension.  The `monitor` argument tells the extension to turn on monitoring and then reload the page.  The URL to be reloaded would be stripped of the `monitor` argument to prevent an infinite loop.  After the page were reloaded, a JSON payload would be constructed from the SpeedTrace data and sent via XHR to the URL http://nowhere.com/ after five seconds.  A property named 'foo' with the value 'bar' would be added to the header of the payload.

## API ##

Adding the `<script>` tag mentioned above to your page makes the following API available to your application.

  * speedtracer.clearData();
> > Clears out data from previous monitoring sessions.  useful after re-running a test on the same page.
  * speedtracer.startMonitoring({name:value, ...}, function callback);
> > Tells the browser to start sending monitoring data.
    * Valid properties for options:
      * clearData: clears any previously recorded timeline data if true
      * reload: a url to load in this tab after turning monitoring on
    * callback: optional function to be called after monitoring has been enabled
  * speedtracer.stopMonitoring(function callback);
> > Stops sending monitoring data.
    * callback: optional function to be called after monitoring has been disabled
  * speedTracer.getDump(function callback);
> > Returns the dump as a string.
    * callback(String data): optional function to be invoked when the data is ready.
  * speedtracer.sendDump(String url, Object header, function callback);
> > Sends the data to the specified URL using XHR.
    * url:  The URL to send the dump to. Cross site is OK.
    * header: An object that will be added as the 'header' property to the dump.
    * callback: optional function to be invoked when the transmission is completed.

## Headless Speed Tracer Dump Format ##

The format of the JSON structure emitted by the getDump() and sendDump() API methods is as follows:

```

{
  header: {
    timeStamp:<time in ms since 1970>,
    name:<name or "">,
    revision:<revision as a string or "">,
    ... custom header properties ...
  },
  data: {
    [ {... speedtrace record ...},
      {... speedtrace record ...},
      ... 
    ]
  }
}

```

The 'header' field is intended to assist in supplying metadata to automated testing datastores.

See the [Data Dump Format](http://code.google.com/webtoolkit/speedtracer/data-dump-format.html) for the speedtrace record definition details.


# Using the Sample Page Startup Latency Dashboard #

A sample [Google App Engine](http://code.google.com/appengine/) app is included in the source repository under the directory `samples/LatencyDashboard/`.  To run the demo, import the LatencyDashboard project into Eclipse, along with the speedtracr-api project under `src/api`.

The demo includes two sample dashboards:

  * **StartupLatencyDashboard** measures the page start and calculates timings related to page load.  This dashboard should be applicable to any app, and has special support for [GWT](http://code.google.com/webtoolkit) applications.
  * **MarkTimelineLatencyDashboard** measures the time between two `console.markTimeline()` calls.  This dashboard would require customization for the app you want to measure.

## StartupLatencyDashboard Demo ##

The StartupLatencyDashboard can be used unaltered to chart the startup latency of any web app.  This section will guide you through configuring an application to send data to the StartupLatencyDashboard demo which keeps track of the startup latency of an application.

First, you will need to install the Headless Speed Tracer extension in an instance of Chrome as described above.

Next, instrument an application to enable the headless Speed Tracer API.  For this example, I chose the GWT Showcase sample application.  All you need to do is add a `<script>` tag in the head of the document as explained above.

```
  <head>
   <script language="javascript" src="chrome-extension://jolleknjmmglebfldiogepklbacoohni/headless_api.js"></script>
   ...
```

Create a Web Application launch configuration in Eclipse for the StartupLatencyDashboard GWT module and start the app in Development Mode, then navigate to your annotated application with the special Speed Tracer query string arguments:

```
  http://www.example.com/Showcase/Showcase.html?SpeedTracer=monitor,xhr(http://127.0.0.1:8888/speedtracereceiver),timeout(5000),header(name:Showcase),header(revision:r123)
```

Note that the name and revision fields above are there for your use.  The revision number is added to each datapoint along with the timestamp for display.  The name field could be used to create multiple dashboards for different a pplications (this is not implemented by the demo.)

When you enter this URL into the browser, the page will immediately reload, removing the 'monitor' argument from the query string.  After 5 second timeout expires, you should see some debugging output on the eclipse debug console indicating the data has been received by the dashboard servlet.

Revisit the URL a few times to generate some datapoints.  Finally, visit the StartupLatencyDashboard app page using the Dev Mode browser:

```
  http://127.0.0.1:8888/StartupLatencyDashboard.html?gwt.codesvr=127.0.0.1:9997
```

You should see a display that looks similar to the following:


> ![http://speedtracer.googlecode.com/svn/wiki/HeadlessExtension-1.png](http://speedtracer.googlecode.com/svn/wiki/HeadlessExtension-1.png)


## MarkTimelineLatencyDashboard Demo ##

This demo will not work without customization for your particular application. It was designed to work with a particular application that was annotated with calls to `console.markTimeline()`.  These markTimeline messages start with a common prefix that are analyzed by the dashboard server.  The resulting timings are extracted and stored in the App Engine datastore.

Here is an outline of what was done on the client side to make this demo work.  First, you need to add the headless extension API reference to the head of the document, just as described in the StartupLatencyDashboard demo above:

```
  <head>
   <script language="javascript" src="chrome-extension://jolleknjmmglebfldiogepklbacoohni/headless_api.js"></script>
   ...
```

The next step is to create some functions to help recording the event data from the client.  For the demo, the `name` describes the high level operating being measured. The `eventName` parameter is a descriptive name for what step in performing the operation has been reached.  All operations begin with a `start` eventName.  Some may have multiple phases before they finish.

```
function markStart(name) {
  markElapsed(name, "start");
}

function markElapsed(name, eventName) {
 mark(JSON.stringify({measurementSet:name,event:eventName});
}

function mark(msg) {
  if(console && console.markTimeline)
     console.markTimeline("__stats_event" + msg);
}
```

Next, find places in your code to add calls to the `markStart()` and `markElapsed()` calls.  In our example app, there were api calls to create entities on the screen and simulate user actions on the entities.


The way the LatencyDashboard server demo code is written, you will have to modify the processCustomForDashboard method in SpeedTracerReceiverServlet.java to add your custom data points to the datastore, and you will also need to modify MarkTimelineLatencyDashboard.java to create corresponding charts.

Create some kind of testing infrastructure to exercise the annotated code.  You might simply have a manual procedure, some special query string on the URL to exercise the annotated code on startup, or you may use a tool like WebDriver or Selenium to automate a set of actions.

Finally, you will need to find a way to get the Speed Tracer dump to the dashboard server.  You can either use the query string method described in the StartupLatencyDashboard above, or you can annotate your code using the headless API to forward the dump only after all the operations are complete. The full API is described above, but the pieces you would use with the MarkTimelineLatencyDashboard are:

```
  // Call this method when you are ready to start the test.
  speedtracer.startMonitoring(null, function() {
    appAction();
  });


  function appAction() {
    ... perform code to be measured ...

    speedTracer.sendDump("http://localhost:8888/speedtracerreceiver/",{revision:'r123',name:'myApp'}, function() {
      speedTracer.stopMonitoring();
    });
  }
```

# Troubleshooting #

If you have any questions about using the demo after working through the instructions above, please post them to the [Speed Tracer Users](http://groups.google.com/group/speedtracer) Google Group.