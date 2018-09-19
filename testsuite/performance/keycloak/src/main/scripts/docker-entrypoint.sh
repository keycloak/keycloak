#!/bin/bash

cat $JBOSS_HOME/standalone/configuration/$CONFIGURATION

. get-ips.sh

PARAMS="-b $PUBLIC_IP -bmanagement $PUBLIC_IP -bprivate $PRIVATE_IP -c $CONFIGURATION $@"
echo "Server startup params: $PARAMS"

# Note: External container connectivity is always provided by eth0 -- irrespective of which is considered public/private by KC.
#       In case the container needs to be accessible on the host computer override -b $PUBLIC_IP by adding: `-b 0.0.0.0` to the docker command.

if [ $KEYCLOAK_ADMIN_USER ] && [ $KEYCLOAK_ADMIN_PASSWORD ]; then
    $JBOSS_HOME/bin/add-user-keycloak.sh --user $KEYCLOAK_ADMIN_USER --password $KEYCLOAK_ADMIN_PASSWORD
fi

exec /opt/jboss/keycloak/bin/standalone.sh $PARAMS
