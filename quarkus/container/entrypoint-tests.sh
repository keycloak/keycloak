#!/bin/bash

# Tester for docker-entrypoint.sh.
# Runs a number of test cases and returns an error if any fails.
#
# Each case is run in a separate sub-environment so they are not destroyed by
# any variables in the parent shell. (env -i)
#
# How to run: ./entrypoint-tests.sh
# No extra requirements.

# Prepare test files
VALID_FILE=$(pwd)/kcdbpw-valid
echo "pw" > $VALID_FILE
EMPTY_FILE=$(pwd)/kcdbpw-empty
rm -f $EMPTY_FILE && touch $EMPTY_FILE
MISSING_FILE=$(pwd)/kcdbpw-missing
UNREADABLE_FILE=$(pwd)/kcdbpw-unreadable
rm -f $UNREADABLE_FILE && touch $UNREADABLE_FILE && chmod -r $UNREADABLE_FILE

FAILING_CASES=(
    # File specified but missing
    "KC_DB_PASSWORD_FILE=$MISSING_FILE"

    # Variable present but empty
    "KC_DB_PASSWORD_FILE= " 
    
    # Both a valid file and the plain value passed
    "KC_DB_PASSWORD_FILE=$VALID_FILE KC_DB_PASSWORD=123"

    # Pass an existing directory as a file
    "KC_DB_PASSWORD_FILE=$(pwd)"

    # Pass missing directory as a file
    "KC_DB_PASSWORD_FILE=$(pwd)/nonexistingforsure"

    # Pass unreadable file
    "KC_DB_PASSWORD_FILE=$UNREADABLE_FILE"
)
OK_CASES=(
    # No parameters, should pass ok
    ""

    # Empty file, should warn but pass
    "KC_DB_PASSWORD_FILE=$EMPTY_FILE"

    # File specified and exists, should pass
    "KC_DB_PASSWORD_FILE=$VALID_FILE"
)

PASSED=0
FAILED=0

# Output coloring
RED=$'\e[0;31m'
NC=$'\e[0m'

for case in "${FAILING_CASES[@]}"; do
    output=$(env -i $case KC_DOCKER_ENTRYPOINT_TEST=1 ./docker-entrypoint.sh)
    code=$?
    if [[ $code == 0 ]]; then
        echo "${RED}TEST FAILED${NC}: Expected return > 0 (got $code) for case: '$case'"
        echo "Entrypoint output:
$output
"
        ((FAILED++))
    else
        ((PASSED++))
    fi
done

for case in "${OK_CASES[@]}"; do
    output=$(env -i $case KC_DOCKER_ENTRYPOINT_TEST=1 ./docker-entrypoint.sh)
    code=$?

    if [[ $code > 0 ]]; then
        echo "${RED}TEST FAILED${NC}: Unexpected return > 0 ($code) for case: '$case'"
        echo "Entrypoint output:
$output"
        ((FAILED++))
    else
        ((PASSED++))
    fi
done

# Clean up
rm -f $VALID_FILE $EMPTY_FILE $UNREADABLE_FILE


if [[ $FAILED > 0 ]]; then
    echo "${RED}Tests executed, passed: $PASSED, failed: $FAILED${NC}"
    exit 1
else
    echo "Tests executed, passed: $PASSED, failed: $FAILED"
    exit 0
fi

