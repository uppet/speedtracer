#!/bin/sh
# run_test.sh {rule filename} {dataset name}
#
# Runs the specified rule .js file against the data in {dataset}.in and 
# compares it to the output in {dataset}.out
#

BASEDIR=`dirname $0`/../../src/Debug
RULE=${1}
TESTNAME=${2}

${BASEDIR}/hintlet_tester --basedir=${BASEDIR} --rule=${RULE} --input=${TESTNAME}.in --expected-output=${TESTNAME}.out --quiet --expected-exceptions=0 $@
if [ $? != 0 ] ; then 
  echo "Test ${TESTNAME} FAILED on ${RULE}"
  exit 1
fi
exit 0

