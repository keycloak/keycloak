#!/bin/bash -e
# Use this script to test different variants of a PR body.

source ./pr-find-issues.sh

function testParsing() {
    echo -n "$1 -> $2 "
    if [ "$(parse_issues "$1")" != "$2" ]; then
        echo "(failure)"
        return 1
    fi
    echo "(success)"
    return 0
}

function testFailed() {
    echo "Test Failed!"
}

trap 'testFailed' ERR

testParsing "Closes #123" "123"
testParsing "Fixes #123" "123"
testParsing "Fixes: #123" "123"
testParsing "Fixes https://github.com/keycloak/keycloak/issues/123" "123"