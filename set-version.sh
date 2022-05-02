#!/bin/bash -e

NEW_VERSION=$1

mvn versions:set -DnewVersion=$NEW_VERSION -DgenerateBackupPoms=false -DgroupId=org.keycloak* -DartifactId=*

sed -i "s/ENV KEYCLOAK_VERSION .*/ENV KEYCLOAK_VERSION $NEW_VERSION/" quarkus/container/Dockerfile