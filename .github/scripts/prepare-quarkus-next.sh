#!/bin/bash
set -xeuo pipefail

git checkout -b new-quarkus-next origin/main

if ! grep -q '<repositories>' pom.xml; then
  sed -i '/<\/project>/i \
          <repositories> \
          </repositories>' pom.xml
fi

# Insert the <repository> element before the closing </repositories> tag
sed -i '/<\/repositories>/i \
        <repository> \
        <id>sonatype-snapshots</id> \
        <name>Sonatype Snapshots</name> \
        <url>https://s01.oss.sonatype.org/content/repositories/snapshots/</url> \
        <snapshots> \
            <enabled>true</enabled> \
            <updatePolicy>daily</updatePolicy> \
        </snapshots> \
        <releases> \
            <enabled>false</enabled> \
        </releases> \
        </repository>' pom.xml

./mvnw -B versions:set-property -Dproperty=quarkus.version -DnewVersion=999-SNAPSHOT
./mvnw -B versions:set-property -Dproperty=quarkus.build.version -DnewVersion=999-SNAPSHOT
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