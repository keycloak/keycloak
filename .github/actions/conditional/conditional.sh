#!/bin/bash -e

REMOTE="$1"
BASE_REF="$2"

CONDITIONS_FILE=".github/actions/conditional/conditions"

[ "$BASE_REF" != "" ] && IS_PR=true || IS_PR=false
[ "$GITHUB_OUTPUT" != "" ] && IS_GITHUB_ACTIONS=true || IS_GITHUB_ACTIONS=false

if [ "$IS_PR" == true ]; then
  # Fetch remote if running on GitHub Actions
  if [ "$IS_GITHUB_ACTIONS" == true ]; then
    echo "========================================================================================"
    echo "Fetching '$BASE_REF' in '$(git remote get-url "$REMOTE")'"
    echo "--------------------------------------------------------------------------------"
    git fetch --depth 1 "$REMOTE" "$BASE_REF"
  fi

  # Get list of changes files
  echo "========================================================================================"
  echo "Changes compared to '$BASE_REF' in '$(git remote get-url "$REMOTE")'"
  echo "----------------------------------------------------------------------------------------"
  CHANGED_FILES=$(git diff "$REMOTE/$BASE_REF" --name-only)
  echo "$CHANGED_FILES"
fi

echo "========================================================================================"
if [ "$IS_PR" == true ]; then
  echo "Matching regex"
  echo "----------------------------------------------------------------------------------------"
else
  echo "Not a pull request, marking everything as changed"
fi

declare -A JOB_CONDITIONS

readarray -t CONDITIONS <<< "$(cat "$CONDITIONS_FILE" | grep -v '^[ ]*#' | grep -v '^[ ]*$')"

for C in "${CONDITIONS[@]}"; do
  read -r -a CONDITION <<< "$C"

  if [ "$IS_PR" == true ]; then
    PATTERN="${CONDITION[0]}"

    # Convert pattern to regex
    REGEX="$PATTERN"
    #REGEX=$(echo "$PATTERN" | sed 's|\.|\\.|g' | sed 's|/$|/.*|g' | sed 's|^*|.*|g')

    # Escape '/' characters
    REGEX=$(echo "$REGEX" | sed 's|\/|\\/|g')

    # Escape '.' to make it match the '.' character only
    REGEX=$(echo "$REGEX" | sed 's|\.|\\.|g')

    # Convert '*' to match anything
    REGEX=$(echo "$REGEX" | sed 's|\*|.*|g')

    # If ends with directory seperator, allow anything within
    REGEX=$(echo "$REGEX" | sed 's|/$|/.*|g')

    # If no directory separators allow any directory structure before
    if ( echo "$REGEX" | grep -v -E '\/' &>/dev/null ); then
      REGEX="(.*\/)?$REGEX"
    fi

    # Check if changed files matches regex
    if ( echo "$CHANGED_FILES" | grep -q -E "^$REGEX$"); then
      RUN_JOB=true
      echo "*  $REGEX"
    else
      RUN_JOB=false
      echo "   $REGEX"
    fi
  else
    # Always run job if not a PR
    RUN_JOB=true
  fi

  # Set what jobs should run for the regex
  for ((i = 1; i < ${#CONDITION[@]}; i++)); do
    JOB=${CONDITION[$i]}

    # If already set to run, ignore
    if [ "${JOB_CONDITIONS[$JOB]}" != true ]; then
      JOB_CONDITIONS[$JOB]=$RUN_JOB
    fi
  done
done

echo "========================================================================================"
echo "Run workflows/jobs"
echo "----------------------------------------------------------------------------------------"

# List all jobs and if they should run or not
for JOB in "${!JOB_CONDITIONS[@]}"
do
  echo "$JOB=${JOB_CONDITIONS[$JOB]}"

  # Set output for GitHub job
  if [ "$IS_GITHUB_ACTIONS" == true ]; then
      echo "$JOB=${JOB_CONDITIONS[$JOB]}" >> $GITHUB_OUTPUT
  fi
done
