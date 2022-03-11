#!/bin/sh

CODEQL_BINARY="./codeql/codeql"

# Check if the binary exists
if [ ! -f "$CODEQL_BINARY" ]; 
then
    printf "CodeQL binary not found!"
    exit 1
fi

upload_results () {
  echo "Uploading $1"
  $CODEQL_BINARY github upload-results --sarif="$1" --repository="$GITHUB_REPOSITORY" --ref="$GITHUB_REF"
}


# Create the database based on the specifics per language
if [ "$1" = "java" ];
then 
    printf "Analyzing CodeQL Java database"
    $CODEQL_BINARY database analyze "$1-database" codeql/java-queries --format=sarifv2.1.0 --output="$1".sarif --download --max-paths=1 --sarif-add-query-help
    < java.sarif jq 'del(.runs[].results[].codeFlows)' > processed-java.sarif
    upload_results processed-java.sarif

elif [ "$1" = "javascript" ];
then
    printf "Analyzing themes database"
    $CODEQL_BINARY database analyze themes-database codeql/javascript-queries --format=sarifv2.1.0 --output=themes.sarif --download --max-paths=1 --sarif-add-query-help
    < themes.sarif jq 'del(.runs[].results[].codeFlows)' > processed-themes.sarif
    upload_results processed-themes.sarif

    printf "Analyzing js-adapter database"
    $CODEQL_BINARY database analyze js-adapter-database codeql/javascript-queries --format=sarifv2.1.0 --output=js-adapter.sarif --download --max-paths=1 --sarif-add-query-help
    < js-adapter.sarif jq 'del(.runs[].results[].codeFlows)' > processed-js-adapter.sarif
    upload_results processed-js-adapter.sarif

fi

 
