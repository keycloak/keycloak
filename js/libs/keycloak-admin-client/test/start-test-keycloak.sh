#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
COMPOSE_FILE="${SCRIPT_DIR}/docker-compose.yml"

if ! command -v docker >/dev/null 2>&1; then
  echo "docker is required to run admin-client tests with a preconfigured container."
  exit 1
fi

echo "Starting Keycloak test container..."
docker compose -f "${COMPOSE_FILE}" up -d

echo "Waiting for Keycloak to be ready on http://127.0.0.1:8180 ..."
for _ in $(seq 1 60); do
  if curl --silent --fail --output /dev/null "http://127.0.0.1:8180/realms/master/.well-known/openid-configuration"; then
    echo "Keycloak is ready."
    exit 0
  fi
  sleep 2
done

echo "Keycloak did not become ready in time. Container logs:"
docker compose -f "${COMPOSE_FILE}" logs keycloak || true
exit 1
