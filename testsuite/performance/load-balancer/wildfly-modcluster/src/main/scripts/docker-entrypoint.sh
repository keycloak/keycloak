#!/bin/bash

cat $JBOSS_HOME/standalone/configuration/standalone.xml

. get-ips.sh

PARAMS="-b $PUBLIC_IP -bmanagement $PUBLIC_IP -bprivate $PRIVATE_IP $@"
echo "Server startup params: $PARAMS"

# Note: External container connectivity is always provided by eth0 -- irrespective of which is considered public/private by KC.
#       In case the container needs to be accessible on the host computer override -b $PUBLIC_IP by adding: `-b 0.0.0.0` to the docker command.

exec /opt/jboss/wildfly/bin/standalone.sh $PARAMS
exit $?
