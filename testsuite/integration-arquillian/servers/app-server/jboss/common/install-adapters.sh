#!/bin/bash
echo "JBOSS_HOME=$JBOSS_HOME"

if [ ! -d "$JBOSS_HOME/bin" ] ; then
    >&2 echo "JBOSS_HOME/bin doesn't exist"
    exit 1
fi

cd $JBOSS_HOME/bin

./standalone.sh &
sleep 3

TIMEOUT=10
DELAY=1
T=0

RESULT=0

until [ $T -gt $TIMEOUT ]
do
    if ./jboss-cli.sh -c --command=":read-attribute(name=server-state)" | grep -q "running" ; then
        echo "Server is running. Installing adapter."

        ./jboss-cli.sh -c --file="adapter-install.cli"

        if [ "$ELYTRON_SUPPORTED" = true ]; then
            ./jboss-cli.sh -c --file="adapter-elytron-install.cli"
        fi

        if [ $? -ne 0 ]; then RESULT=1; fi

        if [ "$SAML_SUPPORTED" = true ]; then
            ./jboss-cli.sh -c --file="adapter-install-saml.cli"

            if [ "$ELYTRON_SUPPORTED" = true ]; then
                ./jboss-cli.sh -c --file="adapter-elytron-install-saml.cli"
            fi

            if [ $? -ne 0 ]; then RESULT=1; fi
        fi

        ./jboss-cli.sh -c --command=":shutdown"
        rm -rf $JBOSS_HOME/standalone/data
        rm -rf $JBOSS_HOME/standalone/log

        exit $RESULT
    fi
    echo "Server is not running."
    sleep $DELAY
    let T=$T+$DELAY
done

exit 1
