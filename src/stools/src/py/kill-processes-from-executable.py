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

import ctypes
import optparse
import os
import subprocess

def NormalizePath(path):
  """Formats paths consistently so they can be compared"""
  return os.path.normcase(os.path.abspath(path))

def KillTask(pid):
  """Use taskkill to kill a process tree"""
  subprocess.call(["taskkill",
                   "/F",
                   "/T",
                   "/PID",
                   str(pid)])

def GetModuleFileName(pid):
  """Get the filename for a process using GetModuleFileNameEx"""
  PROCESS_QUERY_INFORMATION = 0x400
  PROCESS_VM_READ = 0x0010

  kernel = ctypes.windll.kernel32
  psapi = ctypes.windll.psapi

  name = ctypes.c_buffer(255)
  module = ctypes.c_ulong()
  count = ctypes.c_ulong()

  process = kernel.OpenProcess(PROCESS_QUERY_INFORMATION | PROCESS_VM_READ,
                               False, pid)
  # We can expect to not be able to open every process.
  if not process:
    return None

  # Get a handle to the first module.
  psapi.EnumProcessModules(process, ctypes.byref(module), ctypes.sizeof(module),
                           ctypes.byref(count))
  # Get the module file name.
  psapi.GetModuleFileNameExA(process, module.value, name, ctypes.sizeof(name))
  # Convert the result into a decent string.
  result = "".join([x for x in name if x != '\x00'])

  kernel.CloseHandle(process)

  return result

def GetProcessIdsForPath(path):
  """Returns all the process ids for a particular executable"""
  psapi = ctypes.windll.psapi

  results = []

  arr = ctypes.c_ulong * 256
  processes = arr()
  sizeOfProcesses = ctypes.sizeof(processes)
  sizeNeeded = ctypes.c_long()

  psapi.EnumProcesses(ctypes.byref(processes), sizeOfProcesses,
                      ctypes.byref(sizeNeeded))
  nProcesses = sizeNeeded.value / ctypes.sizeof(ctypes.c_ulong())
  pids = [x for x in processes][:nProcesses]
  for pid in pids:
    filename = GetModuleFileName(pid)
    if filename is None:
      continue
    if path == NormalizePath(filename):
      results.append(pid)
  return results
  
def Main():
  """The one"""
  parser = optparse.OptionParser(usage = "%prog path")
  options, args = parser.parse_args()
  if len(args) != 1:
    parser.print_usage()
  path = NormalizePath(args[0])
  pids = GetProcessIdsForPath(path)
  while len(pids) > 0:
    # The first pid should be the parent for many other pids.
    KillTask(pids[0])
    pids = GetProcessIdsForPath(path)

if __name__ == '__main__':
  Main()
