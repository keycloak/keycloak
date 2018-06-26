#!/bin/bash

#$JBOSS_HOME/bin/jboss-cli.sh -c ":read-attribute(name=server-state)" | grep -q "running"

. get-ips.sh

CODE=`curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/`

if [ "$CODE" -eq "200" ]; then
	exit 0
fi

exit 1

