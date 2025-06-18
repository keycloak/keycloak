#!/bin/bash
set -xeuo pipefail

add_repository() {
  local file=$1
  local element=$2

  local id="sonatype-snapshots"
  local name="Sonatype Snapshots"
  local url="https://s01.oss.sonatype.org/content/repositories/snapshots/"

  # Decide the tag based on the element
  local tag
  if [ "$element" = "repository" ]; then
    tag="repositories"
  elif [ "$element" = "pluginRepository" ]; then
    tag="pluginRepositories"
  fi

  # Repository to be inserted
  local repository="<$element> \
            <id>$id</id> \
            <name>$name</name> \
            <url>$url</url> \
            <snapshots> \
                <enabled>true</enabled> \
                <updatePolicy>daily</updatePolicy> \
            </snapshots> \
            <releases> \
                <enabled>false</enabled> \
            </releases> \
            </$element>"

  # Check if the tag exists in the file
  if grep -q "<$tag>" "$file"; then
    # Insert the element before the closing tag
    sed -i "/<\/$tag>/i $repository" "$file"
  else
    # If the tag doesn't exist, create it and insert the element
    sed -i "/<\/project>/i \
            <$tag> \
            $repository \
            </$tag>" "$file"
  fi
}

git checkout -b new-quarkus-next origin/main

add_repository "pom.xml" "repository"
add_repository "quarkus/pom.xml" "pluginRepository"
add_repository "operator/pom.xml" "pluginRepository"

./quarkus/set-quarkus-version.sh
git commit -am "Set quarkus version to 999-SNAPSHOT"

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