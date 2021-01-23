#!/bin/bash

cd "$(dirname $0)"

EXIT_CODE=0
mvn clean
for I in `perl -ne 'print "$1\n" if (m,<id>([^<]+)</id>,)' pom.xml`; do
    echo "========"
    echo "======== Profile $I"
    echo "========"
    mvn test "-P$I" "$@"
    EXIT_CODE=$[$EXIT_CODE + $?]
    mv target/surefire-reports "target/surefire-reports-$I"
done

exit $EXIT_CODE
