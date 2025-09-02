#!/usr/bin/env bash
set -e

if [[ "$RUNNER_DEBUG" == "1" ]]; then
  set -x
fi

SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )
source ${SCRIPT_DIR}/azure_common.sh

requiredEnv AZURE_SUBSCRIPTION

echo "Deleting SQL server ${AZURE_NAME}-sqlsrv and resource group ${AZURE_RG}"
# Deleting the resource group will remove all resources including the server and database
az group delete --name ${AZURE_RG} --subscription ${AZURE_SUBSCRIPTION} --yes --no-wait || true
