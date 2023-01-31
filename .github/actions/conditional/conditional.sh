#!/bin/bash -e

REMOTE=$1
BASE_REF=$2

if [ "$BASE_REF" != "" ]; then
  # Fetch ref if running in GitHub Actions
  if [ "$GITHUB_OUTPUT" != "" ]; then
    echo "========================================================================================"
    echo "Fetching '$BASE_REF' in '`git remote get-url $REMOTE`'"
    echo "--------------------------------------------------------------------------------"
    git fetch --depth 1 $REMOTE $BASE_REF
  fi

  echo "========================================================================================"
  echo "Changes compared to '$BASE_REF' in '`git remote get-url $REMOTE`'"
  echo "----------------------------------------------------------------------------------------"
  CHANGES=`git diff $REMOTE/$BASE_REF --name-only`
  echo "$CHANGES"
fi

echo "========================================================================================"
if [ "$BASE_REF" != "" ]; then
  echo "Patterns"
  echo "----------------------------------------------------------------------------------------"
else
  echo "Not a pull request, marking everything as changed"
fi

declare -A CHANGED
readarray -t CONDITIONS <<< `cat ".github/actions/conditional/conditions" | grep -v '^[ ]*#' | grep -v '^[ ]*$'`

for CONDITION in "${CONDITIONS[@]}"; do
  read -a SPLIT <<< "$CONDITION"

  if [ "$BASE_REF" != "" ]; then
    PATTERN=`echo "${SPLIT[0]}" | sed 's|\.|[.]|g' | sed 's|/$|/.*|g' | sed 's|^*|.*|g'`
    echo "$CHANGES" | grep -q "^$PATTERN$" && MATCH=true || MATCH=false
    if [ "$MATCH" == true ]; then
      echo "*  $PATTERN"
    else
      echo "   $PATTERN"
    fi
  else
    MATCH=true
  fi

  for ((i = 1; i < ${#SPLIT[@]}; i++)); do
    KEY=${SPLIT[$i]}
    if [ "${CHANGED[$KEY]}" != true ]; then
      CHANGED[$KEY]=$MATCH
    fi
  done

done

echo "========================================================================================"
echo "Running workflows/jobs"
echo "----------------------------------------------------------------------------------------"

for KEY in "${!CHANGED[@]}"
do
  echo "$KEY=${CHANGED[$KEY]}"
  if [ "$GITHUB_OUTPUT" != "" ]; then
      echo "$KEY=${CHANGED[$KEY]}" >> $GITHUB_OUTPUT
  fi
done
