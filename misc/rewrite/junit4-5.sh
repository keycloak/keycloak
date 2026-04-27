#!/bin/bash -e

cd $(dirname $0)/../../

MODULE="$1"

cd "$MODULE"

../mvnw org.openrewrite.maven:rewrite-maven-plugin:run \
  -Drewrite.activeRecipes=org.keycloak.JUnit4to5Migration \
  -Drewrite.configLocation=../misc/rewrite/junit4-5.yml \
  -Drewrite.recipeArtifactCoordinates=org.openrewrite.recipe:rewrite-testing-frameworks:3.34.0

../mvnw spotless:apply