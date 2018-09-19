#!/bin/bash

cat $INFINISPAN_SERVER_HOME/standalone/configuration/$CONFIGURATION

. get-ips.sh

PARAMS="-b $PUBLIC_IP -bmanagement $PUBLIC_IP -bprivate $PRIVATE_IP -Djgroups.bind_addr=$PUBLIC_IP -c $CONFIGURATION $@"
echo "Server startup params: $PARAMS"

# Note: External container connectivity is always provided by eth0 -- irrespective of which is considered public/private by KC.
#       In case the container needs to be accessible on the host computer override -b $PUBLIC_IP by adding: `-b 0.0.0.0` to the docker command.

if [ $MGMT_USER ] && [ $MGMT_USER_PASSWORD ]; then
    echo Adding mgmt user: $MGMT_USER
    $INFINISPAN_SERVER_HOME/bin/add-user.sh -u $MGMT_USER -p $MGMT_USER_PASSWORD
fi

if [ $APP_USER ] && [ $APP_USER_PASSWORD ]; then
    echo Adding app user: $APP_USER
    if [ -z $APP_USER_GROUPS ]; then
        $INFINISPAN_SERVER_HOME/bin/add-user.sh -a --user $APP_USER --password $APP_USER_PASSWORD
    else
        $INFINISPAN_SERVER_HOME/bin/add-user.sh -a --user $APP_USER --password $APP_USER_PASSWORD --group $APP_USER_GROUPS
    fi
fi

exec $INFINISPAN_SERVER_HOME/bin/standalone.sh $PARAMS
exit $?
