#!/bin/bash

# Entrypoint for the main image.

# -e exits the script on command failure
# -u exits on unset variables
# -o pipefail sets pipeline exit code to rightmost non-zero
set -euo pipefail

# Enable test mode by setting KC_DOCKER_ENTRYPOINT_TEST=1
# This 
ENTRYPOINT_TEST=${KC_DOCKER_ENTRYPOINT_TEST:-0}
unset KC_DOCKER_ENTRYPOINT_TEST

# Script name printed to make it obvious this is the actual entrypoint script
SCRIPT_NAME=$0
function write_output (){
    echo "$SCRIPT_NAME $1"
}

# Basic support for passing the db password or any other KC_ variable as a mounted file.
# Looks up environment variables like KC_*_FILE, reads the specified file and exports
# the content to KC_*
# e.g. KC_DB_PASSWORD_FILE -> KC_DB_PASSWORD
# 
# If a file is variable is defined but the file not found, a misconfiguration is very likely and the script exits.
# If both the _FILE and the base variable are set, the script exits.

# Find suitable variables
# Ignore non-zero exit from grep if nothing matches
vars=($(set | grep -o KC_.*_FILE || true))

# Enumerate variable names
for varName in ${vars[@]}; do
    # Output variable, trim the _FILE suffix
    # e.g. KC_DB_PASSWORD_FILE -> KC_DB_PASSWORD
    outVarName="${varName%_FILE}"
    
    filePath=${!varName:-}
    outVarValue=${!outVarName:-}

    if [[ $ENTRYPOINT_TEST == 1 ]]; then
        write_output "TRC: Processing $varName -> $outVarName"
    fi

    if [[ $filePath && $outVarValue ]]; then
        write_output "ERR: Both $varName and $outVarName are set (but are exclusive)"
        exit 1
    fi

    # File specified but not found. Very likely a misconfiguration.
    if [[ ! -f $filePath ]]; then
        write_output "ERR: $varName -> file '$filePath' not found"
        exit 1
    fi
    if [[ ! -r $filePath ]]; then
        write_output "ERR: $varName -> file '$filePath' unreadable"
        exit 1
    fi

    # Read contents
    content=$(< $filePath)
    # Export contents if non-empty
    if [[ -n content ]]; then
        export $outVarName=$content
        write_output "INF: exported $outVarName from $varName"
    # Empty contents, warn but don't fail
    else
        write_output "WRN: $varName -> '$filePath' is empty"
    fi


    # Unset the _FILE variable, no longer needed
    unset "$varName"
done

if [[ $ENTRYPOINT_TEST == 1 ]]; then
    # Print any KC_ variables we have. Ignore non-zero exit from grep if none.
    env | grep KC_.*= || true
    write_output "Would execute" /opt/keycloak/bin/kc.sh start "$@"
    write_output "Entrypoint test complete, exiting successfully"
    exit 0
fi

# Pass all command parameters to actual startup script
exec /opt/keycloak/bin/kc.sh start "$@"
