#!/bin/sh
export KC_AUTH_SERVER=http://localhost:8080/auth
export KC_REALM=master
export KC_CLIENT=cli

export KC_ACCESS_TOKEN=`java -DKEYCLOAK_AUTH_SERVER=$KC_AUTH_SERVER -DKEYCLOAK_REALM=$KC_REALM -DKEYCLOAK_CLIENT=$KC_CLIENT -jar target/keycloak-cli-sso-3.3.0.CR1-SNAPSHOT.jar login`




