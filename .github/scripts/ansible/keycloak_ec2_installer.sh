#!/usr/bin/env bash
set -e
cd $(dirname "${BASH_SOURCE[0]}")

if [[ "$RUNNER_DEBUG" == "1" ]]; then
  set -x
fi

REGION=$1
CLUSTER_NAME=$2
KEYCLOAK_SRC=$3
MAVEN_ARCHIVE=$4

ansible-playbook -i ${CLUSTER_NAME}_${REGION}_inventory.yml keycloak.yml \
  -e "keycloak_src=\"${KEYCLOAK_SRC}\"" \
  -e "maven_archive=\"${MAVEN_ARCHIVE}\""
