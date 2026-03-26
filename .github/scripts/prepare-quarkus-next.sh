#!/usr/bin/env bash
set -xeuo pipefail

add_repository() {
  local file=$1
  local element=$2
  local id=$3
  local name=$4
  local url=$5

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
    # Insert the element before the closing tag (use sed substitution to handle same-line tags)
    sed -i "s|</$tag>|$repository \n            </$tag>|" "$file"
  else
    # If the tag doesn't exist, create it and insert the element
    sed -i "/<\/project>/i \
            <$tag> \
            $repository \
            </$tag>" "$file"
  fi
}

git checkout -b new-quarkus-next origin/main

# Sonatype for io.quarkus.platform:quarkus-bom
# GitHub Packages for io.quarkus:*
add_repository "pom.xml" "repository" "sonatype-snapshots" "Sonatype Snapshots" "https://central.sonatype.com/repository/maven-snapshots/"
add_repository "pom.xml" "repository" "github-quarkus" "Quarkus GitHub Packages Snapshots" "https://maven.pkg.github.com/quarkusio/quarkus"
add_repository "quarkus/pom.xml" "pluginRepository" "github-quarkus" "Quarkus GitHub Packages Snapshots" "https://maven.pkg.github.com/quarkusio/quarkus"
add_repository "operator/pom.xml" "pluginRepository" "github-quarkus" "Quarkus GitHub Packages Snapshots" "https://maven.pkg.github.com/quarkusio/quarkus"

# Point Maven to the GitHub Packages settings (PAT with read:packages scope)
sed -i -e '$a\' .mvn/maven.config 2>/dev/null || true
printf '%s\n' "-s" ".github/quarkus-next-settings.xml" >> .mvn/maven.config

./quarkus/set-quarkus-version.sh

git add -A
git commit -m "Set quarkus version to 999-SNAPSHOT"

if ! git rev-parse --verify origin/quarkus-next &>/dev/null; then
  echo "No existing quarkus-next branch found. Nothing to cherry-pick."
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