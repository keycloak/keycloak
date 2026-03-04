#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"

bash "${SCRIPT_DIR}/start-test-keycloak.sh"
trap 'bash "${SCRIPT_DIR}/stop-test-keycloak.sh"' EXIT

cd "${PROJECT_DIR}"
pnpm test
