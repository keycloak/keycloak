#!/usr/bin/env bash

set -euo pipefail

if [[ $# -ne 1 ]]; then
  echo "Usage: $0 <build-version>"
  echo "Example: $0 25.0.12-GW"
  exit 1
fi

VERSION="$1"
DIST_FILE="keycloak-${VERSION}.tar.gz"
IMAGE="maven.ontotext.com/keycloak"

echo "==> Setting Maven modules version to: ${VERSION}"

./mvnw versions:set \
  -DnewVersion="${VERSION}" \
  -DprocessAllModules=true \
  -DgenerateBackupPoms=false

echo "==> Building Keycloak distribution: ${VERSION}"

./mvnw clean install \
  -Pdistribution \
  -DskipTests

echo "==> Preparing Docker build"

cd quarkus/container/

cp "../../quarkus/dist/target/${DIST_FILE}" .

echo "==> Building linux/amd64 & linux/arm64 image"

#docker buildx build \
#  --platform linux/amd64 \
#  --pull \
#  --no-cache \
#  --build-arg "KEYCLOAK_DIST=${DIST_FILE}" \
#  -t "maven.ontotext.com/keycloak:25.0.12-GW-amd64" \
#  --load \
#  .

#echo "==> Building linux/arm64 image"

docker buildx build \
  --platform linux/amd64,linux/arm64 \
  --pull \
  --no-cache \
  --build-arg "KEYCLOAK_VERSION=$VERSION" \
  --build-arg "KEYCLOAK_DIST=${DIST_FILE}" \
  -t "${IMAGE}:${VERSION}" \
  --push \
  .

echo
echo "Build completed successfully:"
echo "  ${IMAGE}:${VERSION}"
#echo "  ${IMAGE}:${VERSION}-arm64"
#
#docker buildx imagetools create \
#  -t ${IMAGE}:${VERSION} \
#  ${IMAGE}:${VERSION}-amd64 \
#  ${IMAGE}:${VERSION}-arm64