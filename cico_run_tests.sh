#!/bin/bash

. cico_setup.sh

function run_tests() {
  echo 'CICO: Run mv clean install -Pdistribution'
  mvn clean install -Pdistribution

  echo 'CICO: keycloak-server tests completed successfully!'
}

load_jenkins_vars;
install_deps;

run_tests;
