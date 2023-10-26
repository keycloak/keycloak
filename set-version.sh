#!/bin/bash -e

NEW_VERSION=$1

# Convert NPM version to semver compatible if needed
if [[ $NEW_VERSION =~ [0-9]+.[0-9]+.[0-9]+.[a-z] ]]; then
  NEW_NPM_VERSION=$(echo $NEW_VERSION | awk -F '.' '{ print $1"."$2"."$3"+"$4 }')
else
  NEW_NPM_VERSION=$NEW_VERSION
fi

# Maven
mvn versions:set -DnewVersion=$NEW_VERSION -DgenerateBackupPoms=false -DgroupId=org.keycloak* -DartifactId=*
mvn versions:set-property --non-recursive -Dproperty=project.version.npm -DnewVersion="$NEW_NPM_VERSION"

# Docker
sed -i "s/ENV KEYCLOAK_VERSION .*/ENV KEYCLOAK_VERSION $NEW_VERSION/" quarkus/container/Dockerfile

# Documentation
cd docs/documentation
SHORT_VERSION=`echo $NEW_VERSION | awk -F '.' '{ print $1"."$2 }'`
sed -i 's/:project_version: .*/:project_version: '$NEW_VERSION'/' topics/templates/document-attributes.adoc
sed -i 's/:project_versionMvn: .*/:project_versionMvn: '$NEW_VERSION'/' topics/templates/document-attributes.adoc
sed -i 's/:project_versionNpm: .*/:project_versionNpm: '$NEW_NPM_VERSION'/' topics/templates/document-attributes.adoc
sed -i 's/:project_versionDoc: .*/:project_versionDoc: '$NEW_VERSION'/' topics/templates/document-attributes.adoc
cd -

# Keycloak JS
echo "$(jq '. += {"version": "'$NEW_NPM_VERSION'"}' js/libs/keycloak-js/package.json)" > js/libs/keycloak-js/package.json

# Keycloak Admin Client
echo "$(jq '. += {"version": "'$NEW_NPM_VERSION'"}' js/libs/keycloak-admin-client/package.json)" > js/libs/keycloak-admin-client/package.json

echo "New Mvn Version: $NEW_VERSION" >&2
echo "New NPM Version: $NEW_NPM_VERSION" >&2
