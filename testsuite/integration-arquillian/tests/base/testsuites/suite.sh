#!/bin/bash -e

DIR=`readlink -f $0 | xargs dirname`
GROUP="$1"
TEST_DIR="$DIR/../src/test/java/"
SUITE_FILE=$DIR/$GROUP-suite

if [ ! -f "$SUITE_FILE" ]; then
  echo "Suite '$1' not found"
  exit 1
fi

SEP=""
for i in `cat "$SUITE_FILE" | grep -v '^[[:space:]]*$' | grep -v '^[[:space:]]*#'`; do
  # Check test exists, ignoring checking packages for now
  if [[ "$i" != *'.'* ]]; then
    SEARCH=`find "$TEST_DIR" -name "$i.java"`
    if [ "$SEARCH" == "" ]; then
      echo "$i not found in testsuite"
      exit 1
    fi
  fi

  echo -n "$SEP$i"
  SEP=","
done