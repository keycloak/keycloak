#!/bin/sh
DIRNAME=`dirname "$0"`
java $KC_OPTS -cp $DIRNAME/client/keycloak-client-registration-cli-${project.version}.jar org.keycloak.client.registration.cli.KcRegMain "$@"
