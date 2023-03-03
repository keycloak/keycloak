#!/bin/bash -e

NEW_VERSION=$1

# Maven
mvn versions:set -DnewVersion=$NEW_VERSION -DgenerateBackupPoms=false -DgroupId=org.keycloak* -DartifactId=*

# Docker
sed -i "s/ENV KEYCLOAK_VERSION .*/ENV KEYCLOAK_VERSION $NEW_VERSION/" quarkus/container/Dockerfile

# NPM
cd js
npm version $NEW_VERSION --allow-same-version --no-git-tag-version --workspaces
cd -