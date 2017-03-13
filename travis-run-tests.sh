#!/bin/bash -e

if [ $1 == "old" ]; then
    mvn test -B --no-snapshot-updates -f testsuite/integration
    mvn test -B --no-snapshot-updates -f testsuite/jetty
    mvn test -B --no-snapshot-updates -f testsuite/tomcat6
    mvn test -B --no-snapshot-updates -f testsuite/tomcat7
    mvn test -B --no-snapshot-updates -f testsuite/tomcat8
fi

if [ $1 == "group1" ]; then
    cd testsuite/integration-arquillian/tests/base
    mvn test -B --no-snapshot-updates -Dtest=org.keycloak.testsuite.ad*.**.*Test
fi

if [ $1 == "group2" ]; then
    cd testsuite/integration-arquillian/tests/base
    mvn test -B --no-snapshot-updates -Dtest=org.keycloak.testsuite.ac*.**.*Test,org.keycloak.testsuite.b*.**.*Test,org.keycloak.testsuite.cli*.**.*Test,org.keycloak.testsuite.co*.**.*Test
fi

if [ $1 == "group3" ]; then
    cd testsuite/integration-arquillian/tests/base
    mvn test -B --no-snapshot-updates -Dtest=org.keycloak.testsuite.d*.**.*Test,org.keycloak.testsuite.e*.**.*Test,org.keycloak.testsuite.f*.**.*Test,org.keycloak.testsuite.i*.**.*Test
fi

if [ $1 == "group4" ]; then
    cd testsuite/integration-arquillian/tests/base
    mvn test -B --no-snapshot-updates -Dtest=org.keycloak.testsuite.k*.**.*Test,org.keycloak.testsuite.m*.**.*Test,org.keycloak.testsuite.o*.**.*Test,org.keycloak.testsuite.s*.**.*Test
fi

if [ $1 == "adapter" ]; then
    cd testsuite/integration-arquillian/tests/other/adapters
    mvn test -B --no-snapshot-updates 
fi

if [ $1 == "console" ]; then
    cd testsuite/integration-arquillian/tests/other/console
    mvn test -B --no-snapshot-updates 
fi

