#!/bin/bash

cd "$(dirname "$0")"
. ./common.sh

HOST_PORT=${1:-localhost:8443}
TRUSTSTORE_PASSWORD=${2:-password}

#secure-sso-sso-perf-01.apps.summit-aws.sysdeseng.com:443

mkdir -p $PROJECT_BUILD_DIRECTORY

echo "Obtaining certificate from $HOST_PORT"
openssl s_client -showcerts -connect $HOST_PORT </dev/null 2>/dev/null|openssl x509 -outform PEM >$PROJECT_BUILD_DIRECTORY/keycloak.pem
if [ ! -s "$PROJECT_BUILD_DIRECTORY/keycloak.pem" ]; then echo "Obtaining cerfificate failed."; exit 1; fi
cat $PROJECT_BUILD_DIRECTORY/keycloak.pem

echo "Importing certificate"
rm $PROJECT_BUILD_DIRECTORY/truststore.jks
keytool -importcert -file $PROJECT_BUILD_DIRECTORY/keycloak.pem -keystore $PROJECT_BUILD_DIRECTORY/truststore.jks -alias "keycloak" -storepass "$TRUSTSTORE_PASSWORD" -noprompt

echo "Keystore file: $PROJECT_BUILD_DIRECTORY/truststore.jks"
