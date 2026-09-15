#!/bin/bash -e

if [[ "$RUNNER_DEBUG" == "1" ]]; then
  set -x
fi

TARGET_BRANCH="$1"
REPO="${2:-keycloak}"
ORG="${3:-keycloak}"

if [[ "${TARGET_BRANCH}" != "release/"* ]]; then
  echo "skip"
  exit 0
fi

ALL_RELEASES=$(gh release list \
  --repo "${ORG}/${REPO}" \
  --exclude-drafts \
  --exclude-pre-releases \
  --json name \
  --template '{{range .}}{{.name}}{{"\n"}}{{end}}'
)
MAJOR_MINOR=${TARGET_BRANCH#"release/"}
MAJOR_MINOR_RELEASES=$(echo "${ALL_RELEASES}" | (grep "${MAJOR_MINOR}" || true))

if [[ -z "${MAJOR_MINOR_RELEASES}" ]]; then
  echo "skip"
else
  echo -n "${MAJOR_MINOR_RELEASES}" | jq -cnMR '[inputs] | map({version: .})'
fi
