#!/bin/bash -e

NEW_VERSION=$1

mvn versions:set -DnewVersion=$NEW_VERSION -DgenerateBackupPoms=false -DgroupId=org.keycloak* -DartifactId=*
mvn versions:use-dep-version -Dincludes=org.keycloak:keycloak-admin-ui -DdepVersion=$NEW_VERSION -DforceVersion=true -pl org.keycloak:keycloak-parent
