#!/bin/sh
# run_in_only.sh {rule filename} {test name}
#
# Runs the specified rule .js file against the data in {testname}.in 
# The hintlet data is written to the standard output.
#

BASEDIR=`dirname $0`/../../src/Debug
#TESTER=/home/zundel/apu-client/src/chrome/Debug/hintlet_tester.exe
TESTER=${BASEDIR}/hintlet_tester.exe
RULE=${1}
shift
TESTNAME=${1}
shift

$TESTER --basedir=${BASEDIR} --rule=${RULE} --input=${TESTNAME}.in  TESTNAME $@


