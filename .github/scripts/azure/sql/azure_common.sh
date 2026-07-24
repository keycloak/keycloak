#!/usr/bin/env bash
set -e

if [[ "$RUNNER_DEBUG" == "1" ]]; then
  set -x
fi

function requiredEnv() {
  for ENV in $@; do
      if [ -z "${!ENV}" ]; then
        echo "${ENV} variable must be set"
        exit 1
      fi
  done
}

requiredEnv AZURE_NAME AZURE_REGION AZURE_ADMIN_PASSWORD AZURE_DB_PASSWORD

SCRIPT_DIR=${SCRIPT_DIR:-$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )}
export AZURE_RG=${AZURE_RG:-"${AZURE_NAME}-rg"}
export AZURE_ADMIN_USER=${AZURE_ADMIN_USER:-"sqladmin"}
export AZURE_ADMIN_PASSWORD=${AZURE_ADMIN_PASSWORD}
export AZURE_DB=${AZURE_DB:-"keycloakdb"}
export AZURE_DB_USER=${AZURE_DB_USER:-"keycloak"}
export AZURE_DB_PASSWORD=${AZURE_DB_PASSWORD}
export AZURE_ALLOW_ALL_FIREWALL=${AZURE_ALLOW_ALL_FIREWALL:-"true"}
