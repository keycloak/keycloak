#!/bin/bash
DIRNAME=`dirname "$0"`
GATLING_HOME=$DIRNAME/tests

java -cp $GATLING_HOME/target/classes org.keycloak.performance.log.LogProcessor "$@"