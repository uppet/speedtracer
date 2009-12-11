# Copyright 2009 Google Inc.
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

# Fixes some common lint problems on files.  Works in place, just put the
# name of the file on the command line.

import os
import sys

def usage():
  """ print a usage message and exit """
  print """
usage: %s file1 [file2 ...]
  Fixes some common lint errors and re-writes the corrected file.
  Also runs 'dos2unix' and 'svn propset svn:eol-style LF' on each file.

  Line ends in whitespace.  [whitespace/end_of_line] [4]
  At least two spaces between code and comments.  [whitespace/comments] [2]
""" % (sys.argv[0])
  sys.exit(1)

def fixWhitespaceAtEOL(contents):
  index = 0
  for line in contents:
    offset = line.find("//");
    contents[index] = line.rstrip() + "\n";
    index = index + 1

def fixSpaceAfterComment(contents):
  index = -1;
  for line in contents:
    index = index + 1
    offset = line.find("//");
    if offset < 0:
      # skip lines without // style comments
      continue
    url_offset = line.find("tp://")
    if (url_offset >= 0 and url_offset < offset):
      # likely a url - skip
      continue;
    if line[:offset].find('"') != -1:
      # Skip any line with a quote preceeding // just to be on the safe side
      continue
    if (line[offset + 2:offset + 3].isspace() == False):
      contents[index] = line[:offset + 2] + " " + line[offset + 2:]

def fixSpacesBeforeComments(contents):
  """ This one is tricky, if // is embedded in a quoted string, the
      automatic fix might make a mistake, so we are conservative and skip
      any lines containing a quote character.
  """  
  index = -1;
  for line in contents:
    index = index + 1
    offset = line.find("//");
    if offset < 1 or line[:offset].isspace():
      # skip lines without // style comments
      # skip lines with // at SOL preceeded only by whitespace
      continue
    url_offset = line.find("tp://")
    if (url_offset >= 0 and url_offset < offset):
      # likely a url - skip
      continue
    if line[:offset].find('"') != -1:
      # skip lines with quotes preceeding // so we don't 
      # accidentally substitute in a string
      continue
    if line[offset - 2:offset].isspace():
      # skip lines with at least 2 spaces in front
      continue
    if line[offset - 1:offset].isspace():
      # tricky case - sometimes the single whitespace char is a tab.  
      # lint will warn about that in a different way so we'll just leave
      # this line alone.
      if (line[offset - 1:offset] == "\t"):
        continue
      # insert 1 space
      line = line[:offset] + " " + line[offset:]
    else:
      # insert 2 spaces
      line = line[:offset] + "  " + line[offset:]
    contents[index] = line;

    
def lintPick(filename):
  # Load the file into memory
  ifile = open(filename, 'r');
  contents = [];
  for line in ifile:
    contents.append(line);
  ifile.close()

  # Make sure // is followed by at least one space.
  # this may inadvertently add whitespace at EOL that will be cleaned by
  # the next rule.
  fixSpaceAfterComment(contents)

  # Pull out the whitespaces at the end of line
  fixWhitespaceAtEOL(contents)

  # Fix problem where // comments need at least 2 spaces after code.'
  fixSpacesBeforeComments(contents)

  # re-write out the file
  ofile = open(filename, 'w');
  ofile.writelines(contents);

def Main(args):
  """The main."""
  # If no argument is passed, pick up the default mods file.
  if len(args) == 0:
    usage();

  for filename in args:
    lintPick(filename);
    os.system("dos2unix " + filename);
    os.system("svn propset svn:eol-style LF " + filename);

if __name__ == "__main__":
  Main(sys.argv[1:])