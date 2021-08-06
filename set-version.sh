#!/bin/bash -e

NEW_VERSION=$1
sed -i 's/"version": .*/"version": "'$NEW_VERSION'",/' package.json
mvn --file=./keycloak-theme/pom.xml versions:set -DnewVersion=$NEW_VERSION -DgenerateBackupPoms=false -DgroupId=org.keycloak* -DartifactId=*