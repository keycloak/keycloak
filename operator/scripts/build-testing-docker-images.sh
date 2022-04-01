#!/usr/bin/env bash

VERSION="${1:-latest}"
KEYCLOAK_IMAGE="${2:-quay.io/keycloak/keycloak}"

echo "Using version: $VERSION"

SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )

docker build -f ${SCRIPT_DIR}/Dockerfile-custom-image \
  --build-arg IMAGE=${KEYCLOAK_IMAGE} \
  --build-arg VERSION=${VERSION} \
  -t custom-keycloak:${VERSION} ${SCRIPT_DIR}
