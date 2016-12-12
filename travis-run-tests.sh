#!/bin/bash -e

if [ $1 == "old" ]; then
    mvn test -B -f testsuite/integration
fi

if [ $1 == "group1" ]; then
    mvn test -B -f testsuite/integration-arquillian/tests/base -Dtest=org.keycloak.testsuite.ad*.**.*Test
fi

if [ $1 == "group2" ]; then
    mvn test -B -f testsuite/integration-arquillian/tests/base -Dtest=org.keycloak.testsuite.ac*.**.*Test
    mvn test -B -f testsuite/integration-arquillian/tests/base -Dtest=org.keycloak.testsuite.b*.**.*Test
    mvn test -B -f testsuite/integration-arquillian/tests/base -Dtest=org.keycloak.testsuite.cli*.**.*Test
    mvn test -B -f testsuite/integration-arquillian/tests/base -Dtest=org.keycloak.testsuite.com*.**.*Test
fi

if [ $1 == "group3" ]; then
    mvn test -B -f testsuite/integration-arquillian/tests/base -Dtest=org.keycloak.testsuite.d*.**.*Test
    mvn test -B -f testsuite/integration-arquillian/tests/base -Dtest=org.keycloak.testsuite.e*.**.*Test
    mvn test -B -f testsuite/integration-arquillian/tests/base -Dtest=org.keycloak.testsuite.f*.**.*Test
    mvn test -B -f testsuite/integration-arquillian/tests/base -Dtest=org.keycloak.testsuite.i*.**.*Test
fi

if [ $1 == "group4" ]; then
    mvn test -B -f testsuite/integration-arquillian/tests/base -Dtest=org.keycloak.testsuite.k*.**.*Test
    mvn test -B -f testsuite/integration-arquillian/tests/base -Dtest=org.keycloak.testsuite.m*.**.*Test
    mvn test -B -f testsuite/integration-arquillian/tests/base -Dtest=org.keycloak.testsuite.o*.**.*Test
    mvn test -B -f testsuite/integration-arquillian/tests/base -Dtest=org.keycloak.*Test
fi

if [ $1 == "adapter" ]; then
    mvn test -B -f testsuite/integration-arquillian/tests/other/adapters
    mvn test -B -f testsuite/jetty
    mvn test -B -f testsuite/tomcat6
    mvn test -B -f testsuite/tomcat7
    mvn test -B -f testsuite/tomcat8
fi

