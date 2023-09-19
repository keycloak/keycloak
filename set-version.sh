#!/bin/bash -e

NEW_VERSION=$1
NEW_NPM_VERSION=${2:-${1}}

if [ $NEW_VERSION == 'use_current' ] ; then
  # obtain NEW_VERSION from maven pom file
  # - useful for downstream projects which already set the version in pom files
  NEW_VERSION=$(./get-version.sh)
  NEW_NPM_VERSION=$(echo $NEW_VERSION | awk -F '.' '{ print $1"."$2"."$3"-"$4 }')
else
  # Maven
  mvn versions:set -DnewVersion=$NEW_VERSION -DgenerateBackupPoms=false -DgroupId=org.keycloak* -DartifactId=*
fi

# Docker
sed -i "s/ENV KEYCLOAK_VERSION .*/ENV KEYCLOAK_VERSION $NEW_VERSION/" quarkus/container/Dockerfile

# Documentation
cd docs/documentation
SHORT_VERSION=`echo $NEW_NPM_VERSION | awk -F '.' '{ print $1"."$2 }'`
sed -i 's/:project_version: .*/:project_version: '$NEW_VERSION'/' topics/templates/document-attributes.adoc
sed -i 's/:project_versionMvn: .*/:project_versionMvn: '$NEW_VERSION'/' topics/templates/document-attributes.adoc
sed -i 's/:project_versionNpm: .*/:project_versionNpm: '$NEW_NPM_VERSION'/' topics/templates/document-attributes.adoc
sed -i 's/:project_versionDoc: .*/:project_versionDoc: '$NEW_VERSION'/' topics/templates/document-attributes.adoc
cd -

# Keycloak JS
echo "$(jq '. += {"version": "'$NEW_NPM_VERSION'"}' js/libs/keycloak-js/package.json)" > js/libs/keycloak-js/package.json

# Keycloak Admin Client
echo "$(jq '. += {"version": "'$NEW_NPM_VERSION'"}' js/libs/keycloak-admin-client/package.json)" > js/libs/keycloak-admin-client/package.json
