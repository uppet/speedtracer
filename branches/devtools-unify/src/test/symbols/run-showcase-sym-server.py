#!/usr/bin/python

# Copyright 2010 Google Inc.
# 
# Licensed under the Apache License, Version 2.0 (the "License"); you may not
# use this file except in compliance with the License. You may obtain a copy of
# the License at
# 
# http://www.apache.org/licenses/LICENSE-2.0
# 
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
# WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
# License for the specific language governing permissions and limitations under
# the License.

# This script runs a symbol server/ source server compatible with SpeedTracer
#
# This script has been configured to run in the GWT trunk directory and serve
# symbols and source for the Showcase sample.

import BaseHTTPServer
import glob
import optparse
import os
import platform
import shutil
import simplejson
import sys

class Server(BaseHTTPServer.HTTPServer):
  def __init__(self, address, hostname, enable_source_server=True):
    self.hostname = hostname
    self.port = address[1]
    self.source_server_enabled = enable_source_server
    BaseHTTPServer.HTTPServer.__init__(self, address, Handler)

class Handler(BaseHTTPServer.BaseHTTPRequestHandler):
  def _SendFile(self, path, type, binary = False):
    self.send_response(200)
    if binary:
      mode = 'rb'
    else:
      mode = 'r'
    f = open(path, mode)
    stat = os.fstat(f.fileno())
    self.send_header("Content-Type", type)
    self.send_header("Content-Length", str(stat[6]))
    self.end_headers()
    try:
      shutil.copyfileobj(f, self.wfile)
    finally:
      f.close()
    
  def _HandleMapRequest(self):
    server = self.server
    base_url = "http://%s:%d" % (server.hostname, server.port)
    self.send_response(200)
    self.send_header("Content-Type", "application/json")
    self.end_headers()
    simplejson.dump(CollectManifestInfo(base_url), self.wfile, indent=4)

  def _HandleSymRequest(self):
    # TODO(knorton): This assumes it's handling a '/sym' path.
    map_file = self.path[4:]
    for f in CollectMapFiles():
      if f.endswith(map_file):
        self._SendFile(f, 'text/plain')
        return
    self.send_error(404)

  def _HandleSrcRequest(self):
    evil_path = self.path[5:]

    # TODO(knorton): Move source search path out.
    search_paths = ['samples/showcase/src', 'dev/core/src', 'user/src']
    for path in search_paths:
      root = os.path.abspath(path)
      path = os.path.abspath(os.path.join(path, evil_path))
      sys.stderr.write("looking for soure file  on path: %s" % path)
      # is this path non-evil?
      if os.path.commonprefix([root, path]) == root:
        if os.path.isfile(path):
          self._SendFile(path, "text/plain")
          return
    self.send_error(404)

  def _HandleFunnyRequest(self):
    self.send_response(200)
    self.send_header("Content-Type", "text/html")
    self.end_headers()
    wfile = self.wfile
    text = "i'm ok. thanks for asking."
    wfile.write("<html><head><title>%s</title></head>" % text)
    wfile.write("<body><h1 style=\"font-size:72pt;\">%s</h1></body></html>" % text)

  def do_GET(self):
    path = self.path
    source_server_enabled = self.server.source_server_enabled
    if path in ['/map', '/symbols.json', '/symbolManifest.json']:
      self._HandleMapRequest()
    elif path.startswith("/sym"):
      self._HandleSymRequest()
    elif source_server_enabled and path.startswith("/src"):
      self._HandleSrcRequest()
    elif path.startswith("/healthz"):
      self._HandleFunnyRequest()
    else:
      self.send_response(404)
  
BASE_DIR = "build/out/samples/Showcase/war/showcase/"

def GetMapDir():
 return os.path.join(BASE_DIR, "symbolMaps")

def GetPubDir():
  return BASE_DIR;

def CollectMapFiles():
  # sys.stderr.write("looking for map files in %s\n" % GetMapDir());
  result = []
  result.extend(glob.glob("%s/*.compactSymbolMap" % GetMapDir()))
  result.extend(glob.glob("%s/*.symbolMap" % GetMapDir()))
  # sys.stderr.write("found:\t\n%s\n" % result);
  return result

def CollectPubFiles():
  result = glob.glob("%s/*.cache.html" % GetPubDir())
  # sys.stderr.write("found:\t\n%s\n" % result);
  return result

def CollectManifestInfo(base_url):
  class Permutation:
    def __init__(self, id, map, resources):
      self.id = id
      self.map = map
      self.resources = resources

  map_files = CollectMapFiles()
  pub_files = CollectPubFiles()

  # TODO(knorton): Not sure why I felt compelled to create a Permutation class,
  # I think I thought I was doing something different when I started.
  permutations = []
  for map in map_files:
    map = os.path.basename(map)
    id = map[0:32]
    resources = []
    for res in pub_files:
      base = os.path.basename(res)
      if base.startswith(id):
        resources.append(base)
    permutations.append(Permutation(id, map, resources))

  resources = {}
  for p in permutations:
    for r in p.resources:
      resources['showcase/%s' % r] = {
        # resources['/showcase/%s' % r] = {
        'symbols' : 'sym/%s' % p.map,
        'sourceServer' : '%s/src' % base_url,
        'type' : 'gwt',
      }
  return resources

def HasValidWorkingDir():
  cwd = os.getcwd()
  if not cwd.endswith('trunk'):
    sys.stderr.write("cwd() not trunk");
    return False
  if not all([os.path.exists(f) for f in ['samples', 'eclipse', 'build']]):
    sys.stderr.write("expected to find samples, eclipse, and build dirs.  did you build with ant?");
    return False
  return True

# TODO(knorton): source serving from jars!! (like the GWT jar)
def Main():
  parser = optparse.OptionParser()
  parser.add_option('--port', dest='port', type='int',
                    help='http port to use (default: 9020)', default=9020)
  parser.add_option('--hostname', dest='hostname',
                   help='hostname for web server (used in creating the symbols manifest)',
                   default=None, type='string')
  parser.add_option("--bind_address", dest="bind_address",
                    help='the address to pass to bind (default: localhost)',
                    default='localhost', type='string')
  parser.add_option("--disable-source-server", dest="disable_source_server",
                   help='turn off the source server (default: False)',
                   action='store_true', default=False)
  options, args = parser.parse_args()

  if options.hostname:
    hostname = options.hostname
  elif options.bind_address:
    hostname = options.bind_address
  else:
    hostname = platform.uname()[1]

  if not HasValidWorkingDir():
    sys.stderr.write("Please run from GWT trunk\n")
    sys.exit(1)

  if not os.path.isdir(GetMapDir()):
    sys.stderr.write("You're missing the directory for symbols maps.\n")
    sys.stderr.write("expected: %s" % GetMapDir())
    sys.exit(1)

  if not os.path.isdir(GetPubDir()):
    sys.stderr.write("You're missing the directory for client output.\n")
    sys.stderr.write("expected: %s" % GetPubDir())
    sys.exit(1)

  httpd = Server(
      (options.bind_address, options.port),
      hostname,
      not options.disable_source_server)
  print "Shazam! You have a server. Manifest is at:"
  print "http://%s:%s/symbols.json" % (hostname, options.port)
  httpd.serve_forever()

if __name__ == "__main__":
  Main()
