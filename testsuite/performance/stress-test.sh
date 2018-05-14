#!/bin/bash

BASEDIR=$(cd "$(dirname "$0")"; pwd)
cd $BASEDIR

. ./stress-test-config.sh

MVN=${MVN:-mvn}
PROVISIONING_PARAMETERS=${PROVISIONING_PARAMETERS:-}
PROVISION_COMMAND="$MVN verify -P provision,import-dump $PROVISIONING_PARAMETERS -Ddataset=$dataset"
TEARDOWN_COMMAND="$MVN verify -P teardown"

function runCommand {
    echo "  $1"
    echo 
    if ! $debug; then eval "$1"; fi
}

function runTest {

    # use specified warmUpPeriod only in the first iteration, or if provisioning is enabled
    if [[ $i == 0 || $provisioning == true ]]; then 
        warmUpParameter="-DwarmUpPeriod=$warmUpPeriod ";
    else 
        warmUpParameter="-DwarmUpPeriod=0 ";
    fi
    if [[ $sequentialUsersFrom == -1 || $provisioning == true ]]; then
        sequentialUsers=$sequentialUsersFrom
    else
        sequentialUsers=`echo "$sequentialUsersFrom * ( $i + 1 )" | bc`
    fi

    TEST_COMMAND="$MVN verify -Ptest $@ -Ddataset=$dataset $warmUpParameter -DfilterResults=true -DsequentialUsersFrom=$sequentialUsers -DusersPerSec=$usersPerSec"

    echo "ITERATION: $(( i+1 )) / $maxIterations      $ITERATION_INFO"
    echo 

    if $provisioning; then 
        runCommand "$PROVISION_COMMAND"
        if [[ $? != 0 ]]; then
            echo "Provisioning failed."
            runCommand "$TEARDOWN_COMMAND" || break
            break
        fi
        runCommand "$TEST_COMMAND"
        export testResult=$?
        runCommand "$TEARDOWN_COMMAND" || exit 1
    else
        runCommand "$TEST_COMMAND"
        export testResult=$?
    fi

    [[ $testResult != 0 ]] && echo "Test exit code: $testResult"

}



echo "Starting ${algorithm} stress test"
echo

usersPerSecTop=0

case "${algorithm}" in

    incremental)

        for (( i=0; i < $maxIterations; i++)); do

            usersPerSec=`echo "$usersPerSec0 + $i * $incrementFactor" | bc`

            runTest $@

            if [[ $testResult == 0 ]]; then 
                usersPerSecTop=$usersPerSec
            else
                echo "INFO: Last iteration failed. Stopping the loop."
                break
            fi

        done

    ;;

    bisection)

        for (( i=0; i < $maxIterations; i++)); do

            intervalSize=`echo "$highPoint - $lowPoint" | bc`
            usersPerSec=`echo "$lowPoint + $intervalSize * 0.5" | bc`
            if [[ `echo "$intervalSize < $tolerance" | bc`  == 1 ]]; then echo "INFO: intervalSize < tolerance. Stopping the loop."; break; fi
            if [[ `echo "$intervalSize < 0" | bc`           == 1 ]]; then echo "ERROR: Invalid state: lowPoint > highPoint. Stopping the loop."; exit 1; fi
            ITERATION_INFO="L: $lowPoint    H: $highPoint   intervalSize: $intervalSize   tolerance: $tolerance"

            runTest $@

            if [[ $testResult == 0 ]]; then 
                usersPerSecTop=$usersPerSec
                echo "INFO: Last iteration succeeded. Continuing with the upper half of the interval."
                lowPoint=$usersPerSec
            else
                echo "INFO: Last iteration failed. Continuing with the lower half of the interval."
                highPoint=$usersPerSec
            fi

        done

    ;;

    *) 
        echo "Algorithm '${algorithm}' not supported."
        exit 1
    ;;

esac

echo "Highest load with passing test: $usersPerSecTop users per second"
