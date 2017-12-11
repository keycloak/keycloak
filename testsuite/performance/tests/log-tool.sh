#!/bin/bash

cd "$(dirname "$0")"
. ./common.sh

java -cp $PROJECT_BUILD_DIRECTORY/classes org.keycloak.performance.log.LogProcessor "$@"