#!/bin/bash -e

REPOSITORY="$1"
REF="$2"

CHANGE_ID=$(echo $REF | cut -f 3 -d '/')

if [ $GITHUB_EVENT_NAME == "pull_request" ]; then
  echo "========================================================================================"
  echo "Checking testsuite module additions/changes."
  echo "----------------------------------------------------------------------------------------"

  ADDED_FILES=$(gh api -X GET --paginate repos/$REPOSITORY/pulls/$CHANGE_ID/files --jq 'map(select(.filename | startswith("testsuite/")) | select (.status | contains("added")) | {filename}) | length')
  CHANGED_FILES=$(gh api -X GET --paginate repos/$REPOSITORY/pulls/$CHANGE_ID/files --jq 'map(select(.filename | startswith("testsuite/")) | select (.additions >= 100) | {filename}) | length')

  # Check if changed files matches regex
  if [[ $ADDED_FILES > 0 || $CHANGED_FILES > 0 ]]  ; then
    echo "========================================================================================"
    echo "Deprecated testsuite module: "
    echo " * Adding new file(s) is forbidden."
    echo " * Maximum 50 lines can be added to a single file."
    echo ""
    echo "Please, migrate the added/changed file(s) and use the new test framework instead."
    echo "See: https://github.com/keycloak/keycloak/tree/main/testsuite/DEPRECATED.md for more details."
    echo "----------------------------------------------------------------------------------------"
    exit 1
  fi
fi
