#!/bin/bash

. cico_setup.sh

function run_tests() {
  echo 'CICO: Run mv clean install -pl :keycloak-server-dist -am -Pdistribution'
  mvn clean install -pl :keycloak-server-dist -am -Pdistribution

  echo 'CICO: keycloak-server tests completed successfully!'
}

load_jenkins_vars;
install_deps;

run_tests;
