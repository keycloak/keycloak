#!/bin/bash

. ./common.sh

if [ -z "$TIME_BETWEEN_FAILURES" ]; then export TIME_BETWEEN_FAILURES=60; fi
if [ -z "$FAILURE_DURATION" ]; then export FAILURE_DURATION=60; fi

echo Running DB failover loop with the following parameters:
echo NODES=$NODES
echo TIME_BETWEEN_FAILURES=$TIME_BETWEEN_FAILURES
echo FAILURE_DURATION=$FAILURE_DURATION
echo
echo Press Ctrl+C to interrupt.
echo

N=1

while :
do
    
    killNode $N

    echo Waiting $FAILURE_DURATION s before attempting to reconnect mariadb_${N}
    sleep $FAILURE_DURATION

    reconnectNode $N

    echo Waiting $TIME_BETWEEN_FAILURES s before inducing another failure.
    echo
    sleep $TIME_BETWEEN_FAILURES

    N=$((N+1))
    if [ "$N" -gt "$NODES" ]; then N=1; fi

done