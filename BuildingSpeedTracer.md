# Getting Started with the Speed Tracer code #

This page describes how to build your own copy of Speed Tracer.

## Prerequisites ##
You're going to need the following things:
  * An installation of [ant](http://ant.apache.org/).
  * A [Java Runtime Environment (JRE)](http://www.oracle.com/technetwork/java/javase/downloads/index.html) (version >= 1.6)

Finally, if you are building on Microsoft Windows, make sure you have [cygwin](http://cygwin.com/) installed with the subversion package to perform the steps below as written.

## Building Speed Tracer for the first time ##

```
$ mkdir speedtracer
```
Create a root directory to hold the project source and all dependencies.  Note: Do not create this directory on a path that contains a space.


```
$ cd speedtracer
```
You can probably figure out what this one does.


```
$ svn co http://src.chromium.org/svn/trunk/tools/depot_tools
```
This gets a copy of [Chrome Depot Tools](http://www.chromium.org/developers/how-tos/depottools) which is used to automatically fetch dependencies.


```
$ depot_tools/gclient config http://speedtracer.googlecode.com/svn/trunk/src
```
If you are a committer, you should use the url **https**://speedtracer.googlecode.com/svn/trunk/src.


```
$ depot_tools/gclient sync
```
This step pulls in Speed Tracer source and its dependencies.


```
$ cd src/
$ ant
```
This will build everything you need.  By default, ant is configured to create a release build and a .crx file will be created in the `src/Release` directory.

To build a Debug build, all you need to do is set the `config` property in Ant:

```
  ant -Dconfig=Debug
```


## Debugging the UI ##

To develop the Monitor, developers typically use GWT's development mode against the MonitorDebug.gwt.xml module.  This setup is configured to bypass the Chrome Extension and loads up some pre-defined data (See MockModelGenerators.java)  It also turns on a debugging log window and sets the CSS styling to output human readable styles.

You can run the Monitor in GWT Development mode in either Safari or Chrome.  Because Speed Tracer is targeted at WebKit browsers and takes advantage of some HTML 5 and CSS 3 features that may not be present in Internet Explorer and Firefox, we do not recommend using those browsers for Speed Tracer development.

To bring up the Monitor, run use the `MonitorDebug.gwt.xml` module and remove the others.  Additionally, you will need to add the parameter 'mock=true' to the URL as follows:

```
  http://localhost:8888/monitor.html?mock=true&gwt.hosted=1.2.3.4:9997
```

## Troubleshooting ##

### Ant build fails with error, "java.io.IOException: ${chrome.path}: not found" ###

The build is currently setup for finding the default build of Google Chrome on Microsoft Windows.  If your ant build has problems resolving ${chrome.path}, find the path to the chrome binary on your system and pass it to ant:

```
$ ant -Dchrome.path=/usr/local/bin/chrome
```