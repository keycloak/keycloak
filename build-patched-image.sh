#!/usr/bin/env bash
#
# Builds a patched Keycloak 26.6.0 container image with the LDAP
# "User LDAP filter leaks into group lookups" fix.
#
# Prerequisites:
#   - Docker (or Podman aliased as docker)
#   - JDK 17+  (javac, jar)
#
# Usage:
#   chmod +x build-patched-image.sh
#   ./build-patched-image.sh
#
# Output image: core.51335.xyz/keycloak:26.6.0
#
set -euo pipefail

IMAGE_TAG="core.51335.xyz/keycloak:26.6.0"
KC_VERSION="26.6.0"
BASE_IMAGE="quay.io/keycloak/keycloak:${KC_VERSION}"
WORK_DIR="$(mktemp -d)"

echo "==> Working directory: ${WORK_DIR}"

cleanup() { rm -rf "${WORK_DIR}"; }
trap cleanup EXIT

# ---------------------------------------------------------------
# 1. Pull the official image and extract the LDAP federation JAR
# ---------------------------------------------------------------
echo "==> Pulling ${BASE_IMAGE} ..."
docker pull "${BASE_IMAGE}"

echo "==> Extracting keycloak-ldap-federation JAR from the image ..."
CONTAINER_ID=$(docker create "${BASE_IMAGE}")
# The JAR lives under /opt/keycloak/lib/lib/main/
JAR_PATH=$(docker run --rm --entrypoint sh "${BASE_IMAGE}" -c \
  "find /opt/keycloak/lib -name 'keycloak-ldap-federation-*.jar' 2>/dev/null" | head -1)

if [ -z "${JAR_PATH}" ]; then
  echo "ERROR: Could not locate keycloak-ldap-federation JAR inside the image."
  docker rm "${CONTAINER_ID}" >/dev/null
  exit 1
fi

echo "    Found JAR at: ${JAR_PATH}"
docker cp "${CONTAINER_ID}:${JAR_PATH}" "${WORK_DIR}/keycloak-ldap-federation.jar"
docker rm "${CONTAINER_ID}" >/dev/null

# ---------------------------------------------------------------
# 2. Extract the JAR so we can compile against its classes
# ---------------------------------------------------------------
echo "==> Extracting JAR for compilation classpath ..."
mkdir -p "${WORK_DIR}/jar-contents"
(cd "${WORK_DIR}/jar-contents" && jar xf "${WORK_DIR}/keycloak-ldap-federation.jar")

# We also need the Keycloak SPI JARs and other deps for compilation.
# Extract ALL jars from the image's lib directory.
echo "==> Extracting all dependency JARs from the image ..."
CONTAINER_ID=$(docker create "${BASE_IMAGE}")
docker cp "${CONTAINER_ID}:/opt/keycloak/lib" "${WORK_DIR}/lib"
docker rm "${CONTAINER_ID}" >/dev/null

# Build the classpath from all JARs
CLASSPATH=$(find "${WORK_DIR}/lib" -name '*.jar' -printf '%p:' 2>/dev/null)

# ---------------------------------------------------------------
# 3. Compile the 4 patched Java source files
# ---------------------------------------------------------------
REPO_DIR="$(cd "$(dirname "$0")" && pwd)"
SRC_BASE="${REPO_DIR}/federation/ldap/src/main/java"

PATCHED_FILES=(
  "org/keycloak/storage/ldap/idm/query/internal/LDAPQuery.java"
  "org/keycloak/storage/ldap/idm/store/ldap/LDAPOperationManager.java"
  "org/keycloak/storage/ldap/idm/store/ldap/LDAPIdentityStore.java"
  "org/keycloak/storage/ldap/LDAPUtils.java"
)

echo "==> Compiling patched source files ..."
mkdir -p "${WORK_DIR}/classes"
javac -source 17 -target 17 \
  -cp "${CLASSPATH}" \
  -d "${WORK_DIR}/classes" \
  "${PATCHED_FILES[@]/#/${SRC_BASE}/}"

echo "    Compiled successfully."

# ---------------------------------------------------------------
# 4. Patch the JAR with the new class files
# ---------------------------------------------------------------
echo "==> Patching JAR with fixed class files ..."
cp "${WORK_DIR}/keycloak-ldap-federation.jar" "${WORK_DIR}/keycloak-ldap-federation-patched.jar"
(cd "${WORK_DIR}/classes" && jar uf "${WORK_DIR}/keycloak-ldap-federation-patched.jar" \
  org/keycloak/storage/ldap/idm/query/internal/LDAPQuery.class \
  org/keycloak/storage/ldap/idm/store/ldap/LDAPOperationManager.class \
  org/keycloak/storage/ldap/idm/store/ldap/LDAPIdentityStore.class \
  org/keycloak/storage/ldap/LDAPUtils.class)

# ---------------------------------------------------------------
# 5. Build the container image
# ---------------------------------------------------------------
echo "==> Building container image ${IMAGE_TAG} ..."

JAR_BASENAME=$(basename "${JAR_PATH}")
cat > "${WORK_DIR}/Dockerfile" <<DOCKERFILE
FROM ${BASE_IMAGE}

# Replace the LDAP federation JAR with the patched version
COPY --chown=1000:0 keycloak-ldap-federation-patched.jar /opt/keycloak/lib/lib/main/${JAR_BASENAME}
DOCKERFILE

docker build -t "${IMAGE_TAG}" -f "${WORK_DIR}/Dockerfile" "${WORK_DIR}"

echo ""
echo "============================================"
echo " Image built successfully!"
echo " Tag: ${IMAGE_TAG}"
echo "============================================"
echo ""
echo " Run it with:"
echo "   docker run -p 8080:8080 ${IMAGE_TAG} start-dev"
echo ""
