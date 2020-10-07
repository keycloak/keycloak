#!/bin/bash

# Execution Parameters
MVN="${MVN:-mvn}"
KEYCLOAK_PROJECT_HOME=${KEYCLOAK_PROJECT_HOME:-$(cd "$(dirname "$0")/../.."; pwd)}
DRY_RUN=${DRY_RUN:-false}

# Performance Testsuite Parameters
DATASET=${DATASET:-1r_10c_100u}
WARMUP_PERIOD=${WARMUP_PERIOD:-120}
RAMPUP_PERIOD=${RAMPUP_PERIOD:-60}
MEASUREMENT_PERIOD=${MEASUREMENT_PERIOD:-120}
FILTER_RESULTS=${FILTER_RESULTS:-true}

# Stress Test Parameters
STRESS_TEST_ALGORITHM=${STRESS_TEST_ALGORITHM:-incremental}
STRESS_TEST_MAX_ITERATIONS=${STRESS_TEST_MAX_ITERATIONS:-10}
STRESS_TEST_PROVISIONING=${STRESS_TEST_PROVISIONING:-false}
STRESS_TEST_PROVISIONING_GENERATE_DATASET=${STRESS_TEST_PROVISIONING_GENERATE_DATASET:-false}
STRESS_TEST_PROVISIONING_PARAMETERS=${STRESS_TEST_PROVISIONING_PARAMETERS:-}

# Stress Test - Incremental Algorithm Parameters
STRESS_TEST_UPS_FIRST=${STRESS_TEST_UPS_FIRST:-1.000}
STRESS_TEST_UPS_INCREMENT=${STRESS_TEST_UPS_INCREMENT:-1.000}

# Stress Test - Bisection Algorithm Parameters
lower_bound=${STRESS_TEST_UPS_LOWER_BOUND:-0.000}
upper_bound=${STRESS_TEST_UPS_UPPER_BOUND:-10.000}
STRESS_TEST_UPS_RESOLUTION=${STRESS_TEST_UPS_RESOLUTION:-1.000}

if $STRESS_TEST_PROVISIONING_GENERATE_DATASET; then DATASET_PROFILE="generate-data"; else DATASET_PROFILE="import-dump"; fi
PROVISIONING_COMMAND="$MVN -f $KEYCLOAK_PROJECT_HOME/testsuite/performance/pom.xml verify -P provision,$DATASET_PROFILE $STRESS_TEST_PROVISIONING_PARAMETERS -Ddataset=$DATASET"
TEARDOWN_COMMAND="$MVN -f $KEYCLOAK_PROJECT_HOME/testsuite/performance/pom.xml verify -P teardown"

function run_command {
    echo "  $1"
    echo 
    if ! $DRY_RUN; then eval "$1"; fi
}

function run_test {

    if [[ $i == 0 || $STRESS_TEST_PROVISIONING == true ]]; then # use specified WARMUP_PERIOD only in the first iteration, or if STRESS_TEST_PROVISIONING is enabled
        WARMUP_PARAMETER="-DwarmUpPeriod=$WARMUP_PERIOD ";
    else 
        WARMUP_PARAMETER="-DwarmUpPeriod=0 ";
    fi

    test_command="$MVN -f $KEYCLOAK_PROJECT_HOME/testsuite/performance/tests/pom.xml verify -Ptest $@ -Ddataset=$DATASET $WARMUP_PARAMETER -DrampUpPeriod=$RAMPUP_PERIOD -DmeasurementPeriod=$MEASUREMENT_PERIOD -DfilterResults=$FILTER_RESULTS -DusersPerSec=$users_per_sec"

    if $STRESS_TEST_PROVISIONING; then 
        run_command "$PROVISIONING_COMMAND"
        if [[ $? != 0 ]]; then
            echo "Provisioning failed."
            run_command "$TEARDOWN_COMMAND" || break
            break
        fi
        run_command "$test_command"
        export test_result=$?
        run_command "$TEARDOWN_COMMAND" || exit 1
    else
        run_command "$test_command"
        export test_result=$?
    fi

    [[ $test_result != 0 ]] && echo "Test exit code: $test_result"

}

cat <<EOM
Stress Test Summary:

Script Execution Parameters:
  MVN: $MVN
  KEYCLOAK_PROJECT_HOME: $KEYCLOAK_PROJECT_HOME
  DRY_RUN: $DRY_RUN

Performance Testsuite Parameters:
  DATASET: $DATASET
  WARMUP_PERIOD: $WARMUP_PERIOD seconds
  RAMPUP_PERIOD: $RAMPUP_PERIOD seconds
  MEASUREMENT_PERIOD: $MEASUREMENT_PERIOD seconds
  FILTER_RESULTS: $FILTER_RESULTS

Stress Test Parameters:
  STRESS_TEST_ALGORITHM: $STRESS_TEST_ALGORITHM
  STRESS_TEST_MAX_ITERATIONS: $STRESS_TEST_MAX_ITERATIONS
  STRESS_TEST_PROVISIONING: $STRESS_TEST_PROVISIONING
EOM
if $STRESS_TEST_PROVISIONING; then cat <<EOM
  STRESS_TEST_PROVISIONING_GENERATE_DATASET: $STRESS_TEST_PROVISIONING_GENERATE_DATASET (MVN -P $DATASET_PROFILE)
  STRESS_TEST_PROVISIONING_PARAMETERS: $STRESS_TEST_PROVISIONING_PARAMETERS
EOM
fi

users_per_sec_max=0

case "${STRESS_TEST_ALGORITHM}" in

    incremental)

        cat <<EOM
  
Incremental Stress Test Parameters:
  STRESS_TEST_UPS_FIRST: $STRESS_TEST_UPS_FIRST users per second
  STRESS_TEST_UPS_INCREMENT: $STRESS_TEST_UPS_INCREMENT users per second
EOM

        for (( i=0; i < $STRESS_TEST_MAX_ITERATIONS; i++)); do

            users_per_sec=`bc <<<"scale=10; $STRESS_TEST_UPS_FIRST + $i * $STRESS_TEST_UPS_INCREMENT"`

            echo
            echo "STRESS TEST ITERATION: $(( i+1 )) / $STRESS_TEST_MAX_ITERATIONS    Load: $users_per_sec users per second"
            echo 

            run_test $@

            if [[ $test_result == 0 ]]; then 
                users_per_sec_max=$users_per_sec
            else
                echo "INFO: Last iteration failed. Stopping the loop."
                break
            fi

        done

    ;;

    bisection)

        cat <<EOM
  
Bisection Stress Test Parameters:
  STRESS_TEST_UPS_LOWER_BOUND: $lower_bound users per second
  STRESS_TEST_UPS_UPPER_BOUND: $upper_bound users per second
  STRESS_TEST_UPS_RESOLUTION: $STRESS_TEST_UPS_RESOLUTION users per second
EOM

        for (( i=0; i < $STRESS_TEST_MAX_ITERATIONS; i++)); do

            interval_size=`bc<<<"scale=10; $upper_bound - $lower_bound"`
            users_per_sec=`bc<<<"scale=10; $lower_bound + $interval_size * 0.5"`

            echo
            echo "STRESS TEST ITERATION: $(( i+1 )) / $STRESS_TEST_MAX_ITERATIONS    Bisection interval: [$lower_bound, $upper_bound], interval_size: $interval_size, STRESS_TEST_UPS_RESOLUTION: $STRESS_TEST_UPS_RESOLUTION,   Load: $users_per_sec users per second"
            echo 

            if [[ `bc<<<"scale=10; $interval_size < $STRESS_TEST_UPS_RESOLUTION"` == 1 ]]; then echo "INFO: interval_size < STRESS_TEST_UPS_RESOLUTION. Stopping the loop."; break; fi
            if [[ `bc<<<"scale=10; $interval_size < 0"` == 1 ]]; then echo "ERROR: Invalid state: lower_bound > upper_bound. Stopping the loop."; exit 1; fi

            run_test $@

            if [[ $test_result == 0 ]]; then 
                users_per_sec_max=$users_per_sec
                echo "INFO: Last iteration succeeded. Continuing with the upper half of the interval."
                lower_bound=$users_per_sec
            else
                echo "INFO: Last iteration failed. Continuing with the lower half of the interval."
                upper_bound=$users_per_sec
            fi

        done

    ;;

    *) 
        echo "Algorithm '${STRESS_TEST_ALGORITHM}' not supported."
        exit 1
    ;;

esac

echo "Maximal load with passing test: $users_per_sec_max users per second"

if ! $DRY_RUN; then # Generate a Jenkins Plot Plugin-compatible data file
    mkdir -p "$KEYCLOAK_PROJECT_HOME/testsuite/performance/tests/target"
    echo "YVALUE=$users_per_sec_max" > "$KEYCLOAK_PROJECT_HOME/testsuite/performance/tests/target/stress-test-result.properties"
fi
