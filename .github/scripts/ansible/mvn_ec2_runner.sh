#!/usr/bin/env bash
set -e
cd $(dirname "${BASH_SOURCE[0]}")

if [[ "$RUNNER_DEBUG" == "1" ]]; then
  set -x
fi

REGION=$1
CLUSTER_NAME=$2
MVN_PARAMS=${@:3}

echo "mvn_params=\"${MVN_PARAMS}\""

ansible-playbook -i ${CLUSTER_NAME}_${REGION}_inventory.yml mvn.yml \
  -e "mvn_params=\"${MVN_PARAMS}\""
