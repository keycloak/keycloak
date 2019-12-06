#!/bin/bash -e

function run-server-tests() {
    cd testsuite/integration-arquillian
    mvn -s $TRAVIS_BUILD_DIR/maven-settings.xml install -B -nsu -Pauth-server-wildfly -DskipTests

    cd tests/base
    mvn test -B -nsu -Pauth-server-wildfly "-Dtest=$1" $2 2>&1 | java -cp ../../../utils/target/classes org.keycloak.testsuite.LogTrimmer
    exit ${PIPESTATUS[0]}
}

travis_fold start source_checkout
# The following lines are due to travis internals. See https://github.com/travis-ci/travis-ci/issues/6069#issuecomment-319710346
git config remote.origin.fetch "+refs/heads/*:refs/remotes/origin/*"
git fetch
travis_fold end source_checkout

function should-tests-run() {
    # If this is not a pull request, it is build as a branch update. In that case test everything
    [ "$TRAVIS_PULL_REQUEST" = "false" ] && return 0

    # Do not run tests for changes in documentation
    git diff --name-only HEAD origin/${TRAVIS_BRANCH} |
        egrep -iv '^misc/.*\.md$|^testsuite/.*\.md$'
}

## You can define a precondition for running a particular test group by defining function should-tests-run-<test-group-name>.
## Its return value determines whether the test group should run.

function should-tests-run-crossdc-server() {
    # If this is not a pull request, it is build as a branch update. In that case test everything
    [ "$TRAVIS_EVENT_TYPE" == "cron" ] && return 0

    git diff --name-only HEAD origin/${TRAVIS_BRANCH} |
        egrep -i 'crossdc|infinispan'
}

function should-tests-run-crossdc-adapter() {
    should-tests-run-crossdc-server
}

function should-tests-run-adapter-tests-authz() {
    [ "$TRAVIS_PULL_REQUEST" = "false" ] && return 0

    git diff --name-only HEAD origin/${TRAVIS_BRANCH} |
        egrep -i 'authz|authorization'
}

if ! should-tests-run; then
    echo "Skipping all tests (including group '$1')"
    exit 0
fi

if declare -f "should-tests-run-$1" > /dev/null && ! eval "should-tests-run-$1"; then
    echo "Skipping group '$1'"
    exit 0
fi

travis_fold start compile_keycloak
echo Compiling Keycloak
( while : ; do echo "Compiling, please wait..." ; sleep 50 ; done ) &
COMPILING_PID=$!
TMPFILE=`mktemp`
if ! mvn -s $TRAVIS_BUILD_DIR/maven-settings.xml install -B -nsu -Pdistribution -DskipTests -Dorg.slf4j.simpleLogger.log.org.apache.maven.cli.transfer.Slf4jMavenTransferListener=warn &> "$TMPFILE"; then
    cat "$TMPFILE"
    exit 1
fi
kill $COMPILING_PID
travis_fold end compile_keycloak

if [ $1 == "unit" ]; then
    mvn -B test -DskipTestsuite
    # Generate documentation to catch potential issues earlier than during the release
    mvn test -B -nsu -f services -Pjboss-release
fi

if [ $1 == "server-group1" ]; then
    run-server-tests org.keycloak.testsuite.adm*.**.*Test,org.keycloak.testsuite.add*.**.*Test
fi

if [ $1 == "server-group2" ]; then
    run-server-tests org.keycloak.testsuite.ac*.**.*Test,org.keycloak.testsuite.cli*.**.*Test,org.keycloak.testsuite.co*.**.*Test,org.keycloak.testsuite.j*.**.*Test
fi

if [ $1 == "server-group3" ]; then
    run-server-tests org.keycloak.testsuite.au*.**.*Test,org.keycloak.testsuite.d*.**.*Test,org.keycloak.testsuite.e*.**.*Test,org.keycloak.testsuite.f*.**.*Test,org.keycloak.testsuite.i*.**.*Test,org.keycloak.testsuite.p*.**.*Test
fi

if [ $1 == "server-group4" ]; then
    run-server-tests org.keycloak.testsuite.k*.**.*Test,org.keycloak.testsuite.m*.**.*Test,org.keycloak.testsuite.o*.**.*Test,org.keycloak.testsuite.s*.**.*Test,org.keycloak.testsuite.t*.**.*Test,org.keycloak.testsuite.u*.**.*Test
fi

if [ $1 == "adapter-tests" ]; then
    run-server-tests org.keycloak.testsuite.adapter.**.*Test,!org.keycloak.testsuite.adapter.**.authorization**.*Test
fi

if [ $1 == "adapter-tests-authz" ]; then
    run-server-tests org.keycloak.testsuite.adapter.**.authorization**.*Test
fi

if [ $1 == "crossdc-server" ]; then
    cd testsuite/integration-arquillian
    mvn -s $TRAVIS_BUILD_DIR/maven-settings.xml install -B -nsu -Pauth-servers-crossdc-jboss,auth-server-wildfly,cache-server-infinispan -DskipTests

    cd tests/base
    mvn clean test -B -nsu -Pcache-server-infinispan,auth-servers-crossdc-jboss,auth-server-wildfly -Dtest=org.keycloak.testsuite.crossdc.**.* 2>&1 |
        java -cp ../../../utils/target/classes org.keycloak.testsuite.LogTrimmer
    exit ${PIPESTATUS[0]}
fi

if [ $1 == "crossdc-adapter" ]; then
    cd testsuite/integration-arquillian
    mvn -s $TRAVIS_BUILD_DIR/maven-settings.xml install -B -nsu -Pauth-servers-crossdc-jboss,auth-server-wildfly,cache-server-infinispan,app-server-wildfly -DskipTests

    cd tests/base
    mvn clean test -B -nsu -Pcache-server-infinispan,auth-servers-crossdc-jboss,auth-server-wildfly,app-server-wildfly -Dtest=org.keycloak.testsuite.adapter.**.crossdc.**.* 2>&1 |
        java -cp ../../../utils/target/classes org.keycloak.testsuite.LogTrimmer
    exit ${PIPESTATUS[0]}
fi

if [ $1 == "broker" ]; then
    run-server-tests org.keycloak.testsuite.broker.**.*Test
fi
