#!/bin/bash -e

mvn --file=./keycloak-theme/pom.xml help:evaluate -Dexpression=project.version -q -DforceStdout