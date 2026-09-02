#!/usr/bin/env bash
set -e

if [[ "$RUNNER_DEBUG" == "1" ]]; then
  set -x
fi

SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )
source ${SCRIPT_DIR}/azure_common.sh

requiredEnv AZURE_SUBSCRIPTION AZURE_ADMIN_USER AZURE_DB_USER

# Login is expected to be done by the workflow via az login with service principal

# Resource group (created via idempotent command)
echo "Creating resource group ${AZURE_RG} in ${AZURE_REGION}"
az group create --name ${AZURE_RG} --location ${AZURE_REGION} --subscription ${AZURE_SUBSCRIPTION}

echo "Creating SQL server ${AZURE_NAME}-sqlsrv"
az sql server create \
  --name ${AZURE_NAME}-sqlsrv \
  --resource-group ${AZURE_RG} \
  --location ${AZURE_REGION} \
  --admin-user ${AZURE_ADMIN_USER} \
  --admin-password "${AZURE_ADMIN_PASSWORD}" \
  --subscription ${AZURE_SUBSCRIPTION}

echo "Creating database ${AZURE_DB}"
az sql db create \
  --resource-group ${AZURE_RG} \
  --server ${AZURE_NAME}-sqlsrv \
  --name ${AZURE_DB} \
  --service-objective Basic \
  --subscription ${AZURE_SUBSCRIPTION}

if [ "${AZURE_ALLOW_ALL_FIREWALL}" = "true" ]; then
  echo "Allowing Azure services and current IP (0.0.0.0) - use with caution"
  az sql server firewall-rule create --resource-group ${AZURE_RG} --server ${AZURE_NAME}-sqlsrv --name AllowAll --start-ip-address 0.0.0.0 --end-ip-address 0.0.0.0 --subscription ${AZURE_SUBSCRIPTION}
fi

# Create login in master and then create user in the database
# Wait until server is provisioned
echo "Waiting for SQL server to be provisioned..."
for i in {1..30}; do
  state=$(az sql server show --name ${AZURE_NAME}-sqlsrv --resource-group ${AZURE_RG} --subscription ${AZURE_SUBSCRIPTION} --query "state" -o tsv || echo "")
  if [ "${state}" = "Ready" ] || [ -z "${state}" ]; then
    break
  fi
  echo "server state=${state}, retrying..."
  sleep 5
done

ENDPOINT="${AZURE_NAME}-sqlsrv.database.windows.net"
ADMIN_USER_FULL="${AZURE_ADMIN_USER}@${AZURE_NAME}-sqlsrv"

echo "Creating login ${AZURE_DB_USER} in master via sqlcmd"
PASS_ESCAPED=${AZURE_DB_PASSWORD//\'/''}
sqlcmd -S ${ENDPOINT} -U "${ADMIN_USER_FULL}" -P "${AZURE_ADMIN_PASSWORD}" -d master -Q "IF EXISTS (SELECT 1 FROM sys.sql_logins WHERE name = '${AZURE_DB_USER}') BEGIN ALTER LOGIN [${AZURE_DB_USER}] WITH PASSWORD='${PASS_ESCAPED}'; END ELSE BEGIN CREATE LOGIN [${AZURE_DB_USER}] WITH PASSWORD='${PASS_ESCAPED}'; END"

echo "Creating user ${AZURE_DB_USER} in database ${AZURE_DB} and adding db_owner"
sqlcmd -S ${ENDPOINT} -U "${ADMIN_USER_FULL}" -P "${AZURE_ADMIN_PASSWORD}" -d ${AZURE_DB} -Q "IF NOT EXISTS (SELECT name FROM sys.database_principals WHERE name = '${AZURE_DB_USER}') BEGIN CREATE USER [${AZURE_DB_USER}] FOR LOGIN [${AZURE_DB_USER}]; END; IF NOT EXISTS (SELECT 1 FROM sys.database_role_members drm JOIN sys.database_principals r ON drm.role_principal_id = r.principal_id JOIN sys.database_principals m ON drm.member_principal_id = m.principal_id WHERE r.name = 'db_owner' AND m.name='${AZURE_DB_USER}') BEGIN ALTER ROLE db_owner ADD MEMBER [${AZURE_DB_USER}]; END"

echo "endpoint=${ENDPOINT}" >> $GITHUB_OUTPUT
echo "db=${AZURE_DB}" >> $GITHUB_OUTPUT
echo "username=${AZURE_DB_USER}@${AZURE_NAME}-sqlsrv" >> $GITHUB_OUTPUT
