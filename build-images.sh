#!/usr/bin/env bash

set -euo pipefail

if [[ $# -ne 1 ]]; then
  echo "Usage: $0 <build-version>"
  echo "Example: $0 25.0.14-GW or $0 25.0.14-GW-RC1"
  exit 1
fi

VERSION="$1"
DIST_FILE="keycloak-${VERSION}.tar.gz"
IMAGE="maven.ontotext.com/keycloak"

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "${ROOT_DIR}"

NPM_PACKAGES=(
  js/libs/keycloak-js/package.json
  js/libs/keycloak-admin-client/package.json
  js/libs/ui-shared/package.json
  js/apps/account-ui/package.json
)
VERSIONED_FILES=(':(glob)**/pom.xml' "${NPM_PACKAGES[@]}")

# The version changes below are reverted with git on exit, which would also
# discard any local edits to these files, so refuse to run if there are any.
if ! git diff --quiet HEAD -- "${VERSIONED_FILES[@]}"; then
  echo "ERROR: pom.xml or package.json files have uncommitted changes:" >&2
  git status --short -- "${VERSIONED_FILES[@]}" >&2
  echo "Commit or stash them before running $0." >&2
  exit 1
fi

restore_versions() {
  echo "==> Restoring development versions"
  git -C "${ROOT_DIR}" checkout -- "${VERSIONED_FILES[@]}"
}
# Runs on success, on failure (set -e) and on Ctrl-C.
trap restore_versions EXIT

# Keycloak 25.x must be built with JDK 21. JDK 23+ no longer runs annotation
# processors found on the classpath, so jboss-logging's *_$logger classes are not
# generated and the build fails with "Invalid logger interface ...".
java_major() { "$1/bin/java" -XshowSettings:properties -version 2>&1 \
  | sed -n 's/^ *java.specification.version = //p'; }
if [[ -z "${JAVA_HOME:-}" || "$(java_major "${JAVA_HOME}")" != "21" ]]; then
  if [[ -x /usr/libexec/java_home ]] && JAVA_HOME="$(/usr/libexec/java_home -v 21 2>/dev/null)"; then
    export JAVA_HOME
  else
    echo "ERROR: JDK 21 is required; set JAVA_HOME to a JDK 21 installation." >&2
    exit 1
  fi
fi
echo "==> Using JDK ${JAVA_HOME}"

echo "==> Setting Maven modules version to: ${VERSION}"

./mvnw versions:set \
  -DnewVersion="${VERSION}" \
  -DprocessAllModules=true \
  -DgenerateBackupPoms=false

echo "==> Setting NPM version to: ${VERSION}"

# keycloak-js packs keycloak-js-${project.version.npm}.tgz, whose name npm derives
# from package.json, so the property and the package.json versions must match.
./mvnw versions:set-property \
  --non-recursive \
  -Dproperty=project.version.npm \
  -DnewVersion="${VERSION}" \
  -DgenerateBackupPoms=false

for PKG in "${NPM_PACKAGES[@]}"; do
  jq --arg v "${VERSION}" '.version = $v' "${PKG}" > "${PKG}.tmp" && mv "${PKG}.tmp" "${PKG}"
done

echo "==> Building Keycloak distribution: ${VERSION}"

./mvnw clean install \
  -Pdistribution \
  -DskipTests

echo "==> Preparing Docker build"

cd quarkus/container/

cp "../../quarkus/dist/target/${DIST_FILE}" .

echo "==> Building linux/amd64 & linux/arm64 image"

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
