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

"""
TODO: document me
"""

import optparse
import os
import subprocess
import sys
import tempfile
import threading
import BaseHTTPServer
import SimpleHTTPServer
import shutil
import platform

BASE = "/breaky"
VALID = "%s/valid" % BASE
INVALID = "%s/invalid" % BASE
keepGoing = True
exitCode = 0

class BreakyHandler(SimpleHTTPServer.SimpleHTTPRequestHandler):
  def do_POST(self):
    global keepGoing
    global exitCode
    print "========= Got a POST ========="

    if self.path == VALID:
      self.send_response(200)
      print "valid"
      body = "Yay"
      exitCode = 0
    elif self.path == INVALID:
      self.send_response(200)
      length = int(self.headers.getheader('content-length'))
      error = self.rfile.read(length)
      print "Error: %s" % error
      body = "Sorry to hear that"
      exitCode = 1
    else:
      self.send_response(404)
      body = "WTF? %s (wanted %s or %s)" % (self.path, VALID, INVALID)
      print body
      exitCode = 1

    self.send_header('Content-Type', 'text/plain')
    self.send_header('Content-Length', len(body))
    self.send_header('Expires', '-1')
    self.send_header('Cache-Control', 'no-cache')
    self.send_header('Pragma', 'no-cache')
    self.end_headers()

    self.wfile.write(body)
    keepGoing = False

class ChromeRunner:
  def __init__(self, chrome_path, headless_path, url):
    self.chrome_path = chrome_path
    self.headless_path = headless_path
    self.url = url
    self.thread = threading.Timer(1, self._run)
    self.user_data_dir = None
    self.chrome_process = None

  def _run(self):

    print "User data dir is %s" % self.user_data_dir
    if(True):
      self.chrome_process = subprocess.Popen([self.chrome_path,
                             "--enable-extension-timeline-api",
                             "--no-first-run",
                             "--user-data-dir=%s" % self.user_data_dir,
                             "--load-extension=%s" % self.headless_path,
                             self.url])

  def start(self):
    #Make a temp user-data-dir so that we don't get polluted by any history
    self.user_data_dir = tempfile.mkdtemp()
    self.thread.start()

  def stop(self):
    #Python 2.5 does not support kill under Windows
    if(platform.system() == "Linux"):
      os.kill(self.chrome_process.pid, 9)
    elif(platform.system() == "Windows"):
      print "Trying to kill chrome PID %s" % (str(self.chrome_process.pid))
      import win32api
      win32api.TerminateProcess(self.chrome_process._handle)
    self.thread.join()
    #the TerminateProcess call is not cleaning up the file handles, so this
    #always fails
    #if self.user_data_dir:
    #  shutil.rmtree(self.user_data_dir)

    
def Main():
  parser = optparse.OptionParser()
  parser.add_option('--port', dest='port', type='int',
                    help='http port to use (default: 9030)', default=9030)
  parser.add_option('--hostname', dest='hostname',
                   help='hostname for web server',
                   default=None, type='string')
  parser.add_option("--bind_address", dest="bind_address",
                    help='the address to pass to bind (default: localhost)',
                    default='localhost', type='string')
  parser.add_option("--chrome_path", dest='chrome_path',
                    help='the path to launch chrome with',
                    default='/opt/google/chrome/chrome')
  parser.add_option("--headless_path", dest='headless_path',
                    help='the path to the headless extension',
                    default='speedtracerheadless')
  parser.add_option("--manual_mode", action="store_true", dest='manual_mode',
                    help="Run the server forever, let the user launch the test",
                    default=False)
  options, args = parser.parse_args()

  if options.hostname:
    hostname = options.hostname
  elif options.bind_address:
    hostname = options.bind_address
  else:
    hostname = platform.uname()[1]

  httpd = BaseHTTPServer.HTTPServer((options.bind_address, options.port), BreakyHandler)
  breakyURL = "http://%s:%s/breaky.html" % (hostname, options.port )
  if options.manual_mode:
    print "Manual Mode. Point chrome at %s" % breakyURL
  else:
    c = ChromeRunner(os.path.abspath(options.chrome_path),
                     os.path.abspath(options.headless_path),
                     breakyURL)
    c.start()
  while(keepGoing or options.manual_mode):
    print "> handle_request"
    httpd.handle_request()

  c.stop()
  sys.exit(exitCode)

if __name__ == "__main__":
  Main()
