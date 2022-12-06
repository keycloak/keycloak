#!/bin/bash -e

DIR=`readlink -f $0 | xargs dirname`
cd $DIR

TESTSUITE_FILE="database-suite"

TESTS=`cat $TESTSUITE_FILE`

echo $TESTS | sed 's/ /,/g'