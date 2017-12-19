#!/bin/bash

. get-ips.sh

CODE=`curl -s -o /dev/null -w "%{http_code}" http://$PUBLIC_IP:9990/console/index.html`

if [ "$CODE" -eq "200" ]; then
	exit 0
fi

exit 1

