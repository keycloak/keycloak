#!/usr/bin/env bash
set -xeuo pipefail

git checkout -b new-quarkus-next origin/main

# Use io.quarkus:quarkus-bom built from source
sed -i 's|<groupId>io.quarkus.platform</groupId>|<groupId>io.quarkus</groupId>|g' pom.xml

# Use io.quarkiverse.operatorsdk:quarkus-operator-sdk-bom built from source (QOSDK_VERSION dynamically parsed by the workflow)
if [ -n "${QOSDK_VERSION:-}" ]; then
  sed -i '/<groupId>io.quarkus.platform<\/groupId>/,/<\/dependency>/{
    s|<groupId>io.quarkus.platform</groupId>|<groupId>io.quarkiverse.operatorsdk</groupId>|
    s|<version>${quarkus.version}</version>|<version>'"${QOSDK_VERSION}"'</version>|
  }' operator/pom.xml
fi

./quarkus/set-quarkus-version.sh
git commit -am "Set quarkus version to 999-SNAPSHOT"

if ! git rev-parse origin/quarkus-next &>/dev/null; then
  echo "No existing quarkus-next branch, skipping cherry-pick."
  exit 0
fi

snapshot_version_hash=$(git log origin/quarkus-next --grep="Set quarkus version to 999-SNAPSHOT" --format="%H" -n 1)
commits_to_cherry_pick=$(git rev-list --right-only --no-merges --reverse new-quarkus-next...origin/quarkus-next | grep -vE "$snapshot_version_hash" || echo "")

if [ -z "$commits_to_cherry_pick" ]; then
  echo "Nothing to cherry-pick."
else
  for commit in $commits_to_cherry_pick
  do
    if git cherry-pick "$commit"; then
      echo "Successfully cherry-picked $commit"
    else
      echo "Failed to cherry-pick $commit"
      exit 1
    fi
  done
fi
