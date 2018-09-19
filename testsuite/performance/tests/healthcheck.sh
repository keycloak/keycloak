#!/bin/bash

cd "$(dirname "$0")"
. ./common.sh

CHECK_TIMEOUT=${CHECK_TIMEOUT:-5}
function isKeycloakServerReady {
    CODE=`curl --connect-timeout $CHECK_TIMEOUT -m $CHECK_TIMEOUT -s -o /dev/null -w "%{http_code}" $1/realms/master`
    [ "$CODE" -eq "200" ]
}


if [ -f "$PROVISIONED_SYSTEM_PROPERTIES_FILE" ] ; then 

    FRONTEND_SERVERS=$( sed -n -e '/keycloak.frontend.servers=/ s/.*\= *//p' "$PROVISIONED_SYSTEM_PROPERTIES_FILE" )
    BACKEND_SERVERS=$( sed -n -e '/keycloak.backend.servers=/ s/.*\= *//p' "$PROVISIONED_SYSTEM_PROPERTIES_FILE" )
    KEYCLOAK_SERVERS="$FRONTEND_SERVERS $BACKEND_SERVERS"

    HEALTHCHECK_ITERATIONS=${HEALTHCHECK_ITERATIONS:-20}
    HEALTHCHECK_WAIT=${HEALTHCHECK_WAIT:-6s}
    
    echo "Waiting for Keycloak servers to be ready."
    echo "Check intervals: $HEALTHCHECK_ITERATIONS x $HEALTHCHECK_WAIT."

    C=0
    READY=false
    while ! $READY; do 
        C=$((C+1))
        if [ $C -gt $HEALTHCHECK_ITERATIONS ]; then
            echo System healthcheck failed.
            exit 1
        fi
        echo $( date "+%Y-%m-%d %H:%M:%S" )
        READY=true
        for SERVER in $KEYCLOAK_SERVERS ; do
            if isKeycloakServerReady $SERVER; then 
                echo -e "Keycloak server: $SERVER\tREADY"
            else
                echo -e "Keycloak server: $SERVER\tNOT READY"
                READY=false
            fi
        done
        if ! $READY; then sleep $HEALTHCHECK_WAIT ; fi
    done
    echo All servers ready.

else
    echo Healthcheck skipped.
fi
