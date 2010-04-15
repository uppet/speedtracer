#!/usr/bin/python2.4
#
# Copyright 2010 Google Inc. All Rights Reserved.
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

"""Driver for the Breaky Test.

This driver does a couple of things:
1) It runs a simple HTTP server to host the test content and receive the results
2) It starts chrome with the necessary options and kills it when the test is
over (unless run with --manual_mode)
"""

import BaseHTTPServer
import optparse
import os
import platform
import SimpleHTTPServer
import subprocess
import sys
import tempfile
import threading


BASE = "/breaky"
VALID = "%s/valid" % BASE
INVALID = "%s/invalid" % BASE

# Globals to communicate between server and handler
_keep_going = True
_exit_code = 0


class BreakyHandler(SimpleHTTPServer.SimpleHTTPRequestHandler):
  """Handle the results of the test."""

  def do_POST(self):
    """Handle a POST to the server.

    The client will POST results when the test is over.

    POST to /valid indicates success
    POST to /invalid indicates failure

    We must use globals to communicate with the outer server since
    SimpleHTTPServer is not designed for stateful interaction.
    """

    global _keep_going
    global _exit_code

    print "========= Got a POST ========="

    if self.path == VALID:
      self.send_response(200)
      print "valid"
      body = "Yay"
      _exit_code = 0
    elif self.path == INVALID:
      self.send_response(200)
      length = int(self.headers.getheader("content-length"))
      error = self.rfile.read(length)
      print "Error: %s" % error
      body = "Sorry to hear that"
      _exit_code = 1
    else:
      self.send_response(404)
      body = "WTF? %s (wanted %s or %s)" % (self.path, VALID, INVALID)
      print body
      _exit_code = 1

    self.send_header("Content-Type", "text/plain")
    self.send_header("Content-Length", len(body))
    self.send_header("Expires", "-1")
    self.send_header("Cache-Control", "no-cache")
    self.send_header("Pragma", "no-cache")
    self.end_headers()

    self.wfile.write(body)
    _keep_going = False


class ChromeRunner(object):
  """Launches chrome in a background thread for the test.

  We must use a temporary user data dir to avoid pollution from previous runs
  or the installed chrome.
  """

  def __init__(self, chrome_path, headless_path, url):
    """Setup a ChromRunner. Does not actually run until Start() is called.

    Args:
      chrome_path: path to the chrome executable
      headless_path: path to the unpacked headless extension
      url: URL to point chrome at for the test
    """

    self.chrome_path = chrome_path
    self.headless_path = headless_path
    self.url = url
    self.thread = threading.Timer(1, self._Run)
    self.user_data_dir = None
    self.chrome_process = None

  def _Run(self):
    """Run the actual chrome process. (Called from a thread)."""

    print "User data dir is %s" % self.user_data_dir
    chrome_args = [self.chrome_path,
                   "--enable-extension-timeline-api",
                   "--no-first-run",
                   "--user-data-dir=%s" % self.user_data_dir,
                   "--load-extension=%s" % self.headless_path,
                   self.url]
    self.chrome_process = subprocess.Popen(chrome_args)

  def Start(self):
    """Launch Chrome."""

    #Make a temp user-data-dir so that we don"t get polluted by any history
    self.user_data_dir = tempfile.mkdtemp()
    self.thread.start()

  def Stop(self):
    """Kill Chrome!"""

    if platform.system() == "Linux" or platform.system() == "Darwin":
      os.kill(self.chrome_process.pid, 9)
    elif platform.system() == "Windows":
      print "Trying to kill chrome PID %s" % (str(self.chrome_process.pid))
      import win32api
      win32api.TerminateProcess(self.chrome_process._handle, -1)
    self.thread.join()


def main():
  parser = optparse.OptionParser()
  parser.add_option("--port", dest="port", type="int",
                    help="http port to use (default: 9033)", default=9033)
  parser.add_option("--hostname", dest="hostname",
                    help="hostname for web server",
                    default=None, type="string")
  parser.add_option("--bind_address", dest="bind_address",
                    help="the address to pass to bind (default: localhost)",
                    default="localhost", type="string")
  parser.add_option("--chrome_path", dest="chrome_path",
                    help="the path to launch chrome with",
                    default="/opt/google/chrome/chrome")
  parser.add_option("--headless_path", dest="headless_path",
                    help="the path to the headless extension",
                    default="speedtracerheadless")
  parser.add_option("--manual_mode", action="store_true", dest="manual_mode",
                    help="Run the server forever, let the user launch the test",
                    default=False)
  options, args = parser.parse_args()

  if options.hostname:
    hostname = options.hostname
  elif options.bind_address:
    hostname = options.bind_address
  else:
    hostname = platform.uname()[1]

  httpd = BaseHTTPServer.HTTPServer((options.bind_address, options.port),
                                    BreakyHandler)
  breaky_url = "http://%s:%s/breaky.html" % (hostname, options.port)
  if options.manual_mode:
    print "Manual Mode. Point chrome at %s" % breaky_url
  else:
    c = ChromeRunner(os.path.abspath(options.chrome_path),
                     os.path.abspath(options.headless_path),
                     breaky_url)
    c.Start()
  while _keep_going or options.manual_mode:
    print "> handle_request"
    httpd.handle_request()

  c.Stop()
  sys.exit(_exit_code)

if __name__ == "__main__":
  main()
