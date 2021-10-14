#!/bin/bash -e

NEW_VERSION=$1
mvn --file=./keycloak-theme/pom.xml versions:set -DnewVersion=$NEW_VERSION -DgenerateBackupPoms=false -DgroupId=org.keycloak* -DartifactId=*