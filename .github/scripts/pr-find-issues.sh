#!/bin/bash -e

PR="$1"
REPO="$2"

if [ "$REPO" == "" ]; then
    REPO="keycloak/keycloak"
fi

function parse_issues() {
    echo "$1" | \
      grep -i -P -o "(close|closes|closed|resolve|resolves|resolved|fixes|fixed):? (#|https://github.com/keycloak/keycloak/issues/)[[:digit:]]*" | \
      sed -e 's|https://github.com/keycloak/keycloak/issues/|#|g' | \
      sed -e 's|keycloak/keycloak/issues/|#|g' | \
      cut -d '#' -f 2 | sort -n
}

if [ "$PR" != "" ]; then
    PR_JSON=$(gh api "/repos/$REPO/pulls/$PR")
    PR_BODY=$(echo "$PR_JSON" | jq -r .body)
    PR_MERGE_COMMIT_SHA=$(echo "$PR_JSON" | jq -r .merge_commit_sha)

    ISSUES=$(parse_issues "$PR_BODY")
    if [ "$ISSUES" == "" ]; then
        COMMIT_JSON=$(gh api "/repos/$REPO/commits/$PR_MERGE_COMMIT_SHA")
        COMMIT_MESSAGE=$(echo "$COMMIT_JSON" | jq -r .commit.message)

        ISSUES=$(parse_issues "$COMMIT_MESSAGE")
    fi

    for i in $ISSUES; do
        echo "$i"
    done
fi
