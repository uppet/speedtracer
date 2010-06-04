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

# Simple HTTP server that writes contents of POST messages to /tmp
# (Ripped from knorton's symbol server python script)

import BaseHTTPServer
import cgi
import glob
import optparse
import os
import platform
import shutil
import sys

class Server(BaseHTTPServer.HTTPServer):
  def __init__(self, address, hostname):
    self.hostname = hostname
    self.port = address[1]
    BaseHTTPServer.HTTPServer.__init__(self, address, Handler)

class Handler(BaseHTTPServer.BaseHTTPRequestHandler):
  def __init__(self,a,b,c):
    self.counter = 0
    BaseHTTPServer.BaseHTTPRequestHandler.__init__(self,a,b,c)

  def do_POST(self):
    print "In do_POST"
    print "do_POST: about to parse header"
    print "got headers: %s" % (self.headers)
    
    ctype, pdict = cgi.parse_header(self.headers.getheader('Content-type'))
    print "do_POST: parsing Headers got content-type: %s" % (ctype)
    query="";
    if ctype != 'application/xml' and ctype != 'application/json':
      self.send_response(404)
      self.end_headers();
      return

    self.send_response(200)
    self.end_headers();

    print "do_POST: Getting data"
    postDataLength = self.headers.getheader('Content-length');
    postData = self.rfile.read(int(postDataLength));

    # pull the data out and save it to a file
    self.counter = self.counter + 1;
    outFile = open("/tmp%s-%d" % (self.path, self.counter), "w")
    outFile.write(postData);
    outFile.close();
    print "Finished do_POST"

def Main():
  '''A simple server that just listents to POST requests and writes the 
     first 1MB of data to a file in /tmp.  Used to test the headless 
     SpeedTracer sendDump() API method.
  '''
  parser = optparse.OptionParser()
  parser.add_option('--port', dest='port', type='int',
                    help='http port to use (default: 9030)', default=9030)
  parser.add_option('--hostname', dest='hostname',
                   help='hostname for web server (used in creating the symbols manifest)',
                   default=None, type='string')
  parser.add_option("--bind_address", dest="bind_address",
                    help='the address to pass to bind (default: localhost)',
                    default='localhost', type='string')
  options, args = parser.parse_args()

  if options.hostname:
    hostname = options.hostname
  elif options.bind_address:
    hostname = options.bind_address
  else:
    hostname = platform.uname()[1]

  httpd = Server(
      (options.bind_address, options.port),
      hostname)
  print "Created server for writing POST data to /tmp:"
  print "http://%s:%s" % (hostname, options.port)
  httpd.serve_forever()

if __name__ == "__main__":
  Main()
