See the Speed Tracer Documentation [FAQ](http://code.google.com/webtoolkit/speedtracer/faq.html).

# What browsers does Speed Tracer support? #

Speed Tracer only supports the Chrome browser at this time.

# When installing, why do I see the message, "This extension can access: All data on your computer and the websites you visit"? #

Chrome Extensions are designed such that extension authors must declare the features they require so that they can minimize the API that needs to be exposed to the extension and tighten up security.

In order to save Speed Trace data to disk and load it back, Speed Tracer requests access to load and save `file://` urls.  Although this is the only data on the computer SpeedTracer attempt to read, this permission could be exploited by a evil intentioned extension to read and write arbitrary files from the disk, hence the warning message from Chrome on installation.

# After I start Speed Tracer for the first time, no data appears and I get a warning. #

First of all, make sure you are running a release of Chrome that supports Speed Tracer (dev channel) and you have added the `--enable-extension-timeline-api` flag.

If all that looks good, make sure all Chrome windows are closed and try restarting.

If that didn't work either, there may be a Chrome process still running without a visible window.  Use the Windows Task Manager or equivalent to find the process and stop it.