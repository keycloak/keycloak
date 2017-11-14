#!/bin/bash

. get-ips.sh

CODE=`curl -s -o /dev/null -w "%{http_code}" http://$PUBLIC_IP:8080/auth/realms/master`

if [ "$CODE" -eq "200" ]; then
	exit 0
fi

exit 1
