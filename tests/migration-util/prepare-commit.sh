#!/bin/bash

backup_staged_files() {
  STAGE_BACKUP="$(git status --porcelain | grep '^[(M|A|D|R|C]' | awk '{print $2}')"
  STAGE_BACKUP+=$'\n'"$(git status --porcelain | grep '^R' | awk -F ' -> ' '{print $2}')"
}

restore_stage() {
  if [ -n "$STAGE_BACKUP" ]; then
    echo "$STAGE_BACKUP" | xargs -I {} git add {} > /dev/null 2>&1
  fi
}

read_commit_message() {
  TEST_NAME=$(basename "$OLD_TEST_PATH")
  echo ""
  echo "Provide a different commit message (optional)"
  echo "Default message: Move $TEST_NAME to the new testsuite"
  read -r COMMIT_MESSAGE
  if [ -z "$COMMIT_MESSAGE" ]; then
    COMMIT_MESSAGE="Move $TEST_NAME to the new testsuite"
  fi
}

commit_test_move() {
  set -e
  git restore --staged .
  git restore "$OLD_TEST_PATH"
  mv "$NEW_TEST_PATH" "$NEW_TEST_PATH.new"
  mv "$OLD_TEST_PATH" "$NEW_TEST_PATH"
  git add "$OLD_TEST_PATH" "$NEW_TEST_PATH"
  git commit --signoff -m "$COMMIT_MESSAGE"
  rm "$NEW_TEST_PATH"
  mv "$NEW_TEST_PATH.new" "$NEW_TEST_PATH"
  set +e
}

TEST_FILE=$1
if [ -z "$1" ]; then
  echo "No test file provided."
fi
echo "Checking git status for $TEST_FILE"
TEST_FOUND=$(git status | grep -w "$TEST_FILE")
if [ -z "$TEST_FOUND" ]; then
  echo "No such test found: $TEST_FILE"
  exit 1
fi

cd "$(git rev-parse --show-toplevel)"
STAGE_BACKUP=""
backup_staged_files
# Add only the migrated test files to the git stage
git restore --staged .
git status --porcelain | grep -E "(testsuite/|tests/).*$TEST_FILE$" | awk '{print $2}' | xargs -I {} git add {}

OLD_TEST_PATH=$(git status | grep "deleted:.*$TEST_FILE" | sed 's/^[ \t]*deleted:[ \t]*//')
if [ -z "$OLD_TEST_PATH" ]; then
  echo "Git registers the test as modified."
  echo "Test can be migrated with a single commit."

  git restore --staged .
  restore_stage
else
  echo "Git registers the test as deleted and created"
  COMMIT_MESSAGE=""
  read_commit_message
  NEW_TEST_PATH=$(git status | grep "new file:.*$TEST_FILE" | sed 's/^[ \t]*new file:[ \t]*//')
  commit_test_move
  restore_stage
  git add "$NEW_TEST_PATH"
fi
