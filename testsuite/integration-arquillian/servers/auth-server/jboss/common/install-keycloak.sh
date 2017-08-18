#!/bin/bash
echo "JBOSS_HOME=$JBOSS_HOME"

if [ ! -d "$JBOSS_HOME/bin" ] ; then
    >&2 echo "JBOSS_HOME/bin doesn't exist"
    exit 1
fi

cd $JBOSS_HOME/bin

RESULT=0
./jboss-cli.sh --file=keycloak-install.cli
if [ $? -ne 0 ]; then exit 1; fi
./jboss-cli.sh --file=keycloak-install-ha.cli
if [ $? -ne 0 ]; then exit 1; fi

exit 0
