#!/bin/bash

declare -a DELETED_FILES_PATHS
declare -a CREATED_FILES_PATHS
declare COMMIT_MESSAGE=""

get_deleted_and_created_files() {
  local deleted_files_paths
  local deleted_file_path
  local file_name
  local new_file_path

  readarray -t deleted_files_paths < <(git diff --cached --name-status --diff-filter=D | grep -E ".*testsuite/integration-arquillian/.*" | awk '{print $2}')
  if [ ${#deleted_files_paths} -eq 0  ]; then
    return
  fi

  for deleted_file_path in "${deleted_files_paths[@]}"; do
    if [ -z "$deleted_file_path" ]; then
        continue
    fi

    file_name=$(basename "$deleted_file_path")
    new_file_path=$(git diff --cached --name-status --diff-filter=A | grep -E ".*\b${file_name}\b" | awk '{print $2}')

    if [ -n "$new_file_path" ]; then
      DELETED_FILES_PATHS+=("$deleted_file_path")
      CREATED_FILES_PATHS+=("$new_file_path")
    fi
  done
}

get_test_names_string() {
  local file_names_string=""
  local -a test_names
  local file_name
  readarray -t test_names < <(git diff --cached --name-status --diff-filter=MR | grep -E ".*Test.java" | awk '{print $2}' | xargs -I {} basename {})
  for file_name in "${test_names[@]}"; do
    if [ -z "$file_names_string" ]; then
      file_names_string="$file_name"
    else
      file_names_string+=", $file_name"
    fi
  done
  echo "$file_names_string"
}

get_default_commit_message() {
  local test_names
  test_names=$(get_test_names_string)
  echo "Move $test_names to the new testsuite"
}

read_commit_message() {
  local default_commit_message
  default_commit_message=$(get_default_commit_message)
  default_commit_message+=$'\n\n'"Part of: #34494"

  echo ""
  echo "Provide a different commit message (optional)"
  echo "============================================="
  echo "$default_commit_message"
  echo "============================================="
  COMMIT_MESSAGE=""
  read -r COMMIT_MESSAGE
  if [ -n "$COMMIT_MESSAGE" ]; then
    COMMIT_MESSAGE+=$'\n'
    while IFS= read -r line; do
      if [ -z "$line" ]; then
        COMMIT_MESSAGE+=$'\n'
      else
        COMMIT_MESSAGE+="$line"$'\n'
      fi
    done
  else
    echo "Default commit message will be used"
    COMMIT_MESSAGE="$default_commit_message"
  fi
}

commit_stage() {
  echo "The following files will be commited:"
  git --no-pager diff --cached --name-status

  read_commit_message

  local confirm
  echo ""
  read -p "Commit? [y/n]: " -r confirm
  if [[ -z "$confirm" || "$confirm" == [yY] || "$confirm" == [yY][eE][sS] ]]; then
    git commit --signoff -m "$COMMIT_MESSAGE"
  else
    exit 1
  fi
}

backup_staged_files() {
  local stage_backup
  stage_backup="$(git diff --cached --name-status --diff-filter=ACDMR | awk '{print $2}')"
  stage_backup+=$'\n'"$(git diff --cached --name-status --diff-filter=R | awk '{print $3}')"
  echo "$stage_backup"
}

restore_stage() {
  local stage_backup
  stage_backup="$1"

  if [ -n "$stage_backup" ]; then
    echo "$stage_backup" | xargs -I {} git add {} > /dev/null 2>&1
  fi
}

commit_test_move() {
  local stage_backup
  stage_backup=$(backup_staged_files)
  set -e
  git restore --staged .
  git restore "${DELETED_FILES_PATHS[@]}"

  for i in "${!CREATED_FILES_PATHS[@]}"; do
    mv "${CREATED_FILES_PATHS[i]}" "${CREATED_FILES_PATHS[i]}.new"
    mv "${DELETED_FILES_PATHS[i]}" "${CREATED_FILES_PATHS[i]}"
  done

  git add "${DELETED_FILES_PATHS[@]}" "${CREATED_FILES_PATHS[@]}"
  git commit --signoff -m "Moving files to the new test suite"

  rm "${CREATED_FILES_PATHS[@]}"
  for NEW_TEST_PATH in "${CREATED_FILES_PATHS[@]}"; do
    mv "$NEW_TEST_PATH.new" "$NEW_TEST_PATH"
  done
  set +e
  restore_stage "$stage_backup"
  echo "Move commit created"
}


if [ -z "$(git diff --cached)" ]; then
  echo "Git stage is empty. Nothing to do."
  exit 0
fi
cd "$(git rev-parse --show-toplevel)"

get_deleted_and_created_files

if [ ${#DELETED_FILES_PATHS[@]} -eq 0 ]; then
  echo "Migration can be done with a single commit."
  commit_stage
else
  echo "Git registers some files as deleted and created"
  commit_test_move
  git add "${CREATED_FILES_PATHS[@]}"
  echo ""
  echo "Committing stage..."
  commit_stage
fi
