#!/usr/bin/env bash

set -Eeuo pipefail

required_variables=(
  DEPLOY_HOST
  DEPLOY_USER
  DEPLOY_PATH
  DEPLOY_SSH_KEY
  DEPLOY_KNOWN_HOSTS
  KEYCLOAK_HOSTNAME
)

for variable_name in "${required_variables[@]}"; do
  if [[ -z "${!variable_name:-}" ]]; then
    echo "Missing required variable: ${variable_name}" >&2
    exit 1
  fi
done

if [[ ! "$DEPLOY_PATH" =~ ^/opt/[A-Za-z0-9._/-]+$ ]]; then
  echo "DEPLOY_PATH must be an absolute path below /opt" >&2
  exit 1
fi

if [[ ! "$KEYCLOAK_HOSTNAME" =~ ^[A-Za-z0-9.-]+$ ]]; then
  echo "KEYCLOAK_HOSTNAME must be a DNS hostname" >&2
  exit 1
fi

ssh_options=(
  -i "$DEPLOY_SSH_KEY"
  -o BatchMode=yes
  -o IdentitiesOnly=yes
  -o StrictHostKeyChecking=yes
  -o "UserKnownHostsFile=$DEPLOY_KNOWN_HOSTS"
)

remote_host="${DEPLOY_USER}@${DEPLOY_HOST}"
deploy_revision="${DEPLOY_REVISION:-unknown}"

ssh "${ssh_options[@]}" "$remote_host" bash -s -- \
  "$DEPLOY_PATH" "$KEYCLOAK_HOSTNAME" "$deploy_revision" <<'REMOTE_SCRIPT'
set -Eeuo pipefail

deploy_path="$1"
keycloak_hostname="$2"
deploy_revision="$3"
environment_file="${deploy_path}/.env"
network_name="hzt-infra"
bootstrap_secret_present=false

if [[ ! -s "$environment_file" ]]; then
  echo "Missing server environment file: ${environment_file}" >&2
  exit 1
fi

if ! command -v docker >/dev/null 2>&1; then
  echo "Docker is not installed" >&2
  exit 1
fi

chmod 600 "$environment_file"
if grep -q '^KC_BOOTSTRAP_ADMIN_PASSWORD=' "$environment_file"; then
  bootstrap_secret_present=true
fi

docker network inspect "$network_name" >/dev/null 2>&1 || docker network create "$network_name"

docker pull postgres:16-alpine
docker pull quay.io/keycloak/keycloak:26.7.3
docker pull caddy:2-alpine

docker rm -f hzt-infra-caddy hzt-infra-keycloak hzt-infra-postgres >/dev/null 2>&1 || true

docker run -d \
  --name hzt-infra-postgres \
  --network "$network_name" \
  --restart unless-stopped \
  --memory 768m \
  --env-file "$environment_file" \
  --volume hzt-infra-postgres-data:/var/lib/postgresql/data \
  --label "hzt.deploy.revision=${deploy_revision}" \
  postgres:16-alpine >/dev/null

for attempt in $(seq 1 30); do
  if docker exec hzt-infra-postgres sh -c 'pg_isready -U "$POSTGRES_USER" -d "$POSTGRES_DB"' >/dev/null 2>&1; then
    break
  fi
  if [[ "$attempt" -eq 30 ]]; then
    docker logs --tail 100 hzt-infra-postgres >&2
    exit 1
  fi
  sleep 2
done

start_keycloak() {
  docker rm -f hzt-infra-keycloak >/dev/null 2>&1 || true
  docker run -d \
    --name hzt-infra-keycloak \
    --network "$network_name" \
    --restart unless-stopped \
    --memory 2g \
    --env-file "$environment_file" \
    --label "hzt.deploy.revision=${deploy_revision}" \
    quay.io/keycloak/keycloak:26.7.3 start >/dev/null
}

start_keycloak

docker run -d \
  --name hzt-infra-caddy \
  --network "$network_name" \
  --restart unless-stopped \
  --memory 128m \
  --publish 80:80 \
  --publish 443:443 \
  --volume hzt-infra-caddy-data:/data \
  --volume hzt-infra-caddy-config:/config \
  --label "hzt.deploy.revision=${deploy_revision}" \
  caddy:2-alpine caddy reverse-proxy \
    --from "https://${keycloak_hostname}" \
    --to hzt-infra-keycloak:8080 >/dev/null

discovery_url="https://${keycloak_hostname}/realms/master/.well-known/openid-configuration"
wait_for_keycloak() {
  for attempt in $(seq 1 60); do
    if curl --fail --silent --show-error --max-time 10 "$discovery_url" >/dev/null 2>&1; then
      return 0
    fi
    if [[ "$attempt" -eq 60 ]]; then
      docker logs --tail 150 hzt-infra-keycloak >&2
      docker logs --tail 100 hzt-infra-caddy >&2
      return 1
    fi
    sleep 5
  done
}

wait_for_keycloak

# Bootstrap credentials are needed only for the first successful startup.
if [[ "$bootstrap_secret_present" == true ]]; then
  sed -i '/^KC_BOOTSTRAP_ADMIN_PASSWORD=/d' "$environment_file"
  start_keycloak
  wait_for_keycloak
fi

curl --fail --silent --show-error "$discovery_url" >/dev/null
docker ps --filter 'name=hzt-infra-' --format 'table {{.Names}}\t{{.Image}}\t{{.Status}}'
REMOTE_SCRIPT
