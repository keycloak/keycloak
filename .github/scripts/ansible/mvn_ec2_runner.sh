#!/usr/bin/env bash
set -e
cd $(dirname "${BASH_SOURCE[0]}")

if [[ "$RUNNER_DEBUG" == "1" ]]; then
  set -x
fi

REGION=$1
MVN_PARAMS=${@:2}

CLUSTER_NAME=${CLUSTER_NAME:-"keycloak_$(whoami)"}

ansible-playbook -i ${CLUSTER_NAME}_${REGION}_inventory.yml mvn.yml \
  -e "mvn_params=\"${MVN_PARAMS}\""
