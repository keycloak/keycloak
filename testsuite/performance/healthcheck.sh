#!/bin/bash

function isKeycloakHealthy {
    echo Checking instance $1
    CODE=`curl -s -o /dev/null -w "%{http_code}" $1/realms/master`
    [ "$CODE" -eq "200" ]
}

function waitUntilSystemHealthy {
    echo Waiting until all Keycloak instances are healthy...
    if [ -z $1 ]; then ITERATIONS=60; else ITERATIONS=$1; fi
    C=0
    SYSTEM_HEALTHY=false
    while ! $SYSTEM_HEALTHY; do 
        C=$((C+1))
        if [ $C -gt $ITERATIONS ]; then
            echo System healthcheck failed.
            exit 1
        fi
        sleep 2
        SYSTEM_HEALTHY=true
        for URI in $KEYCLOAK_SERVER_URIS ; do
            if ! isKeycloakHealthy $URI; then 
                echo Instance is not healthy.
                SYSTEM_HEALTHY=false
            fi
        done
    done
    echo System is healthy.
}

if [ -z "$KEYCLOAK_SERVER_URIS" ]; then KEYCLOAK_SERVER_URIS=http://localhost:8080/auth; fi

waitUntilSystemHealthy
