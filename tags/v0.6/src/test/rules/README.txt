apu/test/rules

This directory contains data sets used by hintlet_rules_unittest.cc

Each unit tests dataset is composed of 2 files:
 <dataset>.in - APU input records for the unit test.  The hintlet rule 
   JavaScript filename must be the only thing on the first line of the file.
   The unit test engine only considers lines that start with a '{' character
   to be valid data records.
 <dataset>.out - Hintlet record output to compare the results to.


Description of scripts:

  run_all.sh 
    A shell that runs a bash command line version of the unit test.
    It finds all the datasets in the current directory and runs the
    command line test program.

  run_in_only.sh <rule filename> <dataset> [--debug]
    Runs hintlet_tester.exe with a single rule against
    the specified dataset input file.  The hintlet data is written 
    to the standard output.  Useful for debugging rules with the --debug
    flag.

  run_test.sh <rule filename> <dataset> 
    Runs hintlet_tester.exe and compares the data to the previously saved
    output data in the dataset.  Called from run_all.sh