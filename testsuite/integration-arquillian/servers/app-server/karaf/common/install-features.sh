#!/bin/bash
echo "JAVA_HOME=$JAVA_HOME"

./start
echo "Karaf container starting"
sleep 5

TIMEOUT=10
DELAY=1
T=0

RESULT=0

until [ $T -gt $TIMEOUT ]
do
    if ./client $CLIENT_AUTH info; then
        echo "Server is reachable."

        if "$UNINSTALL_PAX" == "true"; then
            echo "Uninstalling PAX"
            ./client $CLIENT_AUTH -f uninstall-pax.cli
            if [ $? -ne 0 ]; then RESULT=1; fi
        fi

        if "$UPDATE_CONFIG" == "true"; then
            echo "Updating Config - org.ops4j.pax.url.mvn"
            ./client $CLIENT_AUTH -f update-config.cli
            if [ $? -ne 0 ]; then 
                RESULT=1; 
            else
                ./client $CLIENT_AUTH config:list | grep org.ops4j.pax.url.mvn.
            fi
        fi

        echo "Installing features."
        ./client $CLIENT_AUTH -f install-features.cli
        if [ $? -ne 0 ]; then RESULT=1; fi

        ./stop
        rm -rf ../data/log
        rm -rf ../data/tmp

        sleep 5

        exit $RESULT
    else
        echo "Server is not reachable. Waiting."
        sleep $DELAY
        let T=$T+$DELAY
    fi
done

./stop
exit 1
