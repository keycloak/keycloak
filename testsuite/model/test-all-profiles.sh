#!/bin/bash

##
## To include test coverage data, use -Djacoco.skip=false parameter.
## This will gather the coverage data into target/jacoco.exec file and
## generate the coverage report for each module in keycloak/model/*/target/site.
##

cd "$(dirname $0)"
../../mvnw -version

EXIT_CODE=0
../../mvnw clean
for I in `perl -ne 'print "$1\n" if (m,<id>([^.<]+)</id>,)' pom.xml | grep -E -v '(report|copy-testsuite-providers-to-model-testsuite)'`; do
    echo "========"
    echo "======== Start of Profile $I"
    echo "========"
    ../../mvnw -B -Dsurefire.timeout=900 test "-P$I" "$@" 2>&1 | tee /tmp/surefire.out
    EXIT_CODE=$[$EXIT_CODE + ${PIPESTATUS[0]}]
    mv target/surefire-reports "target/surefire-reports-$I"
    perl -ne "print '::error::| $I | Timed out.' . \"\n\" if (/There was a timeout in the fork/)" /tmp/surefire.out
    echo "========"
    echo "======== End of Profile $I"
    echo "========"
done

## If the jacoco file is present, generate reports in each of the model projects
[ -f target/jacoco.exec ] && ../../mvnw -f ../../model org.jacoco:jacoco-maven-plugin:0.8.7:report -Djacoco.dataFile="$(readlink -f target/jacoco.exec)"

for I in `perl -ne 'print "$1\n" if (m,<id>([^<]+)</id>,)' pom.xml`; do
    grep -A 1 --no-filename '<<<' "target/surefire-reports-$I"/*.txt | perl -pe "print '::error::| $I | ';"
done

exit $EXIT_CODE
