#!/bin/bash -e

REMOTE=$1
BASE_REF=$2

if [ "$BASE_REF" != "" ]; then
  if [ "$GITHUB_OUTPUT" != "" ]; then
    echo "--------------------------------------------------------------------------------"
    echo "Fetching '$BASE_REF' in '`git remote get-url $REMOTE`'"
    echo "--------------------------------------------------------------------------------"
    git fetch --depth 1 $REMOTE $BASE_REF
  fi

  echo "--------------------------------------------------------------------------------"
  echo "Changes compared to '$BASE_REF' in '`git remote get-url $REMOTE`'"
  echo "--------------------------------------------------------------------------------"
  git diff $REMOTE/$BASE_REF --name-only
else
  echo "--------------------------------------------------------------------------------"
  echo "Not a pull request, marking everything as changed"
fi

echo "--------------------------------------------------------------------------------"
echo "Run conditions"
echo "--------------------------------------------------------------------------------"

cat .github/actions/conditional/conditions | grep '=' | grep -v '#' | while read c; do
  KEY=`echo $c | cut -d '=' -f 1`
  PATTERN=`echo $c | cut -d '=' -f 2`

  if [ "$BASE_REF" != "" ]; then
    DIFF=`echo $PATTERN | xargs git diff $REMOTE/$BASE_REF --name-only`
    if [ "$DIFF" != "" ]; then
      CHANGED=true
    else
      CHANGED=false
    fi
  else
    CHANGED=true
  fi

  echo "$KEY=$CHANGED"

  if [ "$GITHUB_OUTPUT" != "" ]; then
    echo "$KEY=$CHANGED" >> $GITHUB_OUTPUT
  fi
done

echo "--------------------------------------------------------------------------------"