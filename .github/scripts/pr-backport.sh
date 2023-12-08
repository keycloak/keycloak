#!/bin/bash -e

TARGET_REMOTE=upstream
KEYCLOAK_REPO=https://github.com/keycloak/keycloak
WORK_BRANCH=$(git rev-parse --abbrev-ref HEAD)

PR=$1
TARGET=$2

function echo_header() {
  echo ""
  echo "======================================================================="
  echo "$1"
  echo "-----------------------------------------------------------------------"
}

function error() {
  echo "======================================================================="
  echo "Error"
  echo "-----------------------------------------------------------------------"
  echo "$1"
  echo ""
  exit 1
}

if ! [ -x "$(command -v gh)" ]; then
  error "The GitHub CLI is not installed. See: https://github.com/cli/cli#installation"
fi

gh auth status

if ! [ -x "$(command -v jq)" ]; then
  error "The jq CLI is not installed. See: https://jqlang.github.io/jq/download/"
fi

if [ "$PR" == "" ] || [ "$TARGET" == "" ]; then
  error "Usage: gh-backport-pr.sh <PR NUMBER> <TARGET MAJOR.MINOR>"
fi

TARGET_BRANCH=release/$TARGET

echo_header "Fetching '$TARGET_REMOTE' remote."
git fetch $TARGET_REMOTE

PR_STATE=$(gh pr view $PR --json state 2>/dev/null | jq -r .state)

if [ "$PR_STATE" == "" ]; then
  error "PR #$PR not found. Make sure the PR exists, and that it's been merged, and your gh repo is set to keycloak/keycloak"
elif [ "$PR_STATE" != "MERGED" ]; then
  error "PR #$PR not merged yet. Only merged PRs can be backported."
fi

MERGE_COMMIT=$(gh pr view $PR --json mergeCommit | jq -r .mergeCommit.oid)

if [ "$MERGE_COMMIT" == "" ]; then
  error "Could not resolve merge commit for PR #$PR"
fi

PR_BRANCH=backport-$PR-$TARGET
PR_BODY=$(gh pr view $PR --json body | jq -r .body)

echo_header "Details"
echo "PR Body:        $PR_BODY"
echo ""
echo "PR:             $KEYCLOAK_REPO/pull/$PR"
echo "Commit:         $KEYCLOAK_REPO/commit/$MERGE_COMMIT"
echo ""
echo "PR branch:      $PR_BRANCH"
echo "Target branch:  $KEYCLOAK_REPO/tree/$TARGET_BRANCH"
echo ""
echo -n "Continue (y/n): "
read PROMPT

if [ "$PROMPT" != "y" ]; then
  exit 1
fi

echo_header "Checkout '$TARGET_REMOTE/$TARGET_BRANCH' to '$PR_BRANCH'"
git checkout $TARGET_REMOTE/$TARGET_BRANCH -B $PR_BRANCH

echo_header "Cherry-pick $MERGE_COMMIT"
git cherry-pick -x $MERGE_COMMIT

echo_header "Push '$PR_BRANCH' to 'origin' remote"
git push origin $PR_BRANCH:$PR_BRANCH --set-upstream

echo_header "Opening web browser to create pull request"
gh pr create -B $TARGET_BRANCH -f -w

echo_header "Checkout to $WORK_BRANCH branch"
git checkout $WORK_BRANCH
