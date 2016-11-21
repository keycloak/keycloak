#!/bin/bash

if [ $1 == "old" ]; then
    mvn test -B -f testsuite/integration
elif [ $1 == "group1" ]; then
    mvn test -B -f testsuite/integration-arquillian/tests/base -Dtest=org.keycloak.testsuite.a**.*Test &&
    mvn test -B -f testsuite/integration-arquillian/tests/base -Dtest=org.keycloak.testsuite.b**.*Test &&
    mvn test -B -f testsuite/integration-arquillian/tests/base -Dtest=org.keycloak.testsuite.c**.*Test
elif [ $1 == "group2" ]; then
    mvn test -B -f testsuite/integration-arquillian/tests/base -Dtest=org.keycloak.testsuite.d**.*Test &&
    mvn test -B -f testsuite/integration-arquillian/tests/base -Dtest=org.keycloak.testsuite.e**.*Test &&
    mvn test -B -f testsuite/integration-arquillian/tests/base -Dtest=org.keycloak.testsuite.f**.*Test &&
    mvn test -B -f testsuite/integration-arquillian/tests/base -Dtest=org.keycloak.testsuite.i**.*Test
elif [ $1 == "group3" ]; then
    mvn test -B -f testsuite/integration-arquillian/tests/base -Dtest=org.keycloak.testsuite.k**.*Test &&
    mvn test -B -f testsuite/integration-arquillian/tests/base -Dtest=org.keycloak.testsuite.m**.*Test &&
    mvn test -B -f testsuite/integration-arquillian/tests/base -Dtest=org.keycloak.testsuite.o**.*Test &&
    mvn test -B -f testsuite/integration-arquillian/tests/base -Dtest=org.keycloak.*Test
elif [ $1 == "adapter" ]; then
    mvn test -B -f testsuite/integration-arquillian/tests/other/adapters
fi

