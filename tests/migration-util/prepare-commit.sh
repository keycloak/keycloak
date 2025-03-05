#!/bin/bash

declare -a TEST_FILES
declare STAGE_BACKUP=""
declare -a DELETED_TESTS_PATHS
declare -a CREATED_TESTS_PATHS
declare DEFAULT_COMMIT_MESSAGE=""
declare COMMIT_MESSAGE=""

parse_test_files() {
  local test_file
  local test_found
  for test_file in "$@"; do
    test_found=$(git diff --cached --name-status | grep -w "$test_file")
    if [ -z "$test_found" ]; then
      echo "No such test found in stage: $test_file"
      exit 1
    fi
    TEST_FILES+=("$test_file")
  done
}

backup_staged_files() {
  STAGE_BACKUP="$(git diff --cached --name-status | grep '^[(M|A|D|R|C]' | awk '{print $2}')"
  STAGE_BACKUP+=$'\n'"$(git diff --cached --name-status | grep '^R' | awk '{print $3}')"
}

get_file_names_string() {
  local file_names_string=""

  for file_path in "$@"; do
    file_name=$(basename "$file_path")
    if [ -z "$file_names_string" ]; then
      file_names_string="$file_name"
    else
      file_names_string+=", $file_name"
    fi
  done
  echo "$file_names_string"
}

get_default_commit_message() {
  local file_names
  file_names=$(get_file_names_string "$@")
  DEFAULT_COMMIT_MESSAGE="Move $file_names to the new testsuite"
}

read_commit_message() {
  get_default_commit_message "${TEST_FILES[@]}"
  DEFAULT_COMMIT_MESSAGE+=$'\n\n'"Part of: #34494"

  echo ""
  echo "Provide a different commit message (optional)"
  echo "$DEFAULT_COMMIT_MESSAGE"
  read -r COMMIT_MESSAGE
  if [ -z "$COMMIT_MESSAGE" ]; then
    COMMIT_MESSAGE="$DEFAULT_COMMIT_MESSAGE"
  fi
}

commit_test_move() {
  set -e
  git restore --staged .
  git restore "${DELETED_TESTS_PATHS[@]}"

  for i in "${!CREATED_TESTS_PATHS[@]}"; do
    mv "${CREATED_TESTS_PATHS[i]}" "${CREATED_TESTS_PATHS[i]}.new"
    mv "${DELETED_TESTS_PATHS[i]}" "${CREATED_TESTS_PATHS[i]}"
  done

  git add "${DELETED_TESTS_PATHS[@]}" "${CREATED_TESTS_PATHS[@]}"
  get_default_commit_message "${CREATED_TESTS_PATHS[@]}"
  git commit --signoff -m "$DEFAULT_COMMIT_MESSAGE"

  rm "${CREATED_TESTS_PATHS[@]}"
  for NEW_TEST_PATH in "${CREATED_TESTS_PATHS[@]}"; do
    mv "$NEW_TEST_PATH.new" "$NEW_TEST_PATH"
  done
  set +e
}

restore_stage() {
  if [ -n "$STAGE_BACKUP" ]; then
    echo "$STAGE_BACKUP" | xargs -I {} git add {} > /dev/null 2>&1
  fi
}

commit_stage() {
  restore_stage
  read_commit_message
  echo "The following files will be commited:"
  git --no-pager diff --cached --name-status

  local confirm
  echo ""
  read -p "Commit? [y/n]: " -r confirm
  if [[ -z "$confirm" || "$confirm" == [yY] || "$confirm" == [yY][eE][sS] ]]; then
    git commit --signoff -m "$COMMIT_MESSAGE"
  else
    exit 1
  fi
}


if [ "$#" -eq 0 ]; then
  echo "No test files provided."
  exit 1
fi

parse_test_files "$@"

cd "$(git rev-parse --show-toplevel)"
backup_staged_files
# Add only the migrated test files to the git stage
git restore --staged .
for test_file in "${TEST_FILES[@]}"; do
  git status --porcelain --untracked-files=all | grep -E "(testsuite/|tests/).*\b${test_file%.*}(\.java)?\b" | awk '{print $2}' | xargs -I {} git add {}
done

# Store tests marked as deleted
for test_file in "${TEST_FILES[@]}"; do
  old_test_path=$(git diff --cached --name-status | grep -E "^D.*$test_file" | awk '{print $2}')
  if [ -n "$old_test_path" ]; then
    DELETED_TESTS_PATHS+=("$old_test_path")
  fi
done

if [ ${#DELETED_TESTS_PATHS[@]} -eq 0 ]; then
  echo "Tests can be migrated with a single commit."
  commit_stage
else
  echo "Git registers some tests as deleted and created"

  # Store the matching tests marked as created
  for test_file in "${DELETED_TESTS_PATHS[@]}"; do
    test_name=$(basename "$test_file")
    new_test_path=$(git diff --cached --name-status | grep -E "^A.*$test_name" | awk '{print $2}')
    if [ -n "$new_test_path" ]; then
      CREATED_TESTS_PATHS+=("$new_test_path")
    fi
  done

  if [ ${#DELETED_TESTS_PATHS[@]} -ne ${#CREATED_TESTS_PATHS[@]} ]; then
    echo "The number of deleted tests must equal to the number of created tests"
    exit 1
  fi

  commit_test_move
  git add "${CREATED_TESTS_PATHS[@]}"
  commit_stage
fi
