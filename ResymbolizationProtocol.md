# Introduction #

This is a quick introduction on how to get Speed Tracer to de-obfuscate JavaScript and display the original symbol names for function boundaries available via the displayed stack traces for selected nodes in the event trace tree.

This document does **not** document the IDE interaction protocol. That is still in the oven baking.

# Details #

Speed Tracer's resymbolization protocol is general in that it can be extended to cover pretty much any symbol map format. However it currently only supports variations on the GWT symbol map format. If you would like to enable support for some other symbol map format, we would simple need to add a custom symbol map parser (see client/ui/src/com/google/speedtracer/client/GwtSymbolMapParserer.java for an example).

If Speed Tracer supports your symbol map format, the only thing you need to do to have resymbolization work for you is to generate a **symbol manifest** which is simply a JSON file that maps String corresponding to JavaScript resources that get served in production (e.g. the GWT compiler output) to information needed to re-symbolize the obfuscated symbols in that resource.

# The Symbol Manifest #

The symbol manifest is a simple JSON structure that maps keys corresponding to the names of the obfuscated JavaScript files you serve in production, to JSON objects (the re-symbolization information) that contain information about how to perform the de-obfuscation.

The keys are URLs for the obfuscated JavaScript that is serve up with your application. These URLs can either be full absolute URLs, origin relative URLs, or resource relative URLs. "Resource relative" simply means paths relative to where the application host page was served. For example, if you are profiling "http://foo.com/index.html", a resource relative path of "js/myJS.js" would resolve to "http://foo.com/js/myJS.js".

By default, Speed Tracer tries to look for a symbol manifest named **"symbolmanifest.json"** relative to each new page you visit in the browser. For example, if you navigate to "http://foo.com/index.html", Speed Tracer will attempt to fetch a manifest at "http://foo.com/symbolmanifest.json".

If you want to serve your manifest at a different location, simply press "CTRL+M" in Speed Tracer to bring up the manifest bindings panel. You can associate a manifest with the page you are profiling using this simple UI. Speed Tracer will remember this manifest for you for future profile runs.

The following is an example symbol manifest:

```
  { 
    "/path/relative/to/origin/resource.js": {
      "symbols": "relative/to/symbolManifest/resource.map",
      "sourceServer": "http://source:8080/mysource/",
      "type": "gwt"
    },
    "resource/relative/path/resource.js": {
      "symbols": "relative/to/symbolManifest/resource.map",
      "sourceServer": "http://source:8080/mysource/",
      "type": "gwt"
    },
    "http://localhost/myapp/myapp.nocache.js" : {
      "symbols": "relative/to/symbolManifest/resource.map",
      "sourceServer": "http://source:8080/",
      "type": "gwt"
    },
    "http://localhost/js/jquery.compressed.js" : {
      "symbols": "relative/to/symbolManifest/resource.map",
      "sourceServer": "http://code.google.com/",
      "type": "implement_your_own_symbol_map_parser"
    }
  }
```

The "symbols" entry in the re-symbolization information object is a URL that points to the symbol mapping file for that resource. It is currently specified **as a path that is resource relative to the symbol manifest**.

TODO: Make the symbol map path be interpreted similarly to the resource Key, that is, allow origin relative and absolute URLs.

We realize that for users of tools like GWT, building the symbol manifest should be automatable. For GWT users, we will post example code for generating the symbol manifest.

TODO: Post example symbol manifest servlet.

# Serving Source #

If there is no IDE found that implements our "SourceViewerServer" protocol (currently undocumented) then Speed Tracer will attempt to simply fetch the original java source and display it within Speed Tracer using the same source viewer used for looking at obfuscated source.

So how does this work? Simply start a web server that serves up your source code. Place an entry in each re-symbolization information object named "sourceServer" that points to where you are serving your source code.

For example:

```
  {
    ...
    "myProductionResourceName.js": {
      ...
      "sourceServer": "http://localhost:8888/path/to/mysource/classpathroot/"  
      ...
    }
    ...
  }
```

Unless your project is open source, you generally want to only serve your source over localhost, or restrict access via IP or cookie based auth.


# Generating symbol map files with GWT #

You need to compile your application with the "-extra" flag which takes in a directory name where your symbol map files will be output to.