#!/bin/bash

cd $(dirname $0)

RESULT=0

for i in $(ls src/test/java/org/keycloak/tests/ | grep -v "suites" | grep -v "utils"); do
  if (! cat src/test/java/org/keycloak/tests/suites/Base*TestSuite.java | grep $i >/dev/null); then
    echo "Package '$i' not defined in any base testsuites"
    RESULT=1
  fi
done

exit $RESULT