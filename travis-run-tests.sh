#!/bin/bash -e

function run-server-tests() {
    cd testsuite/integration-arquillian
    mvn install -B -nsu -Pauth-server-wildfly -DskipTests

    cd tests/base
    mvn test -B -nsu -Pauth-server-wildfly -Dtest=$1 2>&1 | java -cp ../../../utils/target/classes org.keycloak.testsuite.LogTrimmer
    exit ${PIPESTATUS[0]}
}

# The following lines are due to travis internals. See https://github.com/travis-ci/travis-ci/issues/6069#issuecomment-319710346
git config remote.origin.fetch "+refs/heads/*:refs/remotes/origin/*"
git fetch

function should-tests-run() {
    # If this is not a pull request, it is build as a branch update. In that case test everything
    [ "$TRAVIS_PULL_REQUEST" = "false" ] && return 0

    # Do not run tests for changes in documentation
    git diff --name-only HEAD origin/${TRAVIS_BRANCH} |
        egrep -iv '^misc/.*\.md$|^testsuite/.*\.md$'
}

## You can define a precondition for running a particular test group by defining function should-tests-run-<test-group-name>.
## Its return value determines whether the test group should run.

function should-tests-run-crossdc() {
    # If this is not a pull request, it is build as a branch update. In that case test everything
    [ "$TRAVIS_PULL_REQUEST" = "false" ] && return 0

    git diff --name-only HEAD origin/${TRAVIS_BRANCH} |
        egrep -i 'crossdc|infinispan'
}

if ! should-tests-run; then
    echo "Skipping all tests (including group '$1')"
    exit 0
fi

if declare -f "should-tests-run-$1" > /dev/null && ! eval "should-tests-run-$1"; then
    echo "Skipping group '$1'"
    exit 0
fi

mvn install -B -nsu -Pdistribution -DskipTests -Dorg.slf4j.simpleLogger.log.org.apache.maven.cli.transfer.Slf4jMavenTransferListener=warn

if [ $1 == "old" ]; then
    cd testsuite
    mvn test -B -nsu -f integration-deprecated
    mvn test -B -nsu -f jetty
    mvn test -B -nsu -f proxy
    mvn test -B -nsu -f tomcat6
    mvn test -B -nsu -f tomcat7
    mvn test -B -nsu -f tomcat8
fi

if [ $1 == "unit" ]; then
    mvn -B test -DskipTestsuite
fi

if [ $1 == "server-group1" ]; then
    run-server-tests org.keycloak.testsuite.ad*.**.*Test,!**/adapter/undertow/**/*Test
fi

if [ $1 == "server-group2" ]; then
    run-server-tests org.keycloak.testsuite.ac*.**.*Test,org.keycloak.testsuite.b*.**.*Test,org.keycloak.testsuite.cli*.**.*Test,org.keycloak.testsuite.co*.**.*Test
fi

if [ $1 == "server-group3" ]; then
    run-server-tests org.keycloak.testsuite.au*.**.*Test,org.keycloak.testsuite.d*.**.*Test,org.keycloak.testsuite.e*.**.*Test,org.keycloak.testsuite.f*.**.*Test,org.keycloak.testsuite.i*.**.*Test,org.keycloak.testsuite.p*.**.*Test
fi

if [ $1 == "server-group4" ]; then
    run-server-tests org.keycloak.testsuite.k*.**.*Test,org.keycloak.testsuite.m*.**.*Test,org.keycloak.testsuite.o*.**.*Test,org.keycloak.testsuite.s*.**.*Test
fi

if [ $1 == "crossdc" ]; then
    cd testsuite/integration-arquillian
    mvn install -B -nsu -Pauth-servers-crossdc-jboss,auth-server-wildfly,cache-server-infinispan -DskipTests

    cd tests/base
    mvn clean test -B -nsu -Pcache-server-infinispan,auth-servers-crossdc-jboss,auth-server-wildfly -Dtest=*.crossdc.**.* 2>&1 |
        java -cp ../../../utils/target/classes org.keycloak.testsuite.LogTrimmer
    BASE_TESTS_STATUS=${PIPESTATUS[0]}

    mvn clean test -B -nsu -Pcache-server-infinispan,auth-servers-crossdc-jboss,auth-server-wildfly -Dtest=*.crossdc.manual.* -Dmanual.mode=true 2>&1 |
        java -cp ../../../utils/target/classes org.keycloak.testsuite.LogTrimmer
    MANUAL_TESTS_STATUS=${PIPESTATUS[0]}

    echo "BASE_TESTS_STATUS=$BASE_TESTS_STATUS, MANUAL_TESTS_STATUS=$MANUAL_TESTS_STATUS";
    if [ $BASE_TESTS_STATUS -eq 0 -a $MANUAL_TESTS_STATUS -eq 0 ]; then
        exit 0;
    else
        exit 1;
    fi;

fi
