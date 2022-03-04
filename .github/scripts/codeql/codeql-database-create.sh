#!/bin/sh

CODEQL_BINARY="./codeql/codeql"

# Check if the binary exists
if [ ! -f "$CODEQL_BINARY" ]; 
then
    printf "CodeQL binary not found!"
    exit 1
fi

# Create the database based on the specifics per language
if [ "$1" = "java" ];
then 
    printf "Creating CodeQL Java database"
    $CODEQL_BINARY database create "$1-database" --no-run-unnecessary-builds --language="$1" --command='mvn clean install -Dmaven.test.skip -DskipQuarkus -DskipTestsuite -DskipExamples -DskipTests'
elif [ "$1" = "javascript" ];
then
    printf "Creating themes database"
    $CODEQL_BINARY database create themes-database --no-run-unnecessary-builds --language=javascript --source-root=themes/ --command='mvn install -Dmaven.test.skip -DskipQuarkus -DskipTestsuite -DskipExamples -DskipTests'
    printf "Creating js-adapter database"
    $CODEQL_BINARY database create js-adapter-database --no-run-unnecessary-builds --language=javascript --source-root=adapters/oidc/js/ --command='mvn install -Dmaven.test.skip -DskipQuarkus -DskipTestsuite -DskipExamples -DskipTests'
fi

 
