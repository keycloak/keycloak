#!/bin/bash -e

cd $(dirname $0)/../../

./mvnw org.openrewrite.maven:rewrite-maven-plugin:run \
  -Drewrite.activeRecipes=org.openrewrite.java.RemoveUnusedImports \
  -Drewrite.activeStyles=org.keycloak.NoStarImports \
  -Drewrite.configLocation=misc/rewrite/nostarimports.yml