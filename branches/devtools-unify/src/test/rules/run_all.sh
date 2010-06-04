#!/bin/bash
# run_all.sh - command line driver to run unit tests
#
# Runs all a test on all datatests in the current directory
# Relies on the script 'run_test.sh' being in the same directory as
# this script.
#

FAILURES=0

# run_test
#    scriptname -  name of javascript file containing the rule
#    data set prefix  - name of files ending with .in and .out
run_test ()
{
  `dirname $0`/run_test.sh $1 $2 
  if [ $? != 0 ] ; then
    FAILURES=$(( $FAILURES + 1));
  fi
}

# run the tests
for i in *.in; do
  RULE=`head -1 $i`
  FILE_NO_SUFFIX=`echo $i | sed -e s/\.in$//`
  if [ ! -f ${FILE_NO_SUFFIX}.out ] ; then
    echo "\t(Skipping ${FILE_NO_SUFFIX}.in - no ${FILE_NO_SUFFIX}.out found)"
    continue;
  fi
  echo "Testing ${RULE} with dataset ${FILE_NO_SUFFIX}"
  run_test ${RULE} ${FILE_NO_SUFFIX}
done


if [ $FAILURES != 0 ] ; then
  echo "FAILED: Found ${FAILURES} test failure(s)."
  exit 1;
fi

echo "All tests passed."


