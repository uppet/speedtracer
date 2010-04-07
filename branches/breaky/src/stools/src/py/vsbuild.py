# Copyright 2008 Google Inc.
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

import optparse
import os
import shutil
import subprocess
import sys
import tempfile
import time
import threading

__version__ = '0.0.1'

USAGE_TEXT = ("""usage: %prog [options] path.sln <command> project_name config\n\n"""
    """Executes a Visual Studio build process with console output.\n"""
    """Available commands:\n"""
    """  build\n"""
    """  clean\n""")

def DoTail(log, controller):
  """Performs the equivalent of the unix tail -f utility."""
  file = open(log.name)
  try:
    while (True):
      line = file.readline()
      if line == "":
        if controller['active']:
          time.sleep(1)
        else:
          return
      else:
        print line,
  finally:
    file.close()
    log.Dispose()

class BuildLog:
  """Encapsulates a temp path to use as a build log."""
  def __init__(self):
    self._directory = tempfile.mkdtemp()
    self.name = self._directory + os.sep + 'build.log'
    self._TouchFile(self.name)

  def _TouchFile(self, filename):
    open(filename, 'a').close()

  def Dispose(self):
    shutil.rmtree(self._directory)

def DoVsBuild(devenv_path, solution, command, project=None, config=None,
    log=None):
  """Executes a Visual Studio build."""
  cmd = [devenv_path, solution, '/Project', project]

  if not config:
    config = 'Release'
  if command == 'build':
    cmd += ['/Build', config]
  else:
    cmd += ['/Clean', config]

  if log:
    cmd += ['/Out', log.name]
  return subprocess.call(cmd)

def Main(argv):
  """Where it all begins."""
  options = optparse.OptionParser(usage=USAGE_TEXT,
                                  version=__version__)
  options.disable_interspersed_args()
  options.add_option("-d", "--devenv_path",
                     default="c:\\Program Files\\Microsoft Visual Studio 8\\"
                             "Common7\\IDE\\devenv.exe",
                     dest="devenv_path",
                     help="""Set path to Visual Studio (devenv.exe)""")
  opts, args = options.parse_args(argv[1:])

  # No args.
  if len(args) < 3:
    options.print_help()
    return 1

  solution, command, project = args[0:3]
  if len(args) == 4:
    config = args[3]
  else:
    config = None

  if not command in ['build', 'clean']:
    options.print_help()
    return 1

  build_log = BuildLog()

  # Controller is shared state between two threads, which is
  # generally bad ... except that python has a global lock,
  # so we can live dangerously and still feel safe.
  controller = { 'active' : True }
  threading.Thread(target=DoTail, args=[build_log, controller]).start()
  try:
    return DoVsBuild(opts.devenv_path, solution, command, project, config,
      build_log)
  finally:
    controller['active'] = False

if '__main__' == __name__:
  sys.exit(Main(sys.argv))
