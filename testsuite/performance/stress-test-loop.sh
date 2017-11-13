#!/bin/bash

if [ -z $RUN_USERS_INITIAL ]; then RUN_USERS_INITIAL=60; fi
if [ -z $RUN_USERS_INCREMENT ]; then RUN_USERS_INCREMENT=20; fi
if [ -z $MAX_ITERATIONS ]; then MAX_ITERATIONS=10; fi

for (( i=0; i <= $MAX_ITERATIONS; i++)); do

    RUN_USERS=$(( RUN_USERS_INITIAL + RUN_USERS_INCREMENT * i ))
    TEST_COMMAND="mvn verify -Ptest $@ -DrunUsers=$RUN_USERS"

    echo
    echo
    echo "STRESS TEST:"
    echo "============"
    echo
    echo "   ITERATION:  $((i+1)) / $MAX_ITERATIONS"
    echo "   USERS:      $RUN_USERS"
    echo
    echo "   $TEST_COMMAND"
    echo
    echo

    eval "$TEST_COMMAND"

    if [ $? -ne 0 ]; then 
        echo
        echo
        echo "TEST ASSERTIONS FAILED. STOPPING THE STRESS TEST."
        echo
        echo
        exit 1; 
    fi

done
