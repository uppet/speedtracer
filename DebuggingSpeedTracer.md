This page describes some strategies for debugging the Speed Tracer UI.

## Prerequisites ##
  * [Eclipse 3.6](http://wiki.eclipse.org/Older_Versions_Of_Eclipse)
  * [Google Plugin for Eclipse](http://code.google.com/eclipse/docs/getting_started.html)
  * [Get the Speed Tracer code](BuildingSpeedTracer.md)

# Setting up Eclipse #

## Import the Projects ##
In this part we'll assume that you have checked out Speed Tracer at `~/src/speedtracer`.

1. In Eclipse, we first need to setup a GWT SDK for the Speed Tracer project. Go to **Eclipse > Preferences...** (Mac) or **Window > Settings**. Then navigate to **Google > Google Web Toolkit**.

![http://speedtracer.googlecode.com/svn/wiki/images/debug-sdk-a.png](http://speedtracer.googlecode.com/svn/wiki/images/debug-sdk-a.png)

2. Choose **Add...**. Browse to **`~/src/speedtracer/src/third_party/gwt`** for your "Installation Directory" and use **`SPEEDTRACER`** for your "Display name".

![http://speedtracer.googlecode.com/svn/wiki/images/debug-sdk-b.png](http://speedtracer.googlecode.com/svn/wiki/images/debug-sdk-b.png)

3. We now need to import the projects, select **File > Import...**

4. Select **Existing Projects into Workspace** then press **`Next`**

![http://speedtracer.googlecode.com/svn/wiki/images/debug-import-a.png](http://speedtracer.googlecode.com/svn/wiki/images/debug-import-a.png)

5. Select root directory and Browse to **`~/src/speedtracer/src`**. There are 3 projects and they should all be checked. Hit **Finish**
  * _speedtracer-api_ - Some APIs that we moved to a separate project.
  * _speedtracer-stools_ - Some tools needed as part of the build.
  * _speedtracer-ui_ - The code for the Speed Tracer client.

![http://speedtracer.googlecode.com/svn/wiki/images/debug-import-b.png](http://speedtracer.googlecode.com/svn/wiki/images/debug-import-b.png)

6. You should now have all the Speed Tracer source available to you in Eclipse without any reported errors.


# Debugging the UI in "Mock Mode" #

1. We need to create a new Debug Configuration. Select from the menu **Run > Debug Configurations...**.

2. Click on the **Web Application** item, then click the "New" button. ![http://speedtracer.googlecode.com/svn/wiki/images/debug-mock-new.png](http://speedtracer.googlecode.com/svn/wiki/images/debug-mock-new.png)

3. In the **Main** tab, fill in the following values:

![http://speedtracer.googlecode.com/svn/wiki/images/debug-mock-a.png](http://speedtracer.googlecode.com/svn/wiki/images/debug-mock-a.png)

4. Then in the **GWT** tab, remove all the "Available Modules" except for **MonitorDebug**

![http://speedtracer.googlecode.com/svn/wiki/images/debug-mock-b.png](http://speedtracer.googlecode.com/svn/wiki/images/debug-mock-b.png)

5. Click on **Debug** and the debugger will startup and wait for your browser to connect. Using either Safari or Chrome (Safari works best on Mac since
DevMode is faster), navigate to the following URL: http://127.0.0.1:8888/monitor.html?gwt.codesvr=127.0.0.1:9997&mock=true.

6. The Speed Tracer UI should load with no data. You can use one of several mock data sets for debugging. To select one, just choose one from the "Bug" menu.

![http://speedtracer.googlecode.com/svn/wiki/images/debug-mock-bug.png](http://speedtracer.googlecode.com/svn/wiki/images/debug-mock-bug.png)

7. The Java debugger is active, so you can set breakpoints in the Java code.


# Testing in Chrome #
coming soon