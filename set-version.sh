#!/bin/bash -e

NEW_VERSION=$1

# Maven
mvn versions:set -DnewVersion=$NEW_VERSION -DgenerateBackupPoms=false -DgroupId=org.keycloak* -DartifactId=*

# Docker
sed -i "s/ENV KEYCLOAK_VERSION .*/ENV KEYCLOAK_VERSION $NEW_VERSION/" quarkus/container/Dockerfile

# NPM
cd adapters/oidc/js
npm version --no-git-tag-version $NEW_VERSION
cd $OLDPWD
