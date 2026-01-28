#!/bin/bash -e

GROUP="$1"
if [ "$GROUP" == "" ]; then
  echo 'Usage: suite.sh <group>'
  exit
fi

cd "`readlink -f "$0" | xargs dirname`"

TEST_DIR="../src/test/java/"
SUITE_FILE="$GROUP-suite"

if [ ! -f "$SUITE_FILE" ]; then
  echo "$SUITE_FILE not found"
  exit 1
fi

SEP=""
TESTS=""
for i in `cat "$SUITE_FILE" | grep -v '^[[:space:]]*$' | grep -v '^[[:space:]]*#'`; do
  # Check test exists, ignoring checking packages for now
  if [[ "$i" != *'.'* ]]; then
    SEARCH=`find "$TEST_DIR" -name "$i.java"`
    if [ "$SEARCH" == "" ]; then
      echo "$i not found in testsuite"
      exit 1
    fi
  fi

  TESTS="$TESTS$SEP$i"
  SEP=","
done

echo "$TESTS"