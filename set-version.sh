#!/bin/bash -e

NEW_VERSION=$1

# Maven
mvn versions:set -DnewVersion=$NEW_VERSION -DgenerateBackupPoms=false -DgroupId=org.keycloak* -DartifactId=*

# Docker
sed -i "s/ENV KEYCLOAK_VERSION .*/ENV KEYCLOAK_VERSION $NEW_VERSION/" quarkus/container/Dockerfile

# NPM
cd js
npm --workspaces pkg set "dependencies.keycloak-js=$NEW_VERSION"
npm --workspaces pkg set "dependencies.@keycloak/keycloak-admin-client=$NEW_VERSION"
npm --workspaces pkg set "dependencies.keycloak-masthead=$NEW_VERSION"
npm --workspaces pkg set "dependencies.ui-shared=$NEW_VERSION"
npm version $NEW_VERSION --allow-same-version --no-git-tag-version --workspaces
cd -

# Documentation
cd docs/documentation
SHORT_VERSION=`echo $VERSION | awk -F '.' '{ print $1"."$2 }'`
sed -i 's/:project_version: .*/:project_version: '$VERSION'/' topics/templates/document-attributes.adoc
sed -i 's/:project_versionMvn: .*/:project_versionMvn: '$VERSION'/' topics/templates/document-attributes.adoc
sed -i 's/:project_versionNpm: .*/:project_versionNpm: '$VERSION'/' topics/templates/document-attributes.adoc
sed -i 's/:project_versionDoc: .*/:project_versionDoc: '$SHORT_VERSION'/' topics/templates/document-attributes.adoc
cd -

