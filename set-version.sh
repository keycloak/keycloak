#!/bin/bash -e

NEW_VERSION=$1

# Maven
mvn versions:set -DnewVersion=$NEW_VERSION -DgenerateBackupPoms=false -DgroupId=org.keycloak* -DartifactId=*

# Docker
sed -i "s/ENV KEYCLOAK_VERSION .*/ENV KEYCLOAK_VERSION $NEW_VERSION/" quarkus/container/Dockerfile

# NPM
cd js
npm --workspace=admin-ui --workspace=account-ui --workspace=keycloak-masthead pkg set "dependencies.keycloak-js=$NEW_VERSION"
npm --workspace=admin-ui pkg set "dependencies.@keycloak/keycloak-admin-client=$NEW_VERSION"
npm --workspace=keycloak-js --workspace=@keycloak/keycloak-admin-client version $NEW_VERSION --allow-same-version --no-git-tag-version
cd -

# Documentation
cd docs/documentation
SHORT_VERSION=`echo $NEW_VERSION | awk -F '.' '{ print $1"."$2 }'`
sed -i 's/:project_version: .*/:project_version: '$NEW_VERSION'/' topics/templates/document-attributes.adoc
sed -i 's/:project_versionMvn: .*/:project_versionMvn: '$NEW_VERSION'/' topics/templates/document-attributes.adoc
sed -i 's/:project_versionNpm: .*/:project_versionNpm: '$NEW_VERSION'/' topics/templates/document-attributes.adoc
sed -i 's/:project_versionDoc: .*/:project_versionDoc: '$SHORT_VERSION'/' topics/templates/document-attributes.adoc
cd -

