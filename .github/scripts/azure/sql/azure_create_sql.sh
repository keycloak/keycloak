#!/usr/bin/env bash
set -e

if [[ "$RUNNER_DEBUG" == "1" ]]; then
  set -x
fi

SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )
source ${SCRIPT_DIR}/azure_common.sh

requiredEnv AZURE_SUBSCRIPTION AZURE_ADMIN_USER AZURE_DB_USER

# Login is expected to be done by the workflow via az login with service principal

if [ "${AZURE_ALLOW_ALL_FIREWALL}" = "true" ]; then
  RUNNER_IP=$(curl --ipv4 --fail --silent --show-error --connect-timeout 10 --max-time 30 https://ifconfig.me/ip)
  if ! jq -en --arg ip "$RUNNER_IP" '$ip |
    test("^[0-9]{1,3}(\\.[0-9]{1,3}){3}$") and
    (split(".") | all(.[]; tonumber <= 255)) and . != "0.0.0.0"' >/dev/null; then
    echo "ERROR: Could not determine a valid runner IPv4 address." >&2
    exit 1
  fi
fi

# Resource group (created via idempotent command)
echo "Creating resource group ${AZURE_RG} in ${AZURE_REGION}"
group_args=(--name "$AZURE_RG" --location "$AZURE_REGION" --subscription "$AZURE_SUBSCRIPTION")
if [[ "${GITHUB_ACTIONS:-}" == "true" ]]; then
  group_args+=(--tags "keycloak-ci-repository=$GITHUB_REPOSITORY" "keycloak-ci-run=$GITHUB_RUN_ID"
    "keycloak-ci-attempt=$GITHUB_RUN_ATTEMPT")
fi
az group create "${group_args[@]}"

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
  echo "Allowing Azure services (0.0.0.0) - use with caution"
  az sql server firewall-rule create --resource-group ${AZURE_RG} --server ${AZURE_NAME}-sqlsrv --name AllowAll --start-ip-address 0.0.0.0 --end-ip-address 0.0.0.0 --subscription ${AZURE_SUBSCRIPTION}
  echo "Allowing runner IPv4 address ${RUNNER_IP}"
  az sql server firewall-rule create --resource-group "${AZURE_RG}" --server "${AZURE_NAME}-sqlsrv" --name AllowRunner --start-ip-address "$RUNNER_IP" --end-ip-address "$RUNNER_IP" --subscription "${AZURE_SUBSCRIPTION}"
  trap '
    az sql server firewall-rule delete --resource-group "$AZURE_RG" --server "$AZURE_NAME-sqlsrv" --name AllowRunner --subscription "$AZURE_SUBSCRIPTION"
  ' EXIT
fi

ENDPOINT="${AZURE_NAME}-sqlsrv.database.windows.net"
ADMIN_USER_FULL="${AZURE_ADMIN_USER}@${AZURE_NAME}-sqlsrv"

# Firewall rules can take several minutes to propagate.
echo "Waiting for SQL connectivity..."
for attempt in {1..30}; do
  if sqlcmd -S "$ENDPOINT" -U "$ADMIN_USER_FULL" -P "$AZURE_ADMIN_PASSWORD" -d master -b -l 5 -t 5 -Q "SELECT 1"; then
    break
  fi
  if [ "$attempt" -eq 30 ]; then
    echo "ERROR: SQL server is not accessible after 30 connection attempts." >&2
    exit 1
  fi
  sleep 10
done

echo "Creating login ${AZURE_DB_USER} in master via sqlcmd"
PASS_ESCAPED=${AZURE_DB_PASSWORD//\'/''}
sqlcmd -S ${ENDPOINT} -U "${ADMIN_USER_FULL}" -P "${AZURE_ADMIN_PASSWORD}" -d master -Q "IF EXISTS (SELECT 1 FROM sys.sql_logins WHERE name = '${AZURE_DB_USER}') BEGIN ALTER LOGIN [${AZURE_DB_USER}] WITH PASSWORD='${PASS_ESCAPED}'; END ELSE BEGIN CREATE LOGIN [${AZURE_DB_USER}] WITH PASSWORD='${PASS_ESCAPED}'; END"

echo "Creating user ${AZURE_DB_USER} in database ${AZURE_DB} and adding db_owner"
sqlcmd -S ${ENDPOINT} -U "${ADMIN_USER_FULL}" -P "${AZURE_ADMIN_PASSWORD}" -d ${AZURE_DB} -Q "IF NOT EXISTS (SELECT name FROM sys.database_principals WHERE name = '${AZURE_DB_USER}') BEGIN CREATE USER [${AZURE_DB_USER}] FOR LOGIN [${AZURE_DB_USER}]; END; IF NOT EXISTS (SELECT 1 FROM sys.database_role_members drm JOIN sys.database_principals r ON drm.role_principal_id = r.principal_id JOIN sys.database_principals m ON drm.member_principal_id = m.principal_id WHERE r.name = 'db_owner' AND m.name='${AZURE_DB_USER}') BEGIN ALTER ROLE db_owner ADD MEMBER [${AZURE_DB_USER}]; END"

echo "endpoint=${ENDPOINT}" >> $GITHUB_OUTPUT
echo "db=${AZURE_DB}" >> $GITHUB_OUTPUT
echo "username=${AZURE_DB_USER}@${AZURE_NAME}-sqlsrv" >> $GITHUB_OUTPUT
