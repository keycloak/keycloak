#!/bin/bash -e

find . -path '**/src/test/java' -type d \
  | grep -v -E '\./(docs|distribution|misc|operator|((.+/)?tests)|testsuite|test-framework|quarkus)/' \
  | sed 's|/src/test/java||' \
  | sed 's|./||' \
  | sort \
  | tr '\n' ',' \
  | sed 's/,$//'