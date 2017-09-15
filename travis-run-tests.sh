#!/bin/bash -e

function run-server-tests {
    cd testsuite/integration-arquillian
    mvn install -B -nsu -Pauth-server-wildfly -DskipTests

    cd tests/base
    mvn test -B -nsu -Pauth-server-wildfly -Dtest=$1 2>&1 | java -cp ../../../utils/target/classes org.keycloak.testsuite.LogTrimmer
    exit ${PIPESTATUS[0]}
}

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
    run-server-tests org.keycloak.testsuite.au*.**.*Test,org.keycloak.testsuite.d*.**.*Test,org.keycloak.testsuite.e*.**.*Test,org.keycloak.testsuite.f*.**.*Test,org.keycloak.testsuite.i*.**.*Test
fi

if [ $1 == "server-group4" ]; then
    run-server-tests org.keycloak.testsuite.k*.**.*Test,org.keycloak.testsuite.m*.**.*Test,org.keycloak.testsuite.o*.**.*Test,org.keycloak.testsuite.s*.**.*Test
fi
