#!/usr/bin/env bash
set -euo pipefail

# UBI digest is pinned in quarkus/container/Dockerfile; FIPS CI reuses it so Dependabot
# updates do not require a separate digest in .github/workflows/ci.yml.
# Use ubi9@sha256: (not ubi9/ubi) so the repository path matches the digest source.
DOCKERFILE="quarkus/container/Dockerfile"
if [[ ! -f "${DOCKERFILE}" ]]; then
  echo "Dockerfile not found: ${DOCKERFILE}" >&2
  exit 1
fi

UBI9_IMAGE=$(awk '$1 == "FROM" && $2 ~ /^registry\.access\.redhat\.com\/ubi9@/ { print $2; exit }' "${DOCKERFILE}")

if [[ -z "${UBI9_IMAGE}" ]]; then
  echo "Failed to extract registry.access.redhat.com/ubi9@sha256:... digest from ${DOCKERFILE}" >&2
  exit 1
fi

if [[ "${UBI9_IMAGE}" != registry.access.redhat.com/ubi9@sha256:* ]]; then
  echo "Expected registry.access.redhat.com/ubi9@sha256:... from ${DOCKERFILE}, got: ${UBI9_IMAGE}" >&2
  exit 1
fi
echo "${UBI9_IMAGE}"
