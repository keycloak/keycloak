#!/bin/bash

# This script:
# - assumes there are 2 network interfaces connected to $PUBLIC_SUBNET and $PRIVATE_SUBNET respectively
# - finds IP addresses from those interfaces using `ip` command
# - exports the IPs as variables

function getIpForSubnet {
    if [ -z $1 ]; then
        echo Subnet parameter undefined
        exit 1
    else
        ip r | grep $1 | sed 's/.*src\ \([^\ ]*\).*/\1/'
    fi
}

if [ -z $PUBLIC_SUBNET ]; then 
    PUBLIC_IP=0.0.0.0
    echo PUBLIC_SUBNET undefined. PUBLIC_IP defaults to $PUBLIC_IP
else
    export PUBLIC_IP=`getIpForSubnet $PUBLIC_SUBNET`
fi

if [ -z $PRIVATE_SUBNET ]; then
    PRIVATE_IP=127.0.0.1
    echo PRIVATE_SUBNET undefined. PRIVATE_IP defaults to $PRIVATE_IP
else
    export PRIVATE_IP=`getIpForSubnet $PRIVATE_SUBNET`
fi

echo Routing table:
ip r
echo PUBLIC_SUBNET=$PUBLIC_SUBNET
echo PRIVATE_SUBNET=$PRIVATE_SUBNET
echo PUBLIC_IP=$PUBLIC_IP
echo PRIVATE_IP=$PRIVATE_IP

